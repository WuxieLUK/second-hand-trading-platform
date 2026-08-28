package com.itheima.controller;

import org.example.address.DTO.AddressDTO;
import org.example.address.DTO.AddressQueryDTO;
import com.itheima.anno.PreAuthorize;
import org.example.address.POJO.Address;
import org.example.common.PageBean;
import org.example.common.Result;
import com.itheima.service.AddressService;
import com.itheima.util.ThreadLocalUtil;
import org.example.address.VO.AddressDetailVO;
import org.example.address.VO.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 地址管理控制器
 */
@RestController
@RequestMapping("/address")
@Slf4j
@Tag(name = "地址接口", description = "收货地址管理接口")
public class AddressController {

    @Resource
    private AddressService addressService;

    /**
     * 新增地址
     */
    @PreAuthorize("/address/add")
    @PostMapping
    @Operation(summary = "新增地址", description = "添加用户收货地址")
    public Result add(@RequestBody @Validated AddressDTO addressDTO) {
        addressService.add(addressDTO);
        return Result.success("地址添加成功");
    }

    /**
     * 用户分页查询地址列表
     */
    @PreAuthorize("/address/list")
    @PostMapping("/list")
    @Operation(summary = "查询地址列表", description = "分页查询用户的收货地址")
    public Result<PageBean<AddressVO>> list(AddressQueryDTO queryDTO) {
        // 自动填充当前登录用户ID
        if (queryDTO.getUserId() == null) {
            Map<String, Object> userMap = ThreadLocalUtil.get();
            queryDTO.setUserId((Integer) userMap.get("id"));
        }
        log.info("分页查询地址列表，参数：{}", queryDTO);
        PageBean<AddressVO> pb = addressService.list(queryDTO);
        log.info("分页查询地址列表结果：{}", pb);
        return Result.success(pb);
    }

    /**
     * 分页查询所有用户的地址列表
     */
    @PreAuthorize("/address/allList")
    @PostMapping("/allList")
    @Operation(summary = "查询所有用户的地址列表", description = "分页查询所有用户的收货地址")
    public Result<PageBean<AddressVO>> allList(AddressQueryDTO queryDTO) {
        PageBean<AddressVO> pb = addressService.list(queryDTO);
        return Result.success(pb);
    }


    /**
     * 查询地址详情
     */
    @PreAuthorize("/address/detail")
    @PostMapping("/detail/{id}")
    @Operation(summary = "地址详情", description = "查询地址详细信息")
    public Result<AddressDetailVO> detail(@PathVariable Integer id) {
        AddressDetailVO vo = addressService.findById(id);
        return Result.success(vo);
    }

    /**
     * 修改地址
     */
    @PreAuthorize("/address/update")
    @PostMapping("/update")
    @Operation(summary = "修改地址", description = "更新收货地址信息")
    public Result update(@RequestBody @Validated AddressDTO addressDTO) {
        addressService.update(addressDTO);
        return Result.success("地址修改成功");
    }

    /**
     * 删除地址
     */
    @PreAuthorize("/address/delete")
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除地址", description = "删除收货地址")
    public Result delete(@PathVariable Integer id) {
        addressService.delete(id);
        return Result.success("地址删除成功");
    }

    /**
     * 设置默认地址
     */
    @PreAuthorize("/address/setDefault")
    @PostMapping("/default/{addressId}")
    @Operation(summary = "设置默认地址", description = "将指定地址设为用户默认地址")
    public Result setDefault(@PathVariable Integer addressId) {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");

        addressService.setDefault(userId, addressId);
        return Result.success("默认地址设置成功");
    }

    /**
     * 查询用户默认地址
     */
    @PreAuthorize("/address/default")
    @PostMapping("/default")
    @Operation(summary = "查询默认地址", description = "查询用户的默认收货地址")
    public Result<Address> getDefault() {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");

        Address address = addressService.findDefaultAddress(userId);
        return Result.success(address);
    }
}