import request from "@/utils/request";

// 定义书籍数据结构
interface Book {
  title: string
  author: string
  publisher: string
  avaBooks: number
  stock: number
  category: string
  language: string
  description: string
  cover: string // 封面图片路径
}

/**
 * 根据书籍信息搜索图书（支持按标题、作者等字段模糊查询）
 * @param book 查询条件对象（例如：{ title: '长安的荔枝' }）
 * @param pageNum 当前页码
 * @param pageSize 每页数量
 */
export function searchBooks(book: Partial<Book>, pageNum: number, pageSize: number): Promise<{ list: Book[]; total: number }> {
  return request.post("/book/BookBy", book, {
    params: {
      pageNum,
      pageSize
    }
  });
}

/**
 * 根据关键词模糊搜索图书（支持按标题、作者等字段）
 * @param keyword 查询关键词字符串
 * @param pageNum 当前页码
 * @param pageSize 每页数量
 */
export function searchBooksByKeyword(keyword: string, pageNum: number, pageSize: number): Promise<{
  data: any
  list: Book[]; total: number }> {
  return request.get("/book/search", {
    params: {
      keyword,
      pageNum,
      pageSize
    }
  });
}