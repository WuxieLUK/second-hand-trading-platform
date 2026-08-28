package org.example.chat.feign;

import org.example.common.Result;
import org.example.user.POJO.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "service-user")
public interface UserFeignClient {

    @GetMapping("/user/internal/{id}")
    Result<User> getUserById(@PathVariable("id") Integer id);

    @PostMapping("/user/internal/listByIds")
    Result<List<User>> getUsersByIds(@RequestBody List<Integer> ids);
}
