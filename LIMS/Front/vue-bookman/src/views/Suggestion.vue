<template>
  <div class="client-suggestion-container">
    <div class="top-bar">
      <span>留言板</span>
    </div>

    <!-- 留言列表 -->
    <div class="message-list">
      <div v-if="suggestions.length === 0" class="empty-state">
        暂无留言，快来发表你的想法吧！
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

    <!-- 悬浮留言按钮 -->
    <button class="floating-add-btn" @click="showDrawer = true">
      <span style="font-size: 22px;">💬</span> 留言
    </button>

    <!-- 底部弹出留言输入框 -->
    <el-drawer
      v-model="showDrawer"
      direction="btt"
      size="260px"
      :with-header="false"
      custom-class="drawer-style"
    >
      <div class="drawer-content">
        <div class="drawer-title">发表留言</div>
        <el-input
          v-model="newSuggestion.content"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="请输入留言内容（最多200字）"
          class="drawer-input"
        />
        <div class="drawer-actions">
          <el-button @click="showDrawer = false">取消</el-button>
          <el-button type="primary" @click="submitSuggestion" :loading="submitting">发布</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUserId } from '@/utils/user'

const suggestions = ref<Suggestion[]>([])
const pageNum = ref<number>(1)
const pageSize = ref<number>(5)
const totalPages = ref<number>(1)
const showDrawer = ref(false)
const submitting = ref(false)

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

const fetchSuggestions = async (currentPage: number = pageNum.value) => {
  try {
    const params: any = {
      pageNum: currentPage,
      pageSize: pageSize.value
    }
    const response = await request.get('/suggestion/getAllSuggestions', { params })
    if (response.code === 200) {
      const pageInfo = response.data
      suggestions.value = pageInfo.list || []
      totalPages.value = pageInfo.pages || 1
    } else {
      ElMessage.error(response.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error('加载留言失败')
  }
}

const submitSuggestion = async () => {
  // 自动获取当前登录用户id
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  if (!newSuggestion.value.content.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  submitting.value = true
  try {
    const payload = {
      userId: userId,
      content: newSuggestion.value.content
    }
    const response = await request.post('/suggestion/addSuggestion', payload)
    if (response.code === 200) {
      ElMessage.success('留言成功！')
      showDrawer.value = false
      newSuggestion.value.content = ''
      fetchSuggestions(1)
      pageNum.value = 1
    } else {
      ElMessage.error(response.message || '留言失败')
    }
  } catch (error) {
    ElMessage.error('留言失败')
  } finally {
    submitting.value = false
  }
}

const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
    fetchSuggestions(pageNum.value)
  }
}
const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
    fetchSuggestions(pageNum.value)
  }
}

const formatTime = (time: string | undefined) => {
  if (!time) return '未知时间'
  const [datePart, timePart] = time.split('T')
  const timeNoMs = timePart ? timePart.split('.')[0] : ''
  return `${datePart} ${timeNoMs}`
}

fetchSuggestions()
</script>

<style scoped>
.client-suggestion-container {
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
  grid-column: 1/-1;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  margin-top: 32px;
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
.floating-add-btn {
  position: fixed;
  right: 36px;
  bottom: 36px;
  z-index: 1001;
  padding: 16px 32px;
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  color: #fff;
  border: none;
  border-radius: 32px;
  font-size: 18px;
  font-weight: bold;
  box-shadow: 0 4px 20px rgba(139,92,246,0.18);
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s, transform 0.2s;
}
.floating-add-btn:hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  transform: scale(1.05);
}
.drawer-style {
  border-radius: 24px 24px 0 0 !important;
  background: linear-gradient(135deg, #1a1a2e 0%, #08080c 100%) !important;
  color: #fff;
}
.drawer-content {
  padding: 24px 16px 0 16px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.drawer-title {
  font-size: 20px;
  font-weight: bold;
  color: #a78bfa;
  margin-bottom: 8px;
}
.drawer-input {
  margin-bottom: 8px;
}
/* 移除隐藏用户ID输入框的样式 */
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
</style>
