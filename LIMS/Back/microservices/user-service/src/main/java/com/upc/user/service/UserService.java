package com.upc.user.service;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Users;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 查询所有用户
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 用户分页列表
     */
    PageInfo<Users> findAllUsers(Integer pageNum, Integer pageSize);

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    Users getUserById(Integer userId);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Users getUserByUsername(String username);

    /**
     * 条件查询用户
     *
     * @param users    查询条件
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 用户分页列表
     */
    PageInfo<Users> findUsersBy(Users users, Integer pageNum, Integer pageSize);

    /**
     * 添加用户
     *
     * @param users 用户信息
     * @return 是否成功
     */
    boolean addUsers(Users users);

    /**
     * 更新用户
     *
     * @param users 用户信息
     * @return 是否成功
     */
    boolean updateUsers(Users users);

    /**
     * 更新用户密码
     *
     * @param users 用户信息（包含新密码）
     * @return 是否成功
     */
    boolean updatePassword(Users users);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUsers(Integer userId);

    /**
     * 验证用户是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean checkUserExists(Integer userId);

    /**
     * 更新用户人脸数据
     *
     * @param userId       用户ID
     * @param faceData     人脸特征数据
     * @param faceImageUrl 人脸图片URL
     * @return 是否成功
     */
    boolean updateFaceData(Integer userId, String faceData, String faceImageUrl);

    /**
     * 清除用户人脸数据
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean clearFaceData(Integer userId);
}
