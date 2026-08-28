package com.itheima.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表视图对象
 * 用于前端列表展示
 *
 * @author 开发者
 * @date 2026-02-20
 */
@Data
public class GoodsListVO {

    /**
     * 商品ID
     */
    private Integer id;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片URL（多个用逗号分隔）
     */
    private String goodsPic;

    /**
     * 商品分类ID
     */
    private Integer categoryId;

    /**
     * 商品分类名称（前端展示用）
     */
    private String categoryName;

    /**
     * 售卖价格
     */
    private BigDecimal sellPrice;

    /**
     * 新旧程度：0-二手 1-全新 2-9成新 3-8成新 4-7成新及以下
     */
    private Integer isNew;

    /**
     * 新旧程度名称（前端展示用）
     */
    private String isNewName;

    /**
     * 商品状态：1-在售 2-已售出 3-下架 4-审核中 5-违规封禁
     */
    private Integer goodsStatus;

    /**
     * 商品状态名称（前端展示用）
     */
    private String goodsStatusName;

    /**
     * 卖家昵称（前端展示用）
     */
    private String sellerNickname;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
