<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchBooksByKeyword } from '@/api/search'
import { getCurrentUserId } from '@/utils/user' // ✅ 使用新方法获取 user_id
import { addBorrowRecord,decreaseAvailableBooks } from '@/api/borrow' // 假设你已创建该接口
import { ElMessageBox, ElDatePicker } from 'element-plus'
import { getCommentsByTitle, addComment as addCommentApi } from '@/api/comment'
import { getCurrentUsername } from '@/utils/user'
import request from '@/utils/request'

interface Book {
  title: string;
  love: number;
  author: string;
  publisher: string;
  avaBooks: number;
  stock: number;
  category: string;
  language: string;
  description: string;
  cover: string;
}


// 响应式数据
const book = ref<Book & { shelveId?: number } | null>(null)
const route = useRoute()
const router = useRouter()

// 日期选择相关
const showDatePicker = ref(false)
const startDate = ref<Date>(new Date())
const endDate = ref<Date>(new Date())

// 获取推荐书籍列表（用于 popular section）
const popularBooks = [
  { cover: '/images/book1.jpg' },
  { cover: '/images/book2.jpg' },
  { cover: '/images/book3.jpg' },
  { cover: '/images/book4.jpg' },
  { cover: '/images/book5.jpg' }
]

const loading = ref(true)
const shelveMap = ref<Record<string, number>>({});

// 根据路由参数匹配书籍
onMounted(async () => {
  loading.value = true;
  // 批量获取所有书的货架号映射表
  try {
    const shelvesRes = await request.get('/shelves/getAllShelves', { params: { pageNum: 1, pageSize: 1000 } });
    if (shelvesRes.code === 200 && shelvesRes.data?.list) {
      shelveMap.value = {};
      shelvesRes.data.list.forEach((item: any) => {
        shelveMap.value[item.title] = item.shelveId;
      });
    }
  } catch (e) {}

  const title = decodeURIComponent(route.params.bookTitle as string);

  try {
    const result = await searchBooksByKeyword(title, 1, 1);
    console.log('API 返回:', result); // 打印返回数据

    const foundBook = result?.data?.list?.[0] ?? null;

    if (foundBook) {
      const shelveId = shelveMap.value[foundBook.title];
      book.value = { ...foundBook, shelveId };
    } else {
      console.log('未找到相关书籍');
      book.value = null;
    }
  } catch (error) {
    console.error('获取书籍详情失败:', error);
    book.value = null;
  } finally {
    loading.value = false;
  }
});


// 借阅功能
const borrowBook = async () => {
  if (!book.value) return
  if (book.value.stock <= 0) {
    alert('库存不足，无法借阅')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    router.push('/login')
    return
  }

  showDatePicker.value = true
}

// 确认选择日期范围
const confirmDateRange = async () => {
  if (!startDate.value || !endDate.value) {
    alert('请选择完整的借阅日期范围')
    return
  }

  if (endDate.value < startDate.value) {
    alert('结束日期必须大于开始日期')
    return
  }

  try {
    // 构建借阅记录
    const borrowRecord = {
      title: book.value!.title,
      userId: Number(getCurrentUserId()),
      borrowDate: startDate.value.toISOString(),
      dueDate: endDate.value.toISOString(),
      returnDate: null, // 初始借阅时不设置归还日期
      status: 'borrowed'
      // 不传recordId，后端自增或可选
    };
    console.log('提交借阅记录:', borrowRecord);

    // 提交借阅记录
    const response = await addBorrowRecord(borrowRecord)

    if (response.code === 200) {
      // 借阅成功后减少可借数量
      const decreaseResponse = await decreaseAvailableBooks(book.value!.title)
      if (decreaseResponse.code === 200) {
        book.value!.avaBooks -= 1 // 更新本地状态显示
        book.value!.love += 1
      } else {
        alert('可借数量更新失败')
      }
      alert(`您已成功借阅《${book.value!.title}》，借阅日期：${startDate.value.toLocaleDateString()} 到 ${endDate.value.toLocaleDateString()}`)

      // 重置日期选择
      startDate.value = new Date()
      endDate.value = new Date()
      showDatePicker.value = false
    } else {
      alert('借阅失败: ' + (response.message || '服务器错误'))
    }
  } catch (error) {
    console.error('借阅失败:', error)
    alert('借阅失败，请稍后再试')
  }
}

// 取消选择日期范围
const cancelDateRange = () => {
  startDate.value = new Date()
  endDate.value = new Date()
  showDatePicker.value = false
}


// 预约功能
const reserveBook = () => {
  if (book.value) {
    alert(`您已成功预约《${book.value.title}》`)
  }
}

// 评论区相关
import { ref as vueRef } from 'vue'
const showCommentDrawer = vueRef(false)
const comments = vueRef<any[]>([])
const newComment = vueRef('')

function formatTime(time: string | Date) {
  if (!time) return '';
  const date = typeof time === 'string' ? new Date(time) : time;
  return date.toLocaleString();
}

