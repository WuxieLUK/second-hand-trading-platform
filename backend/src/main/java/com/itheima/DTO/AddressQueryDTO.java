package com.itheima.DTO;

import lombok.Data;

/**
 * 地址查询数据传输对象
 * 用于接收前端的查询条件
 */
@Data
public class AddressQueryDTO {
    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    private Integer pageSize = 10;

    /**
     * 用户ID（必传，查询指定用户的地址），但是不是必传参数，如果用户ID为空，则查询所有用户的地址
     */
    private Integer userId;

    /**
     * 地址类型：1-校园地址 2-校外地址
     */
    private Integer addressType;

    /**
     * 地址名称（模糊查询）
     */
    private String addressName;
}