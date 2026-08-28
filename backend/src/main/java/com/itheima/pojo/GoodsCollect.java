package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品收藏实体类（映射goods_collect表）
 */
@Data
public class GoodsCollect {
    private Integer id; // 主键ID
    private Integer userId; // 用户ID
    private Integer goodsId; // 商品ID
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 收藏时间
}