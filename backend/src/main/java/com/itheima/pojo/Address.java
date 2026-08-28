package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地址表实体类（映射数据库address表）
 */
@Data
public class Address {
    /**
     * 地址ID
     */
    private Integer id;

    /**
     * 地址名称（如“宿舍地址”“家里地址”）
     */
    private String addressName;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区县
     */
    private String district;

    /**
     * 详细地址（如XX宿舍X栋X单元X室）
     */
    private String detailAddr;

    /**
     * 邮政编码
     */
    private String zipCode;

    /**
     * 地址类型：0-不默认 1-默认
     */
    private Integer addressType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 是否默认地址：0-否 1-是
     */
    private Integer isDefault;
}