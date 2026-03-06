package com.upc.bookmanagement.common;

import com.upc.bookmanagement.entity.DocumentChunk;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内存的向量存储实现，用于替代Milvus向量数据库
 * 该实现提供向量存储、相似度搜索等功能，完全在内存中运行
 */
public class InMemoryVectorStore {

    // 存储文档分块和向量的映射
    private final Map<Long, DocumentChunk> chunkStore = new HashMap<>();
    // 存储文档ID和文档分块ID列表的映射
    private final Map<Long, List<Long>> documentMap = new HashMap<>();
    // 向量维度
    private final int dimension;
    // 自增ID
    private long currentId = 1;

    public InMemoryVectorStore(int dimension) {
        this.dimension = dimension;
    }

    /**
     * 插入文档分块及其向量
     */
    public void insertDocumentChunk(DocumentChunk chunk) {
        if (chunk.getId() == null) {
            chunk.setId(currentId++);
        }
        
        chunkStore.put(chunk.getId(), chunk);
        
        // 更新文档映射
        documentMap.computeIfAbsent(chunk.getDocumentId(), k -> new ArrayList<>())
                   .add(chunk.getId());
    }

    /**
     * 批量插入文档分块
     */
    public void batchInsert(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            insertDocumentChunk(chunk);
        }
    }

    /**
     * 搜索相似向量
     */
    public List<DocumentChunk> searchSimilarVectors(List<Float> queryVector, int topK) {
        // 计算查询向量与所有存储向量的相似度
        List<Map.Entry<Long, Float>> similarities = new ArrayList<>();
        
        for (Map.Entry<Long, DocumentChunk> entry : chunkStore.entrySet()) {
            float similarity = calculateCosineSimilarity(queryVector, entry.getValue().getEmbedding());
            similarities.add(new AbstractMap.SimpleEntry<>(entry.getKey(), similarity));
        }
        
        // 按相似度排序，取前topK个结果
        similarities.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        
        // 获取前topK个文档分块
        return similarities.stream()
                .limit(topK)
                .map(entry -> chunkStore.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private float calculateCosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1 == null || vec2 == null || vec1.size() != vec2.size()) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0f;
        }
        
        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 根据文档ID获取所有分块
     */
    public List<DocumentChunk> getChunksByDocumentId(Long documentId) {
        List<Long> chunkIds = documentMap.getOrDefault(documentId, Collections.emptyList());
        return chunkIds.stream()
                .map(chunkStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 删除指定文档的所有分块
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    public boolean deleteDocument(Long documentId) {
        if (!documentMap.containsKey(documentId)) {
            return false;
        }
        
        List<Long> chunkIds = documentMap.remove(documentId);
        if (chunkIds != null) {
            for (Long chunkId : chunkIds) {
                chunkStore.remove(chunkId);
            }
        }
        
        return true;
    }

    /**
     * 获取存储的文档分块总数
     */
    public int getChunkCount() {
        return chunkStore.size();
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        chunkStore.clear();
        documentMap.clear();
        currentId = 1;
    }
}