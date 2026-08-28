package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

//分页返回结果对象
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageBean <T> implements Serializable {
    private Long total;//总条数
    private List<T> items;//当前页数据集合
}
