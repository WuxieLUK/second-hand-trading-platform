package com.itheima.service.impl;

import com.itheima.feign.UserFeignClient;
import com.itheima.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.user.POJO.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户服务实现（BFF 层）
 * 不再直连数据库，全部通过 Feign 调用 service-user
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserFeignClient userFeignClient;

    public UserServiceImpl(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @Override
    public User findByUsername(String username) {
        // 通过分页接口查找用户（设置 pageSize=1 只返回第一个匹配）
        Result<PageBean<User>> result = userFeignClient.userList(1, 1, username, null);
        if (result.getCode() == 0 && result.getData() != null
                && result.getData().getItems() != null
                && !result.getData().getItems().isEmpty()) {
            return result.getData().getItems().get(0);
        }
        return null;
    }

    @Override
    public void register(String username, String password) {
        Result<String> result = userFeignClient.register(username, password);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void update(User user) {
        Result<Void> result = userFeignClient.updateUser(user);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        Result<Void> result = userFeignClient.updateAvatar(avatarUrl);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void updatePwd(String newPwd) {
        // 密码修改需要当前 token（由 FeignAuthConfig 自动转发 Authorization header）
        Map<String, String> params = Map.of("newPwd", newPwd);
        Result<Void> result = userFeignClient.updatePwd(params, "");
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public PageBean<User> userList(Integer pageNum, Integer pageSize, String username, String email) {
        Result<PageBean<User>> result = userFeignClient.userList(pageNum, pageSize, username, email);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public void delete(Integer id) {
        Result<Void> result = userFeignClient.delete(id);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public List<User> allUserList() {
        Result<List<User>> result = userFeignClient.allUserList();
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public User getById(Integer userId) {
        Result<User> result = userFeignClient.getById(userId);
        if (result.getCode() != 0) {
            throw new RuntimeException(result.getMessage());
        }
        return result.getData();
    }
}
