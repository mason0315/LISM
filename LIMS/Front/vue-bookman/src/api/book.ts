import request from '@/utils/request'

export function addBook(data: any) {
  return request.post('/book/addBook', data)
}

export function deleteBook(title: string) {
  return request.delete(`/book/${encodeURIComponent(title)}`)
} 