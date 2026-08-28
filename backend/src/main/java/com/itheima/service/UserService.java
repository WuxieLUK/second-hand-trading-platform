package com.itheima.service;

import com.itheima.pojo.PageBean;
import com.itheima.pojo.User;

import java.util.List;


public interface UserService {
    //根据用户名查询用户
    User findByUsername(String username);
    //注册
    void register(String username, String password);
    //更新用户信息
    void update(User user);
    //更新用户头像
    void updateAvatar(String avatarUrl);
    //更新用户密码
    void updatePwd(String newPwd);
    //分页查询用户列表
     PageBean<User> userList(Integer pageNum, Integer pageSize, String username, String email);
    //删除用户
    void delete(Integer id);
    //查询所有用户
    List<User> allUserList();


    User getById(Integer userId);

}
