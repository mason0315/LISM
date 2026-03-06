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
 * 评论实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "评论信息")
@TableName("comment_info")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评论ID")
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Integer commentId;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Integer userId;

    @Schema(description = "书名")
    @TableField("title")
    private String title;

    @Schema(description = "评论内容")
    @TableField("content")
    private String content;

    @Schema(description = "评分(1-5)")
    @TableField("rating")
    private Integer rating;

    @Schema(description = "评论时间")
    @TableField("create_time")
    private String createTime;

    // 非数据库字段
    @Schema(description = "用户名")
    @TableField(exist = false)
    private String username;

    @Schema(description = "用户头像")
    @TableField(exist = false)
    private String userAvatar;
}
