<template>
  <div class="admin-suggestion-container">
    <div class="top-bar">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>留言管理</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </div>

    <!-- 搜索框区域 -->
    <div class="search-section">
      <div class="search-input">
        <input
          type="text"
          class="search-bar"
          placeholder="用户ID"
          v-model="userId"
          @keyup.enter="handleKeyPress"
        />
        <button class="search-button" @click="handleSearchClick">搜索</button>
        <button class="all-button" @click="fetchAllSuggestions">全部留言</button>
      </div>
    </div>

    <!-- 留言列表 -->
    <div class="message-list">
      <!-- 空状态提示 -->
      <div v-if="suggestions.length === 0" class="empty-state">
        暂无留言数据
      </div>

      <div class="message-item" v-for="msg in suggestions" :key="msg.id">
        <div class="message-header">
          <strong>用户ID：</strong>{{ msg.userId }}
        </div>
        <div class="message-content">
          {{ msg.content }}
        </div>
        <div class="message-time">
          留言时间：{{ formatTime(msg.createdAt) }}
        </div>
        <div class="message-actions">
          <button class="delete-button" @click="deleteSuggestion(msg.id)">删除</button>
        </div>
      </div>
    </div>
    <!-- 分页组件 -->
    <div class="pagination-wrapper">
      <div class="pagination">
        <button @click="prevPage" :disabled="pageNum === 1">上一页</button>
        <span>第 {{ pageNum }} 页 / 共 {{ totalPages }} 页</span>
        <button @click="nextPage" :disabled="pageNum === totalPages">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useLogout } from '@/utils/user'
const { handleLogout } = useLogout();

// 留言列表数据
const suggestions = ref<Suggestion[]>([])
// 当前页码
const pageNum = ref<number>(1)
// 每页大小
const pageSize = ref<number>(5)
// 总页数
const totalPages = ref<number>(1)

// 搜索条件
const userId = ref('')

// 新留言
const newSuggestion = ref({
  userId: '',
  content: ''
})

interface Suggestion {
  id: number
  userId: string
  content: string
  createdAt?: string
}

// 通用查询函数
const fetchSuggestions = async (currentPage: number = pageNum.value) => {
  try {
    const params: any = {
      pageNum: currentPage,
      pageSize: pageSize.value
    }

    if (userId.value) params.userId = userId.value

    const response = await request.get('/suggestion/getAllSuggestions', { params })
    console.log("加载留言数据成功:", response);

    if (response.code === 200) {
      const pageInfo = response.data
      suggestions.value = pageInfo.list || []
      totalPages.value = pageInfo.pages || 1
    } else {
      ElMessage.error(response.message || '加载失败')
    }
  } catch (error) {
    console.error('加载留言数据失败:', error)
  }
}

const deleteSuggestion = async (id: number) => {
  console.log("准备删除留言，ID:", id)

  try {
    const confirm = await ElMessageBox.confirm('确定要删除该留言吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (confirm) {
      const response = await request.delete(`/suggestion/deleteSuggestion/${id}`)
      console.log("删除接口返回：", response); // ✅ 查看返回

      if (response.code === 200) {
        ElMessage.success('删除成功')
        fetchSuggestions()
      } else {
        ElMessage.error(response.message || '删除失败')
      }
    }
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

// 回车触发搜索
const handleKeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleSearchClick()
  }
}

// 点击搜索按钮触发
const handleSearchClick = () => {
  pageNum.value = 1
  fetchSuggestions(1)
}

// 点击“全部”按钮
const fetchAllSuggestions = async () => {
  userId.value = ''
  pageNum.value = 1
  pageSize.value = 5
  fetchSuggestions(1)
}

// 上一页
const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
    fetchSuggestions(pageNum.value)
  }
}

// 下一页
const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
    fetchSuggestions(pageNum.value)
  }
}

// 时间格式化（修复了空值问题）
const formatTime = (time: string | undefined) => {
  if (!time) return '未知时间'
  const [datePart, timePart] = time.split('T')
  const timeNoMs = timePart ? timePart.split('.')[0] : ''
  return `${datePart} ${timeNoMs}`
}

// 初始化加载
fetchSuggestions()
</script>

<style scoped>
.admin-suggestion-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  padding: 24px;
  color: #fff;
  font-family: 'Georgia', '思源宋体', serif;
}

.top-bar {
  padding: 20px 24px;
  font-size: 22px;
  font-weight: bold;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
  color: #fff;
}

.search-section {
  margin-bottom: 24px;
}
.search-input {
  display: flex;
  gap: 12px;
  align-items: center;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
}
.search-bar {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid rgba(167,139,250,0.3);
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  color: #fff;
  font-size: 15px;
  transition: border-color 0.3s;
}
.search-bar:focus {
  border-color: #a78bfa;
  outline: none;
}
.search-button {
  padding: 10px 20px;
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}
.search-button:hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  box-shadow: 0 4px 12px rgba(139,92,246,0.2);
}
.all-button {
  padding: 10px 20px;
  background: linear-gradient(135deg, #c4b5fd 0%, #a78bfa 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}
.all-button:hover {
  background: linear-gradient(135deg, #a78bfa 0%, #c4b5fd 100%);
  box-shadow: 0 4px 12px rgba(167,139,250,0.2);
}

.message-list {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  margin-top: 20px;
  min-height: 400px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  margin-top: 32px;
}


.message-list > .pagination {
  margin-top: auto;
}

.message-item {
  background: rgba(255,255,255,0.05);
  padding: 14px 16px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.10);
  border: 1px solid rgba(255,255,255,0.10);
  backdrop-filter: blur(10px);
  transition: box-shadow 0.3s, transform 0.3s;
  min-width: 0;
  max-height: 180px;
  overflow: hidden;
}
.message-item:hover {
  box-shadow: 0 8px 32px rgba(139,92,246,0.18);
  transform: translateY(-2px) scale(1.01);
}

@media (max-width: 1100px) {
  .message-list {
    grid-template-columns: repeat(2, 1fr);
    max-width: 700px;
    gap: 20px;
  }
}
@media (max-width: 700px) {
  .message-list {
    grid-template-columns: 1fr;
    max-width: 100%;
    gap: 12px;
  }
}

.message-header {
  font-weight: bold;
  color: #a78bfa;
  font-size: 15px;
}

.message-content {
  color: #fff;
  margin-top: 2px;
  font-size: 14px;
  line-height: 1.4;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.message-time {
  font-size: 12px;
  color: #aaa;
  margin-top: 2px;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
}

.delete-button {
  padding: 8px 18px;
  background: linear-gradient(135deg, #ff4d4f 0%, #d9480f 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}
.delete-button:hover {
  background: linear-gradient(135deg, #d9480f 0%, #ff4d4f 100%);
  box-shadow: 0 4px 12px rgba(255,77,79,0.18);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #aaa;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  font-size: 18px;
  margin-top: 40px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.10);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 18px;
  font-size: 15px;
  color: #ccc;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  padding: 18px 0;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.10);
  width: 100%;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 0;
}
.pagination button {
  padding: 8px 18px;
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
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
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  box-shadow: 0 4px 12px rgba(139,92,246,0.18);
}
.logout-btn {
  padding: 8px 18px;
  background: linear-gradient(135deg, #ff4d4f 0%, #d9480f 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
  margin-left: 20px;
}
.logout-btn:hover {
  background: linear-gradient(135deg, #d9480f 0%, #ff4d4f 100%);
  box-shadow: 0 4px 12px rgba(255,77,79,0.18);
}
</style>