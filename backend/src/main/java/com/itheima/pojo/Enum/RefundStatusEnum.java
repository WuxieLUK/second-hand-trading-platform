package com.itheima.pojo.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退款状态枚举（核心新增）
 */
@Getter
@AllArgsConstructor
public enum RefundStatusEnum {
    NO_REFUND(0, "无退款"),
    REFUNDING(1, "退款中"),
    REFUND_SUCCESS(2, "退款成功"),
    REFUND_FAILED(3, "退款失败");

    private final Integer code;
    private final String name;

    // 根据编码获取名称
    public static String getNameByCode(Integer code) {
        for (RefundStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status.getName();
            }
        }
        return "未知状态";
    }

    // 根据名称获取编码
    public static Integer getCodeByName(String name) {
        for (RefundStatusEnum status : values()) {
            if (status.getName().equals(name)) {
                return status.getCode();
            }
        }
        return 0;
    }
}