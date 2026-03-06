<template>
  <div class="admin-book-container">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-title">
        <el-icon><document /></el-icon>
        <span>书籍管理</span>
      </div>
      <div class="nav-actions">
        <el-button type="success" @click="showAddBookDialog = true">
          <el-icon><plus /></el-icon>
          添加书籍
        </el-button>
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
            v-model="searchQuery"
            placeholder="书名、作者、出版社..."
            class="search-input"
            @keyup.enter="handleSearchClick"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
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
        </div>

        <div class="category-filters">
          <el-button
            v-for="category in categories"
            :key="category"
            :type="selectedCategory === category ? 'primary' : 'default'"
            @click="handleCategoryClick(category)"
            class="category-btn"
          >
            {{ category }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 书籍列表 -->
    <div class="books-container">
      <!-- 空状态提示 -->
      <div v-if="books.length === 0" class="empty-state">
        <el-icon><document /></el-icon>
        <p>暂无书籍数据</p>
      </div>

      <!-- 书籍卡片列表 -->
      <div class="books-grid">
        <div class="book-card" v-for="book in books" :key="book.title">
          <div class="book-cover-section">
            <img :src="book.cover" alt="Book Cover" class="book-cover" />
            <div class="book-status">
              <span class="status-badge" :class="book.avaBooks > 0 ? 'available' : 'unavailable'">
                {{ book.avaBooks > 0 ? '可借' : '无库存' }}
              </span>
            </div>
          </div>

          <div class="book-info">
            <h3 class="book-title">{{ book.title }}</h3>
            <div class="book-meta">
              <div class="meta-item">
                <el-icon><user /></el-icon>
                <span>{{ book.author }}</span>
              </div>
              <div class="meta-item">
                <el-icon><office-building /></el-icon>
                <span>{{ book.publisher }}</span>
              </div>
              <div class="meta-item">
                <el-icon><collection-tag /></el-icon>
                <span>{{ book.category }}</span>
              </div>
              <div class="meta-item">
                <el-icon><globe /></el-icon>
                <span>{{ book.language }}</span>
              </div>
              <!-- 货架号展示已移除 -->
            </div>

            <div class="book-stock">
              <div class="stock-info">
                <span class="stock-label">库存状态:</span>
                <span class="stock-numbers">{{ book.avaBooks }} / {{ book.stock }}</span>
              </div>
              <div class="stock-progress">
                <el-progress
                  :percentage="(book.avaBooks / book.stock) * 100"
                  :color="getProgressColor(book.avaBooks, book.stock)"
                  :show-text="false"
                />
              </div>
            </div>

            <div class="book-description">
              <p>{{ book.description }}</p>
            </div>
          </div>

          <div class="book-actions">
            <div class="stock-control">
              <el-input-number
                v-model.number="book.changeAmount"
                :min="1"
                :max="100"
                size="small"
                class="amount-input"
              />
              <div class="stock-buttons">
                <el-button
                  size="small"
                  @click="changeStockWithConfirm(book, 1)"
                  :disabled="book.changeAmount <= 0"
                  class="add-btn"
                >
                  <el-icon><plus /></el-icon>
                  入库
                </el-button>
                <el-button
                  size="small"
                  @click="changeStockWithConfirm(book, -1)"
                  :disabled="book.stock <= 0 || book.avaBooks <= 0"
                  class="minus-btn"
                >
                  <el-icon><minus /></el-icon>
                  出库
                </el-button>
              </div>
            </div>
            <el-button type="danger" size="small" @click="handleDeleteBook(book.title)" style="margin-top: 8px; width: 100%;">
              <el-icon><delete /></el-icon>
              删除
            </el-button>
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

  <!-- 添加书籍弹窗 -->
  <el-dialog v-model="showAddBookDialog" title="添加书籍" width="500px">
    <el-form :model="addBookForm" label-width="80px">
      <el-form-item label="书名" required>
        <el-input v-model="addBookForm.title" />
      </el-form-item>
      <el-form-item label="作者" required>
        <el-input v-model="addBookForm.author" />
      </el-form-item>
      <el-form-item label="出版社" required>
        <el-input v-model="addBookForm.publisher" />
      </el-form-item>
      <el-form-item label="可借数" required>
        <el-input-number v-model="addBookForm.avaBooks" :min="1" />
      </el-form-item>
      <el-form-item label="库存" required>
        <el-input-number v-model="addBookForm.stock" :min="1" />
      </el-form-item>
      <el-form-item label="分类" required>
        <el-input v-model="addBookForm.category" />
      </el-form-item>
      <el-form-item label="语言" required>
        <el-input v-model="addBookForm.language" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="addBookForm.description" type="textarea" />
      </el-form-item>
      <el-form-item label="封面URL">
        <el-input v-model="addBookForm.cover" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAddBookDialog = false">取消</el-button>
      <el-button type="primary" @click="handleAddBook">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElDatePicker, ElMessage } from 'element-plus'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { useLogout } from '@/utils/user'
