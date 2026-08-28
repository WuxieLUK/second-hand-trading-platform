package com.itheima.pojo;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

//lombok插件，在编译阶段为实体类添加getter/setter/toString方法
@Data
public class User {
    @NonNull
    private Integer id;//主键ID

    @NotBlank(message = "用户名不能为空")
    private String username;//用户名

    @JsonIgnore//让springmvc把当前对象转换成json字符串的时候，忽略password，最终的json字符串中就没有password这个属性了
    private String password;//密码

    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    private String nickname;//昵称

    @NotEmpty
    @Email( message = "你输入的邮箱格式不正确，请输入有效的邮箱地址")
    private String email;//邮箱

    private String userPic;//用户头像地址
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
    private String roleId;//角色ID
    private String phone;

    // ========== 新增字段 ==========
    private String major;       // 专业
    private String grade;       // 年级
    private String campusScene; // 校园场景
    private String tags;        // 用户标签
}
