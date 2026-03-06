package com.upc.bookmanagement.service;

import com.upc.bookmanagement.entity.DocumentChunk;
import com.upc.bookmanagement.entity.KnowledgeDocument;
import com.upc.bookmanagement.entity.SplitConfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档处理服务接口，定义文档处理的核心功能
 */
public interface DocumentProcessingService {
    // 从不同类型文件提取文本
    String extractText(MultipartFile file, String fileType);
    
    // 文本分块
    List<String> splitText(String text, SplitConfig config);
    
    // 向量化文本
    List<Float> vectorizeText(String text);
    
    // 构建索引
    void buildIndex(List<DocumentChunk> chunks);
    
    // 上传并处理文档
    KnowledgeDocument uploadAndProcessDocument(MultipartFile file, SplitConfig config, Long userId);
    
    // 搜索文档
    List<DocumentChunk> searchDocuments(String query, Integer topK);
    
    // 获取文档列表
    List<KnowledgeDocument> getDocumentList();
    
    // 删除文档
    boolean deleteDocument(Long documentId);
}