import request from '@/utils/request';

// 获取某本书的评论
export function getCommentsByTitle(title: string) {
  return request.get(`/comments/byTitle?title=${encodeURIComponent(title)}`);
}

// 添加评论
export function addComment(data: { title: string; username: string; comment: string }) {
  return request.post('/comments/add', data);
} 