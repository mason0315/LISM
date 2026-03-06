package com.upc.auth.service.impl;

import com.upc.auth.mapper.AuthMapper;
import com.upc.auth.service.AuthService;
import com.upc.common.entity.Users;
import com.upc.common.result.Result;
import com.upc.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Result<Map<String, Object>> login(String username, String password) {
        // 查询用户
        Users user = authMapper.findByUsername(username);
        if (user == null) {
            log.warn("登录失败：用户不存在 - {}", username);
            return Result.fail(401, "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败：密码错误 - {}", username);
            return Result.fail(401, "用户名或密码错误");
        }

        // 生成JWT令牌
        String token = JwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        log.info("用户登录成功：{}", username);
        return Result.success("登录成功", result);
    }

    @Override
    public Result<String> register(Users user) {
        // 检查用户名是否已存在
        Users existUser = authMapper.findByUsername(user.getUsername());
        if (existUser != null) {
            return Result.fail(400, "用户名已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 设置默认角色为普通用户
        if (user.getRole() == null) {
            user.setRole(1);
        }

        // 设置创建时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        user.setCreateTime(now);
        user.setUpdateTime(now);

        // 保存用户
        int result = authMapper.insert(user);
        if (result > 0) {
            log.info("用户注册成功：{}", user.getUsername());
            return Result.success("注册成功");
        } else {
            return Result.fail(500, "注册失败");
        }
    }

    @Override
    public Result<Map<String, Object>> refreshToken(String token) {
        try {
            // 验证原令牌
            if (!JwtUtil.validateToken(token)) {
                return Result.fail(401, "令牌无效");
            }

            // 刷新令牌
            String newToken = JwtUtil.refreshToken(token);

            // 获取用户信息
            Integer userId = JwtUtil.getUserId(token);
            Users user = authMapper.selectById(userId);

            if (user == null) {
                return Result.fail(401, "用户不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("user", user);

            return Result.success("刷新成功", result);
        } catch (Exception e) {
            log.error("刷新令牌失败：{}", e.getMessage());
            return Result.fail(401, "令牌刷新失败");
        }
    }

    @Override
    public boolean verifyToken(String token) {
        return JwtUtil.validateToken(token);
    }
}
