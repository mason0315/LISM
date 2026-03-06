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
 * 图书实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "图书信息")
@TableName("book_info")
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "书名")
    @TableId(value = "title", type = IdType.INPUT)
    private String title;

    @Schema(description = "作者")
    @TableField("author")
    private String author;

    @Schema(description = "出版社")
    @TableField("publisher")
    private String publisher;

    @Schema(description = "可借数量")
    @TableField("ava_books")
    private Integer avaBooks;

    @Schema(description = "喜爱值")
    @TableField("love")
    private Integer love;

    @Schema(description = "库存数量")
    @TableField("stock")
    private Integer stock;

    @Schema(description = "类别")
    @TableField("category")
    private String category;

    @Schema(description = "语言")
    @TableField("language")
    private String language;

    @Schema(description = "简介")
    @TableField("description")
    private String description;

    @Schema(description = "图片")
    @TableField("cover")
    private String cover;

    @Schema(description = "匹配得分")
    @TableField(exist = false)
    private Integer score;
}
