package com.upc.bookmanagement.controller;

import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.FaceVerificationRequest;
import com.upc.bookmanagement.entity.Users;
import com.upc.bookmanagement.service.FaceRecognitionService;
import com.upc.bookmanagement.service.UsersService;
import com.upc.bookmanagement.service.impl.BaiduAiFaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.github.pagehelper.PageInfo;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/auth/face")
@Tag(name = "人脸识别认证", description = "人脸识别登录相关接口")
public class FaceAuthController {
    
    private final UsersService usersService;
    private final FaceRecognitionService faceRecognitionService;
    private final BaiduAiFaceService baiduAiFaceService;
    
    private static final Logger log = LoggerFactory.getLogger(FaceAuthController.class);
    
    @Operation(summary = "人脸注册 - 注册到百度云人脸库")
    @PostMapping("/register")
    public Result<String> registerFace(@RequestBody FaceVerificationRequest request) {
        try {
            // 查找用户
            Users user = usersService.findByUsername(request.getUsername());
            if (user == null) {
                return Result.fail(404, "用户不存在");
            }
            
            // 注册人脸到百度云人脸库
            boolean success = faceRecognitionService.registerFace(user, request.getFaceData());
            if (success) {
                return Result.success("人脸注册成功，已保存到百度云人脸库");
            } else {
                return Result.fail(500, "人脸注册失败");
            }
        } catch (Exception e) {
            log.error("人脸注册异常: {}", e.getMessage(), e);
            return Result.fail(500, "人脸注册异常: " + e.getMessage());
        }
    }
    
    @Operation(summary = "人脸识别登录 - 仅通过人脸识别进行验证")
    @PostMapping("/login")
    public Result<Map<String, Object>> faceLogin(@RequestBody FaceVerificationRequest request) {
        try {
            // 验证人脸数据是否提供
            if (request.getFaceData() == null || request.getFaceData().isEmpty()) {
                return Result.fail(400, "人脸数据不能为空");
            }
            
            // 使用百度云人脸库进行搜索
            Map<String, Object> searchResult = faceRecognitionService.searchFaceInCloud(request.getFaceData());
            
            boolean found = Boolean.TRUE.equals(searchResult.get("found"));
            if (found) {
                // 获取匹配到的用户信息
                String userId = (String) searchResult.get("userId");
                Double score = (Double) searchResult.get("score");
                
                String token = null;
                Users user = null;
                
                // 查找用户 - 通过userId进行匹配（百度云库中的用户ID与本地数据库的user_id相同）
                try {
                    Integer intUserId = Integer.parseInt(userId);
                    // 创建查询对象
                    Users queryUser = new Users();
                    queryUser.setUserId(intUserId);
                    
                    // 使用条件查询方法，设置分页为1获取单个用户
                    PageInfo<Users> usersPage = usersService.findUsersBy(queryUser, 1, 1);
                    if (usersPage != null && !usersPage.getList().isEmpty()) {
                        user = usersPage.getList().get(0);
                    }
                    
                    // 如果用户不存在，自动创建新用户
                    if (user == null) {
                        user = new Users();
                        user.setUserId(intUserId);
                        user.setUsername("人脸识别用户_" + intUserId);
                        user.setRealName("人脸识别用户");
                        user.setPassword("123456"); // 设置默认密码
                        user.setRole(0); // 设置默认角色为普通读者(0)
                        
                        // 保存新用户
                        boolean result = usersService.addUsers(user);
                        log.info("自动创建人脸识别用户，用户ID: {}, 结果: {}", intUserId, result);
                    }
                    
                    // 生成token
                    token = UUID.randomUUID().toString();
                } catch (NumberFormatException e) {
                    log.error("百度云用户ID格式错误: {}", userId, e);
                    return Result.fail(400, "用户ID格式错误");
                } catch (Exception e) {
                    log.error("人脸识别登录过程中发生错误", e);
                    return Result.fail(500, "系统内部错误");
                }
                
                // 构建返回数据
                Map<String, Object> result = new HashMap<>();
                result.put("token", token);
                result.put("user", user);
                result.put("authType", "cloud_face_search");
                result.put("score", score);
                
                return Result.success(result);
            } else {
                return Result.fail(401, "人脸识别失败，未找到匹配的用户");
            }
        } catch (Exception e) {
            log.error("人脸识别异常: {}", e.getMessage(), e);
            return Result.fail(500, "人脸识别异常: " + e.getMessage());
        }
    }
    
