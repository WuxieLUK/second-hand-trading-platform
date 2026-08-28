package org.example.user.service;

import org.example.common.PageBean;
import org.example.user.POJO.User;

import java.util.List;

public interface UserDomainService {
    User findByUsername(String username);

    void register(String username, String password);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePwd(String newPwd);

    PageBean<User> userList(Integer pageNum, Integer pageSize, String username, String email);

    void delete(Integer id);

    List<User> allUserList();

    User getById(Integer id);

    List<User> getByIds(List<Integer> ids);
}
