package org.example.user.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PageBean;
import org.example.user.POJO.User;
import org.example.user.mapper.RolesMapper;
import org.example.user.mapper.UserMapper;
import org.example.user.service.UserDomainService;
import org.example.user.util.Md5Util;
import org.example.user.util.ThreadLocalUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserDomainServiceImpl implements UserDomainService {

    private final UserMapper userMapper;
    private final RolesMapper rolesMapper;

    public UserDomainServiceImpl(UserMapper userMapper, RolesMapper rolesMapper) {
        this.userMapper = userMapper;
        this.rolesMapper = rolesMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password) {
        userMapper.add(username, Md5Util.getMD5String(password));
        Integer userId = userMapper.findByUsername(username).getId();
        rolesMapper.registerUserRolesAdd(userId, 2);
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
        Map<String, Object> claims = ThreadLocalUtil.get();
        userMapper.updateAvatar(avatarUrl, (Integer) claims.get("id"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePwd(String newPwd) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        log.info("update user password, id={}", id);
        userMapper.updatePwd(Md5Util.getMD5String(newPwd), id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<User> userList(Integer pageNum, Integer pageSize, String username, String email) {
        PageHelper.startPage(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        List<User> users = userMapper.userList(username, email);
        Page<User> page = (Page<User>) users;
        PageBean<User> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
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
    @Transactional(readOnly = true)
    public User getById(Integer id) {
        return userMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.findByIds(ids);
    }
}
