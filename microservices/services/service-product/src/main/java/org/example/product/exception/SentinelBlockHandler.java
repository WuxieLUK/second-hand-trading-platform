package org.example.product.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.VO.GoodsVO;

public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<Void> addBlocked(GoodsDTO goodsDTO, BlockException exception) {
        return Result.error("商品发布已触发限流: " + exception.getClass().getSimpleName());
    }

    public static Result<PageBean<GoodsVO>> goodsListBlocked(GoodsQueryDTO queryDTO, BlockException exception) {
        return Result.error("商品查询已触发限流: " + exception.getClass().getSimpleName());
    }

    /**
     * SQL 搜索熔断：静默返回空分页，用户看到"暂无搜索结果"
     */
    public static PageBean<GoodsVO> goodsSearchBlocked(GoodsQueryDTO queryDTO, BlockException exception) {
        PageBean<GoodsVO> pageBean = new PageBean<>();
        pageBean.setTotal(0L);
        pageBean.setItems(java.util.Collections.emptyList());
        return pageBean;
    }

    /**
     * RAG 搜索熔断：返回空列表
     */
    public static java.util.List<GoodsVO> ragSearchBlocked(String query, BlockException exception) {
        return java.util.Collections.emptyList();
    }
}
