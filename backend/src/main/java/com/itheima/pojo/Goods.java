package com.itheima.pojo;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品表实体类
 *
 * @author 开发者
 * @date 2026-02-20
 */
@Data
@TableName("goods") // 对应数据库表名
public class Goods {

    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO) // 自增主键
    private Integer id;

    /**
     * 商品名称
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 商品描述（二手商品详情、新旧程度等）
     */
    @TableField("goods_desc")
    private String goodsDesc;

    /**
     * 商品图片URL（多个用逗号分隔）
     */
    @TableField("goods_pic")
    private String goodsPic;

    /**
     * 商品分类ID（如1-教材 2-电子产品 3-生活用品）
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 商品原价
     */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /**
     * 售卖价格
     */
    @TableField("sell_price")
    private BigDecimal sellPrice;

    /**
     * 卖家ID（关联user表id）
     */
    @TableField("seller_id")
    private Integer sellerId;

    /**
     * 商品状态：1-在售 2-已售出 3-下架 4-审核中 5-违规封禁
     */
    @TableField("goods_status")
    private Integer goodsStatus;

    /**
     * 新旧程度：0-二手 1-全新 2-9成新 3-8成新 4-7成新及以下
     */
    @TableField("is_new")
    private Integer isNew;

    /**
     * 库存数量
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
