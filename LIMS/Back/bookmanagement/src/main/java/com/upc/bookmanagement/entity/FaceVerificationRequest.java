package com.upc.bookmanagement.entity;

import lombok.Data;

@Data
public class FaceVerificationRequest {
    private String username;
    private String faceData; // Base64 encoded face image
    private String realName; // 真实姓名
    private String idCardNumber; // 身份证号码
}