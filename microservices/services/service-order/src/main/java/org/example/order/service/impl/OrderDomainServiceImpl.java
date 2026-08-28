package org.example.order.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.address.POJO.Address;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.RefundApplyDTO;
import org.example.goods.DTO.RefundHandleDTO;
import org.example.goods.POJO.Goods;
import org.example.goods.VO.GoodsDetailVO;
import org.example.order.DTO.OrderCreateDTO;
import org.example.order.DTO.OrderQueryDTO;
import org.example.order.POJO.OrderInfo;
import org.example.order.VO.OrderDetailVO;
import org.example.order.VO.OrderVO;
import org.example.order.constant.GoodsStatusEnum;
import org.example.order.constant.OrderStatusEnum;
import org.example.order.constant.PayTypeEnum;
import org.example.order.constant.RefundStatusEnum;
import org.example.order.feign.ProductFeignClient;
import org.example.order.feign.UserFeignClient;
import org.example.order.mapper.AddressMapper;
import org.example.order.mapper.OrderMapper;
import org.example.order.service.OrderDomainService;
import org.example.order.util.RedisDistributedLock;
import org.example.user.POJO.User;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 订单域服务实现
 * 注意：不再直接访问 goods/user 数据库，改为通过 Feign 调用对应服务
 */
@Slf4j
@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    private static final String STOCK_CACHE_KEY = "goods:stock:";
    private static final String ORDER_IDEMPOTENT_KEY = "order:idempotent:";

    private final OrderMapper orderMapper;
    private final AddressMapper addressMapper;
    private final RedisDistributedLock redisDistributedLock;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;

    public OrderDomainServiceImpl(OrderMapper orderMapper,
                                  AddressMapper addressMapper,
                                  RedisDistributedLock redisDistributedLock,
                                  RedisTemplate<Object, Object> redisTemplate,
                                  ProductFeignClient productFeignClient,
                                  UserFeignClient userFeignClient) {
        this.orderMapper = orderMapper;
        this.addressMapper = addressMapper;
        this.redisDistributedLock = redisDistributedLock;
        this.redisTemplate = redisTemplate;
        this.productFeignClient = productFeignClient;
        this.userFeignClient = userFeignClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderCreateDTO orderCreateDTO) {
        if (orderCreateDTO.getGoodsNum() == null || orderCreateDTO.getGoodsNum() < 1) {
            orderCreateDTO.setGoodsNum(1);
        }
        if (orderCreateDTO.getRequestId() == null || orderCreateDTO.getRequestId().isBlank()) {
            throw new IllegalArgumentException("requestId不能为空");
        }

        // 幂等性校验
        String idempotentKey = ORDER_IDEMPOTENT_KEY + orderCreateDTO.getRequestId();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(success)) {
            throw new IllegalStateException("请勿重复提交订单");
        }

        Integer goodsId = orderCreateDTO.getGoodsId();
        String lockValue = null;
        try {
            lockValue = redisDistributedLock.acquireLock(goodsId.toString());
            if (lockValue == null) {
                throw new IllegalStateException("系统繁忙，请稍后重试");
            }

            // 通过 Feign 获取商品信息（替代原来的 goodsMapper.findById）
            Goods goods = fetchGoodsById(goodsId);
            if (goods == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            if (!Objects.equals(goods.getGoodsStatus(), GoodsStatusEnum.ON_SALE.getCode())) {
                throw new IllegalStateException("商品当前不可下单");
            }
            if (goods.getStock() < orderCreateDTO.getGoodsNum()) {
                throw new IllegalStateException("库存不足");
            }

            // 创建订单
            OrderInfo orderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderCreateDTO, orderInfo);
            LocalDateTime now = LocalDateTime.now();
            orderInfo.setOrderNo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + String.format("%06d", new Random().nextInt(999999)));
            orderInfo.setGoodsName(goods.getGoodsName());
            orderInfo.setGoodsPic(goods.getGoodsPic());
            orderInfo.setGoodsPrice(goods.getSellPrice());
            orderInfo.setTotalAmount(goods.getSellPrice().multiply(BigDecimal.valueOf(orderCreateDTO.getGoodsNum())));
            orderInfo.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
            orderInfo.setPayType(PayTypeEnum.UNPAID.getCode());
            orderInfo.setRefundStatus(RefundStatusEnum.NO_REFUND.getCode());
            orderInfo.setRefundAmount(BigDecimal.ZERO);
            orderInfo.setCreateTime(now);
            orderInfo.setUpdateTime(now);
            orderMapper.add(orderInfo);

            // 通过 Feign 调用 product 服务更新库存（替代原来的 goodsMapper.update）
            try {
                Map<String, Integer> stockParams = new HashMap<>();
                stockParams.put("id", goodsId);
                stockParams.put("stockDelta", -orderCreateDTO.getGoodsNum());
                Result<Void> stockResult = productFeignClient.updateStock(stockParams);
                if (stockResult.getCode() != 0) {
                    throw new RuntimeException("更新库存失败: " + stockResult.getMessage());
                }
            } catch (Exception e) {
                log.error("更新商品库存失败，商品ID:{}", goodsId, e);
                throw new RuntimeException("库存更新失败，订单已回滚", e);
            }

            // 更新本地 Redis 缓存
            redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goodsId,
                    Math.max(0, goods.getStock() - orderCreateDTO.getGoodsNum()),
                    1, TimeUnit.HOURS);

            return findById(orderInfo.getId()).getOrderNo();
        } catch (Exception ex) {
            redisTemplate.delete(idempotentKey);
            throw ex;
        } finally {
            if (lockValue != null) {
                redisDistributedLock.releaseLock(goodsId.toString(), lockValue);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<OrderVO> list(OrderQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<OrderVO> list = orderMapper.list(queryDTO);
        for (OrderVO order : list) {
            order.setOrderStatusName(OrderStatusEnum.getNameByCode(order.getOrderStatus()));
            order.setPayTypeName(PayTypeEnum.getNameByCode(order.getPayType()));
            order.setRefundStatusName(RefundStatusEnum.getNameByCode(order.getRefundStatus()));
            Address address = addressMapper.findById(order.getAddressId());
            if (address != null) {
                order.setAddress(address.getProvince() + address.getCity()
                        + address.getDistrict() + address.getDetailAddr());
            }
        }
        Page<OrderVO> page = (Page<OrderVO>) list;
        PageBean<OrderVO> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailVO findById(Integer id) {
        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        OrderDetailVO detailVO = new OrderDetailVO();
        BeanUtils.copyProperties(orderInfo, detailVO);

        // 买家信息通过 Feign 获取
        User buyer = fetchUserById(orderInfo.getBuyerId());
        if (buyer != null) {
            detailVO.setBuyerNickname(buyer.getNickname());
            detailVO.setBuyerPhone(buyer.getPhone());
        }

        // 卖家信息通过 Feign 获取
        User seller = fetchUserById(orderInfo.getSellerId());
        if (seller != null) {
            detailVO.setSellerNickname(seller.getNickname());
            detailVO.setSellerPhone(seller.getPhone());
        }

        Address address = addressMapper.findById(orderInfo.getAddressId());
        if (address != null) {
            detailVO.setAddress(address.getProvince() + address.getCity()
                    + address.getDistrict() + address.getDetailAddr());
        }

        detailVO.setOrderStatusName(OrderStatusEnum.getNameByCode(orderInfo.getOrderStatus()));
        detailVO.setPayTypeName(PayTypeEnum.getNameByCode(orderInfo.getPayType()));
        detailVO.setRefundStatusName(RefundStatusEnum.getNameByCode(orderInfo.getRefundStatus()));
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id) {
        LocalDateTime now = LocalDateTime.now();
        orderMapper.updateStatus(id, OrderStatusEnum.PENDING_DELIVERY.getCode(), PayTypeEnum.WECHAT.getCode(), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Integer id) {
        OrderInfo orderInfo = requireOrder(id);
        if (!Arrays.asList(OrderStatusEnum.PENDING_PAY.getCode(), OrderStatusEnum.PENDING_DELIVERY.getCode())
                .contains(orderInfo.getOrderStatus())) {
            throw new IllegalStateException("当前订单状态不支持取消");
        }
        restoreStockAndCancel(orderInfo, orderInfo.getBuyerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Integer id) {
        OrderInfo orderInfo = requireOrder(id);
        if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PENDING_RECEIVE.getCode())) {
            throw new IllegalStateException("当前订单状态不支持确认收货");
        }
        orderMapper.updateConfirmStatus(id, OrderStatusEnum.COMPLETED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(RefundApplyDTO refundApplyDTO) {
        OrderInfo orderInfo = requireOrder(refundApplyDTO.getOrderId());
        orderMapper.applyRefund(orderInfo.getId(),
                RefundStatusEnum.REFUNDING.getCode(),
                refundApplyDTO.getRefundAmount(),
                refundApplyDTO.getRefundReason(),
                LocalDateTime.now(),
                refundApplyDTO.getRefundRemark(),
                LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefund(RefundHandleDTO refundHandleDTO) {
        OrderInfo orderInfo = requireOrder(refundHandleDTO.getOrderId());
        orderMapper.handleRefund(orderInfo.getId(),
                refundHandleDTO.getHandleResult(),
                refundHandleDTO.getRefundRemark(),
                LocalDateTime.now(),
                LocalDateTime.now());
        OrderInfo latestOrder = requireOrder(orderInfo.getId());
        if (Objects.equals(refundHandleDTO.getHandleResult(), RefundStatusEnum.REFUND_SUCCESS.getCode())) {
            restoreStockAndCancel(latestOrder, latestOrder.getSellerId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Integer id) {
        orderMapper.deleteOrder(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendOrder(Integer id) {
        LocalDateTime now = LocalDateTime.now();
        orderMapper.updateSendStatus(id, OrderStatusEnum.PENDING_RECEIVE.getCode(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderVO findByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateStatus(Integer id, Integer orderStatus) {
        orderMapper.adminUpdateStatus(id, orderStatus);
    }

    @PostConstruct
    public void initStockCache() {
        try {
            Result<List<Goods>> result = productFeignClient.findAllOnSale();
            if (result.getCode() == 0 && result.getData() != null) {
                for (Goods goods : result.getData()) {
                    redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goods.getId(),
                            goods.getStock(), 24, TimeUnit.HOURS);
                }
                log.info("init stock cache finished, size={}", result.getData().size());
            }
        } catch (Exception e) {
            log.error("init stock cache failed", e);
        }
    }

    // ========== 私有辅助方法 ==========

    private void restoreStockAndCancel(OrderInfo orderInfo, Integer orderOperatorId) {
        Integer goodsId = orderInfo.getGoodsId();
        String lockValue = redisDistributedLock.acquireLock(goodsId.toString());
        if (lockValue == null) {
            throw new IllegalStateException("系统繁忙，请稍后重试");
        }
        try {
            orderMapper.updateCancelStatus(orderInfo.getId(), OrderStatusEnum.CANCELED.getCode());
            // 通过 Feign 恢复库存
            try {
                Map<String, Integer> stockParams = new HashMap<>();
                stockParams.put("id", goodsId);
                stockParams.put("stockDelta", orderInfo.getGoodsNum());
                productFeignClient.updateStock(stockParams);
            } catch (Exception e) {
                log.error("恢复库存失败，商品ID:{}", goodsId, e);
            }
            redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goodsId, 0, 1, TimeUnit.HOURS);
        } finally {
            redisDistributedLock.releaseLock(goodsId.toString(), lockValue);
        }
    }

    private OrderInfo requireOrder(Integer id) {
        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return orderInfo;
    }

    private Goods fetchGoodsById(Integer goodsId) {
        // 通过 Feign 获取商品详情
        Result<?> result = productFeignClient.getGoodsById(goodsId);
        if (result.getCode() == 0 && result.getData() != null) {
            // 从 GoodsDetailVO 中提取基础 Goods 数据
            if (result.getData() instanceof GoodsDetailVO detail) {
                Goods goods = new Goods();
                goods.setId(goodsId);
                goods.setGoodsName(detail.getGoodsName());
                goods.setSellPrice(detail.getSellPrice());
                goods.setGoodsStatus(detail.getGoodsStatus());
                goods.setStock(detail.getStock());
                goods.setGoodsPic(detail.getGoodsPic());
                return goods;
            }
        }
        return null;
    }

    private User fetchUserById(Integer userId) {
        if (userId == null) return null;
        try {
            Result<User> result = userFeignClient.getById(userId);
            return result.getCode() == 0 ? result.getData() : null;
        } catch (Exception e) {
            log.warn("获取用户信息失败 userId:{}", userId, e);
            return null;
        }
    }
}
