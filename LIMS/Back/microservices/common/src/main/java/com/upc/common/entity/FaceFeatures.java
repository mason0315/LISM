package com.upc.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人脸特征实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "人脸特征信息")
@TableName("face_features")
public class FaceFeatures implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableId(value = "user_id", type = IdType.INPUT)
    private Integer userId;

    @Schema(description = "人脸特征数据")
    @TableField("face_features")
    private String faceFeatures;

    @Schema(description = "人脸图片")
    @TableField("face_image")
    private String faceImage;
}
