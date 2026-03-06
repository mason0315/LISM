package com.upc.user.controller;

import com.github.pagehelper.PageInfo;
import com.upc.common.entity.Users;
import com.upc.common.result.Result;
import com.upc.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息管理接口")
public class UserController {

    private final UserService userService;

    @Operation(summary = "查询所有用户")
    @GetMapping("/AllUsers")
    public Result<PageInfo<Users>> findAllUsers(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        PageInfo<Users> usersList = userService.findAllUsers(pageNum, pageSize);
        return Result.success(usersList);
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{userId}")
    public Result<Users> getUserById(@PathVariable Integer userId) {
        Users user = userService.getUserById(userId);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.fail(404, "用户不存在");
        }
    }

    @Operation(summary = "根据用户名查询用户")
    @GetMapping("/username/{username}")
    public Result<Users> getUserByUsername(@PathVariable String username) {
        Users user = userService.getUserByUsername(username);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.fail(404, "用户不存在");
        }
    }

    @Operation(summary = "条件查询用户")
    @PostMapping("/UsersBy")
    public Result<PageInfo<Users>> findUsersBy(
            @RequestBody Users users,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        PageInfo<Users> usersList = userService.findUsersBy(users, pageNum, pageSize);
        return Result.success(usersList);
    }

    @Operation(summary = "添加用户")
    @PostMapping("/addUsers")
    public Result<String> addUsers(@RequestBody Users users) {
        boolean result = userService.addUsers(users);
        if (result) {
            return Result.success("添加成功");
        } else {
            return Result.fail(500, "添加失败");
        }
    }

    @Operation(summary = "更新用户")
    @PostMapping("/updateUsers")
    public Result<String> updateUsers(@RequestBody Users users) {
        boolean result = userService.updateUsers(users);
        if (result) {
            return Result.success("更新成功");
        } else {
            return Result.fail(500, "更新失败");
        }
    }

    @Operation(summary = "更新用户密码")
    @PostMapping("/updatepa")
    public Result<String> updatePassword(@RequestBody Users users) {
        boolean result = userService.updatePassword(users);
        if (result) {
            return Result.success("密码更新成功");
        } else {
            return Result.fail(500, "密码更新失败");
        }
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/deleteUser/{userId}")
    public Result<String> deleteUsers(@PathVariable Integer userId) {
        boolean result = userService.deleteUsers(userId);
        if (result) {
            return Result.success("删除成功");
        } else {
            return Result.fail(500, "删除失败");
        }
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<Users> getCurrentUser(@RequestParam String username) {
        Users user = userService.getUserByUsername(username);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.fail(404, "用户不存在");
        }
    }

    @Operation(summary = "验证用户是否存在")
    @GetMapping("/exists/{userId}")
    public Result<Boolean> checkUserExists(@PathVariable Integer userId) {
        boolean exists = userService.checkUserExists(userId);
        return Result.success(exists);
    }

    @Operation(summary = "更新用户人脸数据")
    @PostMapping("/face/update/{userId}")
    public Result<String> updateFaceData(
            @PathVariable Integer userId,
            @RequestParam String faceData,
            @RequestParam String faceImageUrl) {
        boolean result = userService.updateFaceData(userId, faceData, faceImageUrl);
        if (result) {
            return Result.success("人脸数据更新成功");
        } else {
            return Result.fail(500, "人脸数据更新失败");
        }
    }

    @Operation(summary = "清除用户人脸数据")
    @DeleteMapping("/face/clear/{userId}")
    public Result<String> clearFaceData(@PathVariable Integer userId) {
        boolean result = userService.clearFaceData(userId);
        if (result) {
            return Result.success("人脸数据清除成功");
        } else {
            return Result.fail(500, "人脸数据清除失败");
        }
    }
}
