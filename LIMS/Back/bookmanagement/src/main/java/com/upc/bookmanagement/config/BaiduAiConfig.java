package com.upc.bookmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百度AI人脸识别配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "baidu.ai")
public class BaiduAiConfig {
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * Secret Key
     */
    private String secretKey;
    
    /**
     * 人脸检测接口地址
     */
    private String faceDetectUrl = "https://aip.baidubce.com/rest/2.0/face/v3/detect";
    
    /**
     * 人脸特征提取接口地址
     */
    private String faceFeatureUrl = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add";
    
    /**
     * 人脸识别接口地址
     */
    private String faceVerifyUrl = "https://aip.baidubce.com/rest/2.0/face/v3/search";
}