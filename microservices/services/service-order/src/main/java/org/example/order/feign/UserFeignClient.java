package org.example.order.feign;

import org.example.common.Result;
import org.example.user.POJO.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * service-user 的 Feign 客户端
 * 替代原有的 UserMapper 直接数据库访问
 */
@FeignClient(name = "service-user")
public interface UserFeignClient {

    @GetMapping("/user/internal/{id}")
    Result<User> getById(@PathVariable("id") Integer id);

    @PostMapping("/user/internal/listByIds")
    Result<List<User>> getByIds(@RequestBody List<Integer> ids);
}
