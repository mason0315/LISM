<template>
  <div class="admin-record-container">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-title">
        <el-icon><reading /></el-icon>
        <span>借阅管理</span>
      </div>
      <div class="nav-actions">
        <el-button type="primary" @click="fetchAllBooks">
          <el-icon><refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="danger" @click="handleLogout">
          <el-icon><lock /></el-icon>
          退出登录
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <div class="search-card">
        <div class="search-input-group">
          <el-input
            v-model="bookname"
            placeholder="书名"
            class="search-input"
            @keyup.enter="handleKeyPress"
          >
            <template #prefix>
              <el-icon><document /></el-icon>
            </template>
          </el-input>
          <el-input
            v-model="username"
            placeholder="用户ID"
            class="search-input"
            @keyup.enter="handleKeyPress"
          >
            <template #prefix>
              <el-icon><user /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearchClick" class="search-btn">
            <el-icon><search /></el-icon>
            搜索
          </el-button>
          <el-button @click="fetchAllBooks" class="all-btn">
            <el-icon><list /></el-icon>
            全部
          </el-button>
          <el-button type="warning" @click="fetchBorrowed" class="borrowed-btn">
            <el-icon><clock /></el-icon>
            未归还
          </el-button>
        </div>
      </div>
    </div>

    <!-- 借阅记录列表 -->
    <div class="records-container">
      <!-- 空状态提示 -->
      <div v-if="books.length === 0" class="empty-state">
        <el-icon><reading /></el-icon>
        <p>暂无借阅记录</p>
      </div>

      <!-- 借阅记录卡片列表 -->
      <div class="records-grid">
        <div class="record-card" v-for="book in books" :key="book.id">
          <div class="book-cover-section">
            <img :src="book.cover" alt="Book Cover" class="book-cover" />
            <div class="status-badge" :class="book.status">
              {{ book.status === 'returned' ? '已归还' : '借阅中' }}
            </div>
          </div>

          <div class="record-info">
            <h3 class="book-title">{{ book.title }}</h3>
            <div class="book-meta">
              <div class="meta-item">
                <el-icon><office-building /></el-icon>
                <span>{{ book.publisher }}</span>
              </div>
              <div class="meta-item">
                <el-icon><user /></el-icon>
                <span>{{ book.author }}</span>
              </div>
              <div class="meta-item">
                <el-icon><collection-tag /></el-icon>
                <span>{{ book.fileType }}</span>
              </div>
              <!-- 货架号展示已移除 -->
            </div>

            <div class="borrow-details">
              <div class="detail-item">
                <span class="detail-label">借阅状态:</span>
                <el-tag :type="book.status === 'returned' ? 'success' : 'warning'" size="small">
                  {{ book.status === 'returned' ? '已归还' : '借阅中' }}
                </el-tag>
              </div>
              <div v-if="book.status === 'returned'" class="detail-item">
                <span class="detail-label">归还日期:</span>
                <span class="detail-value">{{ book.returnDate }}</span>
              </div>
            </div>
          </div>

          <div class="record-actions">
            <!-- 归还按钮 -->
            <el-button
              v-if="!book.showDatePicker && book.status !== 'returned'"
              type="success"
              @click="book.showDatePicker = true"
              class="return-btn"
            >
              <el-icon><check /></el-icon>
              归还
            </el-button>

            <!-- 日期选择器 + 按钮组 -->
            <div v-if="book.showDatePicker && book.status !== 'returned'" class="date-picker-group">
              <el-date-picker
                v-model="book.returnDateTemp"
                type="date"
                placeholder="选择归还日期"
                :min-date="new Date()"
                format="YYYY-MM-DD"
                size="small"
                class="date-picker"
              />
              <div class="action-buttons">
                <el-button type="success" size="small" @click="confirmReturn(book)">
                  <el-icon><check /></el-icon>
                  确定
                </el-button>
                <el-button type="danger" size="small" @click="book.showDatePicker = false">
                  <el-icon><close /></el-icon>
                  取消
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页组件 -->
      <div class="pagination-section">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="totalPages * pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElDatePicker, ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { searchBooksByKeyword } from '@/api/search'
import Borrowing from './Borrowing.vue'
import { addBorrowRecord } from '@/api/borrow'
import { format } from 'date-fns'
import { useLogout } from '@/utils/user'
const { handleLogout } = useLogout();

