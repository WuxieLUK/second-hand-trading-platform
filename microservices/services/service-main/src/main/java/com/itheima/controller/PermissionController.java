package com.itheima.controller;


import com.itheima.anno.PreAuthorize;
import org.example.common.PageBean;
import org.example.user.POJO.Permission;
import org.example.user.POJO.PermissionRoles;
import org.example.common.Result;
import com.itheima.service.PermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
@Slf4j
public class PermissionController {

    @Resource
    private PermissionService permissionService;


    //增加权限
    @PreAuthorize("/permission/add")
    @PostMapping("/add")
    public Result add(@RequestBody @Validated Permission permission) {

        if(permissionService.checkPermissionExists(permission.getPermissionName())){
            return Result.error("权限已存在");
        }
        permissionService.add(permission);
        return Result.success("添加成功");
    }

    //获取权限分页列表
    @PreAuthorize("/permission/list")
    @GetMapping("/list")
    public Result<PageBean<Permission>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) String permissionName,
            @RequestParam(required = false) String permissionDescription
    ){
        PageBean<Permission> pb =permissionService.list(pageNum,pageSize,permissionName,permissionDescription);
        return Result.success(pb);
    }


    //根据列表id获取详细信息
    @PreAuthorize("/permission/info/{id}")
    @GetMapping("/info/{id}")
    public Result<Permission> info(@PathVariable Integer id) {
        Permission c = permissionService.findById(id);
        return Result.success(c);
    }

    //更新权限信息
    @PreAuthorize("/permission/update")
    @PutMapping("/update")
    public Result update(@RequestBody Permission permission) {
        permissionService.update(permission);
        return Result.success("更新成功");
    }

    //删除权限信息
    @PreAuthorize("/permission/delete/{id}")
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        permissionService.delete(id);
        return Result.success();
    }

    //获取权限列表
    @PreAuthorize("/permission/allPermissionList")
    @GetMapping("/allPermissionList")
    public Result<List<Permission>> allPermissionList() {
        List<Permission> list = permissionService.allPermissionList();
        return Result.success(list);
    }

    //用户角色表分页查询
    @PreAuthorize("/permission/permissionRolesList")
    @GetMapping("/permissionRolesList")
    public Result<PageBean<PermissionRoles>> userRolesList(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer permissionId,
            @RequestParam(required = false) Integer roleId
    ) {
        PageBean<PermissionRoles> pb =permissionService.permissionRolesList(pageNum,pageSize, permissionId, roleId);
        return Result.success(pb);
    }

    //添加用户角色关联
    @PreAuthorize("/permission/permissionRolesAdd")
    @PostMapping("/permissionRolesAdd")
    public Result permissionRolesAdd(@RequestBody PermissionRoles permissionRoles) {
        permissionService.permissionRolesAdd(permissionRoles);
        return Result.success("添加成功");
    }

    //删除用户角色关联
    @PreAuthorize("/permission/permissionRolesDelete/{id}")
    @DeleteMapping("/permissionRolesDelete/{id}")
    public Result permissionRolesDelete(@PathVariable Integer id) {
        permissionService.permissionRolesDelete(id);
        return Result.success();
    }

}
