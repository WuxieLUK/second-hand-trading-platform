package org.example.user.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolesMapper {

    @Delete("delete from user_roles where user_id = #{userId}")
    void deleteByuserid(Integer userId);

    @Insert("insert into user_roles(user_id, role_id, create_time, update_time) values(#{userId}, #{roleId}, now(), now())")
    void registerUserRolesAdd(Integer userId, Integer roleId);
}
