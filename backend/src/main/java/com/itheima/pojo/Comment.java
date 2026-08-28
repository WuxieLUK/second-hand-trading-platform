package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Integer id;//主键ID
    private String nickname;
    @NotEmpty(message = "评论内容不能为空") // 修正提示：原“文章标题”是错误的
    private String content;
    @NotNull(message = "商品ID不能为空") // 核心：替换文章ID为商品ID
    private Integer goodsId; // 字段名从articleId改为goodsId
    private Integer likeCount;
    private String userUrl;
    private Integer parentId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;//更新时间
    //comment_user_id
    private Integer commentUserId;
}