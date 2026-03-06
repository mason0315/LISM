package com.upc.bookmanagement.entity;

import lombok.Data;

import java.util.List;

/**
 * 文档分块实体类，用于存储文档分块内容及其向量表示
 */
@Data
public class DocumentChunk {
    private Long id;
    private String content;
    private String metadata;
    private List<Float> embedding;
    private Long documentId;
    private Integer chunkIndex;
}