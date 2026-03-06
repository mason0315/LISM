import request from '@/utils/request';

// 上传参数接口
export interface UploadParams {
  file: File;
  splitType: string; // fixed/semantic
  maxLength: number;
  overlap: number;
  userId?: number;
}

// 搜索参数接口
export interface SearchParams {
  query: string;
  topK?: number;
}

// 文档接口
export interface KnowledgeDocument {
  id: number;
  title: string;
  fileName: string;
  fileType: string;
  uploadTime: string;
  userId: number;
  chunkCount: number;
}

// 文档分块接口
export interface DocumentChunk {
  id?: number;
  content: string;
  metadata?: string;
  embedding?: number[];
  documentId: number;
  chunkIndex?: number;
}

/**
 * 上传文档并构建索引
 * @param params 上传参数
 * @returns 上传结果
 */
export function uploadDocument(params: UploadParams) {
  const formData = new FormData();
  formData.append('file', params.file);
  formData.append('splitType', params.splitType);
  formData.append('maxLength', params.maxLength.toString());
  formData.append('overlap', params.overlap.toString());
  if (params.userId) {
    formData.append('userId', params.userId.toString());
  }
  
  return request.post('/api/knowledge/upload', formData);
}

/**
 * 搜索知识库
 * @param params 搜索参数
 * @returns 搜索结果
 */
export function searchKnowledge(params: SearchParams) {
  return request.post('/api/knowledge/search', params);
}

/**
 * 获取文档列表
 * @returns 文档列表
 */
export function getDocumentList() {
  return request.get('/api/knowledge/list');
}

/**
 * 删除文档
 * @param documentId 文档ID
 * @returns 删除结果
 */
export function deleteDocument(documentId: number) {
  return request.delete(`/api/knowledge/${documentId}`);
}