package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.DTO.OrderCreateDTO;
import com.itheima.DTO.OrderQueryDTO;
import com.itheima.DTO.RefundApplyDTO;
import com.itheima.DTO.RefundHandleDTO;
import com.itheima.mapper.AddressMapper;
import com.itheima.mapper.GoodsMapper;
import com.itheima.mapper.OrderMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.*;
import com.itheima.pojo.Enum.GoodsStatusEnum;
import com.itheima.pojo.Enum.OrderStatusEnum;
import com.itheima.pojo.Enum.PayTypeEnum;
import com.itheima.pojo.Enum.RefundStatusEnum;
import com.itheima.service.OrderService;
import com.itheima.util.RedisDistributedLock;
import com.itheima.vo.OrderDetailVO;
import com.itheima.vo.OrderVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private RedisDistributedLock redisDistributedLock;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 库存缓存前缀
    private static final String STOCK_CACHE_KEY = "goods:stock:";
    // 订单幂等性前缀
    private static final String ORDER_IDEMPOTENT_KEY = "order:idempotent:";

    // 1. 创建订单（单商品，扣减库存）- 高并发优化版
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderCreateDTO orderCreateDTO) {
        // ========== 1. 参数校验 ==========
        if (orderCreateDTO == null) {
            throw new IllegalArgumentException("订单参数不能为空");
        }
        if (orderCreateDTO.getGoodsId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (orderCreateDTO.getGoodsNum() == null || orderCreateDTO.getGoodsNum() < 1) {
            orderCreateDTO.setGoodsNum(1); // 二手默认购买1件
        }
        // 幂等性校验（防止重复下单）
        String requestId = orderCreateDTO.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            throw new IllegalArgumentException("请求ID不能为空（用于幂等性校验）");
        }
        String idempotentKey = ORDER_IDEMPOTENT_KEY + requestId;
        Boolean exists = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(exists)) {
            throw new RuntimeException("请勿重复提交订单");
        }

        Integer goodsId = orderCreateDTO.getGoodsId();
        Integer buyNum = orderCreateDTO.getGoodsNum();
        String lockValue = null;

        try {
            // ========== 2. 获取分布式锁 ==========
            lockValue = redisDistributedLock.acquireLock(goodsId.toString());
            if (lockValue == null) {
                throw new RuntimeException("系统繁忙，请稍后再试");
            }

            // ========== 3. 库存校验（先查缓存，再查数据库） ==========
            // 3.1 先查Redis缓存库存
            Integer cacheStock = (Integer) redisTemplate.opsForValue().get(STOCK_CACHE_KEY + goodsId);
            if (cacheStock != null && cacheStock < buyNum) {
                throw new RuntimeException("商品库存不足，当前库存：" + cacheStock);
            }

            // 3.2 查数据库商品信息（最终校验）
            Goods goods = goodsMapper.findById(goodsId);
            if (goods == null) {
                throw new RuntimeException("商品不存在：ID=" + goodsId);
            }
            if (!Objects.equals(goods.getGoodsStatus(), GoodsStatusEnum.ON_SALE.getCode())) {
                throw new RuntimeException("商品[" + goods.getGoodsName() + "]非在售状态，无法下单");
            }
            if (goods.getStock() < buyNum) {
                throw new RuntimeException("商品[" + goods.getGoodsName() + "]库存不足，当前库存：" + goods.getStock());
            }

            // ========== 4. 生成订单编号 ==========
            String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    String.format("%06d", new Random().nextInt(999999));

            // ========== 5. 构建并保存订单 ==========
            OrderInfo orderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderCreateDTO, orderInfo);
            // 商品信息冗余存储
            orderInfo.setGoodsName(goods.getGoodsName());
            orderInfo.setGoodsPic(goods.getGoodsPic());
            orderInfo.setGoodsPrice(goods.getSellPrice());
            // 计算总金额
            orderInfo.setTotalAmount(goods.getSellPrice().multiply(new BigDecimal(buyNum)));
            // 订单初始状态
            orderInfo.setOrderNo(orderNo);
            orderInfo.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
            orderInfo.setPayType(PayTypeEnum.UNPAID.getCode()); // 未支付
            orderInfo.setRefundStatus(RefundStatusEnum.NO_REFUND.getCode()); // 初始无退款
            orderInfo.setRefundAmount(BigDecimal.ZERO);
            orderInfo.setCreateTime(LocalDateTime.now());
            orderInfo.setUpdateTime(LocalDateTime.now());

            // 保存订单
            orderMapper.add(orderInfo);

            // ========== 6. 扣减库存（数据库+缓存） ==========
            // 6.1 扣减数据库库存
            int newStock = goods.getStock() - buyNum;
            goods.setStock(newStock);
            //库存为零时改为已售罄
            if (newStock == 0) {
                goods.setGoodsStatus(GoodsStatusEnum.SOLD_OUT.getCode());
            }
            goods.setUpdateTime(LocalDateTime.now());
            goodsMapper.update(goods);

            // 6.2 更新Redis缓存库存（同步更新）
            redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goodsId, newStock, 1, TimeUnit.HOURS);

            log.info("订单创建成功，订单号：{}，商品ID：{}", orderNo, goodsId);
            return orderNo;

        } catch (Exception e) {
            // 异常时删除幂等性标识，允许重试
            redisTemplate.delete(idempotentKey);
            log.error("创建订单失败", e);
            throw e;
        } finally {
            // ========== 7. 释放分布式锁 ==========
            if (lockValue != null) {
                redisDistributedLock.releaseLock(goodsId.toString(), lockValue);
            }
        }
    }

    // ========== 以下为原有方法（仅优化取消订单/退款的库存一致性） ==========

    // 2. 分页查询订单（含退款状态）- 无修改
