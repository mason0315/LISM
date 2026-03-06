<template>
  <div class="admin-shelves-container">
    <div class="top-bar">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>书架管理</span>
        <div>
          <button class="logout-btn" @click="handleLogout">退出登录</button>
          <el-button type="success" @click="showAddShelveDialog = true" style="margin-left: 16px;">添加货架</el-button>
        </div>
      </div>
    </div>
    <div class="shelves-list">
      <div
        v-for="shelve in shelves.slice((pageNum-1)*pageSize, pageNum*pageSize)"
        :key="shelve.shelveId"
        class="shelve-row"
      >
        <div
          class="shelve-card"
          :class="{ active: activeShelveId === shelve.shelveId }"
          @click="toggleActive(shelve.shelveId)"
        >
          <div class="shelve-header">
            <span class="shelve-id">书架ID: {{ shelve.shelveId }}</span>
            <span class="shelve-symbol">📚</span>
          </div>
        </div>
        <el-button type="danger" size="small" @click.stop="handleDeleteShelve(shelve.shelveId)" style="margin-left: 16px; height: 40px; align-self: center;">删除货架</el-button>
        <div
          v-if="activeShelveId === shelve.shelveId"
          class="books-slider"
        >
          <button class="arrow left" @click.stop="scrollLeft(shelve.shelveId)">‹</button>
          <div class="books-list" :ref="el => booksListRefs[getBooksListIndex(shelve.shelveId)] = (el as HTMLElement | null)">
            <div
              v-for="(book, idx) in shelve.books"
              :key="book.title + idx"
              class="book-card"
            >
              <img :src="book.cover || 'default-book.jpg'" class="book-card-cover" />
              <div class="book-card-title">{{ book.title }}</div>
              <div class="book-card-category">{{ book.category }}</div>
              <el-button type="danger" size="small" @click.stop="handleRemoveBookFromShelve(shelve.shelveId, book.title)" style="margin-top: 6px; width: 90px;">移出货架</el-button>
            </div>
          </div>
          <button class="arrow right" @click.stop="scrollRight(shelve.shelveId)">›</button>
        </div>
      </div>
    </div>
    <div class="pagination">
      <button @click="prevPage" :disabled="pageNum === 1">上一页</button>
      <span>第 {{ pageNum }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="pageNum === totalPages">下一页</button>
    </div>
  </div>
  <!-- 添加货架弹窗 -->
  <el-dialog v-model="showAddShelveDialog" title="添加货架" width="400px">
    <el-form :model="addShelveForm" label-width="80px">
      <el-form-item label="货架ID">
        <el-input-number v-model="addShelveForm.shelveId" :min="1" />
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model="addShelveForm.category" />
      </el-form-item>
      <el-form-item label="书名">
        <el-input v-model="addShelveForm.title" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAddShelveDialog = false">取消</el-button>
      <el-button type="primary" @click="handleAddShelve">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useLogout } from '@/utils/user'
import { addShelves, deleteShelves, removeBookFromShelve } from '@/api/shelves'
import { ElMessageBox } from 'element-plus'
const { handleLogout } = useLogout();

interface BookCard {
  title: string
  category: string
  cover?: string
}
// Shelve类型只包含shelveId和books
interface Shelve {
  shelveId: number
  books: BookCard[]
}

const shelves = ref<Shelve[]>([])
const activeShelveId = ref<number | null>(null)
const pageNum = ref(1)
const pageSize = 3 // 每页3行
const totalPages = ref(1)

// 横向滚动ref管理（用数组+索引，避免类型不兼容）
const booksListRefs: (HTMLElement | null)[] = []
function getBooksListIndex(shelveId: number) {
  return shelves.value.findIndex(s => s.shelveId === shelveId)
}
function scrollLeft(shelveId: number) {
  const idx = getBooksListIndex(shelveId)
  const el = booksListRefs[idx]
  if (el && el instanceof HTMLElement) el.scrollBy({ left: -300, behavior: 'smooth' })
}
function scrollRight(shelveId: number) {
  const idx = getBooksListIndex(shelveId)
  const el = booksListRefs[idx]
  if (el && el instanceof HTMLElement) el.scrollBy({ left: 300, behavior: 'smooth' })
}
function toggleActive(id: number) {
  activeShelveId.value = activeShelveId.value === id ? null : id
  nextTick(() => {
    if (activeShelveId.value !== null) {
      const idx = getBooksListIndex(activeShelveId.value)
      const el = booksListRefs[idx]
      if (el && el instanceof HTMLElement) el.scrollLeft = 0
    }
  })
}

