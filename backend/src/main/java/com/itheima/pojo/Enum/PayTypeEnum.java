package com.itheima.pojo.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举类
 * 统一管理支付方式的编码和名称，避免硬编码
 */
@Getter
@AllArgsConstructor
public enum PayTypeEnum {
    UNPAID(0, "未支付"),
    WECHAT(1, "微信"),
    ALIPAY(2, "支付宝");

    /**
     * 支付方式编码
     */
    private final Integer code;

    /**
     * 支付方式名称（前端展示/日志输出用）
     */
    private final String name;

    // ========== 核心工具方法（统一判断/转换逻辑）==========

    /**
     * 根据编码获取支付方式名称（最常用）
     * @param code 支付方式编码（0/1/2）
     * @return 名称（未支付/微信/支付宝），未知编码返回"未知支付方式"
     */
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

    /**
     * 根据名称获取编码（少用，主要用于前端传名称转编码）
     * @param name 支付方式名称（微信/支付宝）
     * @return 编码（1/2），未知名称返回0（未支付）
     */
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

    /**
     * 校验编码是否合法（用于参数校验）
     * @param code 支付方式编码
     * @return true=合法，false=不合法
     */
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

    /**
     * 校验是否为已支付状态（排除未支付）
     * @param code 支付方式编码
     * @return true=已支付（微信/支付宝），false=未支付/非法编码
     */
    public static boolean isPaid(Integer code) {
        return WECHAT.getCode().equals(code) || ALIPAY.getCode().equals(code);
    }
}