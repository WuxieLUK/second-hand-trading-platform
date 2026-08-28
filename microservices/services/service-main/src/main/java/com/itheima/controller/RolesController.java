package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.user.POJO.Roles;
import org.example.user.POJO.UserRoles;
import com.itheima.service.RolesService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@Slf4j
public class RolesController {

    @Resource
    private RolesService rolesService;

    //增加角色
    @PreAuthorize("/roles/add")
    @PostMapping("/add")
    public Result add(@RequestBody @Validated Roles roles) {
        if(rolesService.checkRoleExists(roles.getRoleName())){
            return Result.error("角色已存在");
        }
        rolesService.add(roles);
        return Result.success("添加成功");
    }

    //获取角色分页列表
    @PreAuthorize("/roles/list")
    @GetMapping("/list")
    public Result<PageBean<Roles>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleDescription
    ){
        PageBean<Roles> pb =rolesService.list(pageNum,pageSize, roleName,roleDescription);
        return Result.success(pb);
    }


    //根据id获取角色详细信息
    @PreAuthorize("/roles/info/{id}")
    @GetMapping("/info/{id}")
    public Result<Roles> info(@PathVariable Integer id) {
        Roles c = rolesService.findById(id);
        return Result.success(c);
    }

    //更新角色信息
    @PreAuthorize("/roles/update")
    @PutMapping("/update")
    public Result update(@RequestBody Roles roles) {
        rolesService.update(roles);
        return Result.success("修改成功");
    }

    //删除角色信息
    @PreAuthorize("/roles/delete/{id}")
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        rolesService.delete(id);
        return Result.success();
    }

    //查找所有角色
    @PreAuthorize("/roles/allRolesList")
    @GetMapping("/allRolesList")
    public Result<List<Roles>> allRolesList() {
        return Result.success(rolesService.allRolesList());
    }


    //用户角色表分页查询
    @PreAuthorize("/roles/userRolesList")
    @GetMapping("/userRolesList")
    public Result<PageBean<UserRoles>> userRolesList(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer roleId
    ) {
        PageBean<UserRoles> pb =rolesService.userRolesList(pageNum,pageSize, userId, roleId);
        return Result.success(pb);
    }

    //删除用户角色关联表
    @PreAuthorize("/roles/userRolesDelete/{id}")
    @DeleteMapping("/userRolesDelete/{id}")
    public Result userRolesDelete(@PathVariable Integer id) {
        rolesService.userRolesDelete(id);
        return Result.success();
    }

    //添加用户角色关联表
    @PreAuthorize("/roles/userRolesAdd")
    @PostMapping("/userRolesAdd")
    public Result userRolesAdd(@RequestBody UserRoles userRoles) {
        rolesService.userRolesAdd(userRoles);
        return Result.success();
    }

}