const fetchShelves = async (currentPage = pageNum.value) => {
  try {
    // 1. 获取所有书架-书籍映射
    const shelvesRes = await request.get('/shelves/getAllShelves', { params: { pageNum: 1, pageSize: 1000 } })
    if (shelvesRes.code !== 200) return ElMessage.error('加载书架失败')
    const allShelves = shelvesRes.data.list as Array<{ shelveId: number; category: string; title: string }>

    // 2. 聚合成 { shelveId: BookCard[] }
    const shelveMap: Record<number, BookCard[]> = {}
    allShelves.forEach((item: { shelveId: number; category: string; title: string }) => {
      if (!shelveMap[item.shelveId]) shelveMap[item.shelveId] = []
      shelveMap[item.shelveId].push({ title: item.title, category: item.category })
    })

    // 3. 获取所有书的封面（并发请求）
    const allTitles = Array.from(new Set(allShelves.map((i: { title: string }) => i.title)))
    const titleToCover: Record<string, string> = {}
    await Promise.all(allTitles.map(async (title: string) => {
      const res = await request.get(`/book/findByTitle/${encodeURIComponent(title)}`)
      if (res.code === 200 && res.data && res.data.cover) {
        titleToCover[title] = res.data.cover
      }
    }))

    // 4. 组装最终 shelves 列表
    shelves.value = (Object.entries(shelveMap) as [string, BookCard[]][]).map(([shelveId, books]) => ({
      shelveId: Number(shelveId),
      books: books.map(b => ({
        ...b,
        cover: titleToCover[b.title] || 'default-book.jpg'
      }))
    }))
    // 分页信息
    totalPages.value = Math.max(1, Math.ceil(shelves.value.length / pageSize))
    pageNum.value = 1
  } catch (e) {
    ElMessage.error('网络错误，请重试')
  }
}
const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
  }
}
const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
  }
}
const showAddShelveDialog = ref(false)
const addShelveForm = ref({
  shelveId: 1,
  category: '',
  title: ''
})
const resetAddShelveForm = () => {
  addShelveForm.value = {
    shelveId: 1,
    category: '',
    title: ''
  }
}
const handleAddShelve = async () => {
  try {
    await addShelves(addShelveForm.value)
    ElMessage.success('添加成功')
    showAddShelveDialog.value = false
    resetAddShelveForm()
    fetchShelves()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}
const handleDeleteShelve = (shelveId: number) => {
  ElMessageBox.confirm(`确认删除货架ID为${shelveId}的货架吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteShelves(shelveId)
      ElMessage.success('删除成功')
      fetchShelves()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}
const handleRemoveBookFromShelve = (shelveId: number, title: string) => {
  ElMessageBox.confirm(`确认将《${title}》从货架ID为${shelveId}中移除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await removeBookFromShelve(shelveId, title)
      ElMessage.success('移除成功')
      fetchShelves()
    } catch (e) {
      ElMessage.error('移除失败')
    }
  }).catch(() => {})
}
onMounted(() => {
  fetchShelves()
})
</script>