    @Operation(summary = "人脸实名认证登录 - 仅使用姓名、身份证号和人脸进行认证")
    @PostMapping("/login/real-name")
    public Result<Map<String, Object>> realNameFaceLogin(@RequestBody FaceVerificationRequest request) {
        try {
            // 验证必要参数
            if (request.getRealName() == null || request.getRealName().isEmpty() ||
                request.getIdCardNumber() == null || request.getIdCardNumber().isEmpty()) {
                return Result.fail(400, "姓名和身份证号不能为空");
            }
            
            // 调用人脸实名认证接口
            Map<String, Object> authResult = baiduAiFaceService.authenticateFace(
                    request.getFaceData(), request.getRealName(), request.getIdCardNumber());
            
            boolean success = Boolean.TRUE.equals(authResult.get("success"));
            if (success) {
                // 实名认证成功后，查找或创建用户
                Users user = findOrCreateUserByRealName(request.getRealName());
                
                // 生成token
                String token = UUID.randomUUID().toString();
                
                // 构建返回数据
                Map<String, Object> result = new HashMap<>();
                result.put("token", token);
                result.put("user", user);
                result.put("score", authResult.get("score"));
                result.put("message", authResult.get("message"));
                
                return Result.success(result);
            } else {
                String errorMsg = (String) authResult.get("errorMsg");
                return Result.fail(401, errorMsg != null ? errorMsg : "人脸实名认证失败");
            }
        } catch (Exception e) {
            log.error("人脸实名认证异常: {}", e.getMessage(), e);
            return Result.fail(500, "人脸实名认证异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据真实姓名查找或创建用户
     * 注意：实际项目中可能需要更复杂的逻辑，如验证用户是否已存在等
     */
    private Users findOrCreateUserByRealName(String realName) {
        // 这里简化处理，实际项目中应该根据业务需求实现
        // 例如：先尝试查找用户，如果不存在则创建新用户
        Users user = new Users();
        user.setUsername(realName);
        user.setRealName(realName);
        return user;
    }
    
    @Operation(summary = "更新人脸数据 - 更新百度云人脸库")
    @PostMapping("/update")
    public Result<String> updateFace(@RequestBody FaceVerificationRequest request) {
        try {
            // 查找用户
            Users user = usersService.findByUsername(request.getUsername());
            if (user == null) {
                return Result.fail(404, "用户不存在");
            }
            
            // 更新人脸数据到百度云人脸库
            boolean success = faceRecognitionService.updateFaceData(user, request.getFaceData());
            if (success) {
                return Result.success("人脸数据更新成功，已同步到百度云人脸库");
            } else {
                return Result.fail(500, "人脸数据更新失败");
            }
        } catch (Exception e) {
            log.error("人脸数据更新异常: {}", e.getMessage(), e);
            return Result.fail(500, "人脸数据更新异常: " + e.getMessage());
        }
    }
    
    @Operation(summary = "删除人脸数据 - 同时删除百度云人脸库中的数据")
    @DeleteMapping("/delete/{userId}")
    public Result<String> deleteFace(@PathVariable Integer userId) {
        try {
            // 获取用户信息 - 通过条件查询获取用户
            // 由于UsersService没有直接的findById方法，我们通过条件查询获取用户
            // 创建一个查询对象
            Users queryUser = new Users();
            queryUser.setUserId(userId);
            
            // 使用条件查询方法，设置分页为1获取单个用户
            PageInfo<Users> usersPage = usersService.findUsersBy(queryUser, 1, 1);
            Users user = null;
            if (usersPage != null && !usersPage.getList().isEmpty()) {
                user = usersPage.getList().get(0);
            }
            if (user == null) {
                return Result.fail(404, "用户不存在");
            }
            
            // 先从百度云人脸库删除人脸数据
            boolean cloudDeleteSuccess = baiduAiFaceService.deleteFaceFromCloud(String.valueOf(userId));
            log.info("百度云人脸库删除结果: {}", cloudDeleteSuccess ? "成功" : "失败");
            
            // 然后更新本地用户信息，清除人脸数据
            user.setFaceFeatures(null);
            user.setFaceImage(null);
            boolean dbUpdateSuccess = usersService.updateUsers(user);
            
            if (dbUpdateSuccess) {
                log.info("人脸数据删除成功，用户ID: {}, 百度云人脸库状态: {}", 
                        userId, cloudDeleteSuccess ? "已删除" : "可能未存在");
                return Result.success("人脸数据删除成功，已从百度云人脸库中移除");
            } else {
                return Result.fail(500, "人脸数据删除失败：数据库更新失败");
            }
        } catch (Exception e) {
            log.error("人脸数据删除异常: {}", e.getMessage(), e);
            return Result.fail(500, "人脸数据删除异常: " + e.getMessage());
        }
    }
    
    @Operation(summary = "验证人脸库连接状态")
    @GetMapping("/test-connection")
    public Result<String> testConnection() {
        log.info("测试百度云人脸库连接状态");
        
        try {
            // 尝试获取accessToken来验证连接
            String accessToken = baiduAiFaceService.getAccessToken();
            if (accessToken != null && !accessToken.isEmpty()) {
                return Result.success("百度云人脸库连接正常");
            } else {
                return Result.fail(500, "无法获取百度云人脸库访问令牌");
            }
        } catch (Exception e) {
            log.error("测试百度云人脸库连接异常: {}", e.getMessage(), e);
            return Result.fail(500, "百度云人脸库连接失败：" + e.getMessage());
        }
    }
}