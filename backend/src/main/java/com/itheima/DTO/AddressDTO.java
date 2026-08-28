package com.itheima.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 地址新增/修改数据传输对象
 * 用于接收前端提交的地址信息，包含参数校验
 */
@Data
public class AddressDTO {
    /**
     * 地址ID（修改时传，新增时不传）
     */
    private Integer id;

    /**
     * 地址名称（如“宿舍地址”“家里地址”）
     */
    private String addressName;

    /**
     * 省份
     */
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 城市
     */
    @NotBlank(message = "城市不能为空")
    private String city;

    /**
     * 区县
     */
    @NotBlank(message = "区县不能为空")
    private String district;

    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    private String detailAddr;

    /**
     * 邮政编码
     */
    private String zipCode;

    /**
     * 关联用户ID（后台从登录信息获取，前端无需传）
     */
    private Integer userId;
}