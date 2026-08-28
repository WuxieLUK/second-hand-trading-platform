package org.example.goods.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品查询数据传输对象
 * 用于接收前端的查询条件
 *
 * @author 开发者
 * @date 2026-02-20
 */
@Data
public class GoodsQueryDTO {

    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    private Integer pageSize = 10;

    /**
     * 商品名称（模糊查询）
     */
    private String goodsName;

    /**
     * 商品分类ID
     */
    private Integer categoryId;

    /**
     * 商品状态
     */
    private Integer goodsStatus;

    /**
     * 新旧程度
     */
    private Integer isNew;

    /**
     * 价格区间-最低
     */
    private BigDecimal minPrice;

    /**
     * 价格区间-最高
     */
    private BigDecimal maxPrice;

    /**
     * 卖家ID
     */
    private Integer sellerId;

    /**
     * 搜索关键词（模糊匹配商品名称 + 描述）
     */
    private String keyword;

    /**
     * 分类ID列表（用于展开父分类到所有子分类）
     */
    private List<Integer> categoryIds;

    /**
     * 协同过滤推荐用户ID（仅"猜你喜欢"场景使用，由服务端设置）
     */
    private Integer recommendUserId;

    /**
     * 排序字段：price / time（默认 time）
     */
    private String sortField = "time";

    /**
     * 排序方向：asc / desc，仅 price 排序有效（默认 desc）
     */
    private String sortOrder = "desc";
}
