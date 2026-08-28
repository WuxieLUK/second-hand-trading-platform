package com.itheima.pojo.Enum;

import lombok.Getter;

/**
     * 商品状态枚举
     */
    public enum GoodsStatusEnum {
        ON_SALE(1, "在售"),
        SOLD_OUT(2, "已售罄"),
        OFF_SHELF(3, "下架"),
        AUDITING(4, "审核中"),
        VIOLATION(5, "违规封禁");

        @Getter
        private final Integer code;
        private final String name;

        GoodsStatusEnum(Integer code, String name) {
            this.code = code;
            this.name = name;
        }

        // 根据编码获取名称
        public static String getNameByCode(Integer code) {
            for (GoodsStatusEnum e : values()) {
                if (e.code.equals(code)) {
                    return e.name;
                }
            }
            return "未知状态";
        }

}


