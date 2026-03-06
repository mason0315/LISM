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
 * 留言/建议实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "留言信息")
@TableName("suggestion_info")
public class Suggestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "留言ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Integer userId;

    @Schema(description = "留言内容")
    @TableField("content")
    private String content;

    @Schema(description = "留言类型(1-建议,2-反馈,3-投诉)")
    @TableField("type")
    private Integer type;

    @Schema(description = "状态(0-未处理,1-已处理)")
    @TableField("status")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private String createTime;

    @Schema(description = "回复内容")
    @TableField("reply")
    private String reply;

    @Schema(description = "回复时间")
    @TableField("reply_time")
    private String replyTime;

    // 非数据库字段
    @Schema(description = "用户名")
    @TableField(exist = false)
    private String username;
}
