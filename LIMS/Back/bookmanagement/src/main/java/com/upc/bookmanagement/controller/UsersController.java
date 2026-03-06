package com.upc.bookmanagement.controller;

import com.github.pagehelper.PageInfo;
import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.upc.bookmanagement.service.UsersService;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-13
 */

@RestController
@CrossOrigin(
        origins = "http://localhost:5173", // 前端实际运行的地址（如 Vue 项目默认 5173）
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, // 允许所有常用方法
        allowCredentials = "true" // 允许携带 cookie/token（关键！否则前端无法传递 token）
)
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "用户管理",description = "图书管理接口")
public class UsersController {

    private final UsersService usersService;

    @Operation(summary = "查询所有用户")
    @GetMapping("/AllUsers")
    public Result<PageInfo<Users>> findAllUsers(@RequestParam Integer pageNum, @RequestParam Integer pageSize){
        PageInfo<Users> usersList = usersService.findAllUsers(pageNum, pageSize);
        return Result.success(usersList);
    }

    @Operation(summary = "条件查询用户")
    @PostMapping("/UsersBy")
    public Result<PageInfo<Users>> findUsersBy(@RequestBody Users users, @RequestParam Integer pageNum, @RequestParam Integer pageSize){
        PageInfo<Users> usersList = usersService.findUsersBy(users, pageNum, pageSize);
        return Result.success(usersList);
    }

    @Operation(summary = "添加用户")
    @PostMapping("/addUsers")
    public Result<String> addUsers(@RequestBody Users users){
        boolean result = usersService.addUsers(users);
        if(result){
            return Result.success("添加成功");
        }else{
            return Result.fail(500, "添加失败");
        }
    }
    @Operation(summary = "更新用户")
    @PostMapping("/updateUsers")
    public Result<String> updateUsers(@RequestBody Users users){
        // 新增：校验userId是否为空
        System.out.println("接收的用户信息：" + users);

        boolean result = usersService.updateUsers(users);
        if(result){
            return Result.success("更新成功");
        }else{
            return Result.fail(500, "更新失败");
        }
    }

    @Operation(summary = "更新用户密码")
    @PostMapping("/updatepa")
    public Result<String> updatepa(@RequestBody Users users){
        boolean result = usersService.updatepa(users);
        if(result){
            return Result.success("更新成功");
        }
        else{
            return Result.fail(500, "更新失败");
        }
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/deleteUser/{userId}")
    public Result<String> deleteUsers(@PathVariable Integer userId){
        boolean result = usersService.deleteUsers(userId);
        if(result){
            return Result.success("删除成功");
        }else{
            return Result.fail(500, "删除失败");
        }
    }

    // 新增：根据用户名获取用户详情（用于用户中心）
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<Users> getCurrentUser(@RequestParam String username) {
        Users user = usersService.findByUsername(username); // 复用之前的根据用户名查询方法
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.fail(404, "用户不存在");
        }
    }




}

