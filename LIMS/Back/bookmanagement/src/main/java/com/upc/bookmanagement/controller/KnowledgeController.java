package com.upc.bookmanagement.controller;

import com.upc.bookmanagement.common.Result;
import com.upc.bookmanagement.entity.DocumentChunk;
import com.upc.bookmanagement.entity.KnowledgeDocument;
import com.upc.bookmanagement.entity.SplitConfig;
import com.upc.bookmanagement.service.DocumentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * 知识库控制器，处理文档上传、查询、搜索等功能
 */
@RestController
@CrossOrigin(
        origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
        allowCredentials = "true")
@RequestMapping("/api/knowledge")
@Tag(name = "知识库管理", description = "知识库相关接口")
public class KnowledgeController {
    
    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    /**
     * 上传文档并构建索引
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档并构建向量索引")
    public Result<KnowledgeDocument> uploadDocument(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("splitType") String splitType,
                                                    @RequestParam("maxLength") Integer maxLength,
                                                    @RequestParam("overlap") Integer overlap,
                                                    @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.fail(400, "文件不能为空");
            }
            
            // 创建分块配置
            SplitConfig config = new SplitConfig();
            config.setSplitType(splitType);
            config.setMaxLength(maxLength);
            config.setOverlap(overlap);
            
            // 上传并处理文档
            KnowledgeDocument document = documentProcessingService.uploadAndProcessDocument(file, config, userId);
            
            return Result.success(document);
        } catch (Exception e) {
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文档列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取文档列表", description = "获取所有已上传的文档列表")
    public Result<List<KnowledgeDocument>> getDocumentList() {
        try {
            List<KnowledgeDocument> documents = documentProcessingService.getDocumentList();
            return Result.success(documents);
        } catch (Exception e) {
            return Result.fail(500, "获取文档列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索文档
     */
    @PostMapping("/search")
    @Operation(summary = "搜索文档", description = "根据查询内容搜索相关文档")
    public Result<List<DocumentChunk>> search(@RequestParam("query") String query,
                                           @RequestParam(value = "topK", defaultValue = "10") Integer topK) {
        try {
            List<DocumentChunk> results = documentProcessingService.searchDocuments(query, topK);
            return Result.success(results);
        } catch (Exception e) {
            return Result.fail(500, "搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "删除文档", description = "根据文档ID删除文档")
    public Result<Boolean> deleteDocument(@PathVariable("documentId") Long documentId) {
        try {
            boolean success = documentProcessingService.deleteDocument(documentId);
            if (success) {
                return Result.success(true);
            } else {
                return Result.fail(404, "文档不存在");
            }
        } catch (Exception e) {
            return Result.fail(500, "删除文档失败: " + e.getMessage());
        }
    }
}