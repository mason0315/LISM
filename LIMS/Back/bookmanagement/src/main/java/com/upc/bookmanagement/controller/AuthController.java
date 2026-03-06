package com.upc.bookmanagement.controller;

import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.Users;
import com.upc.bookmanagement.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author pu
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-14
 */

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "用户认证", description = "登录注册接口")

public class AuthController {

    private final UsersService usersService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Users user) {
        // 验证用户名密码
        Users dbUser = usersService.findByUsername(user.getUsername());
        if (dbUser == null || !dbUser.getPassword().equals(user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }
        // 生成token（实际项目建议使用JWT）
        String token = UUID.randomUUID().toString();

        // 临时打印，确认dbUser包含userId
        System.out.println("dbUser.toString(): " + dbUser);
        System.out.println("dbUser.getUserId(): " + dbUser.getUserId());
        System.out.println("dbUser.getUsername(): " + dbUser.getUsername());
        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", dbUser);


        return Result.success(result);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody Users user) {
        // 检查用户名是否已存在
        Users existUser = usersService.findByUsername(user.getUsername());
        if (existUser != null) {
            return Result.fail(400, "用户名已存在");
        }

        // 注册用户（实际项目需加密密码）
        boolean success = usersService.register(user);
        if (success) {
            return Result.success("注册成功");
        } else {
            return Result.fail(500, "注册失败");
        }
    }
    
    @Operation(summary = "清除用户人脸数据")
    @DeleteMapping("/face/clear/{userId}")
    public Result<String> clearFaceData(@PathVariable Integer userId) {
        try {
            boolean success = usersService.clearFaceData(userId);
            if (success) {
                return Result.success("人脸数据清除成功");
            } else {
                return Result.fail(500, "人脸数据清除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "人脸数据清除异常: " + e.getMessage());
        }
    }
}