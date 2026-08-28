package com.itheima.pojo.Enum;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    PENDING_PAY(1, "待付款"),
    PENDING_DELIVERY(2, "待发货"),
    PENDING_RECEIVE(3, "待收货"),
    COMPLETED(4, "已完成"),
    CANCELED(5, "已取消");

    private final Integer code;
    private final String name;

    OrderStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (OrderStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value.name;
            }
        }
        return "未知状态";
    }
}