// 书籍列表数据
const books = ref<any[]>([])
// 当前页码
const pageNum = ref<number>(1)
// 每页大小
const pageSize = ref<number>(5)
// 总页数
const totalPages = ref<number>(1)

const shelveMap = ref<Record<string, number>>({});

const router = useRouter()

// 定义书籍类型接口
interface Book {
  title: string
  avaBooks: number
  author: string
  publisher: string
  category: string
  language: string
  description: string
  cover: string
}

interface BorrowRecord {
  recordId: number
  title: string
  userId: number
  borrowDate: string
  dueDate: string
  returnDate: string
  status: 'borrowed' | 'returned' | 'reserved'
  showDatePicker: boolean
  returnDateTemp: Date | undefined
}

// 搜索条件
const bookname = ref('')
const username = ref('')

// 通用查询函数，用于初始加载 / 搜索 / 翻页
const fetchBooks = async (currentPage: number = pageNum.value) => {
  try {
    // 批量获取所有书的货架号映射表
    if (Object.keys(shelveMap.value).length === 0) {
      const shelvesRes = await request.get('/shelves/getAllShelves', { params: { pageNum: 1, pageSize: 1000 } });
      if (shelvesRes.code === 200 && shelvesRes.data?.list) {
        shelveMap.value = {};
        shelvesRes.data.list.forEach((item: any) => {
          shelveMap.value[item.title] = item.shelveId;
        });
      }
    }
    const response = await request.get('/borrowrecord/allrecord', {
      params: {
        title: bookname.value,
        userId: username.value,
        pageNum: currentPage,
        pageSize: pageSize.value,
      },
    })

    if (response.code === 200) {
      const rawBooks = response.data.list || []
      const detailedBooks = await Promise.all(
        rawBooks.map(async (book: any) => {
          const detailedResponse = await request.get(`/book/findByTitle/${book.title}`)
          if (detailedResponse.code === 200) {
            return { ...book, ...detailedResponse.data }
          } else {
            return { ...book }
          }
        }),
      )

      books.value = detailedBooks
      totalPages.value = response.data.pages
    } else {
      ElMessage.error(response.message || '加载失败')
    }
  } catch (error) {
    console.error('加载书籍列表失败:', error)
  }
}

const handleSearchClick = () => {
  pageNum.value = 1
  fetchBooks(1)
}

// 回车键触发搜索
const handleKeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleSearchClick()
  }
}

// 加载全部书籍
const fetchAllBooks = async () => {
  bookname.value = ''
  username.value = ''
  pageNum.value = 1
  pageSize.value = 5

  try {
    const response = await request.get('/borrowrecord/findA', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      },
    })

    if (response.code === 200) {
      const rawBooks = response.data.list || []
      const detailedBooks = await Promise.all(
        rawBooks.map(async (book: any) => {
          const detailedResponse = await request.get(`/book/findByTitle/${book.title}`)
          if (detailedResponse.code === 200) {
            return { ...book, ...detailedResponse.data }
          } else {
            console.error(`Failed to get details for book: ${book.title}`)
            return book
          }
        }),
      )

      books.value = detailedBooks
      totalPages.value = response.data.pages
    } else {
      ElMessage.error(response.message || '加载失败')
    }
  } catch (error) {
    console.error('加载全部借阅记录失败:', error)
  }
}

// 确认归还
const confirmReturn = async (borrowRecord: BorrowRecord) => {
  if (!borrowRecord.returnDateTemp) {
    ElMessage.warning('请选择归还日期')
    return
  }

  if (borrowRecord.status !== 'borrowed') {
    ElMessage.warning('该书籍状态不允许归还')
    return
  }

  if (!borrowRecord.recordId) {
    ElMessage.error('借阅记录ID缺失，无法处理归还请求')
    console.error('借阅记录缺少record_id:', borrowRecord)
    return
  }

  const formattedDate = format(borrowRecord.returnDateTemp, 'yyyy-MM-dd')
  //const formattedDate = borrowRecord.returnDateTemp.toISOString().split('T')[0]
  try {
    const response = await request.put('/borrowrecord/updateRecord', {
      recordId: borrowRecord.recordId,
      status: 'returned',
      returnDate: formattedDate,
    })

    if (response.code === 200) {
      borrowRecord.status = 'returned'
      borrowRecord.returnDate = formattedDate
      borrowRecord.showDatePicker = false
      borrowRecord.returnDateTemp = undefined

      returnBook(borrowRecord.title)
      ElMessage.success('归还成功')
    } else {
      ElMessage.error(response.message || '归还失败，请重试')
    }
  } catch (error: any) {
    console.error('归还请求失败:', error)

    if (error.response) {
      ElMessage.error(
        `服务器错误 ${error.response.status}: ${error.response.data.message || '未知错误'}`,
      )
    } else if (error.request) {
      ElMessage.error('没有收到服务器响应，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误: ' + error.message)
    }
  }
}

