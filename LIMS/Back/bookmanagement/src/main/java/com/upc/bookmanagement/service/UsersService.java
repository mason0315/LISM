package com.upc.bookmanagement.service;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Users;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-13
 */
public interface UsersService {

    /**
     * 管理员功能
     * 查询所有用户
     */
    PageInfo<Users> findAllUsers(Integer pageNum, Integer pageSize);

    /**
     * 管理员功能
     * 条件查询用户
     */
    PageInfo<Users> findUsersBy(Users users, Integer pageNum, Integer pageSize);

    /**
     * 用户功能
     * 添加用户
     */
    boolean addUsers(Users users);

    /**
     * 用户、管理员功能
     * 更新用户
     */
    boolean updateUsers(Users users);

    /**
     * 用户功能
     * 删除用户(注销账户)
     */
    boolean deleteUsers(Integer userId);


    Users findByUsername(String username);
    boolean register(Users user);

    boolean updatepa(Users  users);
    
    /**
     * 清除用户的人脸数据
     */
    boolean clearFaceData(Integer userId);

}