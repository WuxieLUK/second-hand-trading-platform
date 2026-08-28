package com.itheima.DTO;

import lombok.Data;

import java.math.BigDecimal;

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
     *
     */
}
