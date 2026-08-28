package com.itheima.service;

import com.itheima.DTO.AddressDTO;
import com.itheima.DTO.AddressQueryDTO;
import com.itheima.pojo.Address;
import com.itheima.pojo.PageBean;
import com.itheima.vo.AddressDetailVO;
import com.itheima.vo.AddressVO;

/**
 * 地址服务接口
 */
public interface AddressService {

    /**
     * 新增地址
     */
    void add(AddressDTO addressDTO);

    /**
     * 分页查询地址列表
     */
    PageBean<AddressVO> list(AddressQueryDTO queryDTO);

    /**
     * 根据ID查询地址详情
     */
    AddressDetailVO findById(Integer id);

    /**
     * 修改地址
     */
    void update(AddressDTO addressDTO);

    /**
     * 删除地址
     */
    void delete(Integer id);

    /**
     * 设置默认地址
     */
    void setDefault(Integer userId, Integer addressId);

    /**
     * 查询用户默认地址
     */
    Address findDefaultAddress(Integer userId);
}