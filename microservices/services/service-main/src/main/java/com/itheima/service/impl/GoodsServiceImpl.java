package com.itheima.service.impl;

import com.itheima.feign.ProductFeignClient;
import com.itheima.feign.UserFeignClient;
import com.itheima.service.GoodsService;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.VO.GoodsVO;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.example.user.VO.BuyerViewSellerVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 商品服务实现（BFF 层）
 * 不再直连数据库，全部通过 Feign 调用 service-product 和 service-user
 * Milvus 向量检索、AI 对话等能力已迁移到 service-product
 */
@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {

    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;

    public GoodsServiceImpl(ProductFeignClient productFeignClient,
                            UserFeignClient userFeignClient) {
        this.productFeignClient = productFeignClient;
        this.userFeignClient = userFeignClient;
    }

    @Override
    public void add(GoodsDTO goodsDTO) {
        Result<Void> result = productFeignClient.add(goodsDTO);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public PageBean<GoodsVO> list(GoodsQueryDTO queryDTO) {
        // 公开商品列表（仅在售），通过 Feign 调用 service-product
        Result<PageBean<GoodsVO>> result = productFeignClient.goodsOpenList(queryDTO);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public GoodsDetailVO findById(Integer id) {
        Result<GoodsDetailVO> result = productFeignClient.detail(id);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public void update(GoodsDTO goodsDTO) {
        Result<Void> result = productFeignClient.update(goodsDTO);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void delete(Integer id) {
        Result<Void> result = productFeignClient.delete(id);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void updateStatus(Integer id, Integer status) {
        Result<Void> result = productFeignClient.updateStatus(id, status);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public BuyerViewSellerVO findSellerByUserId(Integer id) {
        Result<BuyerViewSellerVO> result = productFeignClient.findSellerByUserId(id);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public PageBean<GoodsVO> alllist(GoodsQueryDTO queryDTO) {
        Result<PageBean<GoodsVO>> result = productFeignClient.list(queryDTO);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public List<GoodsVO> listByIds(List<Integer> ids) {
        Result<List<GoodsVO>> result = productFeignClient.listByIds(ids);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public PageBean<GoodsVO> search(GoodsQueryDTO queryDTO) {
        Result<PageBean<GoodsVO>> result = productFeignClient.search(queryDTO);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public List<GoodsVO> ragSearch(String query) {
        // 通过 Feign 调用 service-product 的内部 RAG 搜索 API
        Map<String, String> params = Map.of("query", query);
        Result<List<GoodsVO>> result = productFeignClient.ragSearch(params);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public Map<String, Object> ragSearchWithSummary(String query) {
        // 通过 Feign 调用 service-product 的完整 RAG 搜索（含向量检索 + AI 摘要）
        Map<String, String> params = Map.of("query", query);
        Result<Map<String, Object>> result = productFeignClient.ragSearchWithSummary(params);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public void cleanMilvusDirtyData() {
        throw new UnsupportedOperationException(
                "Milvus 脏数据清理已迁移到 service-product，请直接调用 product 服务");
    }

    @Override
    public List<GoodsVO> collaborativeRecommend(Integer userId, int limit) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", userId);
        params.put("limit", limit);
        Result<List<GoodsVO>> result = productFeignClient.collaborativeRecommend(params);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public List<GoodsVO> recommendByCategory(String keyword, Integer userId, int limit) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("keyword", keyword);
        params.put("userId", userId);
        params.put("limit", limit);
        Result<List<GoodsVO>> result = productFeignClient.recommendByCategory(params);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }
}
