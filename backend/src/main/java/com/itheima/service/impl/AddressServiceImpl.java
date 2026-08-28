package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.DTO.AddressDTO;
import com.itheima.DTO.AddressQueryDTO;
import com.itheima.mapper.AddressMapper;
import com.itheima.pojo.Address;
import com.itheima.pojo.Enum.AddressDefaultEnum;
import com.itheima.pojo.PageBean;
import com.itheima.service.AddressService;
import com.itheima.util.ThreadLocalUtil;
import com.itheima.vo.AddressDetailVO;
import com.itheima.vo.AddressVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 地址服务实现类
 */
@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AddressDTO addressDTO) {
        // 1. 参数校验
        if (addressDTO == null) {
            throw new IllegalArgumentException("地址信息不能为空");
        }

        // 2. 获取当前登录用户ID
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");

        // 3. 转换为实体类
        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        // 4. 如果设置为默认地址，先重置该用户原有默认地址
        if (AddressDefaultEnum.OUTSIDE.getCode().equals(address.getIsDefault())) {
            Address currentDefault = findDefaultAddress(userId);
            if (currentDefault != null) {
                currentDefault.setIsDefault(AddressDefaultEnum.CAMPUS.getCode());
                addressMapper.update(currentDefault);
            }
        } else {
            // 默认设为非默认地址
            address.setIsDefault(AddressDefaultEnum.CAMPUS.getCode());
        }

        // 5. 保存地址
        addressMapper.add(address);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<AddressVO> list(AddressQueryDTO queryDTO) {
        // 1. 开启分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 查询地址列表
        List<AddressVO> addressList = addressMapper.list(queryDTO);
        Page<AddressVO> p = (Page<AddressVO>) addressList;

        // 3. 封装分页结果
        PageBean<AddressVO> pb = new PageBean<>();
        pb.setTotal(p.getTotal());
        pb.setItems(addressList);
        return pb;
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDetailVO findById(Integer id) {
        // 1. 参数校验
        if (id == null || id < 1) {
            throw new IllegalArgumentException("地址ID不合法");
        }

        // 2. 查询地址
        Address address = addressMapper.findById(id);
        if (address == null) {
            throw new RuntimeException("地址不存在：" + id);
        }

        // 3. 转换为详情VO
        AddressDetailVO vo = new AddressDetailVO();
        BeanUtils.copyProperties(address, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AddressDTO addressDTO) {
        // 1. 参数校验
        if (addressDTO == null || addressDTO.getId() == null) {
            throw new IllegalArgumentException("地址ID不能为空");
        }

        // 2. 查询地址是否存在
        Address oldAddress = addressMapper.findById(addressDTO.getId());
        if (oldAddress == null) {
            throw new RuntimeException("地址不存在：" + addressDTO.getId());
        }

        // 3. 转换为实体类
        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(oldAddress.getUserId()); // 防止篡改用户ID
        address.setCreateTime(oldAddress.getCreateTime()); // 创建时间不修改
        address.setUpdateTime(LocalDateTime.now());

        // 4. 如果修改为默认地址，先重置该用户原有默认地址
        if (AddressDefaultEnum.OUTSIDE.getCode().equals(address.getIsDefault())) {
            Address currentDefault = findDefaultAddress(oldAddress.getUserId());
            if (currentDefault != null && !currentDefault.getId().equals(address.getId())) {
                currentDefault.setIsDefault(AddressDefaultEnum.CAMPUS.getCode());
                addressMapper.update(currentDefault);
            }
        }

        // 5. 修改地址
        addressMapper.update(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        // 1. 参数校验
        if (id == null || id < 1) {
            throw new IllegalArgumentException("地址ID不合法");
        }

        // 2. 查询地址是否存在
        Address address = addressMapper.findById(id);
        if (address == null) {
            throw new RuntimeException("地址不存在：" + id);
        }

        // 3. 删除地址
        addressMapper.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Integer userId, Integer addressId) {
        // 1. 参数校验
        if (userId == null || addressId == null) {
            throw new IllegalArgumentException("用户ID和地址ID不能为空");
        }

        // 2. 校验地址是否属于该用户
        Address targetAddress = addressMapper.findById(addressId);
        if (targetAddress == null || !userId.equals(targetAddress.getUserId())) {
            throw new RuntimeException("地址不存在或不属于当前用户");
        }

        // 3. 先查询该用户是否已经有默认地址
        Address currentDefault = findDefaultAddress(userId);
        if (currentDefault != null && !currentDefault.getId().equals(addressId)) {
            // 4. 重置原有默认地址
            currentDefault.setIsDefault(AddressDefaultEnum.CAMPUS.getCode());
            addressMapper.update(currentDefault);
        }

        // 5. 将目标地址设为默认（核心逻辑补全）
        targetAddress.setIsDefault(AddressDefaultEnum.OUTSIDE.getCode());
        targetAddress.setUpdateTime(LocalDateTime.now());
        addressMapper.update(targetAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public Address findDefaultAddress(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return addressMapper.findDefaultAddressByUserId(userId);
    }
}