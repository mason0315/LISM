package com.upc.auth.service;

import com.upc.common.entity.Users;
import com.upc.common.result.Result;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（包含JWT令牌）
     */
    Result<Map<String, Object>> login(String username, String password);

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    Result<String> register(Users user);

    /**
     * 刷新令牌
     *
     * @param token 原令牌
     * @return 新令牌
     */
    Result<Map<String, Object>> refreshToken(String token);

    /**
     * 验证令牌
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    boolean verifyToken(String token);
}
