package com.upc.bookmanagement.service.impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.entity.Users;
import com.upc.bookmanagement.mapper.UsersMapper;
import com.upc.bookmanagement.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {
    public final UsersMapper usersMapper;

    @Override
    public PageInfo<Users> findAllUsers(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Users> usersList = usersMapper.findAllUsers();
        return PageInfo.of(usersList);
    }

    @Override
    public PageInfo<Users> findUsersBy(Users users, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Users> usersList = usersMapper.findUsersBy(users);
        return PageInfo.of(usersList);
    }

    @Override
    public boolean addUsers(Users users) {
        return usersMapper.insert(users) > 0;
    }

    @Override
    public boolean updateUsers(Users users) {
        return usersMapper.updateById(users) > 0;
    }

    @Override
    public boolean updatepa(Users  users) {
        int i=usersMapper.updatepa(users);
        return i>0;
    }

    @Override
    public boolean deleteUsers(Integer userId) {
        return usersMapper.deleteById(userId) > 0;
    }

    @Override
    public Users findByUsername(String username) {
        return usersMapper.findByUsername(username);
    }

    @Override
    public boolean register(Users user) {
        user.setRole(0);
        return usersMapper.insert(user) > 0;
    }
    
    @Override
    public boolean clearFaceData(Integer userId) {
        int result = usersMapper.clearFaceData(userId);
        return result > 0;
    }

}