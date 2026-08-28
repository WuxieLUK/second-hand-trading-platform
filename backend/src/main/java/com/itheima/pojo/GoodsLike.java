package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品点赞实体类（替换原文章点赞Like类）
 */
@Data
public class GoodsLike { // 类名语义化修改
    private Integer id;//主键ID
    private Integer userId;
    private Integer goodsId; // 替换articleId为goodsId
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间
}