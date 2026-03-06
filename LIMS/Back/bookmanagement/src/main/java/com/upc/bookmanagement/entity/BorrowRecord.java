package com.upc.bookmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "借阅信息")
@TableName("borrow_record")
public class BorrowRecord {

    @Schema(description = "借阅ID")
    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer recordId;

    @Schema(description = "书名")
    private String title;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Integer userId;

    @Schema(description = "借阅日期")
    @TableField("borrow_date")
    private Date borrowDate;

    @Schema(description = "应还日期")
    @TableField("due_date")
    private Date dueDate;

    @Schema(description = "实际归还日期")
    @TableField("return_date")
    private Date returnDate;

    @Schema(description = "借阅状态")
    private BorrowStatus status;

}

