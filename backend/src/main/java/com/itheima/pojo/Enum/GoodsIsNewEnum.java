package com.itheima.pojo.Enum;

/**
 * 商品新旧程度枚举
 */
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

    // 根据编码获取名称
    public static String getNameByCode(Integer code) {
        for (GoodsIsNewEnum e : values()) {
            if (e.code.equals(code)) {
                return e.name;
            }
        }
        return "未知";
    }
}
