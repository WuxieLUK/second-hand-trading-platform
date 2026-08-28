package org.example.order.constant;

import lombok.Getter;

@Getter
public enum RefundStatusEnum {
    NO_REFUND(0, "无退款"),
    REFUNDING(1, "退款中"),
    REFUND_SUCCESS(2, "退款成功"),
    REFUND_FAIL(3, "退款失败");

    private final Integer code;
    private final String name;

    RefundStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (RefundStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value.name;
            }
        }
        return "未知";
    }
}
