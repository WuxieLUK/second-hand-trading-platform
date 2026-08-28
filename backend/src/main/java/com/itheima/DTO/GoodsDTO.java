package com.itheima.DTO;

import com.itheima.pojo.GoodsImage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsDTO {
    private Integer id; // 修改时传，新增时不传

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    private String goodsDesc;
    private String goodsPic;

    @NotNull(message = "商品分类ID不能为空")
    private Integer categoryId;

    @PositiveOrZero(message = "原价不能为负数")
    private BigDecimal originalPrice;

    @NotNull(message = "售卖价格不能为空")
    @Positive(message = "售卖价格必须大于0")
    private BigDecimal sellPrice;

    @NotNull(message = "卖家ID不能为空")
    private Integer sellerId;

    private Integer goodsStatus; // 默认1-在售

    @NotNull(message = "新旧程度不能为空")
    private Integer isNew;

    // 新增库存字段，添加校验规则
    @NotNull(message = "库存不能为空")
    @Positive(message = "库存必须大于0")
    private Integer stock;

    private List<GoodsImage> imageList;
}