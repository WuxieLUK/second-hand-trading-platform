package com.itheima.vo;

import com.itheima.pojo.GoodsImage;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情视图对象
 * 用于前端商品详情页展示
 *
 * @author 开发者
 * @date 2026-02-20
 */
@Data
public class GoodsDetailVO implements Serializable {

    /**
     * 商品ID
     */
    private Integer id;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品描述（二手商品详情、新旧程度等）
     */
    private String goodsDesc;

    /**
     * 商品图片URL列表（拆分逗号，转成List）
     */
    private String goodsPic;

    /**
     * 商品分类ID
     */
    private Integer categoryId;

    /**
     * 商品分类名称
     */
    private String categoryName;

    /**
     * 商品原价
     */
    private BigDecimal originalPrice;

    /**
     * 售卖价格
     */
    private BigDecimal sellPrice;

    /**
     * 价格折扣（原价/售价，前端展示用）
     */
    private String discount;

    /**
     * 卖家ID
     */
    private Integer sellerId;

    /**
     * 卖家昵称
     */
    private String sellerNickname;

    /**
     * 卖家头像
     */
    private String sellerAvatar;


    /**
     * 商品状态：1-在售 2-已售出 3-下架 4-审核中 5-违规封禁
     */
    private Integer goodsStatus;

    /**
     * 商品状态名称
     */
    private String goodsStatusName;

    /**
     * 新旧程度名称
     */
    private String isNewName;

    /**
     * 商品库存数量
     */
    private Integer stock;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 商品多态图片
     */
    private List<GoodsImage> imageList;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 商品收藏数
     */
    private Integer CollectCount;
}