<style scoped>
.admin-shelves-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #10101a 0%, #23233a 100%);
  padding: 24px;
  color: #fff;
  font-family: 'Arial', '思源宋体', serif;
}
.top-bar {
  padding: 20px 24px;
  font-size: 22px;
  font-weight: bold;
  background: rgba(30,30,40,0.85);
  border-radius: 16px;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
  border: 1px solid #23233a;
  color: #fff;
  box-shadow: 0 2px 12px #0003;
}
.shelves-list {
  display: flex;
  flex-direction: column;
  gap: 32px;
  margin: 0 auto 32px auto;
  max-width: 900px;
}
.shelve-row {
  display: flex;
  align-items: flex-start;
  position: relative;
  min-height: 180px;
}
.shelve-card {
  background: linear-gradient(135deg, #23233a 60%, #232526 100%);
  border-radius: 16px;
  padding: 32px 32px 32px 48px;
  box-shadow: 0 4px 20px #0002;
  border: 1.5px solid #23233a;
  min-width: 260px;
  min-height: 120px;
  max-width: 300px;
  transition: box-shadow 0.38s cubic-bezier(.4,1.6,.6,1),
              transform 0.48s cubic-bezier(.4,1.6,.6,1),
              width 0.4s cubic-bezier(.4,1.6,.6,1),
              min-width 0.4s cubic-bezier(.4,1.6,.6,1),
              background 0.3s cubic-bezier(.4,1.6,.6,1);
  cursor: pointer;
  z-index: 2;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: visible;
  border-bottom: 3px solid #23233a;
}
.shelve-card:hover, .shelve-card.active {
  will-change: transform, box-shadow, background;
}
.shelve-card:hover {
  background: linear-gradient(135deg, #23233a 80%, #232526 100%);
  box-shadow: 0 8px 32px #4f8cff33;
  transform: translateY(-2px) scale(1.03);
}
.shelve-card.active {
  transform: translateX(-60px) scale(0.88) rotate(-2deg);
  box-shadow: 0 8px 32px #4f8cff33;
  min-width: 140px;
  max-width: 160px;
  background: linear-gradient(135deg, #23233a 80%, #232526 100%);
}
.shelve-header {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 19px;
  margin-bottom: 8px;
  letter-spacing: 1px;
}
.shelve-id {
  font-weight: bold;
  color: cornflowerblue;
  text-shadow: 0 2px 8px #0002;
  font-size: 20px;
}
.shelve-symbol {
  font-size: 22px;
  transition: transform 0.3s;
}
.shelve-card:hover .shelve-symbol {
  transform: scale(1.12) rotate(-8deg);
}
.books-slider {
  display: flex;
  align-items: center;
  margin-left: 10px;
  position: relative;
  flex: 1;
  min-width: 0;
  z-index: 1;
}
.books-list {
  display: flex;
  gap: 32px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 18px 0 10px 0;
  min-height: 230px;
  max-width: 800px;
  background: none;
  position: relative;
}
.books-list::before,
.books-list::after {
  content: '';
  position: absolute;
  top: 0; bottom: 0;
  width: 24px;
  z-index: 2;
  pointer-events: none;
}
.books-list::before {
  left: 0;
  background: linear-gradient(90deg, #10101a 80%, transparent);
}
.books-list::after {
  right: 0;
  background: linear-gradient(-90deg, #10101a 80%, transparent);
}
.book-card {
  background: linear-gradient(135deg, #23233a 80%, #232526 100%);
  color: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px #4f8cff22;
  width: 160px;
  min-width: 160px;
  height: 230px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 12px 12px 12px;
  gap: 8px;
  transition: box-shadow 0.38s cubic-bezier(.4,1.6,.6,1),
              transform 0.38s cubic-bezier(.4,1.6,.6,1),
              background 0.28s cubic-bezier(.4,1.6,.6,1),
              opacity 0.28s cubic-bezier(.4,1.6,.6,1),
              filter 0.28s cubic-bezier(.4,1.6,.6,1);
  cursor: pointer;
  position: relative;
  opacity: 0;
  transform: scale(0.96) translateY(18px);
  animation: book-pop-in 0.45s cubic-bezier(.4,2,.6,1) forwards;
  animation-delay: calc(var(--book-idx, 0) * 50ms);
}
.book-card img {
  box-shadow: 0 2px 8px #4f8cff22;
}
.book-card:hover {
  box-shadow: 0 16px 40px #4f8cff55;
  transform: scale(1.09) translateY(-8px) rotate(-2deg);
  background: linear-gradient(135deg, #232526 80%, #23233a 100%);
  z-index: 10;
  transition: box-shadow 0.38s cubic-bezier(.4,1.6,.6,1),
              transform 0.38s cubic-bezier(.4,1.6,.6,1),
              background 0.28s cubic-bezier(.4,1.6,.6,1),
              opacity 0.28s cubic-bezier(.4,1.6,.6,1),
              filter 0.28s cubic-bezier(.4,1.6,.6,1);
}
.book-card-cover {
  width: 100px;
  height: 130px;
  object-fit: cover;
  border-radius: 7px;
  margin-bottom: 8px;
  background: #222;
  box-shadow: 0 2px 8px #4f8cff22;
}
.book-card-title {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 2px;
  color: cornflowerblue;
  text-align: center;
  width: 100%;
  overflow-wrap: break-word;
  word-break: break-all;
  white-space: normal;
  letter-spacing: 0.5px;
  line-height: 1.2;
}
.book-card-category {
  font-size: 13px;
  color: #8fa7d6;
  text-align: center;
  width: 100%;
  overflow-wrap: break-word;
  word-break: break-all;
  white-space: normal;
  line-height: 1.2;
}
@keyframes book-pop-in {
  0% {
    opacity: 0;
    transform: scale(0.92) translateY(32px);
    filter: blur(2px);
  }
  60% {
    opacity: 0.7;
    transform: scale(1.04) translateY(-6px);
    filter: blur(0.5px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
    filter: blur(0);
  }
}
.arrow {
  background: linear-gradient(135deg, #23233a 60%, #232526 100%);
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin: 0 8px;
  z-index: 10;
  transition: background 0.18s, box-shadow 0.18s;
  position: relative;
  box-shadow: 0 2px 8px #4f8cff22;
}
.arrow:hover {
  background: linear-gradient(135deg, #232526 60%, #23233a 100%);
  box-shadow: 0 4px 12px #4f8cff33;
}
.arrow.left {
  left: 0;
}
.arrow.right {
  right: 0;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 18px;
  margin-top: 28px;
  font-size: 15px;
  color: #ccc;
  background: rgba(30,30,40,0.85);
  border-radius: 16px;
  padding: 18px 0;
  backdrop-filter: blur(10px);
  border: 1px solid #23233a;
}
.pagination button {
  padding: 8px 18px;
  background: linear-gradient(90deg, #4f8cff 0%, #6a82fb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}
.pagination button:disabled {
  background: #555;
  cursor: not-allowed;
}
.pagination button:not(:disabled):hover {
  background: linear-gradient(90deg, #6a82fb 0%, #4f8cff 100%);
  box-shadow: 0 4px 12px #4f8cff33;
}
.logout-btn {
  padding: 8px 18px;
  background: linear-gradient(90deg, #ff4d4f 0%, #d9480f 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
  margin-left: 20px;
}
.logout-btn:hover {
  background: linear-gradient(90deg, #d9480f 0%, #ff4d4f 100%);
  box-shadow: 0 4px 12px #ff4d4f33;
}
</style>