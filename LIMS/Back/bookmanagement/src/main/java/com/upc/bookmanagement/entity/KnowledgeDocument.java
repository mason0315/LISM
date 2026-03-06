package com.upc.bookmanagement.entity;

import lombok.Data;

/**
 * 知识库文档实体类，用于存储上传的文档基本信息
 */
@Data
public class KnowledgeDocument {
    private Long id;
    private String title;
    private String fileName;
    private String fileType;
    private String uploadTime;
    private Long userId;
    private Integer chunkCount;
}