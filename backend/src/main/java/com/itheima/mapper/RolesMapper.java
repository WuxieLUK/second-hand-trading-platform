package com.itheima.mapper;

import com.itheima.pojo.Roles;
import com.itheima.pojo.UserRoles;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RolesMapper {

    // 分页查询所有角色
    List<Roles> list(String roleName, String roleDescription);

    // 添加角色
    @Insert("insert into roles(role_name,role_description,create_time,update_time) values(#{roleName},#{roleDescription},now(),now())")
    void add(Roles roles);

    // 根据id查询角色
    @Select("select * from roles where role_id = #{roleId}")
    Roles findById(Integer id);

    // 修改角色
    @Update("update roles set role_name = #{roleName},role_description = #{roleDescription},update_time = now() where role_id = #{roleId}")
    void update(Roles roles);

    // 删除角色
    @Delete("delete from roles where role_id = #{roleId}")
    void delete(Integer roleId);

    // 根据用户id和角色id查询用户角色关联表
    List<UserRoles> userRolesList(Integer userId, Integer roleId);

    // 查询所有角色
    @Select("select * from roles")
    List<Roles> allRolesList();

    // 根据用户角色关联表id删除用户角色关联表
    @Delete("delete from user_roles where user_role_id = #{userRoleId}")
    void userRolesDelete(Integer userRoleId);

    // 根据用户id删除用户角色关联表
    @Delete("delete from user_roles where user_id = #{userId}")
    void deleteByuserid(Integer userId);

    // 添加用户角色关联表
    @Insert("insert into user_roles(user_id,role_id,create_time,update_time) values(#{userId},#{roleId},now(),now())")
    void userRolesAdd(UserRoles userRoles);

    // 检查角色是否存在
    @Select("select count(*) from roles where role_name = #{roleName}")
    int checkRoleExists(String roleName);

    //注册自动添加角色
    @Insert("insert into user_roles(user_id,role_id,create_time,update_time) values(#{userId},#{roleId},now(),now())")
    void registerUserRolesAdd(Integer userId,Integer roleId);
}