// 2. 分页查询订单（含退款状态）- 适配 OrderVO 字段
    @Override
    @Transactional(readOnly = true)
    public PageBean<OrderVO> list(OrderQueryDTO queryDTO) {
        PageBean<OrderVO> pb = new PageBean<>();
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 1. 查询订单列表（SQL直接返回OrderVO基础字段）
        List<OrderVO> orderList = orderMapper.list(queryDTO);
        Page<OrderVO> p = (Page<OrderVO>) orderList;

        // 2. 补充枚举名称（仅填充VO已有字段，不新增）
        orderList.forEach(order -> {
            // 订单状态名称（匹配 OrderStatusEnum）
            order.setOrderStatusName(OrderStatusEnum.getNameByCode(order.getOrderStatus()));
            // 支付方式名称（匹配 PayTypeEnum）
            order.setPayTypeName(PayTypeEnum.getNameByCode(order.getPayType()));
            // 退款状态名称（匹配 RefundStatusEnum）
            order.setRefundStatusName(RefundStatusEnum.getNameByCode(order.getRefundStatus()));
            //根据地址id查询地址
            Address address = addressMapper.findById(order.getAddressId());
            String fullAddress = address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddr();
            order.setAddress(fullAddress);
        });

        // 3. 封装分页结果
        pb.setTotal(p.getTotal());
        pb.setItems(orderList);
        return pb;
    }

    // 3. 查询订单详情（含退款信息）- 无修改
