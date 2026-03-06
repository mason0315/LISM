package com.upc.bookmanagement.service.impl;

import com.upc.bookmanagement.common.InMemoryVectorStore;
import com.upc.bookmanagement.entity.DocumentChunk;
import com.upc.bookmanagement.entity.KnowledgeDocument;
import com.upc.bookmanagement.entity.SplitConfig;
import com.upc.bookmanagement.service.DocumentProcessingService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档处理服务实现类
 */
@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    // 基于内存的向量存储
    private final InMemoryVectorStore vectorStore;
    private final int DIMENSION = 768; // 向量维度
    // 存储上传的文档列表（实际项目中应该使用数据库）
    private final List<KnowledgeDocument> documentList = new ArrayList<>();

    public DocumentProcessingServiceImpl() {
        try {
            // 初始化基于内存的向量存储
            vectorStore = new InMemoryVectorStore(DIMENSION);
            System.out.println("初始化基于内存的向量存储成功");
        } catch (Exception e) {
            // 注意：在实际环境中，这里应该使用日志记录错误
            System.err.println("向量存储初始化失败: " + e.getMessage());
            throw new RuntimeException("向量存储初始化失败", e);
        }
    }

    @Override
    public String extractText(MultipartFile file, String fileType) {
        try {
            if ("pdf".equalsIgnoreCase(fileType)) {
                return extractTextFromPDF(file);
            } else if ("docx".equalsIgnoreCase(fileType)) {
                return extractTextFromDOCX(file);
            } else if ("txt".equalsIgnoreCase(fileType)) {
                return extractTextFromTXT(file);
            } else {
                throw new IllegalArgumentException("不支持的文件类型: " + fileType);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件文本提取失败: " + e.getMessage(), e);
        }
    }

    private String extractTextFromPDF(MultipartFile file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDOCX(MultipartFile file) throws Exception {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    private String extractTextFromTXT(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public List<String> splitText(String text, SplitConfig config) {
        List<String> chunks = new ArrayList<>();
        
        if ("fixed".equalsIgnoreCase(config.getSplitType())) {
            // 固定长度分块
            int maxLength = config.getMaxLength() != null ? config.getMaxLength() : 500;
            int overlap = config.getOverlap() != null ? config.getOverlap() : 50;
            
            for (int i = 0; i < text.length(); i += maxLength - overlap) {
                int endIndex = Math.min(i + maxLength, text.length());
                chunks.add(text.substring(i, endIndex));
            }
        } else if ("semantic".equalsIgnoreCase(config.getSplitType())) {
            // 语义分块（简化实现，基于段落分割）
            String[] paragraphs = text.split("\n\n");
            chunks.addAll(Arrays.asList(paragraphs));
        } else {
            throw new IllegalArgumentException("不支持的分块类型: " + config.getSplitType());
        }
        
        return chunks;
    }

    @Override
    public List<Float> vectorizeText(String text) {
        // 注意：这是一个简化的向量化实现，实际项目中应该使用真实的Embedding模型
        // 例如BERT、Sentence-BERT等
        List<Float> embedding = new ArrayList<>(DIMENSION);
        Random random = new Random(text.hashCode()); // 使用文本哈希作为随机种子，确保相同文本生成相同向量
        
        for (int i = 0; i < DIMENSION; i++) {
            embedding.add(random.nextFloat() * 2 - 1); // 生成范围在[-1, 1]之间的随机浮点数
        }
        
        return embedding;
    }

    @Override
    public void buildIndex(List<DocumentChunk> chunks) {
        try {
            // 使用内存向量存储批量插入文档分块
            vectorStore.batchInsert(chunks);
            System.out.println("索引构建成功，插入了 " + chunks.size() + " 个文档分块");
        } catch (Exception e) {
            throw new RuntimeException("索引构建失败: " + e.getMessage(), e);
        }
    }

    @Override
    public KnowledgeDocument uploadAndProcessDocument(MultipartFile file, SplitConfig config, Long userId) {
        try {
            // 创建文档对象
            KnowledgeDocument document = new KnowledgeDocument();
            document.setFileName(file.getOriginalFilename());
            document.setTitle(file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf('.')));
            document.setFileType(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1));
            document.setUploadTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            document.setUserId(userId);
            document.setId(System.currentTimeMillis()); // 临时ID，实际应该从数据库生成
            
            // 1. 提取文本
            String text = extractText(file, document.getFileType());
            
            // 2. 文本分块
            List<String> chunks = splitText(text, config);
            document.setChunkCount(chunks.size());
            
            // 3. 向量化并构建索引
            List<DocumentChunk> documentChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(chunks.get(i));
                chunk.setEmbedding(vectorizeText(chunks.get(i)));
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                documentChunks.add(chunk);
            }
            
            // 4. 构建索引
            buildIndex(documentChunks);
            
            // 添加到文档列表
            documentList.add(document);
            
            return document;
        } catch (Exception e) {
            throw new RuntimeException("文档上传和处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DocumentChunk> searchDocuments(String query, Integer topK) {
        List<DocumentChunk> results = new ArrayList<>();
        
        try {
            // 向量化查询文本
            List<Float> queryEmbedding = vectorizeText(query);
            
            // 使用内存向量存储执行搜索
            results = vectorStore.searchSimilarVectors(queryEmbedding, topK);
            
            System.out.println("搜索完成，找到 " + results.size() + " 个相关文档分块");
        } catch (Exception e) {
            // 注意：在实际环境中，这里应该使用日志记录错误
            System.err.println("搜索失败: " + e.getMessage());
        }
        
        return results;
    }

    @Override
    public List<KnowledgeDocument> getDocumentList() {
        // 返回保存的文档列表
        return new ArrayList<>(documentList);
    }
    
    @Override
    public boolean deleteDocument(Long documentId) {
        // 从向量存储中删除文档及其分块
        boolean deleted = vectorStore.deleteDocument(documentId);
        
        // 从文档列表中删除
        if (deleted) {
            documentList.removeIf(doc -> doc.getId().equals(documentId));
        }
        
        return deleted;
    }
}