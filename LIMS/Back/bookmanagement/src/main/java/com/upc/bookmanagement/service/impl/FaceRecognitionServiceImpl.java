package com.upc.bookmanagement.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.upc.bookmanagement.entity.Users;
import com.upc.bookmanagement.service.FaceRecognitionService;
import com.upc.bookmanagement.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 人脸识别服务实现类
 * 结合百度云人脸库和本地数据库管理人脸信息
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private final BaiduAiFaceService baiduAiFaceService;
    private final UsersService usersService;
    
    // 人脸库组ID常量
    private static final String FACE_GROUP_ID = "book_system_users";

    /**
     * 提取人脸特征
     * 使用百度云人脸库获取face_token并构建特征引用数据
     */
    @Override
    public byte[] extractFaceFeatures(String faceImageData) {
        return baiduAiFaceService.extractFaceFeatures(faceImageData);
    }

    /**
     * 验证人脸特征是否匹配
     * 使用百度云人脸库的搜索功能进行验证
     */
    @Override
    public boolean verifyFace(String faceImageData, byte[] storedFaceFeatures) {
        return baiduAiFaceService.verifyFace(faceImageData, storedFaceFeatures);
    }

    /**
     * 注册新人脸
     * 1. 清理用户已有的人脸数据
     * 2. 注册到百度云人脸库
     * 3. 更新本地数据库中的人脸引用信息
     */
    @Override
    public boolean registerFace(Users user, String faceImageData) {
        try {
            log.info("开始注册用户[{}]人脸信息到百度云人脸库", user.getUsername());

            // 首先清理用户已有的人脸数据
            boolean cleared = usersService.clearFaceData(user.getUserId());
            if (!cleared) {
                log.warn("清理用户[{}]现有人脸数据失败", user.getUsername());
            }

            // 生成用户在百度云人脸库中的唯一标识（使用用户ID或用户名）
            String userId = String.valueOf(user.getUserId());
            String userName = user.getUsername();

            // 注册到百度云人脸库
            boolean cloudRegistered = baiduAiFaceService.registerFaceToCloud(userId, userName, faceImageData);
            if (!cloudRegistered) {
                log.error("百度云人脸库注册失败，用户ID: {}", userId);
                return false;
            }

            // 构建人脸库引用数据，不存储实际的人脸图像，只存储引用信息
            JSONObject faceReferenceData = new JSONObject();
            faceReferenceData.put("user_id", userId);
            faceReferenceData.put("use_cloud_face_lib", true);
            faceReferenceData.put("registered_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            faceReferenceData.put("face_group_id", FACE_GROUP_ID);

            // 更新用户信息，存储人脸引用数据
            user.setFaceFeatures(faceReferenceData.toJSONString().getBytes());
            user.setFaceImage(null); // 不再存储实际的人脸图像BLOB

            boolean updated = usersService.updateUsers(user);
            if (!updated) {
                log.error("更新用户[{}]人脸引用信息失败，尝试从百度云人脸库删除已注册的人脸", user.getUsername());
                // 如果本地数据库更新失败，尝试从百度云人脸库删除已注册的人脸
                baiduAiFaceService.deleteFaceFromCloud(userId);
                return false;
            }

            log.info("用户[{}]人脸注册成功，已注册到百度云人脸库并更新本地引用信息", user.getUsername());
            return true;
        } catch (Exception e) {
            log.error("人脸注册异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新用户人脸数据
     * 1. 清理用户已有的人脸数据
     * 2. 更新百度云人脸库
     * 3. 更新本地数据库中的人脸引用信息
     */
    @Override
    public boolean updateFaceData(Users user, String faceImageData) {
        try {
            log.info("开始更新用户[{}]人脸信息到百度云人脸库", user.getUsername());

            // 生成用户在百度云人脸库中的唯一标识
            String userId = String.valueOf(user.getUserId());

            // 尝试更新百度云人脸库中的人脸信息
            boolean cloudUpdated = baiduAiFaceService.updateFaceInCloud(userId, faceImageData);
            
            // 如果更新失败，可能是用户未在人脸库中注册，尝试进行注册
            if (!cloudUpdated) {
                log.info("百度云人脸库更新失败，尝试注册用户[{}]人脸信息", user.getUsername());
                cloudUpdated = baiduAiFaceService.registerFaceToCloud(userId, user.getUsername(), faceImageData);
            }

            if (!cloudUpdated) {
                log.error("百度云人脸库更新/注册失败，用户ID: {}", userId);
                return false;
            }

            // 构建更新后的人脸库引用数据
            JSONObject faceReferenceData = new JSONObject();
            faceReferenceData.put("user_id", userId);
            faceReferenceData.put("use_cloud_face_lib", true);
            faceReferenceData.put("updated_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            faceReferenceData.put("face_group_id", FACE_GROUP_ID);

            // 更新用户信息，存储更新后的人脸引用数据
            user.setFaceFeatures(faceReferenceData.toJSONString().getBytes());
            user.setFaceImage(null); // 确保不存储实际的人脸图像BLOB

            boolean updated = usersService.updateUsers(user);
            if (!updated) {
                log.error("更新用户[{}]人脸引用信息失败", user.getUsername());
                return false;
            }

            log.info("用户[{}]人脸数据更新成功，已同步到百度云人脸库并更新本地引用信息", user.getUsername());
            return true;
        } catch (Exception e) {
            log.error("人脸数据更新异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> authenticateFace(String faceImageData, String realName, String idCardNumber) {
        try {
            log.info("开始进行人脸实名认证，用户姓名: {}", realName);
            
            // 直接调用百度云人脸服务进行实名认证
            Map<String, Object> result = baiduAiFaceService.authenticateFace(faceImageData, realName, idCardNumber);
            
            log.info("人脸实名认证完成，结果: {}", result.get("success"));
            return result;
        } catch (Exception e) {
            log.error("人脸实名认证异常: {}", e.getMessage(), e);
            
            // 返回认证失败结果
            Map<String, Object> errorResult = new JSONObject();
            errorResult.put("success", false);
            errorResult.put("errorMsg", "认证过程发生异常: " + e.getMessage());
            return errorResult;
        }
    }
    
    @Override
    public Map<String, Object> searchFaceInCloud(String faceImageData) {
        try {
            log.info("开始在百度云人脸库中搜索人脸");
            
            // 直接调用百度云人脸服务在人脸库中搜索
            Map<String, Object> result = baiduAiFaceService.searchFaceInCloud(faceImageData, null);
            
            log.info("人脸搜索完成，是否找到匹配: {}", result.get("found"));
            return result;
        } catch (Exception e) {
            log.error("百度云人脸库搜索异常: {}", e.getMessage(), e);
            
            // 返回搜索失败结果
            Map<String, Object> errorResult = new JSONObject();
            errorResult.put("found", false);
            errorResult.put("errorMsg", "搜索过程发生异常: " + e.getMessage());
            return errorResult;
        }
    }
}