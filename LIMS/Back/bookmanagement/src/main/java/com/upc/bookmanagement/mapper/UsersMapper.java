package com.upc.bookmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upc.bookmanagement.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UsersMapper extends BaseMapper<Users> {

    List<Users> findAllUsers();

    List<Users> findUsersBy(Users users);

    int updatepa(Users  users);
    Users findByUsername(@Param("username") String username);
    
    int clearFaceData(Integer userId);
}