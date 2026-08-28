package org.example.user.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.user.POJO.User;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select * from user where username = #{username}")
    User findByUsername(String username);

    @Insert("insert into user (username,password,create_time,update_time) values(#{username},#{password},now(),now())")
    void add(@Param("username") String username, @Param("password") String password);

    @Update("update user set nickname = #{nickname}, email = #{email}, phone = #{phone}, major = #{major}, grade = #{grade}, campus_scene = #{campusScene}, tags = #{tags}, update_time = now() where id = #{id}")
    void update(User user);

    @Update("update user set user_pic = #{avatarUrl}, update_time = now() where id = #{id}")
    void updateAvatar(@Param("avatarUrl") String avatarUrl, @Param("id") Integer id);

    @Update("update user set password = #{password}, update_time = now() where id = #{id}")
    void updatePwd(@Param("password") String password, @Param("id") Integer id);

    List<User> userList(@Param("username") String username, @Param("email") String email);

    @Delete("delete from user where id = #{id}")
    void delete(Integer id);

    @Select("select * from user")
    List<User> allUserList();

    @Select("select * from user where id = #{id}")
    User findById(Integer id);

    List<User> findByIds(@Param("userIds") List<Integer> userIds);
}
