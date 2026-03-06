package com.upc.bookmanagement.entity;

import lombok.Data;

/**
 * 文本分块配置类，用于配置分块策略和参数
 */
@Data
public class SplitConfig {
    private String splitType; // 分块类型：fixed（固定长度）或semantic（语义分块）
    private Integer maxLength; // 单段文本的最大长度
    private Integer overlap; // 相邻文本重合长度
}