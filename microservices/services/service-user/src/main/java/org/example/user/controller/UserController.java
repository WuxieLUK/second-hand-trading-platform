package org.example.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.user.POJO.User;
import org.example.user.exception.SentinelBlockHandler;
import org.example.user.service.UserDomainService;
import org.example.user.util.JwtUtil;
import org.example.user.util.Md5Util;
import org.example.user.util.ThreadLocalUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/register")
    public Result<String> register(@RequestParam("username")
                                   @Pattern(regexp = "^\\S{1,16}$", message = "用户名不能为空")
                                   String username,
                                   @RequestParam("password")
                                   @Pattern(regexp = "^\\S{1,16}$", message = "密码不能为空")
                                   String password) {
        User user = userDomainService.findByUsername(username);
        if (user != null) {
            return Result.error("用户名已被占用");
        }
        userDomainService.register(username, password);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @SentinelResource(value = "userLogin", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "loginBlocked")
    public Result<String> login(@RequestParam("username")
                                @Pattern(regexp = "^\\S{1,16}$", message = "用户名不能为空")
                                String username,
                                @RequestParam("password")
                                @Pattern(regexp = "^\\S{1,16}$", message = "密码不能为空")
                                String password) {
        User loginUser = userDomainService.findByUsername(username);
        if (loginUser == null) {
            return Result.error("用户不存在");
        }
        if (!Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
            return Result.error("密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", loginUser.getId());
        claims.put("username", loginUser.getUsername());
        String token = JwtUtil.genToken(claims);

        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set(token, token, 12, TimeUnit.HOURS);
        return Result.success(token);
    }

    @GetMapping("/info")
    public Result<User> userInfo() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");
        return Result.success(userDomainService.findByUsername(username));
    }

    @PutMapping("/updates")
    public Result<Void> updateUser(@RequestBody @Validated User user) {
        userDomainService.update(user);
        return Result.success();
    }

    @PostMapping("/updateAvatar")
    public Result<Void> updateAvatar(@RequestParam String avatarUrl) {
        userDomainService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PostMapping("/updatePwd")
    public Result<Void> updatePwd(@RequestBody Map<String, String> params,
                                  @RequestHeader("Authorization") String token) {
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("请完整填写表单内容");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");
        User loginUser = userDomainService.findByUsername(username);
        if (!loginUser.getPassword().equals(Md5Util.getMD5String(oldPwd))) {
            return Result.error("原密码填写不正确");
        }
        if (!newPwd.equals(rePwd)) {
            return Result.error("两次输入的新密码不一致");
        }

        userDomainService.updatePwd(newPwd);
        stringRedisTemplate.delete(token);
        return Result.success();
    }

    @GetMapping("/userList")
    @SentinelResource(value = "userList", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "userListBlocked")
    public Result<PageBean<User>> userList(Integer pageNum,
                                           Integer pageSize,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String email) {
        return Result.success(userDomainService.userList(pageNum, pageSize, username, email));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userDomainService.delete(id);
        return Result.success();
    }

    @GetMapping("/allUserList")
    public Result<List<User>> allUserList() {
        return Result.success(userDomainService.allUserList());
    }

    @GetMapping("/internal/{id}")
    public Result<User> getById(@PathVariable Integer id) {
        return Result.success(userDomainService.getById(id));
    }

    @PostMapping("/internal/listByIds")
    public Result<List<User>> getByIds(@RequestBody List<Integer> ids) {
        return Result.success(userDomainService.getByIds(ids));
    }
}
