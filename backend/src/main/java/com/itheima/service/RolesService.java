package com.itheima.service;

import com.itheima.pojo.PageBean;
import com.itheima.pojo.Roles;
import com.itheima.pojo.UserRoles;

import java.util.List;

public interface RolesService {

    //添加角色
    void add(Roles roles);

    //分页查询角色
    PageBean<Roles> list(Integer pageNum, Integer pageSize, String roleName, String roleDescription);

    //根据id查询角色
    Roles findById(Integer id);

    //修改角色
    void update(Roles roles);

    //删除角色
    void delete(Integer id);

    //根据用户id查询角色
    PageBean<UserRoles> userRolesList(Integer pageNum, Integer pageSize, Integer userId, Integer roleId);

    //查询所有角色
    List<Roles> allRolesList();

    //根据id删除用户角色
    void userRolesDelete(Integer id);

    //添加用户角色
    void userRolesAdd(UserRoles userRoles);

    //判断角色是否存在
    boolean checkRoleExists(String roleName);

}
