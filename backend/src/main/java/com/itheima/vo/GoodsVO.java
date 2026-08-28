package com.itheima.vo;

import com.itheima.pojo.GoodsImage;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GoodsVO implements Serializable {
    private Integer id;
    private String goodsName;
    private String goodsPic;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal sellPrice;
    private Integer isNew;
    private String isNewName; // 新旧程度名称
    private Integer goodsStatus;
    private String goodsStatusName; // 商品状态名称
    private String sellerNickname; // 卖家昵称
    private Integer stock;
    private List<GoodsImage> imageList;
    private LocalDateTime createTime;
    private Integer CollectCount;// 商品收藏数
    private String sellerPic;

    private Integer SellerId;
}

