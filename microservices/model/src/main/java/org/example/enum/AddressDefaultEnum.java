package com.itheima.pojo.Enum;

import lombok.Getter;

@Getter
public enum AddressDefaultEnum {
    NOT_DEFAULT(0, "非默认地址"),
    DEFAULT(1, "默认地址"),
    CAMPUS(0, "非默认地址"),
    OUTSIDE(1, "默认地址");

    private final Integer code;
    private final String name;

    AddressDefaultEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (AddressDefaultEnum value : values()) {
            if (value.code.equals(code)) {
                return value.name;
            }
        }
        return "未知类型";
    }
}
