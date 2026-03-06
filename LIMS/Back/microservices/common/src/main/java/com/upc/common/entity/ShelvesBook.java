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
 * 货架图书关联实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "货架图书关联")
@TableName("shelves_book")
public class ShelvesBook implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "货架ID")
    @TableField("shelve_id")
    private Integer shelveId;

    @Schema(description = "书名")
    @TableField("title")
    private String title;
}
