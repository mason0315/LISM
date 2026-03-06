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
 * 用户实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "用户信息")
@TableName("users")
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "密码")
    @TableField("password")
    private String password;

    @Schema(description = "角色(0-管理员,1-普通用户)")
    @TableField("role")
    private Integer role;

    @Schema(description = "邮箱")
    @TableField("email")
    private String email;

    @Schema(description = "电话")
    @TableField("phone")
    private String phone;

    @Schema(description = "人脸特征数据")
    @TableField("face_data")
    private String faceData;

    @Schema(description = "人脸图片URL")
    @TableField("face_image_url")
    private String faceImageUrl;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private String createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private String updateTime;
}
