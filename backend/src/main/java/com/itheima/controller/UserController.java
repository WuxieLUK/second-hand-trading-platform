package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.util.JwtUtil;
import com.itheima.util.Md5Util;
import com.itheima.util.ThreadLocalUtil;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//接受请求转为json格式
@RestController
@RequestMapping("/user")
@Validated
@Slf4j
//@Validated 注解用于验证 User 对象的属性是否满足定义的约束条件
//@Pattern代码校验
public class UserController {
    //将UserService类型的bean自动注入到当前类的userService字段
    //直接注入bean对象，不用new对象创建实例
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //注册代码
    @PostMapping("/register")
    public Result register(
            @RequestParam("username")
            @Pattern(regexp = "^\\S{1,16}$", message = "用户名不能为空")
            String username,
            @RequestParam("password")
            @Pattern(regexp = "^\\S{1,16}$",message = "密码不能为空")
            String password) {
        User u = userService.findByUsername(username);
        if (u == null) {
            //没有占用就注册
            userService.register(username, password);
            return Result.success("注册成功");
        } else {
            return Result.error("用户名已被占用");
        }
    }

    //登录代码
    @PostMapping("/login")
    public Result login(
            @RequestParam("username")
            @Pattern(regexp = "^\\S{1,16}$", message = "用户名不能为空")
            String username,
            @RequestParam("password")
            @Pattern(regexp = "^\\S{1,16}$",message = "密码不能为空")
            String password) {

        //查找是否存在用户
        User loginUser = userService.findByUsername(username);
        if (loginUser == null) {
            return Result.error("用户不存在");
        } else
        //用户存在
        {
            if (Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
                //生成令牌代码
                Map<String, Object> claims = new HashMap<>();
                claims.put("id", loginUser.getId());
                claims.put("username", loginUser.getUsername());
                String token = JwtUtil.genToken(claims);
                //把token存储到redis中
                //直接调用ValueOperations对象操作redis，减少了对redis序列化的配置
                ValueOperations<String, String> operations= stringRedisTemplate.opsForValue();
                operations.set(token,token,2, TimeUnit.HOURS);//键值设为id，token过期时间
                return Result.success(token);
            }
            return Result.error("密码错误");
        }
    }

    @PreAuthorize("/user/info")
    @GetMapping("/info")
    public Result<User> userInfo(/*@RequestHeader(name = "Authorization") String token*/) {
        //根据用户名查询用户
           /* Map<String, Object> map = JwtUtil.parseToken(token);
            String username = (String) map.get("username");*/
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User user = userService.findByUsername(username);
        return Result.success(user);
    }

    //更新用户信息
    @PreAuthorize("/user/updates")
    @PutMapping("/updates")
    public Result updateUser(@RequestBody @Validated User user){
        userService.update(user);
        return Result.success();
    }

    //更新用户头像
    //@RequestParam主要用于从 HTTP 请求中获取参数值，并将其转换为方法参数的合适类型
    @PreAuthorize("/user/updateAvatar")
    @PostMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam String avatarUrl){
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    //更新用户密码
    @PreAuthorize("/user/updatePwd")
    @PostMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params,@RequestHeader("Authorization") String token){
        //1.校验参数
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        //判断是否为空
        if(!StringUtils.hasLength(oldPwd)||!StringUtils.hasLength(newPwd)||!StringUtils.hasLength(rePwd)){
            return Result.error("请完整填写表单内容");
        }
        //原密码是否正确
        //调用service根据用户名查询用户原密码，再和传入的旧密码进行比较
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User loginUser = userService.findByUsername(username);
        //此密码已加密
        if (!loginUser.getPassword().equals(Md5Util.getMD5String(oldPwd))){
            return Result.error("原密码填写不正确");
        }

        //判断新密码和确认密码是否一致
        if (!newPwd.equals(rePwd)){
            return Result.error("新密码和确认密码不一致");
        }
        //2.调用service完成密码更新
        userService.updatePwd(newPwd);
        //删除redis中对应的token
        ValueOperations<String, String> operations= stringRedisTemplate.opsForValue();
        operations.getOperations().delete(token);
        return Result.success();
    }


    //用户管理
    @PreAuthorize("/user/userList")
    @GetMapping("/userList")
    public Result<PageBean<User>> userList(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email
    ){
        PageBean<User> pb =userService.userList(pageNum,pageSize,username,email);
        return Result.success(pb);

    }

    //删除用户
    @PreAuthorize("/user/{id}")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        userService.delete(id);
        return Result.success();
    }

    //所有用户列表
    @PreAuthorize("/user/allUserList")
    @GetMapping("/allUserList")
    public Result<List<User>> allUserList(){
        List<User> list = userService.allUserList();
        return Result.success(list);
    }
}

/*
* UserService 是一个接口，其中定义了 getUserName 方法。
UserServiceImpl 类实现了 UserService 接口，并且使用 @Service 注解将其注册为 Spring 服务。
UserController 类使用 @Autowired 注解自动注入 UserService 实例，在 getUserName 方法里调用 UserService 的 getUserName 方法。
* */