package com.upc.bookmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "用户信息")
@TableName("users")
public class Users {

    @Schema(description = "用户ID")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer user_id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "角色")
    private Integer role = 0;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "真实姓名")
    private String realName;
    
    @Schema(description = "人脸特征向量")
    @TableField("face_features")
    private byte[] faceFeatures;
    
    @Schema(description = "人脸图像")
    @TableField("face_image")
    private byte[] faceImage;

    public Integer getUserId() { return user_id; }
    public void setUserId(Integer userId) { this.user_id = userId; }
}