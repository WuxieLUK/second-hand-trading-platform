package com.itheima.pojo.Enum;

import lombok.Getter;

/**
 * 地址类型枚举
 */
public enum AddressDefaultEnum {
    CAMPUS(0, "非默认地址"),
    OUTSIDE(1, "默认地址");

    @Getter
    private final Integer code;
    private final String name;

    AddressDefaultEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    // 根据编码获取名称
    public static String getNameByCode(Integer code) {
        for (AddressDefaultEnum e : values()) {
            if (e.code.equals(code)) {
                return e.name;
            }
        }
        return "未知类型";
    }

}