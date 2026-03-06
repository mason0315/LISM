<template>
  <div class="admin-user-container">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-title">
        <el-icon><user /></el-icon>
        <span>用户管理</span>
      </div>
      <div class="nav-actions">
        <el-button type="primary" @click="fetchAllUsers">
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
            v-model="searchUsername"
            placeholder="用户名"
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
          <el-button @click="fetchAllUsers" class="all-btn">
            <el-icon><list /></el-icon>
            全部
          </el-button>
        </div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="users-container">
      <!-- 空状态提示 -->
      <div v-if="users.length === 0" class="empty-state">
        <el-icon><user /></el-icon>
        <p>暂无用户数据</p>
      </div>

      <!-- 用户卡片列表 -->
      <div class="users-grid">
        <div class="user-card" v-for="user in users" :key="user.user_id">
                     <div class="user-header">
             <div class="user-avatar">
               <img :src="getRandomAvatar(user.user_id)" alt="User Avatar" class="avatar-img" />
             </div>
            <div class="user-basic-info">
              <h3 class="user-name">{{ user.username }}</h3>
              <div class="user-id">ID: {{ user.user_id }}</div>
            </div>
          </div>

          <div class="user-details">
            <div class="detail-item">
              <el-icon><message /></el-icon>
              <span class="detail-label">邮箱:</span>
              <span class="detail-value">{{ user.email || '未设置' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><phone /></el-icon>
              <span class="detail-label">电话:</span>
              <span class="detail-value">{{ user.phone || '未设置' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><lock /></el-icon>
              <span class="detail-label">密码:</span>
              <span class="detail-value password-mask">{{ user.password }}</span>
            </div>
          </div>

          <div class="user-actions">
            <el-button
              type="danger"
              size="small"
              @click="confirmDeleteUser(user)"
              class="action-btn delete-btn"
            >
              <el-icon><delete /></el-icon>
              删除用户
            </el-button>
            <el-button
              type="warning"
              size="small"
              @click="confirmResetPassword(user)"
              class="action-btn reset-btn"
            >
              <el-icon><refresh-left /></el-icon>
              重置密码
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
          :page-sizes="[9, 18, 36, 72]"
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
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { useLogout } from '@/utils/user'
const { handleLogout } = useLogout();

// 用户列表数据
const users = ref<any[]>([])

// 当前页码
const pageNum = ref<number>(1)
// 每页大小
const pageSize = ref<number>(9)
// 总页数
const totalPages = ref<number>(1)

// 搜索条件
const searchUsername = ref<string>('')

// 头像图片列表
const avatarImages = [
  '/Avatar/19728D6B9BC0E614C58002AA45B81425.jpg',
  '/Avatar/FE0B8E156ABDCCF4657A5FAE44E585F0.jpg',
  '/Avatar/4BF389DDF638F996FF4E8F9AC357C569.jpg',
  '/Avatar/D2393DF293F4DE1AEF8064D916774A71.jpg',
  '/Avatar/BF61DF5B8514A3EE820E46CDEF8BA8DC.jpg',
  '/Avatar/249D55C7CEB859780181FA479335A59A.jpg',
  '/Avatar/0EE38B826A1A75DC4D5742BFCD4ECF5E.jpg',
  '/Avatar/A6F3E792BA6566C1487F01A902AC334C.jpg',
  '/Avatar/1F8358AEF12898C373A33C4754F31DD2.jpg',
  '/Avatar/571AEDDAB82D37D205136FABDCD0A334.jpg',
  '/Avatar/AA40F980D849BFBCBA557B9255CFA246.jpg'
]

// 获取随机头像
const getRandomAvatar = (userId: number) => {
  const index = userId % avatarImages.length
  return avatarImages[index]
}

// 获取所有用户
const fetchAllUsers = async () => {
  try {
    const response = await request.get('/users/AllUsers', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      },
    })

    if (response.code === 200) {
      users.value = response.data.list
      const pageInfo = response.data
      totalPages.value = pageInfo.pages
    } else {
      ElMessage.error('加载用户列表失败')
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

// 根据用户名搜索用户
const fetchUsersBy = async () => {
  try {
    const response = await request.post(
      '/users/UsersBy',
      {
        username: searchUsername.value,
      },
      {
        params: {
          pageNum: pageNum.value,
          pageSize: pageSize.value,
        },
      },
    )

    if (response.code === 200) {
      users.value = response.data.list
      const pageInfo = response.data
      totalPages.value = pageInfo.pages
    } else {
      ElMessage.error('搜索用户失败')
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
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
  fetchUsersBy()
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  searchUsername.value ? fetchUsersBy() : fetchAllUsers()
}

const handleCurrentChange = (page: number) => {
  pageNum.value = page
  searchUsername.value ? fetchUsersBy() : fetchAllUsers()
}

// 加载全部用户
const fetchAllUsersAndReset = () => {
  searchUsername.value = ''
  pageNum.value = 1
  fetchAllUsers()
}

// 删除用户
const confirmDeleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(`确认要删除用户【${user.username}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteUser(user.user_id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请重试')
    }
  }
}

const deleteUser = async (userId: number) => {
  try {
    const response = await request.delete(`/users/deleteUser/${userId}`)

    if (response.code === 200) {
      ElMessage.success('用户删除成功')
      fetchAllUsers()
    } else {
      ElMessage.error('删除失败，请重试')
    }
  } catch (error) {
    console.error('删除用户失败:', error)
  }
}

// 重置密码为 123456
const confirmResetPassword = async (user: any) => {
  try {
    await ElMessageBox.confirm(`确认要将用户【${user.username}】的密码重置为"123456"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await resetPassword(user.user_id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败，请重试')
    }
  }
}

const resetPassword = async (userId: number) => {
  try {
    const response = await request.post('/users/updatepa', {
      user_id: userId,
      password: '123456',
    })

    if (response.code === 200) {
      ElMessage.success('密码重置成功')
      fetchAllUsers()
    } else {
      ElMessage.error('重置失败，请重试')
    }
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

// 初始化加载
fetchAllUsersAndReset()
</script>

<style scoped>
.admin-user-container {
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
  color: #a78bfa;
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
}

.search-input {
  flex: 1;
}

.search-btn, .all-btn {
  white-space: nowrap;
}

.search-btn {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
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
}

.all-btn:hover {
  background: linear-gradient(135deg, #a78bfa 0%, #c4b5fd 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(167, 139, 250, 0.3);
}

.users-container {
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

.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
}

.user-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.user-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-basic-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 4px;
}

.user-id {
  font-size: 14px;
  color: #aaa;
}

.user-role {
  margin-left: auto;
}

.user-details {
  margin-bottom: 20px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.detail-item .el-icon {
  color: #a78bfa;
  font-size: 16px;
  width: 16px;
}

.detail-label {
  color: #aaa;
  font-size: 14px;
  min-width: 40px;
}

.detail-value {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  flex: 1;
}

.password-mask {
  font-family: monospace;
  letter-spacing: 2px;
}

.user-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
}

.delete-btn {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
}

.delete-btn:hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.reset-btn {
  background: linear-gradient(135deg, #c4b5fd 0%, #a78bfa 100%);
  border: none;
  color: #fff;
}

.reset-btn:hover {
  background: linear-gradient(135deg, #a78bfa 0%, #c4b5fd 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(167, 139, 250, 0.3);
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
  .admin-user-container {
    padding: 16px;
  }
  
  .users-grid {
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
  
  .user-actions {
    flex-direction: column;
  }
}
</style>
