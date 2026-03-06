package com.upc.user.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Users;
import com.upc.user.mapper.UserMapper;
import com.upc.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public PageInfo<Users> findAllUsers(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Users> users = userMapper.selectList(null);
        return new PageInfo<>(users);
    }

    @Override
    public Users getUserById(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public Users getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public PageInfo<Users> findUsersBy(Users users, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Users> userList = userMapper.findUsersByCondition(users);
        return new PageInfo<>(userList);
    }

    @Override
    public boolean addUsers(Users users) {
        // 检查用户名是否已存在
        Users existUser = userMapper.findByUsername(users.getUsername());
        if (existUser != null) {
            log.warn("添加用户失败：用户名已存在 - {}", users.getUsername());
            return false;
        }

        // 设置创建时间和更新时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        users.setCreateTime(now);
        users.setUpdateTime(now);

        // 设置默认角色
        if (users.getRole() == null) {
            users.setRole(1);
        }

        int result = userMapper.insert(users);
        return result > 0;
    }

    @Override
    public boolean updateUsers(Users users) {
        if (users.getUserId() == null) {
            log.warn("更新用户失败：用户ID为空");
            return false;
        }

        // 设置更新时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        users.setUpdateTime(now);

        int result = userMapper.updateById(users);
        return result > 0;
    }

    @Override
    public boolean updatePassword(Users users) {
        if (users.getUserId() == null || users.getPassword() == null) {
            log.warn("更新密码失败：用户ID或密码为空");
            return false;
        }

        // 设置更新时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        users.setUpdateTime(now);

        int result = userMapper.updatePassword(users.getUserId(), users.getPassword());
        return result > 0;
    }

    @Override
    public boolean deleteUsers(Integer userId) {
        int result = userMapper.deleteById(userId);
        return result > 0;
    }

    @Override
    public boolean checkUserExists(Integer userId) {
        Users user = userMapper.selectById(userId);
        return user != null;
    }

    @Override
    public boolean updateFaceData(Integer userId, String faceData, String faceImageUrl) {
        Users users = new Users();
        users.setUserId(userId);
        users.setFaceData(faceData);
        users.setFaceImageUrl(faceImageUrl);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        users.setUpdateTime(now);

        int result = userMapper.updateFaceData(userId, faceData, faceImageUrl);
        return result > 0;
    }

    @Override
    public boolean clearFaceData(Integer userId) {
        int result = userMapper.clearFaceData(userId);
        return result > 0;
    }
}
