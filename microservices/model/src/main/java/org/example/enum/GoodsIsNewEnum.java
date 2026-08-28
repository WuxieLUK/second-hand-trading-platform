package com.itheima.pojo.Enum;

import lombok.Getter;

@Getter
public enum GoodsIsNewEnum {
    SECOND_HAND(0, "二手"),
    BRAND_NEW(1, "全新"),
    NINETY_NEW(2, "9成新"),
    EIGHTY_NEW(3, "8成新"),
    SEVENTY_NEW(4, "7成新及以下");

    private final Integer code;
    private final String name;

    GoodsIsNewEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (GoodsIsNewEnum value : values()) {
            if (value.code.equals(code)) {
                return value.name;
            }
        }
        return "未知";
    }
}
