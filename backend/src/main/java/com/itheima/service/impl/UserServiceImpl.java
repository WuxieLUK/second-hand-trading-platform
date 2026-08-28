package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.RolesMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.util.Md5Util;
import com.itheima.util.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

//明确地表示该类是一个服务类，用于处理业务逻辑
@Service
@Slf4j
public class UserServiceImpl implements UserService {


    //直接注入bean对象，不用new对象创建实例
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RolesMapper rolesMapper;

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        //加密
        String md5String =Md5Util.getMD5String(password);
        //添加
        userMapper.add(username,md5String);
        Integer id = userMapper.findByUsername(username).getId();
        rolesMapper.registerUserRolesAdd(id,2);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(String avatarUrl) {
       Map<String,Object> map = ThreadLocalUtil.get();
       Integer id = (Integer)map.get("id");
       userMapper.updateAvatar(avatarUrl,id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePwd(String newPwd) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer id = (Integer)map.get("id");
        log.info("id:{}",id);
        userMapper.updatePwd(Md5Util.getMD5String(newPwd),id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<User> userList(Integer pageNum, Integer pageSize, String username, String email) {
        PageBean<User> pb = new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);

        //调用Mapper
        //直接获取全部的数据
        List<User> as =userMapper.userList(username,email);
        //Page中提供了方法，可以获取PageHelper分页查询后，查询的总记录条数和当前页数据
        Page<User> p = (Page<User>) as;
        //Page<>是list<>的一个实现类，是list的子类，所以可以强转
        //把数据填充到PageBean对象中
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        rolesMapper.deleteByuserid(id);
        userMapper.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> allUserList() {
        return userMapper.allUserList();
    }

    @Override
    public User getById(Integer userId) {
        return userMapper.findById(userId);
    }
}