import { addBook, deleteBook } from '@/api/book'
const { handleLogout } = useLogout();

// 书籍列表数据
const books = ref<any[]>([])
// 当前页码
const pageNum = ref<number>(1)
// 每页大小
const pageSize = ref<number>(5)
// 总页数
const totalPages = ref<number>(1)

// 搜索条件
const searchQuery = ref<string>('')
const selectedCategory = ref<string>('')

// 分类按钮数据
const categories = [
  '历史',
  '传记',
  '科幻',
  '少儿',
  '小说',
  '悬疑',
  '心理'
]

// 货架号映射表
const shelveMap = ref<Record<string, number>>({});

// 定义书籍类型接口
interface Book {
  title: string
  author: string
  publisher: string
  avaBooks: number
  stock: number
  category: string
  language: string
  description: string
  cover: string
}

// 移除 findShelveId 相关函数

onMounted(() => {
  fetchAllBooks()
})

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
    const response = await request.get(`/book/search`, {
      params: {
        pageNum: currentPage,
        pageSize: pageSize.value,
        keyword: searchQuery.value
      }
    })

    if (response.code === 200) {
      books.value = response.data.list.map((book: any) => ({
        ...book,
        avaBooks: book.avaBooks,
        changeAmount: 1
        // 不再添加 shelveId
      }))
      totalPages.value = response.data.totalPages
    } else {
      ElMessage.error('加载书籍列表失败')
    }
  } catch (error) {
    console.error('加载书籍列表失败:', error)
  }
}

// 回车键触发搜索
const handleKeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleSearchClick()
  }
}

// 点击搜索按钮
const handleSearchClick = () => {
  pageNum.value = 1
  fetchBooks(pageNum.value)
}

// 上一页
const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
    fetchBooks(pageNum.value)
  }
}

// 下一页
const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
    fetchBooks(pageNum.value)
  }
}

// 加载全部书籍（调用 /AllBook 接口）
const fetchAllBooksFromApi = async () => {
  try {
    const response = await request.get('/book/getAllBooks', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })

    if (response.code === 200) {
      books.value = response.data.list.map((book: any) => ({
        ...book,
        avaBooks: book.avaBooks,
        changeAmount: 1
      }))
      totalPages.value = response.data.totalPages
    } else {
      ElMessage.error('加载全部书籍失败')
    }
  } catch (error) {
    console.error('加载全部书籍失败:', error)
  }
}

// 加载全部书籍（使用 /AllBook 接口）
const fetchAllBooks = async () => {
  searchQuery.value = ''
  selectedCategory.value = ''
  pageNum.value = 1
  await fetchAllBooksFromApi()
}

// 分类按钮点击
const handleCategoryClick = (category: string) => {
  selectedCategory.value = category
  searchQuery.value = category
  pageNum.value = 1
  fetchBooks(pageNum.value)
}

// 修改库存和可借数量，并提交到后端
const changeStock = async (book: any, delta: number) => {
  const amount = book.changeAmount || 1

  if (delta === -1 && (book.stock <= 0 || book.avaBooks <= 0)) {
    ElMessage.warning('库存或可借数量不足')
    return
  }

  // 更新本地数据
  book.stock += delta * amount
  book.avaBooks += delta * amount

  try {
    const response = await request.post('/book/updateBook', book)

    if (response.code === 200) {
      ElMessage.success('库存更新成功')
    } else {
      ElMessage.error('更新失败，请重试')
      // 回滚数据
      book.stock -= delta * amount
      book.avaBooks -= delta * amount
    }
  } catch (error) {
    console.error('更新库存失败:', error)
    // 回滚数据
    book.stock -= delta * amount
    book.avaBooks -= delta * amount
  }
}