async function returnBook(title: string): Promise<void> {
  const book = books.value.find((book) => book.title === title)
  if (book) {
    book.avaBooks += 1
    console.log(`书籍 "${book.title}" 可借数量已更新为：${book.avaBooks}`)

    const success = await updateBookOnServer(book)
    if (!success) {
      book.avaBooks -= 1
    }
  } else {
    console.warn(`未找到书名为 "${title}" 的书籍`)
  }
}

const updateBookOnServer = async (book: Book): Promise<boolean> => {
  console.log('发送给后端的数据:', book)
  try {
    const response = await request.post('/book/updateBook', book)
    if (response.code === 200) {
      console.log(`书籍 "${book.title}" 已成功更新到数据库`)
      return true
    } else {
      ElMessage.error(`书籍 "${book.title}" 更新失败`)
      return false
    }
  } catch (error) {
    console.error('更新书籍到服务器失败:', error)
    ElMessage.error('更新书籍失败，请检查网络')
    return false
  }
}

const fetchBorrowed = async () => {
  bookname.value = ''
  username.value = ''
  pageNum.value = 1
  pageSize.value = 5

  try {
    const response = await request.get('/borrowrecord/findBorrowed', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      },
    })

    if (response.code === 200) {
      const rawBooks = response.data.list || []
      const detailedBooks = await Promise.all(
        rawBooks.map(async (book: any) => {
          const detailedResponse = await request.get(`/book/findByTitle/${book.title}`)
          if (detailedResponse.code === 200) {
            return { ...book, ...detailedResponse.data }
          } else {
            console.error(`Failed to get details for book: ${book.title}`)
            return book
          }
        }),
      )

      books.value = detailedBooks
      totalPages.value = response.data.pages
    } else {
      ElMessage.error(response.message || '加载失败')
    }
  } catch (error) {
    console.error('加载未归还借阅记录失败:', error)
  }
}

// 移除 findShelveId 相关函数

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  fetchBooks(1)
}

const handleCurrentChange = (page: number) => {
  pageNum.value = page
  fetchBooks(page)
}

onMounted(() => {
  fetchAllBooks()
})
</script>

<style scoped>
.admin-record-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  padding: 24px;
  color: #fff;
  font-family: 'Georgia', '思源宋体', serif;
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: bold;
  color: #fff;
}

.nav-title .el-icon {
  font-size: 28px;
  color: #4f8cff;
}

.search-section {
  margin-bottom: 24px;
}

.search-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-input-group {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.search-btn, .all-btn, .borrowed-btn {
  white-space: nowrap;
}

.records-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.empty-state .el-icon {
  font-size: 48px;
  color: #aaa;
  margin-bottom: 16px;
}

.empty-state p {
  color: #aaa;
  font-size: 16px;
}

.records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

.record-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.record-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.book-cover-section {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.book-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.returned {
  background: rgba(103, 194, 58, 0.2);
  color: #67c23a;
  border: 1px solid rgba(103, 194, 58, 0.3);
}

.status-badge.borrowed {
  background: rgba(230, 162, 60, 0.2);
  color: #e6a23c;
  border: 1px solid rgba(230, 162, 60, 0.3);
}

.record-info {
  padding: 20px;
}

.book-title {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 16px;
  line-height: 1.4;
}

.book-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #aaa;
  font-size: 14px;
}

.meta-item .el-icon {
  color: #4f8cff;
  font-size: 16px;
}

.borrow-details {
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.detail-label {
  color: #aaa;
  font-size: 14px;
}

.detail-value {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}

.record-actions {
  padding: 0 20px 20px;
}

.return-btn {
  width: 100%;
}

.date-picker-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.date-picker {
  width: 100%;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  flex: 1;
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

@media (max-width: 768px) {
  .admin-record-container {
    padding: 16px;
  }

  .records-grid {
    grid-template-columns: 1fr;
  }

  .search-input-group {
    flex-direction: column;
  }

  .top-nav {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}
</style>