// 加载评论
const loadComments = async () => {
  if (!book.value) return
  try {
    const res = await getCommentsByTitle(book.value.title)
    comments.value = (res.data || []).map((c: any) => ({
      id: c.commentId,
      user: c.username,
      content: c.comment,
      createdAt: c.createdAt
    }))
  } catch (e) {
    comments.value = []
  }
}

// 打开评论抽屉时加载评论
const openCommentDrawer = async () => {
  showCommentDrawer.value = true
  await loadComments()
}

// 添加评论
const addComment = async () => {
  if (!newComment.value.trim() || !book.value) return
  const username = getCurrentUsername()
  if (!username) {
    alert('请先登录')
    router.push('/login')
    return
  }
  try {
    await addCommentApi({
      title: book.value.title,
      username,
      comment: newComment.value
    })
    newComment.value = ''
    await loadComments()
  } catch (e) {
    alert('评论发送失败')
  }
}

// 点击推荐书籍跳转到借阅页面
const goToBorrowPage = (selectedBook: any) => {
  router.push({ name: 'Borrowing', params: { bookTitle: encodeURIComponent(selectedBook.title) } })
}

const closeCommentDrawer = () => {
  showCommentDrawer.value = false
}

</script>



<template>
  <div class="back-button" @click="$router.go(-1)">
    <i class="fas fa-arrow-left"></i>返回
  </div>
  <div v-if="loading" style="display: none;"></div>
  <div v-else-if="book" class="book-detail">
    <!-- 左侧：书籍封面 -->
    <div class="book-cover">
      <img :src="book.cover" alt="Book Cover">
    </div>

    <!-- 右侧：书籍信息 -->
    <div class="book-info">
      <h1>{{ book.title }}</h1>
      <p class="author"><strong>作者:</strong> {{ book.author }}</p>
      <p class="description">{{ book.description }}</p>
      <div class="meta-info">
        <div class="info-item">
          <i class="fas fa-book"></i>
          <span><strong>种类:</strong> {{ book.category }}</span>
        </div>
        <div class="info-item">
          <i class="fas fa-building"></i>
          <span><strong>出版社:</strong> {{ book.publisher }}</span>
        </div>
        <!-- 添加库存量和可借阅量显示 -->
        <div class="info-item">
          <i class="fas fa-box"></i>
          <span><strong>库存量:</strong> {{ book.stock }}</span>
        </div>
        <div class="info-item">
          <i class="fas fa-book-reader"></i>
          <span><strong>可借阅量:</strong> {{ book?.avaBooks }}</span>
        </div>
        <div class="info-item">
          <i class="fas fa-archive"></i>
          <span><strong>货架号:</strong> {{ book.shelveId || '无' }}</span>
        </div>
        <!-- 操作按钮 -->
        <div class="book-actions">
          <button @click="borrowBook" class="action-button">借阅</button>
          <button @click="reserveBook" class="action-button">预约</button>
          <button class="comment-btn" @click="openCommentDrawer">
            <i class="fas fa-comment-dots"></i>
          </button>
        </div>
        <!-- 日期选择器 -->
        <div v-if="showDatePicker" class="date-picker-container">
          <div class="date-picker-row">
            <label class="date-label">开始日期：</label>
            <el-date-picker
              v-model="startDate"
              type="date"
              placeholder="选择开始日期"
              :size="'default'"
              :clearable="false"
              class="date-input"
            />
          </div>
          <div class="date-picker-row">
            <label class="date-label">结束日期：</label>
            <el-date-picker
              v-model="endDate"
              type="date"
              placeholder="选择结束日期"
              :size="'default'"
              :clearable="false"
              class="date-input"
            />
          </div>
          <div class="date-picker-buttons">
            <button @click="confirmDateRange" class="date-button confirm">确定</button>
            <button @click="cancelDateRange" class="date-button cancel">取消</button>
          </div>
        </div>
        <div class="popular-section">
          <div class="popular-title-container">
            <h2 class="popular-title">最受欢迎</h2>
          </div>
          <div class="popular-books">
            <img
              v-for="(book, index) in popularBooks"
              :key="index"
              :src="book.cover"
              alt="Book Cover"
              class="book-cover"
              @click="goToBorrowPage(book)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="not-found">
    <p>未找到该书籍，请返回重新搜索。</p>
  </div>

  <!-- 评论区弹窗 -->
  <transition name="slide-up">
    <div v-if="showCommentDrawer" class="comment-drawer">
      <div class="comment-header">
        <span>评论区</span>
        <button class="close-btn" @click="closeCommentDrawer">×</button>
      </div>
      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-main">
            <span class="comment-user">{{ comment.user }}</span>
            <span class="comment-content">{{ comment.content }}</span>
            <span class="comment-time" style="margin-left:10px;color:#aaa;font-size:12px;">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <div class="comment-actions">
            <!-- Removed reply button -->
          </div>
        </div>
      </div>
      <div class="add-comment-row">
        <input v-model="newComment" class="add-comment-input" placeholder="说点什么..." @keyup.enter="addComment" />
        <button class="send-comment-btn" @click="addComment">发送</button>
      </div>
    </div>
  </transition>
</template>



<style scoped>
.book-detail {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 150px;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  width: 80%;
  max-width: 100%;
}

