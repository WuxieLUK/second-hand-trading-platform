package com.itheima.feign;

import org.example.common.Result;
import org.example.user.POJO.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * service-user Feign 客户端（BFF 层调用）
 */
@FeignClient(name = "service-user")
public interface UserFeignClient {

    @PostMapping("/user/register")
    Result<String> register(@RequestParam("username") String username, @RequestParam("password") String password);

    @PostMapping("/user/login")
    Result<String> login(@RequestParam("username") String username, @RequestParam("password") String password);

    @GetMapping("/user/info")
    Result<User> userInfo();

    @PutMapping("/user/updates")
    Result<Void> updateUser(@RequestBody User user);

    @PostMapping("/user/updateAvatar")
    Result<Void> updateAvatar(@RequestParam("avatarUrl") String avatarUrl);

    @PostMapping("/user/updatePwd")
    Result<Void> updatePwd(@RequestBody java.util.Map<String, String> params,
                           @RequestHeader("Authorization") String token);

    @GetMapping("/user/userList")
    Result<org.example.common.PageBean<User>> userList(@RequestParam("pageNum") Integer pageNum,
                                                       @RequestParam("pageSize") Integer pageSize,
                                                       @RequestParam(value = "username", required = false) String username,
                                                       @RequestParam(value = "email", required = false) String email);

    @DeleteMapping("/user/{id}")
    Result<Void> delete(@PathVariable("id") Integer id);

    @GetMapping("/user/allUserList")
    Result<List<User>> allUserList();

    @GetMapping("/user/internal/{id}")
    Result<User> getById(@PathVariable("id") Integer id);

    @PostMapping("/user/internal/listByIds")
    Result<List<User>> getByIds(@RequestBody List<Integer> ids);
}
