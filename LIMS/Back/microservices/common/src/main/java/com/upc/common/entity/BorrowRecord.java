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
 * 借阅记录实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "借阅记录")
@TableName("borrow_record")
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer recordId;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Integer userId;

    @Schema(description = "书名")
    @TableField("title")
    private String title;

    @Schema(description = "借阅日期")
    @TableField("borrow_date")
    private String borrowDate;

    @Schema(description = "应还日期")
    @TableField("due_date")
    private String dueDate;

    @Schema(description = "实际归还日期")
    @TableField("return_date")
    private String returnDate;

    @Schema(description = "状态(0-借阅中,1-已归还,2-逾期)")
    @TableField("status")
    private Integer status;

    @Schema(description = "续借次数")
    @TableField("renew_count")
    private Integer renewCount;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    // 非数据库字段
    @Schema(description = "用户名")
    @TableField(exist = false)
    private String username;

    @Schema(description = "图书信息")
    @TableField(exist = false)
    private Book bookInfo;
}
