package com.itheima.pojo.Enum;

import lombok.Getter;

@Getter
public enum GoodsStatusEnum {
    ON_SALE(1, "在售"),
    SOLD_OUT(2, "已售罄"),
    OFF_SHELF(3, "已下架"),
    AUDITING(4, "审核中"),
    VIOLATION(5, "违规封禁");

    private final Integer code;
    private final String name;

    GoodsStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (GoodsStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value.name;
            }
        }
        return "未知状态";
    }
}