// 3. 查询订单详情（含完整退款信息）- 适配 OrderDetailVO 字段
    @Override
    @Transactional(readOnly = true)
    public OrderDetailVO findById(Integer id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("订单ID不合法");
        }

        // 1. 查询订单主信息（含退款字段）
        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + id);
        }

        // 2. 转换为详情VO（仅填充VO已有字段）
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(orderInfo, vo); // 复制基础字段

        // 3. 补充关联信息（买家/卖家/地址）
        // 3.1 买家信息
        User buyer = userMapper.findById(orderInfo.getBuyerId());
        if (buyer != null) {
            vo.setBuyerNickname(buyer.getNickname());
            vo.setBuyerPhone(buyer.getPhone());
        }
        // 3.2 卖家信息
        User seller = userMapper.findById(orderInfo.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerPhone(seller.getPhone());
        }
        // 3.3 收货地址
        //根据地址id查询地址
        Address address = addressMapper.findById(orderInfo.getAddressId());
        if (address != null) {
            vo.setAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddr());
        } else {
            // 地址不存在时，显示友好提示
            if (orderInfo.getAddressId() == null) {
                vo.setAddress("无收货地址信息");
            } else {
                vo.setAddress("地址已删除或不存在");
            }
        }
        // 4. 补充枚举名称（仅填充VO已有字段）
        vo.setOrderStatusName(OrderStatusEnum.getNameByCode(orderInfo.getOrderStatus()));
        vo.setPayTypeName(PayTypeEnum.getNameByCode(orderInfo.getPayType()));
        vo.setRefundStatusName(RefundStatusEnum.getNameByCode(orderInfo.getRefundStatus()));

        return vo;
    }

    // 4. 更新订单状态支付
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("订单ID和状态不能为空");
        }

        // 校验订单是否存在
        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + id);
        }

        Integer payType =  1;
        // 更新订单状态
        orderMapper.updateStatus(id, OrderStatusEnum.PENDING_DELIVERY.getCode(), payType,LocalDateTime.now());
    }

    // 5. 取消订单（恢复库存）- 高并发优化版
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Integer id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("订单ID不合法");
        }

        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + id);
        }

        // 校验订单状态（只能取消待付款/待发货，且无退款中）
        if (!Arrays.asList(1,2).contains(orderInfo.getOrderStatus())) {
            throw new RuntimeException("当前订单状态不支持取消，状态：" + orderInfo.getOrderStatus());
        }
        if (Objects.equals(orderInfo.getRefundStatus(), RefundStatusEnum.REFUNDING.getCode())) {
            throw new RuntimeException("订单正在退款中，无法取消");
        }

        Integer goodsId = orderInfo.getGoodsId();
        Integer goodsNum = orderInfo.getGoodsNum();
        String lockValue = null;

        try {
            // 获取分布式锁，保证库存恢复的原子性
            lockValue = redisDistributedLock.acquireLock(goodsId.toString());
            if (lockValue == null) {
                throw new RuntimeException("系统繁忙，请稍后再试");
            }

            // 1. 更新订单状态为已取消
            orderMapper.updateCancelStatus(id, OrderStatusEnum.CANCELED.getCode());

            // 3. 恢复商品库存+状态
            Goods goods = goodsMapper.findById(goodsId);
            if (goods != null) {
                int newStock = goods.getStock() + goodsNum;
                goods.setStock(newStock);
                goods.setGoodsStatus(GoodsStatusEnum.ON_SALE.getCode());
                goods.setUpdateTime(LocalDateTime.now());
                goodsMapper.update(goods);

                // 同步更新Redis缓存库存
                redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goodsId, newStock, 1, TimeUnit.HOURS);

                log.info("订单取消，商品[{}]库存恢复，恢复数量：{}，当前库存：{}",
                        goods.getGoodsName(), goodsNum, newStock);
            }

            log.info("订单取消成功，订单ID：{}", id);

        } catch (Exception e) {
            log.error("取消订单失败", e);
            throw e;
        } finally {
            // 释放分布式锁
            if (lockValue != null) {
                redisDistributedLock.releaseLock(goodsId.toString(), lockValue);
            }
        }
    }

    // 6. 确认收货
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Integer id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("订单ID不合法");
        }

        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + id);
        }

        // 校验订单状态（只能确认待收货，且无退款中）
        if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PENDING_RECEIVE.getCode())) {
            throw new RuntimeException("当前订单状态不支持确认收货");
        }
        if (Objects.equals(orderInfo.getRefundStatus(), RefundStatusEnum.REFUNDING.getCode())) {
            throw new RuntimeException("订单正在退款中，无法确认收货");
        }

        // 更新订单状态为已完成
        orderMapper.updateConfirmStatus(id, OrderStatusEnum.COMPLETED.getCode());
    }

    // 7. 申请退款（核心新增）- 无修改
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(RefundApplyDTO refundApplyDTO) {
        if (refundApplyDTO == null) {
            throw new IllegalArgumentException("退款参数不能为空");
        }

        Integer orderId = refundApplyDTO.getOrderId();
        // 校验订单是否存在
        OrderInfo orderInfo = orderMapper.findById(orderId);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + orderId);
        }

        // 校验退款状态（只能对“无退款”的订单申请）
        if (!Objects.equals(orderInfo.getRefundStatus(), RefundStatusEnum.NO_REFUND.getCode())) {
            throw new RuntimeException("当前订单已申请退款，无需重复申请");
        }

        // 校验订单状态（只能对已支付、待发货、待收货、已完成的订单申请退款）
        List<Integer> allowStatus = Arrays.asList(2,3,4); // 待发货、待收货、已完成
        if (orderInfo.getPayType() == 0 || !allowStatus.contains(orderInfo.getOrderStatus())) {
            throw new RuntimeException("当前订单状态不支持申请退款");
        }

        // 校验退款金额（不能大于订单总金额）
        if (refundApplyDTO.getRefundAmount().compareTo(orderInfo.getTotalAmount()) > 0) {
            throw new RuntimeException("退款金额不能大于订单总金额");
        }

        // 申请退款：更新退款状态为“退款中”
        orderMapper.applyRefund(
                orderId,
                RefundStatusEnum.REFUNDING.getCode(),  // refundStatus
                refundApplyDTO.getRefundAmount(),      // refundAmount
                refundApplyDTO.getRefundReason(),      // refundReason
                LocalDateTime.now(),                   // refundTime
                refundApplyDTO.getRefundRemark(),      // refundRemark
                LocalDateTime.now()                    // updateTime
        );

        log.info("订单退款申请成功，订单ID：{}，退款金额：{}", orderId, refundApplyDTO.getRefundAmount());
    }

    // 8. 处理退款（商家/平台操作，核心新增）- 高并发优化版
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefund(RefundHandleDTO refundHandleDTO) {
        if (refundHandleDTO == null) {
            throw new IllegalArgumentException("退款处理参数不能为空");
        }

        Integer orderId = refundHandleDTO.getOrderId();
        Integer handleResult = refundHandleDTO.getHandleResult();
        // 校验处理结果合法性
        if (!Arrays.asList(2,3).contains(handleResult)) {
            throw new IllegalArgumentException("退款处理结果不合法（只能是2-成功/3-失败）");
        }

        // 校验订单是否存在
        OrderInfo orderInfo = orderMapper.findById(orderId);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + orderId);
        }

        // 校验退款状态（只能处理“退款中”的订单）
        if (!Objects.equals(orderInfo.getRefundStatus(), RefundStatusEnum.REFUNDING.getCode())) {
            throw new RuntimeException("当前订单非退款中状态，无法处理");
        }

        Integer goodsId = orderInfo.getGoodsId();
        Integer goodsNum = orderInfo.getGoodsNum();
        String lockValue = null;
        LocalDateTime now = LocalDateTime.now();

        try {
            // 退款成功时需要获取锁恢复库存
            if (Objects.equals(handleResult, RefundStatusEnum.REFUND_SUCCESS.getCode())) {
                lockValue = redisDistributedLock.acquireLock(goodsId.toString());
                if (lockValue == null) {
                    throw new RuntimeException("系统繁忙，请稍后再试");
                }
            }

            // 处理退款：更新退款状态、时间、备注
            orderMapper.handleRefund(
                    orderId,
                    handleResult,
                    refundHandleDTO.getRefundRemark(),
                    now,
                    now
            );

            // 若退款成功：恢复商品库存+更新订单状态为“已取消”
            if (Objects.equals(handleResult, RefundStatusEnum.REFUND_SUCCESS.getCode())) {
                // 更新订单状态为已取消
                orderMapper.updateCancelStatus(orderId, OrderStatusEnum.CANCELED.getCode());

                // 恢复商品库存+状态
                Goods goods = goodsMapper.findById(goodsId);
                if (goods != null) {
                    int newStock = goods.getStock() + goodsNum;
                    goods.setStock(newStock);
                    goods.setGoodsStatus(GoodsStatusEnum.ON_SALE.getCode());
                    goods.setUpdateTime(now);
                    goodsMapper.update(goods);

                    // 同步更新Redis缓存库存
                    redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goodsId, newStock, 1, TimeUnit.HOURS);

                    log.info("订单退款成功，商品[{}]库存恢复，恢复数量：{}，当前库存：{}",
                            goods.getGoodsName(), goodsNum, newStock);
                }
            }

            log.info("订单退款处理完成，订单ID：{}，处理结果：{}", orderId, handleResult == 2 ? "退款成功" : "退款失败");

        } catch (Exception e) {
            log.error("处理退款失败", e);
            throw e;
        } finally {
            // 释放分布式锁
            if (lockValue != null) {
                redisDistributedLock.releaseLock(goodsId.toString(), lockValue);
            }
        }
    }

    @Override
    public void deleteOrder(Integer id){
        orderMapper.deleteOrder(id);
    }

    // 9. 订单发货（核心完善）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendOrder(Integer id) {
        // 1. 参数合法性校验
        if (id == null || id < 1) {
            throw new IllegalArgumentException("订单ID不合法");
        }

        // 2. 查询订单并校验是否存在
        OrderInfo orderInfo = orderMapper.findById(id);
        if (orderInfo == null) {
            throw new RuntimeException("订单不存在：" + id);
        }

        // 3. 校验订单状态（只能对"待发货"状态的订单发货，且无退款中）
        // 待发货状态码对应 OrderStatusEnum.PENDING_DELIVERY.getCode()（假设值为2）
        if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PENDING_DELIVERY.getCode())) {
            throw new RuntimeException("当前订单状态不支持发货，状态：" + orderInfo.getOrderStatus() +
                    "（仅支持待发货状态订单）");
        }
        // 校验退款状态：退款中/已退款的订单不能发货
        if (Objects.equals(orderInfo.getRefundStatus(), RefundStatusEnum.REFUNDING.getCode()) ||
                Objects.equals(orderInfo.getRefundStatus(), RefundStatusEnum.REFUND_SUCCESS.getCode())) {
            throw new RuntimeException("订单存在退款操作，无法发货（退款状态：" + orderInfo.getRefundStatus() + "）");
        }

        try {
            // 4. 更新订单状态为"待收货"
            // 待收货状态码对应 OrderStatusEnum.PENDING_RECEIVE.getCode()
            orderMapper.updateSendStatus(id,
                    OrderStatusEnum.PENDING_RECEIVE.getCode(),
                    LocalDateTime.now()); // 更新发货状态和发货时间

            log.info("订单发货成功，订单ID：{}，订单号：{}", id, orderInfo.getOrderNo());
        } catch (Exception e) {
            log.error("订单发货失败，订单ID：{}", id, e);
            throw new RuntimeException("订单发货处理异常：" + e.getMessage());
        }
    }

    @Override
    public OrderVO findByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    @Override
    public void adminUpdateStatus(Integer id, Integer status) {
        orderMapper.adminUpdateStatus(id,status);
    }


    // ========== 新增：初始化库存缓存（项目启动时调用） ==========
    /**
     * 初始化商品库存到Redis缓存
     */
    // 可通过@PostConstruct注解在项目启动时执行
     @PostConstruct
    public void initStockCache() {
        log.info("开始初始化商品库存缓存...");
        List<Goods> goodsList = goodsMapper.findAllOnSale(); // 需新增该Mapper方法
        for (Goods goods : goodsList) {
            redisTemplate.opsForValue().set(STOCK_CACHE_KEY + goods.getId(), goods.getStock(), 24, TimeUnit.HOURS);
        }
        log.info("商品库存缓存初始化完成，共初始化{}个商品", goodsList.size());
    }
}