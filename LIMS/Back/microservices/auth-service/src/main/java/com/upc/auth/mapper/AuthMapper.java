package com.upc.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.common.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 认证Mapper接口
 */
@Mapper
public interface AuthMapper extends BaseMapper<Users> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM auth_user WHERE username = #{username}")
    Users findByUsername(@Param("username") String username);
}
