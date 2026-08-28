package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.RolesMapper;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Roles;
import com.itheima.pojo.UserRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.itheima.service.RolesService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolesServiceImpl implements RolesService{

    @Autowired
    private RolesMapper rolesMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Roles roles) {
          rolesMapper.add(roles);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<Roles> list(Integer pageNum, Integer pageSize, String roleName, String roleDescription) {
        PageBean<Roles> pb = new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);
        List<Roles> as =rolesMapper.list(roleName,roleDescription);
        //Page中提供了方法，可以获取PageHelper分页查询后，查询的总记录条数和当前页数据
        Page<Roles> p = (Page<Roles>) as;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    @Transactional(readOnly = true)
    public Roles findById(Integer id) {
        return rolesMapper.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Roles roles) {
        rolesMapper.update(roles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        rolesMapper.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<UserRoles> userRolesList(Integer pageNum, Integer pageSize, Integer userId, Integer roleId) {
        PageBean<UserRoles> pb = new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);
        List<UserRoles> as =rolesMapper.userRolesList(userId,roleId);
        //Page中提供了方法，可以获取PageHelper分页查询后，查询的总记录条数和当前页数据
        Page<UserRoles> p = (Page<UserRoles>) as;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Roles> allRolesList() {
        return rolesMapper.allRolesList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userRolesDelete(Integer id) {
        rolesMapper.userRolesDelete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userRolesAdd(UserRoles userRoles) {
        rolesMapper.userRolesAdd(userRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkRoleExists(String roleName) {
        return rolesMapper.checkRoleExists(roleName) > 0;
    }
}
