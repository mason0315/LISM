package com.upc.face.service.impl;

import com.upc.common.entity.Users;
import com.upc.common.feign.UserFeignClient;
import com.upc.common.result.Result;
import com.upc.face.service.FaceService;
import com.upc.face.utils.FaceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceServiceImpl implements FaceService {

    private final UserFeignClient userFeignClient;

    @Override
    public String extractFaceFeature(MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            // 使用FaceUtil提取人脸特征
            return FaceUtil.extractFeature(imageBytes);
        } catch (IOException e) {
            log.error("提取人脸特征失败", e);
            return null;
        }
    }

    @Override
    public Double compareFace(MultipartFile file1, MultipartFile file2) {
        try {
            byte[] imageBytes1 = file1.getBytes();
            byte[] imageBytes2 = file2.getBytes();
            return FaceUtil.compareFace(imageBytes1, imageBytes2);
        } catch (IOException e) {
            log.error("人脸比对失败", e);
            return null;
        }
    }

    @Override
    public Boolean verifyFace(Integer userId, MultipartFile file) {
        try {
            // 获取用户已存储的人脸特征
            Result<Users> userResult = userFeignClient.getUserById(userId);
            if (userResult.getData() == null) {
                log.warn("用户不存在: {}", userId);
                return false;
            }

            Users user = userResult.getData();
            String storedFeature = user.getFaceData();
            if (storedFeature == null || storedFeature.isEmpty()) {
                log.warn("用户未录入人脸: {}", userId);
                return false;
            }

            // 提取当前上传图片的人脸特征
            byte[] imageBytes = file.getBytes();
            String currentFeature = FaceUtil.extractFeature(imageBytes);
            if (currentFeature == null) {
                log.warn("无法提取当前图片人脸特征");
                return false;
            }

            // 比对特征
            double similarity = FaceUtil.compareFeature(storedFeature, currentFeature);
            log.info("人脸相似度: {}", similarity);

            // 相似度阈值设为0.8
            return similarity >= 0.8;
        } catch (IOException e) {
            log.error("人脸验证失败", e);
            return false;
        }
    }

    @Override
    public Boolean detectFace(MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            return FaceUtil.detectFace(imageBytes);
        } catch (IOException e) {
            log.error("人脸检测失败", e);
            return false;
        }
    }
}