// 带确认的库存修改
const changeStockWithConfirm = async (book: any, delta: number) => {
  const actionText = delta === 1 ? '增加' : '减少'
  const amount = book.changeAmount || 1

  try {
    await ElMessageBox.confirm(
      `确认要${actionText}《${book.title}》的库存吗？数量：${amount}`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await changeStock(book, delta)
  } catch (error) {
    if (error === 'cancel') {
      ElMessage.info('已取消操作')
    } else {
      ElMessage.error('操作失败，请重试')
    }
  }
}

// 获取进度条颜色
const getProgressColor = (available: number, total: number) => {
  const percentage = (available / total) * 100
  if (percentage > 50) return '#67c23a'
  if (percentage > 20) return '#e6a23c'
  return '#f56c6c'
}

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

// 初始化加载
fetchBooks()

const showAddBookDialog = ref(false)
const addBookForm = ref({
  title: '',
  author: '',
  publisher: '',
  avaBooks: 1,
  stock: 1,
  category: '',
  language: '',
  description: '',
  cover: ''
})
const resetAddBookForm = () => {
  addBookForm.value = {
    title: '',
    author: '',
    publisher: '',
    avaBooks: 1,
    stock: 1,
    category: '',
    language: '',
    description: '',
    cover: ''
  }
}
const handleAddBook = async () => {
  try {
    await addBook(addBookForm.value)
    ElMessage.success('添加成功')
    showAddBookDialog.value = false
    resetAddBookForm()
    fetchAllBooks()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}
const handleDeleteBook = (title: string) => {
  ElMessageBox.confirm(`确认删除《${title}》吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteBook(title)
      ElMessage.success('删除成功')
      fetchAllBooks()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.admin-book-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  padding: 24px;
  color: #fff;
  font-family: 'Georgia', '思源宋体', serif;
  display: flex;
  flex-direction: column;
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
  flex-shrink: 0;
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
  color: #a78bfa;
}

.search-section {
  margin-bottom: 24px;
  flex-shrink: 0;
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
  margin-bottom: 16px;
  align-items: center;
}

.search-input {
  flex: 1;
}

/* 美化搜索输入框 */
:deep(.el-input) {
  --el-input-border-color: rgba(167, 139, 250, 0.3);
  --el-input-hover-border-color: #a78bfa;
  --el-input-focus-border-color: #8b5cf6;
  --el-input-bg-color: rgba(255, 255, 255, 0.05);
  --el-input-text-color: #fff;
  --el-input-placeholder-color: #aaa;
}

:deep(.el-input .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(167, 139, 250, 0.3);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  box-shadow: none;
}

:deep(.el-input .el-input__wrapper:hover) {
  border-color: #a78bfa;
  box-shadow: 0 0 0 1px rgba(167, 139, 250, 0.2);
}

:deep(.el-input .el-input__wrapper.is-focus) {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 1px rgba(139, 92, 246, 0.3);
}

:deep(.el-input .el-input__inner) {
  color: #fff;
}

:deep(.el-input .el-input__prefix) {
  color: #a78bfa;
}

.search-btn, .all-btn {
  white-space: nowrap;
}

.search-btn {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
  transition: all 0.3s ease;
}

.search-btn:hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.all-btn {
  background: linear-gradient(135deg, #c4b5fd 0%, #a78bfa 100%);
  border: none;
  color: #fff;
  transition: all 0.3s ease;
}

.all-btn:hover {
  background: linear-gradient(135deg, #a78bfa 0%, #c4b5fd 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(167, 139, 250, 0.3);
}

.category-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-btn {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.category-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.2);
}

.books-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px 0;
  min-height: 500px;
  flex: 1;
  position: relative;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin: 0 auto;
  max-width: 400px;
  margin-top: 100px;
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

.books-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 80px;
  position: relative;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  margin-top: 20px;
}

.book-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  width: 100%;
  min-width: 0;
  margin-bottom: 0;
  transform: none;
  z-index: 1;
}

.book-card:nth-child(1) {
  z-index: 10;
  transform: translateY(0);
}

.book-card:nth-child(2) {
  z-index: 9;
  transform: translateY(-4px);
}

.book-card:nth-child(3) {
  z-index: 8;
  transform: translateY(-8px);
}

.book-card:nth-child(4) {
  z-index: 7;
  transform: translateY(-12px);
}

.book-card:nth-child(5) {
  z-index: 6;
  transform: translateY(-16px);
}

.book-card:nth-child(6) {
  z-index: 5;
  transform: translateY(-20px);
}

.book-card:nth-child(7) {
  z-index: 4;
  transform: translateY(-24px);
}

.book-card:nth-child(8) {
  z-index: 3;
  transform: translateY(-28px);
}

.book-card:nth-child(9) {
  z-index: 2;
  transform: translateY(-32px);
}

.book-card:nth-child(10) {
  z-index: 1;
  transform: translateY(-36px);
}

.book-card:hover {
  transform: translateY(-20px) translateX(60px) scale(1.03);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 30px rgba(167, 139, 250, 0.2);
  z-index: 20;
}

.book-card:hover + .book-card {
  transform: translateY(-12px) translateX(50px);
}

.book-card:hover + .book-card + .book-card {
  transform: translateY(-16px) translateX(25px);
}

.book-cover-section {
  position: relative;
  height: 120px;
  overflow: hidden;
}

.book-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-status {
  position: absolute;
  top: 12px;
  right: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.available {
  background: rgba(103, 194, 58, 0.2);
  color: #67c23a;
  border: 1px solid rgba(103, 194, 58, 0.3);
}

.status-badge.unavailable {
  background: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
  border: 1px solid rgba(245, 108, 108, 0.3);
}

.book-info {
  padding: 10px;
}

.book-title {
  font-size: 15px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8px;
  line-height: 1.4;
}

.book-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #aaa;
  font-size: 12px;
}

.meta-item .el-icon {
  color: #a78bfa;
  font-size: 16px;
}

.book-stock {
  margin-bottom: 8px;
}

.stock-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.stock-label {
  color: #aaa;
  font-size: 12px;
}

.stock-numbers {
  color: #fff;
  font-weight: 500;
  font-size: 12px;
}

.stock-progress {
  margin-bottom: 8px;
}

/* 美化进度条 */
:deep(.el-progress) {
  --el-progress-bg-color: rgba(255, 255, 255, 0.1);
  --el-progress-bar-bg-color: #a78bfa;
}

:deep(.el-progress .el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  overflow: hidden;
}

:deep(.el-progress .el-progress-bar__inner) {
  border-radius: 10px;
  transition: all 0.3s ease;
}

.book-description {
  margin-bottom: 8px;
}

.book-description p {
  color: #ccc;
  font-size: 11px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-actions {
  padding: 0 10px 10px;
}

.stock-control {
  display: flex;
  gap: 8px;
  align-items: center;
}

.amount-input {
  width: 110px;
}

/* 美化Element Plus输入框 */
:deep(.el-input-number) {
  --el-input-border-color: rgba(167, 139, 250, 0.3);
  --el-input-hover-border-color: #a78bfa;
  --el-input-focus-border-color: #8b5cf6;
  --el-input-bg-color: rgba(255, 255, 255, 0.05);
  --el-input-text-color: #fff;
  --el-input-placeholder-color: #aaa;
}

:deep(.el-input-number .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(167, 139, 250, 0.3);
  border-radius: 8px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

:deep(.el-input-number .el-input__wrapper:hover) {
  border-color: #a78bfa;
  box-shadow: 0 0 0 1px rgba(167, 139, 250, 0.2);
}

:deep(.el-input-number .el-input__wrapper.is-focus) {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 1px rgba(139, 92, 246, 0.3);
}

:deep(.el-input-number .el-input__inner) {
  color: #fff;
  text-align: center;
}

:deep(.el-input-number .el-input-number__decrease),
:deep(.el-input-number .el-input-number__increase) {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
  transition: all 0.3s ease;
}

:deep(.el-input-number .el-input-number__decrease:hover),
:deep(.el-input-number .el-input-number__increase:hover) {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  transform: scale(1.05);
}

.stock-buttons {
  display: flex;
  gap: 8px;
}

.add-btn {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
  border-radius: 6px;
  transition: all 0.3s ease;
  min-width: 50px;
  height: 26px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  padding: 0 6px;
}

.add-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.add-btn:disabled {
  background: rgba(167, 139, 250, 0.3);
  color: rgba(255, 255, 255, 0.5);
  cursor: not-allowed;
}

.minus-btn {
  background: linear-gradient(135deg, #c4b5fd 0%, #a78bfa 100%);
  border: none;
  color: #fff;
  border-radius: 6px;
  transition: all 0.3s ease;
  min-width: 50px;
  height: 26px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  padding: 0 6px;
}

.minus-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #a78bfa 0%, #c4b5fd 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(167, 139, 250, 0.3);
}

.minus-btn:disabled {
  background: rgba(196, 181, 253, 0.3);
  color: rgba(255, 255, 255, 0.5);
  cursor: not-allowed;
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: 20px;
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .books-grid {
    grid-template-columns: repeat(2, 1fr);
    max-width: 700px;
    gap: 40px;
  }
}
@media (max-width: 700px) {
  .books-grid {
    grid-template-columns: 1fr;
    max-width: 100%;
    margin: 0;
    gap: 28px;
  }
  .book-card {
    margin-bottom: 12px;
  }
}
:deep(.el-input__inner),
:deep(.el-textarea__inner),
:deep(.el-input-number__inner) {
  color: #222 !important;
}
</style>