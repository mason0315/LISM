package com.upc.bookmanagement.service;

import com.upc.bookmanagement.entity.Users;

import java.util.Map;

public interface FaceRecognitionService {
    
    /**
     * 提取人脸特征
     * @param faceImageData 人脸图像数据(Base64编码)
     * @return 人脸特征向量的字节数组
     */
    byte[] extractFaceFeatures(String faceImageData);
    
    /**
     * 验证人脸特征是否匹配
     * @param faceImageData 实时人脸图像数据(Base64编码)
     * @param storedFaceFeatures 存储的人脸特征向量
     * @return 是否匹配
     */
    boolean verifyFace(String faceImageData, byte[] storedFaceFeatures);
    
    /**
     * 注册新人脸
     * @param user 用户对象
     * @param faceImageData 人脸图像数据(Base64编码)
     * @return 是否注册成功
     */
    boolean registerFace(Users user, String faceImageData);
    
    /**
     * 更新用户人脸数据
     * @param user 用户对象
     * @param faceImageData 新的人脸图像数据(Base64编码)
     * @return 是否更新成功
     */
    boolean updateFaceData(Users user, String faceImageData);
    
    /**
     * 进行人脸实名认证（与百度云人脸实名认证接口对接）
     * @param faceImageData 人脸图像数据(Base64编码)
     * @param realName 真实姓名
     * @param idCardNumber 身份证号码
     * @return 认证结果Map，包含success标志、得分等信息
     */
    Map<String, Object> authenticateFace(String faceImageData, String realName, String idCardNumber);
    
    /**
     * 搜索百度云人脸库（用于在不知道用户名时进行人脸登录）
     * @param faceImageData 人脸图像数据(Base64编码)
     * @return 搜索结果，包含用户信息和匹配得分
     */
    Map<String, Object> searchFaceInCloud(String faceImageData);
}