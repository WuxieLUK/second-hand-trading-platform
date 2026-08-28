package org.example.order.constant;

import lombok.Getter;

@Getter
public enum PayTypeEnum {
    UNPAID(0, "未支付"),
    WECHAT(1, "微信"),
    ALIPAY(2, "支付宝");

    private final Integer code;
    private final String name;

    PayTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (PayTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value.name;
            }
        }
        return "未知";
    }
}
