package com.itheima.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

//统一响应结果，有参无参构造
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Result<T> implements Serializable {
    private Integer code;//业务状态码  0-成功  1-失败
    private String message;//提示信息
    private T data;//响应数据

    //T为泛型
    //快速返回操作成功响应结果(带响应数据)
    //<E>声明泛型类
    public static <E> Result<E> success(E data) {
        return new Result<>(0, "操作成功", data);
    }

    //快速返回操作成功响应结果（不带响应数据）
    public static Result success() {
        return new Result(0, "操作成功", null);
    }

    public static Result error(String message) {
        return new Result(1, message, null);
    }
}
