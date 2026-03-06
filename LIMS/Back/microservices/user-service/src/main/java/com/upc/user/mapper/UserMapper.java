package com.upc.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<Users> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM user_info WHERE username = #{username}")
    Users findByUsername(@Param("username") String username);

    /**
     * 条件查询用户
     *
     * @param users 查询条件
     * @return 用户列表
     */
    List<Users> findUsersByCondition(@Param("users") Users users);

    /**
     * 更新用户密码
     *
     * @param userId   用户ID
     * @param password 新密码
     * @return 影响行数
     */
    @Update("UPDATE user_info SET password = #{password}, update_time = NOW() WHERE user_id = #{userId}")
    int updatePassword(@Param("userId") Integer userId, @Param("password") String password);

    /**
     * 更新用户人脸数据
     *
     * @param userId       用户ID
     * @param faceData     人脸特征数据
     * @param faceImageUrl 人脸图片URL
     * @return 影响行数
     */
    @Update("UPDATE user_info SET face_data = #{faceData}, face_image_url = #{faceImageUrl}, update_time = NOW() WHERE user_id = #{userId}")
    int updateFaceData(@Param("userId") Integer userId, @Param("faceData") String faceData, @Param("faceImageUrl") String faceImageUrl);

    /**
     * 清除用户人脸数据
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE user_info SET face_data = NULL, face_image_url = NULL, update_time = NOW() WHERE user_id = #{userId}")
    int clearFaceData(@Param("userId") Integer userId);
}