.book-cover img {
  width: 250px;
  height: 350px;
  object-fit: cover;
  margin-right: 30px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.book-info {
  margin-left: 100px;
  color: #e0e0e0;
}

.book-info h1 {
  font-size: 24px;
  margin-bottom: 1px;
  color: #ffffff;
}

.book-info .author {
  font-size: 16px;
  margin-bottom: 10px;
  color: #5b82f6;
}

.rating-comments i {
  margin-right: 5px;
}

.description {
  font-size: 13px;
  margin-bottom: 50px;
  line-height: 1.8;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  margin-right: 20px;
}

.info-item i {
  margin-right: 5px;
}

.book-actions {
  display: flex;
  justify-content: flex-start;
  margin-top: 63px;
}

.action-button {
  margin: 5px 25px;
  padding: 8px 50px;
  font-size: 13px;
  cursor: pointer;
  background-color: #4688e1;
  color: black;
  border: none;
  border-radius: 13px;
  transition: background-color 0.9s;
}

.action-button:hover {
  background-color: #c1c9e6;
}

.not-found {
  text-align: center;
  margin: 20px;
  color: #e0e0e0;
}

.popular-section {
  max-width: 800px;
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-left: -210px; /* 根据需要调整值 */
}

.popular-title-container {
  width: 130%;
  padding-bottom: 10px;
  margin-bottom: 20px;
  position: relative;
}

.popular-title {
  font-size: 18px;
  color: cornflowerblue;
  margin: 0;
  text-align: left;
  position: absolute;
  bottom: 15px;
  left: 0;
}

.popular-title-container::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 100%;
  height: 1px;
  background-color: cornflowerblue;
}

.popular-books {
  display: flex;
  justify-content: space-between;
  gap: 15px;
}

.book-cover {
  width: calc(25% - 7.5px);
  height: auto;
  cursor: pointer;
  transition: transform 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.book-cover:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.back-button {
  position: absolute;
  top: 20px;
  left: 20px;
  display: flex;
  align-items: center;
  color: cornflowerblue;
  font-size: 16px;
  cursor: pointer;
  z-index: 10;
  background-color: transparent;
  border: none;
}

.back-button i {
  margin-right: 8px;
  font-size: 25px;
}
.date-picker-container .block {
  text-align: center;
}

.date-picker-buttons {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
}

.date-button {
  margin-left: 10px;
  padding: 4px 30px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.date-button.confirm {
  background-color: #112c68;
  color: white;
}

.date-button.confirm:hover {
  background-color: #74a6d1;
}

.date-button.cancel {
  background-color: #5e0a04;
  color: white;
}

.date-button.cancel:hover {
  background-color: #ef9090;
}

.comment-btn {
  background: transparent;
  border: none;
  margin-left: 10px;
  font-size: 22px;
  color: cornflowerblue;
  cursor: pointer;
  transition: color 0.3s;
}
.comment-btn:hover {
  color: #74a6d1;
}
.comment-drawer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #181828;
  border-top-left-radius: 18px;
  border-top-right-radius: 18px;
  box-shadow: 0 -2px 16px #0008;
  z-index: 1000;
  max-height: 60vh;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.4s;
}
@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.slide-up-enter-active, .slide-up-leave-active {
  transition: all 0.4s cubic-bezier(.25,.8,.25,1);
}
.slide-up-enter-from, .slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px 8px 20px;
  font-size: 18px;
  color: cornflowerblue;
  border-bottom: 1px solid #23233a;
}
.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #aaa;
  cursor: pointer;
}
.comment-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 20px;
}
.comment-item {
  margin-bottom: 18px;
  background: #23233a;
  border-radius: 10px;
  padding: 10px 14px;
}
.comment-main {
  display: flex;
  align-items: center;
  gap: 10px;
}
.comment-user {
  color: #5b82f6;
  font-weight: bold;
  margin-right: 8px;
}
.comment-content {
  color: #fff;
}
.comment-actions {
  margin-top: 6px;
}
.reply-btn {
  background: none;
  border: none;
  color: #74a6d1;
  cursor: pointer;
  font-size: 14px;
}
.replies {
  margin-top: 8px;
  padding-left: 18px;
}
.reply-item {
  margin-bottom: 4px;
  color: #ccc;
}
.reply-user {
  color: #5b82f6;
  margin-right: 6px;
}
.reply-input-row {
  display: flex;
  align-items: center;
  margin-top: 8px;
  gap: 8px;
}
.reply-input {
  flex: 1;
  padding: 4px 10px;
  border-radius: 6px;
  border: none;
  background: #23233a;
  color: #fff;
}
.send-reply-btn {
  background: #4688e1;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 4px 16px;
  cursor: pointer;
}
.add-comment-row {
  display: flex;
  align-items: center;
  padding: 12px 20px 18px 20px;
  border-top: 1px solid #23233a;
  background: #181828;
}
.add-comment-input {
  flex: 1;
  padding: 6px 12px;
  border-radius: 8px;
  border: none;
  background: #23233a;
  color: #fff;
  font-size: 15px;
}
.send-comment-btn {
  background: #4688e1;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 6px 22px;
  margin-left: 10px;
  cursor: pointer;
}
</style>

