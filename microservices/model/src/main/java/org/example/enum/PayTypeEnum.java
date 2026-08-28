package com.itheima.pojo.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayTypeEnum {
    UNPAID(0, "未支付"),
    WECHAT(1, "微信"),
    ALIPAY(2, "支付宝");

    private final Integer code;
    private final String name;

    public static String getNameByCode(Integer code) {
        if (code == null) {
            return "未知支付方式";
        }
        for (PayTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type.getName();
            }
        }
        return "未知支付方式";
    }

    public static Integer getCodeByName(String name) {
        if (name == null || name.isEmpty()) {
            return UNPAID.getCode();
        }
        for (PayTypeEnum type : values()) {
            if (type.getName().equals(name)) {
                return type.getCode();
            }
        }
        return UNPAID.getCode();
    }

    public static boolean isValidCode(Integer code) {
        if (code == null) {
            return false;
        }
        for (PayTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPaid(Integer code) {
        return WECHAT.getCode().equals(code) || ALIPAY.getCode().equals(code);
    }
}
