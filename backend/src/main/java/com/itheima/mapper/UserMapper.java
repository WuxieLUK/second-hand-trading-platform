package com.itheima.mapper;

import com.itheima.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    //根据用户名查询用户
    @Select("select * from user where username = #{username}")
    User findByUsername(String username);
    //插入注册数据
    @Insert("insert into user (username,password,create_time,update_time )" + " values(#{username},#{md5String},now(),now())")
    void add(String username,String md5String);
    //更新用户信息
    @Update("update user set nickname = #{nickname},email = #{email},phone = #{phone},major = #{major},grade = #{grade},campus_scene = #{campusScene},tags = #{tags},update_time = now() where id = #{id}")
    void update(User user);
    //更新用户头像
    @Update("update user set user_pic = #{avatarUrl},update_time = now() where id = #{id}")
    void updateAvatar(String avatarUrl, Integer id);
    //更新用户密码
    @Update("update user set password = #{md5String},update_time = now() where id = #{id}")
    void updatePwd(String md5String, Integer id);
    //分页查询文章列表
    List<User> userList(String username,String email);
    //删除用户
    @Delete("delete from user where id = #{id}")
    void delete(Integer id);
    //查询所有用户
    @Select("select * from user")
    List<User> allUserList();
    //根据id查询用户名
    @Select("select username from user where id = #{id}")
    String findUsernameById(Integer id);
    //根据id查询用户信息
    @Select("select * from user where id = #{id}")
    User findById(Integer id);

    /**
     * 批量查询用户信息
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<User> findByIds(@Param("userIds") List<Integer> userIds);
}
