package com.itheima.mapper;

import com.itheima.pojo.Permission;
import com.itheima.pojo.PermissionRoles;
import org.apache.ibatis.annotations.*;

import java.util.List;


//根据用户id查找该用户的所有权限信息
@Mapper
public interface PermissionsMapper {
    @Select("SELECT p.permission_name " +
            "FROM user u " +
            "         JOIN user_roles ur ON u.id = ur.user_id " +
            "         JOIN permissions_roles pr ON ur.role_id = pr.role_id " +
            "         JOIN permissions p ON pr.permission_id = p.permission_id " +
            "WHERE u.id = #{id}")
    public List<String> selectMenuPermsByUserId(Integer id);


    //添加权限
    @Insert("insert into permissions (permission_name,permission_description,create_time,update_time)" +
            "values(#{permissionName},#{permissionDescription},now(),now())")
    void add(Permission permission);

    //查询权限列表
    List<Permission> list(String permissionName, String permissionDescription);

    //根据id查询权限信息
    @Select("select * from permissions where permission_id = #{permissionId}")
    Permission findById(Integer permissionId);

    //修改权限信息
    @Update("update permissions set permission_name = #{permissionName},permission_description = #{permissionDescription},update_time = now() where permission_id = #{permissionId}")
    void update(Permission permission);

    //删除权限信息
    @Update("delete from permissions where permission_id = #{permissionId}")
    void delete(Integer permissionId);

    //查询权限角色关联列表信息
    List<PermissionRoles> permissionRolesList(Integer permissionId, Integer roleId);

    //添加权限角色关联信息
    @Insert("insert into permissions_roles(permission_id,role_id,create_time,update_time) values(#{permissionId},#{roleId},now(),now())")
    void permissionRolesAdd(PermissionRoles permissionRoles);

    //删除权限角色关联信息
    @Delete("delete from permissions_roles where permission_role_id = #{permissionRoleId}")
    void permissionRolesDelete(Integer permissionRoleId);

    //查询所有权限信息
    @Select("select * from permissions")
    List<Permission> allPermissionList();

    @Select("select count(*) from permissions where permission_name = #{methodPermission}")
    boolean checkPermissionExists(String methodPermission);

    @Select("SELECT p.permission_name " +
            "FROM user u " +
            "         JOIN user_roles ur ON u.id = ur.user_id " +
            "         JOIN permissions_roles pr ON ur.role_id = pr.role_id " +
            "         JOIN permissions p ON pr.permission_id = p.permission_id " +
            "WHERE u.id = #{userId}")
    List<String> getUserPermissions(Integer userId);
}
