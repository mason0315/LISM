package com.upc.face.service;

import org.springframework.web.multipart.MultipartFile;

public interface FaceService {

    /**
     * 提取人脸特征
     *
     * @param file 图片文件
     * @return 人脸特征值
     */
    String extractFaceFeature(MultipartFile file);

    /**
     * 比对两张人脸
     *
     * @param file1 图片1
     * @param file2 图片2
     * @return 相似度 (0-1)
     */
    Double compareFace(MultipartFile file1, MultipartFile file2);

    /**
     * 验证人脸
     *
     * @param userId 用户ID
     * @param file   图片文件
     * @return 是否验证通过
     */
    Boolean verifyFace(Integer userId, MultipartFile file);

    /**
     * 检测人脸
     *
     * @param file 图片文件
     * @return 是否检测到人脸
     */
    Boolean detectFace(MultipartFile file);
}
