package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Category {
    @NotNull(groups = Update.class, message = "分类ID不能为空")
    private Integer id;//主键ID

    @NotEmpty(groups = {Add.class,Update.class}, message = "分类名称不能为空")
    private String categoryName;//分类名称

    @NotEmpty(groups = {Add.class,Update.class}, message = "分类别名不能为空")
    private String categoryAlias;//分类别名

    private String createUser;//创建人姓名

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;//更新时间

    // 父ID：非负数（0表示一级分类）
    @PositiveOrZero(groups = {Add.class,Update.class}, message = "父分类ID必须为非负数（0表示一级分类）")
    private Integer parentId;

    public interface Add{}
    public interface Update{}
}