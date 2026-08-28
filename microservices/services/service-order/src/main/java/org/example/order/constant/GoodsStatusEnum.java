package org.example.order.constant;

import lombok.Getter;

@Getter
public enum GoodsStatusEnum {
    ON_SALE(1, "在售"),
    SOLD_OUT(2, "已售罄");

    private final Integer code;
    private final String name;

    GoodsStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
