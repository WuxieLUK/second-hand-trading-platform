package com.itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地址列表视图对象
 * 用于前端列表展示
 */
@Data
public class AddressVO {
    /**
     * 地址ID
     */
    private Integer id;

    /**
     * 地址名称（如“宿舍地址”“家里地址”）
     */
    private String addressName;

    /**
     * 完整地址（省+市+区县+详细地址）
     */
    private String fullAddress;

    /**
     * 是否默认地址
     */
    private Integer isDefault;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}