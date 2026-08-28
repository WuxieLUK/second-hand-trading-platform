package org.example.product.infrastructure.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ES 商品索引文档
 * 使用 ik_smart 分词器（需 ES 安装 analysis-ik 插件）
 * 索引名：goods
 */
@Data
@Document(indexName = "goods")
@Setting(settingPath = "/es/goods-setting.json")
public class GoodsDocument {

    @Id
    private Integer id;

    /** 商品名称（主搜索字段，ik 分词） */
    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String goodsName;

    /** 商品描述（辅助搜索） */
    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String goodsDesc;

    /** 商品图片 */
    @Field(type = FieldType.Keyword)
    private String goodsPic;

    /** 分类 ID */
    @Field(type = FieldType.Integer)
    private Integer categoryId;

    /** 分类名称 */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /** 售价 */
    @Field(type = FieldType.Double)
    private BigDecimal sellPrice;

    /** 新旧程度 */
    @Field(type = FieldType.Integer)
    private Integer isNew;

    /** 商品状态 */
    @Field(type = FieldType.Integer)
    private Integer goodsStatus;

    /** 卖家 ID */
    @Field(type = FieldType.Integer)
    private Integer sellerId;

    /** 卖家昵称 */
    @Field(type = FieldType.Keyword)
    private String sellerNickname;

    /** 卖家头像 */
    @Field(type = FieldType.Keyword)
    private String sellerPic;

    /** 创建时间 */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    // ========== 行为数据（用于排序） ==========

    /** 浏览次数 */
    @Field(type = FieldType.Integer)
    private Integer viewCount = 0;

    /** 收藏次数 */
    @Field(type = FieldType.Integer)
    private Integer collectCount = 0;

    /** 订单数量 */
    @Field(type = FieldType.Integer)
    private Integer orderCount = 0;
}
