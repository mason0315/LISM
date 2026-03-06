package com.upc.common.feign;

import com.upc.common.entity.Users;
import com.upc.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "user-service", path = "/users")
public interface UserFeignClient {

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{userId}")
    Result<Users> getUserById(@PathVariable("userId") Integer userId);

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/username/{username}")
    Result<Users> getUserByUsername(@PathVariable("username") String username);

    /**
     * 验证用户是否存在
     */
    @GetMapping("/exists/{userId}")
    Result<Boolean> checkUserExists(@PathVariable("userId") Integer userId);
}
