import request from '@/utils/request'

export interface BorrowRecord {
  recordId?: number // 可选字段，创建时由后端生成
  title: string
  userId: number
  borrowDate: string
  dueDate: string
  returnDate: string | null
  status: string
}

/**
 * 添加借阅记录
 * @param record 借阅记录对象
 */
export const addBorrowRecord = (record: BorrowRecord) => {
  return request.post('/borrowrecord/addRecord', record)
}
export const decreaseAvailableBooks = (title: string) => {
  return request.put(`/book/decreaseAvaBooks/${encodeURIComponent(title)}`)
}