package com.upc.bookmanagement.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.upc.bookmanagement.config.BaiduAiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 百度AI人脸识别服务实现（接入百度云控制台可视化人脸库）
 * 使用百度云人脸库管理人脸信息，支持人脸注册、搜索和验证
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BaiduAiFaceService {

    private final BaiduAiConfig baiduAiConfig;
    private final RestTemplate restTemplate;
    
    // Access Token缓存
    private String accessToken;
    private long tokenExpireTime = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    // 人脸库组ID（可以在百度云控制台创建）
    private static final String FACE_GROUP_ID = "book_system_users";
    
    /**
     * 身份证号脱敏处理
     * @param idCardNumber 原始身份证号
     * @return 脱敏后的身份证号（显示前3位和后4位）
     */
    private String maskIdCardNumber(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.length() < 8) {
            return idCardNumber;
        }
        return idCardNumber.substring(0, 3) + "********" + 
               idCardNumber.substring(idCardNumber.length() - 4);
    }
    
    /**
     * 获取Access Token
     */
    public String getAccessToken() {
        // 检查token是否过期或不存在
        if (accessToken == null || System.currentTimeMillis() > tokenExpireTime) {
            try {
                lock.lock();
                // 双重检查
                if (accessToken == null || System.currentTimeMillis() > tokenExpireTime) {
                    String url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" + 
                                baiduAiConfig.getApiKey() + "&client_secret=" + baiduAiConfig.getSecretKey();
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    
                    HttpEntity<String> request = new HttpEntity<>(headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                        accessToken = jsonObject.getString("access_token");
                        int expiresIn = jsonObject.getIntValue("expires_in");
                        // 设置过期时间，提前10分钟刷新
                        tokenExpireTime = System.currentTimeMillis() + (expiresIn - 600) * 1000;
                        log.info("获取百度AI Access Token成功，有效期至: {}", tokenExpireTime);
                    }
                }
            } catch (Exception e) {
                log.error("获取百度AI Access Token失败: {}", e.getMessage(), e);
                throw new RuntimeException("获取百度AI Access Token失败", e);
            } finally {
                lock.unlock();
            }
        }
        return accessToken;
    }
    
    /**
     * 检测人脸
     */
    public boolean detectFace(String imageBase64) {
        try {
            String url = baiduAiConfig.getFaceDetectUrl() + "?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("face_field", "faceshape,facetype,beauty");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    JSONObject result = jsonObject.getJSONObject("result");
                    int faceNum = result.getIntValue("face_num");
                    return faceNum > 0;
                } else {
                    log.warn("百度AI人脸检测失败: {}", jsonObject.getString("error_msg"));
                }
            }
            return false;
        } catch (Exception e) {
            log.error("百度AI人脸检测异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 提取人脸特征 - 现在通过百度云人脸库进行管理
     * 不再存储人脸特征到本地数据库，而是使用百度云人脸库的face_token
     */
    public byte[] extractFaceFeatures(String imageBase64) {
        try {
            // 先检测是否有人脸
            if (!detectFace(imageBase64)) {
                log.warn("图像中未检测到人脸");
                return null;
            }
            
            // 使用百度AI人脸检测接口获取人脸特征
            String url = baiduAiConfig.getFaceDetectUrl() + "?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("face_field", "face_token,landmark");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    JSONObject result = jsonObject.getJSONObject("result");
                    JSONArray faceList = result.getJSONArray("face_list");
                    
                    if (faceList != null && faceList.size() > 0) {
                        JSONObject face = faceList.getJSONObject(0);
                        String faceToken = face.getString("face_token");
                        
                        // 创建一个包含face_token的JSON对象，用于标识百度云人脸库中的人脸
                        JSONObject featuresData = new JSONObject();
                        featuresData.put("face_token", faceToken);
                        featuresData.put("use_cloud_face_lib", true);
                        featuresData.put("extracted_time", System.currentTimeMillis());
                        
                        String featuresJson = featuresData.toJSONString();
                        log.info("人脸特征提取成功，获取到百度云face_token: {}", faceToken);
                        return featuresJson.getBytes();
                    }
                } else {
                    log.warn("百度AI人脸特征提取失败: {}, 错误代码: {}", 
                            jsonObject.getString("error_msg"), 
                            jsonObject.getIntValue("error_code"));
                }
            }
            return null;
        } catch (Exception e) {
            log.error("百度AI人脸特征提取异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 人脸实名认证 - 使用百度云人脸实名认证接口与公安数据源比对
     * @param imageBase64 人脸图像Base64编码
     * @param realName 真实姓名
     * @param idCardNumber 身份证号码
     * @return 认证结果，包含是否成功和相似度得分
     */
    public Map<String, Object> authenticateFace(String imageBase64, String realName, String idCardNumber) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证参数
            if (realName == null || realName.isEmpty() || 
                idCardNumber == null || idCardNumber.isEmpty()) {
                log.warn("姓名或身份证号为空");
                result.put("success", false);
                result.put("errorMsg", "姓名和身份证号不能为空");
                return result;
            }
            
            // 使用百度云人脸实名认证接口
            String url = "https://aip.baidubce.com/rest/2.0/face/v3/person/verify?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("name", realName);
            requestBody.put("id_card_number", idCardNumber);
            requestBody.put("quality_control", "NORMAL"); // 质量控制
            requestBody.put("liveness_control", "LOW");  // 活体检测
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    JSONObject verifyResult = jsonObject.getJSONObject("result");
                    double score = verifyResult.getDoubleValue("score");
                    
                    log.info("人脸实名认证成功，姓名: {}, 身份证号: {}, 匹配得分: {}", 
                            realName, maskIdCardNumber(idCardNumber), score);
                    
                    // 相似度大于80分认为匹配成功
                    boolean isSuccess = score > 80;
                    result.put("success", isSuccess);
                    result.put("score", score);
                    result.put("message", isSuccess ? "人脸实名认证成功" : "人脸与身份证信息不匹配");
                } else {
                    String errorMsg = jsonObject.getString("error_msg");
                    int errorCode = jsonObject.getIntValue("error_code");
                    log.warn("百度AI人脸实名认证失败: {}, 错误代码: {}", errorMsg, errorCode);
                    result.put("success", false);
                    result.put("errorMsg", errorMsg);
                    result.put("errorCode", errorCode);
                }
            } else {
                log.warn("百度AI人脸实名认证请求失败，HTTP状态码: {}", response.getStatusCodeValue());
                result.put("success", false);
                result.put("errorMsg", "请求失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("人脸实名认证异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("errorMsg", "认证过程发生异常");
        }
        return result;
    }
    
    /**
     * 验证人脸 - 使用百度云人脸库的搜索功能
     */
    public boolean verifyFace(String imageBase64, byte[] storedFaceFeatures) {
        try {
            // 检查存储的人脸特征是否有效
            if (storedFaceFeatures == null || storedFaceFeatures.length == 0) {
                log.warn("存储的人脸特征为空");
                return false;
            }
            
            // 先检测是否有人脸
            if (!detectFace(imageBase64)) {
                log.warn("图像中未检测到人脸");
                return false;
            }
            
            // 从存储的特征中获取用户信息
            String userId = null;
            try {
                String storedFeaturesJson = new String(storedFaceFeatures, "UTF-8");
                JSONObject storedFeatures = JSONObject.parseObject(storedFeaturesJson);
                userId = storedFeatures.getString("user_id");
            } catch (Exception e) {
                log.warn("无法解析存储的人脸特征: {}", e.getMessage());
            }
            
            // 使用百度云人脸库搜索接口进行验证
            String url = "https://aip.baidubce.com/rest/2.0/face/v3/search?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("group_id_list", FACE_GROUP_ID);
            
            // 如果知道userId，可以指定搜索特定用户
            if (userId != null) {
                requestBody.put("user_id", userId);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                        JSONObject responseResult = jsonObject.getJSONObject("result");
                        JSONArray userList = responseResult.getJSONArray("user_list");
                    
                    if (userList != null && userList.size() > 0) {
                        JSONObject user = userList.getJSONObject(0);
                        double score = user.getDoubleValue("score");
                        String foundUserId = user.getString("user_id");
                        
                        log.info("人脸搜索成功，用户ID: {}, 匹配得分: {}", foundUserId, score);
                        
                        // 如果指定了userId，需要验证找到的用户是否匹配
                        if (userId != null && !userId.equals(foundUserId)) {
                            log.warn("找到的用户ID: {} 与预期的用户ID: {} 不匹配", foundUserId, userId);
                            return false;
                        }
                        
                        // 相似度大于80分认为匹配成功
                        return score > 80;
                    } else {
                        log.warn("在百度云人脸库中未找到匹配的人脸");
                    }
                } else {
                    log.warn("百度AI人脸搜索失败: {}, 错误代码: {}", 
                            jsonObject.getString("error_msg"), 
                            jsonObject.getIntValue("error_code"));
                }
            } else {
                log.warn("百度AI人脸搜索请求失败，HTTP状态码: {}", response.getStatusCodeValue());
            }
            return false;
        } catch (Exception e) {
            log.error("百度AI人脸验证异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 注册人脸到百度云人脸库
     * @param userId 用户ID
     * @param userName 用户名称
     * @param imageBase64 人脸图像Base64编码
     * @return 是否注册成功
     */
    public boolean registerFaceToCloud(String userId, String userName, String imageBase64) {
        try {
            // 先检测是否有人脸
            if (!detectFace(imageBase64)) {
                log.warn("图像中未检测到人脸，注册失败");
                return false;
            }
            
            // 使用百度云人脸库添加用户接口
            String url = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("group_id", FACE_GROUP_ID);
            requestBody.put("user_id", userId);
            requestBody.put("user_info", userName);
            requestBody.put("quality_control", "NORMAL"); // 质量控制级别
            requestBody.put("liveness_control", "LOW");  // 活体检测级别
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    log.info("人脸注册到百度云人脸库成功，用户ID: {}", userId);
                    return true;
                } else {
                    log.warn("百度云人脸库注册失败: {}, 错误代码: {}", 
                            jsonObject.getString("error_msg"), 
                            jsonObject.getIntValue("error_code"));
                    
                    // 如果是用户已存在的错误，可以尝试更新而不是失败
                    if (jsonObject.getIntValue("error_code") == 222207) {
                        log.info("用户已存在，尝试更新人脸信息");
                        return updateFaceInCloud(userId, imageBase64);
                    }
                }
            } else {
                log.warn("百度云人脸库注册请求失败，HTTP状态码: {}", response.getStatusCodeValue());
            }
            return false;
        } catch (Exception e) {
            log.error("百度云人脸库注册异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 更新百度云人脸库中的人脸信息
     * @param userId 用户ID
     * @param imageBase64 新的人脸图像Base64编码
     * @return 是否更新成功
     */
    public boolean updateFaceInCloud(String userId, String imageBase64) {
        try {
            // 先检测是否有人脸
            if (!detectFace(imageBase64)) {
                log.warn("图像中未检测到人脸，更新失败");
                return false;
            }
            
            // 使用百度云人脸库更新用户接口
            String url = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/update?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("group_id", FACE_GROUP_ID);
            requestBody.put("user_id", userId);
            requestBody.put("quality_control", "NORMAL");
            requestBody.put("liveness_control", "LOW");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    log.info("百度云人脸库人脸信息更新成功，用户ID: {}", userId);
                    return true;
                } else {
                    log.warn("百度云人脸库更新失败: {}, 错误代码: {}", 
                            jsonObject.getString("error_msg"), 
                            jsonObject.getIntValue("error_code"));
                }
            } else {
                log.warn("百度云人脸库更新请求失败，HTTP状态码: {}", response.getStatusCodeValue());
            }
            return false;
        } catch (Exception e) {
            log.error("百度云人脸库更新异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 搜索百度云人脸库并返回详细结果
     * @param imageBase64 人脸图像Base64编码
     * @param userId 可选，如果指定则搜索特定用户
     * @return 搜索结果Map，包含是否找到、用户ID、得分等信息
     */
    public Map<String, Object> searchFaceInCloud(String imageBase64, String userId) {
        try {
            // 先检测是否有人脸
            if (!detectFace(imageBase64)) {
                log.warn("图像中未检测到人脸，搜索失败");
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("found", false);
                errorResult.put("errorMsg", "图像中未检测到人脸");
                return errorResult;
            }
            
            String url = "https://aip.baidubce.com/rest/2.0/face/v3/search?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", imageBase64);
            requestBody.put("image_type", "BASE64");
            requestBody.put("group_id_list", FACE_GROUP_ID);
            
            // 如果指定了userId，搜索特定用户
            if (userId != null) {
                requestBody.put("user_id", userId);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    JSONObject apiResult = jsonObject.getJSONObject("result");
                    JSONArray userList = apiResult.getJSONArray("user_list");
                    
                    if (userList != null && userList.size() > 0) {
                        JSONObject user = userList.getJSONObject(0);
                        double score = user.getDoubleValue("score");
                        String foundUserId = user.getString("user_id");
                        String userInfo = user.getString("user_info");
                        
                        log.info("人脸搜索成功，用户ID: {}, 匹配得分: {}, 用户信息: {}", foundUserId, score, userInfo);
                        
                        // 匹配得分大于80分认为匹配成功
                        boolean matched = score >= 80;
                        
                        Map<String, Object> searchResult = new HashMap<>();
                        searchResult.put("found", matched);
                        searchResult.put("userId", foundUserId);
                        searchResult.put("userInfo", userInfo);
                        searchResult.put("score", score);
                        searchResult.put("message", matched ? "人脸匹配成功" : "人脸相似度不足");
                        
                        return searchResult;
                    } else {
                        log.info("未找到匹配的人脸");
                        Map<String, Object> notFoundResult = new HashMap<>();
                        notFoundResult.put("found", false);
                        notFoundResult.put("errorMsg", "未找到匹配的人脸");
                        return notFoundResult;
                    }
                } else {
                    log.warn("百度AI人脸搜索失败: {}, 错误代码: {}", 
                            jsonObject.getString("error_msg"), 
                            jsonObject.getIntValue("error_code"));
                    Map<String, Object> apiErrorResult = new HashMap<>();
                    apiErrorResult.put("found", false);
                    apiErrorResult.put("errorMsg", jsonObject.getString("error_msg"));
                    return apiErrorResult;
                }
            }
            log.error("人脸搜索请求失败，HTTP状态码: {}", response.getStatusCodeValue());
            Map<String, Object> httpErrorResult = new HashMap<>();
            httpErrorResult.put("found", false);
            httpErrorResult.put("errorMsg", "请求失败: " + response.getStatusCodeValue());
            return httpErrorResult;
        } catch (Exception e) {
            log.error("百度AI人脸搜索异常: {}", e.getMessage(), e);
            Map<String, Object> exceptionResult = new HashMap<>();
            exceptionResult.put("found", false);
            exceptionResult.put("errorMsg", "搜索过程发生异常: " + e.getMessage());
            return exceptionResult;
        }
    }
    
    /**
     * 搜索百度云人脸库（简化版）
     * @param imageBase64 人脸图像Base64编码
     * @param userId 可选，如果指定则搜索特定用户
     * @return 是否找到匹配的人脸
     */
    public boolean searchFace(String imageBase64, String userId) {
        Map<String, Object> result = searchFaceInCloud(imageBase64, userId);
        return result != null && Boolean.TRUE.equals(result.get("found"));
    }
    
    /**
     * 从百度云人脸库中删除人脸
     * @param userId 用户ID
     * @return 是否删除成功
     */
    public boolean deleteFaceFromCloud(String userId) {
        try {
            String url = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/delete?access_token=" + getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("group_id", FACE_GROUP_ID);
            requestBody.put("user_id", userId);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                if (jsonObject.getIntValue("error_code") == 0) {
                    log.info("从百度云人脸库删除用户成功，用户ID: {}", userId);
                    return true;
                } else {
                    log.warn("百度云人脸库删除失败: {}, 错误代码: {}", 
                            jsonObject.getString("error_msg"), 
                            jsonObject.getIntValue("error_code"));
                }
            } else {
                log.warn("百度云人脸库删除请求失败，HTTP状态码: {}", response.getStatusCodeValue());
            }
            return false;
        } catch (Exception e) {
            log.error("百度云人脸库删除异常: {}", e.getMessage(), e);
            return false;
        }
    }
}