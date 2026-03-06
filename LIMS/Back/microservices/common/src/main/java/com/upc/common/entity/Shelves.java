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
 * 货架实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "货架信息")
@TableName("shelves_info")
public class Shelves implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "货架ID")
    @TableId(value = "shelve_id", type = IdType.AUTO)
    private Integer shelveId;

    @Schema(description = "货架名称")
    @TableField("shelve_name")
    private String shelveName;

    @Schema(description = "货架位置")
    @TableField("location")
    private String location;

    @Schema(description = "类别")
    @TableField("category")
    private String category;

    @Schema(description = "容量")
    @TableField("capacity")
    private Integer capacity;

    @Schema(description = "当前数量")
    @TableField("current_count")
    private Integer currentCount;

    @Schema(description = "描述")
    @TableField("description")
    private String description;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private String createTime;
}
