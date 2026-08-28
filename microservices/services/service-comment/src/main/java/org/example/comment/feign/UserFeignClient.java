package org.example.comment.feign;

import org.example.common.Result;
import org.example.user.POJO.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-user")
public interface UserFeignClient {

    @GetMapping("/user/internal/{id}")
    Result<User> getUserById(@PathVariable("id") Integer id);
}
