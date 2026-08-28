package com.itheima.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 面向买家的卖家基本信息VO
 * 用于商品详情页/订单页展示卖家信息（买家视角）
 *
 * @author 开发者
 * @date 2026-02-25
 */
@Data
public class BuyerViewSellerVO implements Serializable {

    /**
     * 卖家ID（仅用于前端关联操作，不展示）
     */
    private Integer sellerId;

    /**
     * 卖家昵称（核心展示字段）
     */
    private String sellerNickname;

    /**
     * 卖家头像URL（核心展示字段）
     */
    private String sellerAvatar;

    /**
     * 卖家认证状态（0-未认证 1-实名认证 2-商家认证）
     */
    private Integer authStatus;

    /**
     * 卖家认证状态名称（前端直接展示：实名认证/商家认证/未认证）
     */
    private String authStatusName;

    /**
     * 卖家发布商品数（可选：展示卖家活跃度）
     */
    private Integer publishGoodsCount;

    /**
     * 卖家好评率（可选：展示卖家可信度，如98.5%）
     */
    private String praiseRate;

    /**
     * 卖家是否在线（0-离线 1-在线，可选：提升沟通体验）
     */
    private Integer isOnline;

    /**
     * 卖家在线状态名称（离线/在线）
     */
    private String isOnlineName;
}