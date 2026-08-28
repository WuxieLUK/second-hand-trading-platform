package com.itheima.service;

import com.itheima.pojo.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface PermissionService {

    // ============== 【新增核心方法】：查询用户权限（带缓存，解决重复查询问题） ==============
    List<String> getUserPermissions(Integer userId);

    //添加权限
    void add(Permission permission);

    //分页查询
    PageBean<Permission> list(Integer pageNum, Integer pageSize,String permissionName,String permissionDescription);

    //根据id查询
    Permission findById(Integer id);

    //修改权限
    void update(Permission permission);

    //删除权限
    void delete(Integer id);

    //查询权限和角色的关联关系
    PageBean<PermissionRoles> permissionRolesList(Integer pageNum, Integer pageSize, Integer permissionId, Integer roleId);

    //添加权限和角色的关联关系
    void permissionRolesAdd(PermissionRoles permissionRoles);

    //删除权限和角色的关联关系
    void permissionRolesDelete(Integer id);

    //查询所有权限
    List<Permission> allPermissionList();

    //检查权限是否已存在
    boolean checkPermissionExists(String permissionName);
}
