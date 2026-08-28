// 订单状态枚举
package com.itheima.pojo.Enum;

import lombok.Getter;

public enum OrderStatusEnum {
    PENDING_PAY(1, "待付款"),
    PENDING_DELIVERY(2, "待发货"),
    PENDING_RECEIVE(3, "待收货"),
    COMPLETED(4, "已完成"),
    CANCELED(5, "已取消");

    @Getter
    private final Integer code;
    private final String name;

    OrderStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        for (OrderStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e.name;
            }
        }
        return "未知状态";
    }

}