package com.itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地址详情视图对象
 * 用于前端详情页展示
 */
@Data
public class AddressDetailVO {
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
     * 详细地址
     */
    private String detailAddr;

    /**
     * 邮政编码
     */
    private String zipCode;

    /**
     * 是否默认地址
     */
    private Integer isDefault;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}