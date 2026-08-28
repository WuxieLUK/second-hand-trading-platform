package com.itheima.pojo.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatusEnum {
    NO_REFUND(0, "无退款"),
    REFUNDING(1, "退款中"),
    REFUND_SUCCESS(2, "退款成功"),
    REFUND_FAILED(3, "退款失败");

    private final Integer code;
    private final String name;

    public static String getNameByCode(Integer code) {
        for (RefundStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status.getName();
            }
        }
        return "未知状态";
    }

    public static Integer getCodeByName(String name) {
        for (RefundStatusEnum status : values()) {
            if (status.getName().equals(name)) {
                return status.getCode();
            }
        }
        return 0;
    }
}
