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
@Schema(name = "货架信息")
@TableName("shelves")
public class Shelves {
    @Schema(description = "货架ID")
    @TableId(value = "shelve_id", type = IdType.AUTO)
    private Integer shelveId;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "书名")
    private String title;


}
