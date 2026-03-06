package com.upc.auth.controller;

import com.upc.auth.service.AuthService;
import com.upc.common.entity.Users;
import com.upc.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "登录注册接口")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Users user) {
        return authService.login(user.getUsername(), user.getPassword());
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody Users user) {
        return authService.register(user);
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail(401, "无效的令牌");
        }
        String token = authHeader.substring(7);
        return authService.refreshToken(token);
    }

    @Operation(summary = "验证令牌")
    @GetMapping("/verify")
    public Result<Boolean> verifyToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.success(false);
        }
        String token = authHeader.substring(7);
        return Result.success(authService.verifyToken(token));
    }
}
