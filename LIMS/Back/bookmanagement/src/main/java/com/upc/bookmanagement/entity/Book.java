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
@Schema(name = "图书信息")
@TableName("book")
public class Book {

    @Schema(description = "书名")
    @TableId(value = "title", type = IdType.INPUT)
    private String title;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "可借数量")
    @TableField("ava_books")
    private Integer avaBooks;

    @Schema(description = "喜爱值")
    private Integer love;

    @Schema(description = "库存数量")
    private Integer stock;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "图片")
    private String cover;

    @Schema(description = "匹配得分")
    @TableField(exist = false)
    private Integer score;
}
