<template>
  <div class="dashboard-container">
    <!-- 顶栏导航 -->
    <div v-if="showAside" class="top-navbar">
      <div class="top-menu-container">
        <el-menu :default-openeds="['1', '2', '3']" background-color="#0f0f1a" text-color="#fff" active-text-color="blue" mode="horizontal">
          <!-- 探索板块 -->
          <el-sub-menu index="1">
            <template #title>
              <el-icon><document /></el-icon>探索🔍
            </template>
            <el-menu-item-group title="种类">
              <el-menu-item index="1-1" @click="goToBookClass('历史')">历史</el-menu-item>
              <el-menu-item index="1-2" @click="goToBookClass('传记')">传记</el-menu-item>
              <el-menu-item index="1-3" @click="goToBookClass('科幻')">科幻</el-menu-item>
              <el-menu-item index="1-4" @click="goToBookClass('少儿')">少儿</el-menu-item>
              <el-menu-item index="1-5" @click="goToBookClass('小说')">小说</el-menu-item>
              <el-menu-item index="1-6" @click="goToBookClass('悬疑')">悬疑</el-menu-item>
              <el-menu-item index="1-7" @click="goToBookClass('心理')">心理</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>

          <!-- 每日推荐板块 -->
          <el-sub-menu index="2">
            <template #title>
              <el-icon><clock /></el-icon>每日推荐👍
            </template>
            <el-menu-item-group>
              <el-menu-item index="2-1" @click="goToTop">好书榜单💴</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <!-- 交流与分享板块 -->
          <el-sub-menu index="3">
            <template #title>
              <el-icon><chat-line-round /></el-icon>交流与分享✍
            </template>
            <el-menu-item-group>
              <el-menu-item index="3-2" @click="goToReadingAndSharing">热心留言✏</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <!-- 智能问答板块 -->
          <el-sub-menu index="4">
            <template #title>
              <el-icon><chat-dot-round /></el-icon>智能问答💡
            </template>
            <el-menu-item-group>
              <el-menu-item index="4-1" @click="goToAiQa">开始问答🔮</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
        </el-menu>
      </div>
    </div>
    <!-- 登录/注册按钮 -->
    <div class="floating-buttons" v-if="showAside">
      <template v-if="!isLoggedIn">
        <button class="nav-button" @click="showLoginModal = true">登录</button>
        <button class="nav-button" @click="showRegisterModal = true">注册</button>
      </template>
      <template v-else>
        <button class="nav-button" @click="goToPersonalCenter">个人中心</button>
      </template>
    </div>

    <!-- 主体内容 -->
    <main :class="['main-content', !showAside && 'fullscreen']">
      <!-- Logo 和标语 -->
      <div class="logo-section">
        <img src="@/assets/logoLIMS.png" alt="Library Logo" class="library-logo" />
        <p class="slogan">过去的一切精华尽在书中！</p>
      </div>

      <!-- 搜索框 -->
      <div class="search-section">
        <div class="search-tabs">
          <button class="search-tab active">通用搜索</button>
        </div>
        <div class="search-input">
          <input
            type="text"
            class="search-bar"
            placeholder="按书名、作者、ISBN、DOI、出版社、MD5等搜索..."
            v-model="searchQuery"
            @keyup.enter="handleKeyPress"
          />
          <button class="search-button" @click="handleSearchClick">搜索</button>
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
    </main>

    <!-- 登录模态框 -->
    <transition name="fade">
      <div v-if="showLoginModal" class="login-wrapper">
        <Login @close="onLoginClose" />
      </div>
    </transition>

    <!-- 注册模态框 -->
    <transition name="fade">
      <div v-if="showRegisterModal" class="register-wrapper">
        <Register @close="onRegisterClose" @register-success="onRegisterSuccess" />
      </div>
    </transition>
  </div>
</template>


<script>
import { useRouter } from 'vue-router'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import { searchBooksByKeyword } from '@/api/search'

export default {
  name: 'UserDashboard',
  components: {
    Register,
    Login
  },
  setup() {

    const router = useRouter()

    // 管理员拦截逻辑
    if (typeof window !== 'undefined') {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          const user = JSON.parse(userStr);
          const role = user.role;
          if (role === 1) {
            router.push('/adminuser');
          } else if (role === 2) {
            router.push('/adminbook');
          } else if (role === 3) {
            router.push('/recordctrl');
          } else if (role === 4) {
            router.push('/suggestionctrl');
          } else if (role === 5) {
            router.push('/shelve');
          }
        } catch (e) {}
      }
    }

    const goToReadingAndSharing = () => {
      // 隐藏侧栏
      if (typeof window !== 'undefined' && window.__VUE_DEVTOOLS_GLOBAL_HOOK__) {
        // 兼容devtools
      }
      // 通过this访问showAside
      if (typeof this !== 'undefined') this.showAside = false;
      router.push('/reading-and-sharing')
    }
    const goToTop = () => {
      if (typeof this !== 'undefined') this.showAside = false;
      router.push('/top')
    }

    return {
      // ... existing return values ...
      goToTop,
      goToReadingAndSharing
    }
  },

  data() {
    return {
      popularBooks: [
        { cover: '/images/book15.jpg', title: '万历十五年' },
        { cover: '/images/book6.jpg', title: '人类简史：从动物到上帝' },
        { cover: '/images/book2.jpg', title: '你当像鸟飞向你的山' },
        { cover: '/images/book8.jpg', title: '哈利波特全集' },
        { cover: '/images/book7.jpg', title: '小王子' },
        { cover: '/images/book13.jpg', title: '平凡的世界' },
        { cover: '/images/book11.jpg', title: '底层逻辑：看清这个世界的底牌' },
        { cover: '/images/book17.jpg', title: '恶意' },
        { cover: '/images/book1.jpg', title: '我与地坛' },
        { cover: '/images/book4.jpg', title: '明朝那些事儿' },
        { cover: '/images/book20.jpg', title: '杀死一只知更鸟' },
        { cover: '/images/book3.jpg', title: '毛泽东选集' },
        { cover: '/images/book18.jpg', title: '流俗地' },
        { cover: '/images/book19.jpg', title: '瓦尔登湖' },
        { cover: '/images/book9.jpg', title: '白夜行' },
        { cover: '/images/book16.jpg', title: '百年孤独' },
        { cover: '/images/book12.jpg', title: '盗墓笔记' },
        { cover: '/images/book10.jpg', title: '红楼梦' },
        { cover: '/images/book14.jpg', title: '资本论' },
        { cover: '/images/book5.jpg', title: '长安的荔枝' }
      ],
      showAside: true,
      showLoginModal: false,
      showRegisterModal: false,
      searchQuery: '', // 用于保存输入框内容
      isLoggedIn: false
    }
  },
  mounted() {
    // 管理员拦截逻辑
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        const role = user.role;
        if (role === 1) {
          this.$router.push('/adminuser');
        } else if (role === 2) {
          this.$router.push('/adminbook');
        } else if (role === 3) {
          this.$router.push('/recordctrl');
        } else if (role === 4) {
          this.$router.push('/suggestionctrl');
        } else if (role === 5) {
          this.$router.push('/shelve');
        }
      } catch (e) {}
    }
    this.checkLoginStatus();
  },
  methods: {
    goToBorrowPage(book) {
      if (!book.title) return;
      this.$router.push({ name: 'Borrowing', params: { bookTitle: encodeURIComponent(book.title) } });
    },
    async handleSearchClick() {
      const query = this.searchQuery?.trim();
      if (!query) return;

      try {
        const result = await searchBooksByKeyword(query, 1, 10);
        const books = result?.data?.list ?? [];

        if (books.length > 0) {
          // 改为跳转到 SearchList 并带关键词参数
          this.$router.push(`/search?keyword=${encodeURIComponent(query)}`);
        } else {
          alert('未找到相关书籍');
        }
      } catch (error) {
        console.error('搜索失败:', error);
        alert('搜索过程中发生错误');
      }
    },
    handleKeyPress(e) {
      if (e.key === 'Enter') {
        this.handleSearchClick()
      }
    },
    goToBookClass(category) {
      console.log('跳转方法被调用，种类:', category);
      this.$router.push(`/bookClass?category=${encodeURIComponent(category)}`);
    },
    goToHotTags() {
      this.$router.push('/hot-tags') // 确保路由路径正确
    },
    checkLoginStatus() {
      const user = localStorage.getItem('user');
      try {
        const parsed = user ? JSON.parse(user) : null;
        console.log('checkLoginStatus parsed user:', parsed); // 调试用
        this.isLoggedIn = !!(parsed && parsed.username);
      } catch (e) {
        this.isLoggedIn = false;
      }
    },
    // 登录弹窗关闭时回调
    onLoginClose() {
      this.showLoginModal = false;
      this.checkLoginStatus();
    },
    // 注册弹窗关闭时回调
    onRegisterClose() {
      this.showRegisterModal = false;
      this.checkLoginStatus();
    },
    onRegisterSuccess() {
      this.showRegisterModal = false;
      this.showLoginModal = true;
    },
    goToPersonalCenter() {
      this.$router.push('/personal-center');
    },
    goToAiQa() {
      if (typeof this !== 'undefined') this.showAside = false;
      this.$router.push('/aiqa');
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  font-family: Arial, sans-serif;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  color: #333;
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 顶部导航栏样式 */
.top-navbar {
  position: fixed;
  top: 16px; /* 向下微调，与页面顶部保持间距 */
  left: 50%;
  transform: translateX(-50%);
  width: 96%;
  max-width: 1400px;
  background: rgba(15, 15, 26, 0.7); /* 半透明背景 */
  backdrop-filter: blur(12px);
  z-index: 20;
  border: 1px solid rgba(100, 149, 237, 0.3); /* 边框 */
  border-radius: 24px; /* 大圆角矩形轮廓 */
  box-shadow: 
    0 4px 24px rgba(0, 0, 0, 0.3),
    0 0 20px rgba(100, 149, 237, 0.2); /* 发光阴影效果 */
  padding: 8px 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.top-menu-container {
  display: flex;
  justify-content: center;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 16px;
  align-items: center;
}

.el-menu {
  background: transparent !important;
  border: none;
  width: 100%;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.el-menu--horizontal {
  border-bottom: none !important;
}

.el-menu--horizontal.el-menu {
  border-bottom: none !important;
}


.el-menu--horizontal > .el-sub-menu {
  margin: 0 !important;
}

.el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
  color: #e0e0e0 !important;
  font-weight: 500;
  font-size: 16px;
  border-radius: 24px;
  margin: 0;
  height: 48px;
  line-height: 48px;
  padding: 0 24px;
  background: linear-gradient(135deg, #1e1e2e 0%, #23233a 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(35, 35, 58, 0.5);
  overflow: visible !important;
  /* 确保文字内容完全在按钮内 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover {
  background: linear-gradient(135deg, #23233a 0%, #2a2a4a 100%) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  color: cornflowerblue !important;
}

.el-menu--horizontal .el-menu-item {
  position: relative;
  color: #e0e0e0 !important;
  font-weight: 500;
  font-size: 16px;
  border-radius: 20px;
  margin: 0;
  height: 44px;
  line-height: 44px;
  padding: 0 20px;
  background: linear-gradient(135deg, #1e1e2e 0%, #23233a 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(35, 35, 58, 0.5);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  /* 确保所有文字内容都应用大圆角矩形按钮设计 */
}

.el-menu--horizontal .el-menu-item:hover {
  background: linear-gradient(135deg, #23233a 0%, #2a2a4a 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  color: cornflowerblue !important;
  border-color: rgba(100, 149, 237, 0.3);
}

.el-menu--horizontal .el-menu-item.is-active {
  background: linear-gradient(135deg, #2a2a4a 0%, #333355 100%) !important;
  color: cornflowerblue !important;
  box-shadow: 0 4px 12px rgba(100, 149, 237, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border-color: rgba(100, 149, 237, 0.5);
  transform: translateY(-1px);
}

.el-icon {
  margin-right: 8px;
  font-size: 18px;
  vertical-align: middle;
}

/* 下拉菜单样式 */
/* 隐藏下拉列表，只保留按钮 */
.el-sub-menu {
  position: relative;
}

.el-sub-menu__popup {
  display: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
  transform: scale(0) !important;
}

/* 确保下拉按钮点击后不会显示菜单 */
.el-sub-menu:hover .el-sub-menu__popup,
.el-sub-menu:focus-within .el-sub-menu__popup,
.el-sub-menu.is-opened .el-sub-menu__popup {
  display: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
}

/* 移除下拉箭头 */
.el-sub-menu__title i.el-icon-arrow-down {
  display: none !important;
}

/* 两列布局 */
.el-sub-menu__popup .el-menu {
  display: block !important;
}

.el-menu-item-group {
  display: flex;
  flex-wrap: wrap;
  padding: 0 !important;
}

.el-menu-item-group__title {
  color: #aaa !important;
  font-size: 13px;
  padding: 12px 0 6px 24px;
  margin-bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  font-weight: 500;
  width: 100%;
  order: -1;
}

.el-sub-menu__popup .el-menu-item {
  color: #e0e0e0 !important;
  font-size: 14px;
  height: 40px;
  line-height: 40px;
  margin: 0;
  padding: 0 20px 0 40px !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  transition: all 0.2s ease;
  width: 50%;
  box-sizing: border-box;
  flex-shrink: 0;
}

.el-sub-menu__popup .el-menu-item:hover {
  background: rgba(35, 35, 58, 0.8) !important;
  color: cornflowerblue !important;
  padding-left: 44px !important;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .top-navbar {
    padding: 6px 0;
    width: 98%;
    top: 12px;
    border-radius: 20px;
  }
  
  .top-menu-container {
    padding: 0 12px;
    max-width: 100%;
  }
  
  .el-menu--horizontal {
    gap: 8px;
  }
  
  .el-menu--horizontal .el-menu-item {
    font-size: 14px;
    height: 40px;
    line-height: 40px;
    padding: 0 16px;
    border-radius: 18px;
  }
  
  .el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
    font-size: 14px;
    height: 40px;
    line-height: 40px;
    padding: 0 20px;
    border-radius: 18px;
  }
  
  .nav-button {
    font-size: 14px;
    padding: 0 16px;
    height: 40px;
    border-radius: 18px;
  }
  
  .el-sub-menu__popup {
    min-width: 350px;
  }
  
  .floating-buttons {
    right: calc((100% - 98%) / 2 + 16px);
    top: 20px;
  }
}

@media (max-width: 768px) {
  .top-navbar {
    padding: 6px 0;
    width: 96%;
    top: 8px;
    border-radius: 16px;
  }
  
  .top-menu-container {
    width: 100%;
    overflow-x: auto;
    padding: 0 12px;
  }
  
  .el-menu {
    min-width: 100%;
    gap: 8px;
    padding-bottom: 4px;
  }
  
  .el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
    font-size: 13px;
    padding: 0 16px;
    height: 40px;
    line-height: 40px;
    border-radius: 20px;
  }
  
  .el-menu--horizontal .el-menu-item {
    font-size: 13px;
    padding: 0 12px;
    height: 36px;
    line-height: 36px;
    border-radius: 18px;
  }
  
  .floating-buttons {
    position: fixed;
    top: 16px;
    right: calc((100% - 96%) / 2 + 12px);
    gap: 8px;
  }
  
  .nav-button {
    padding: 0 12px;
    font-size: 13px;
    height: 36px;
    border-radius: 18px;
  }
  
  .el-sub-menu__popup {
    border-radius: 12px !important;
    margin-top: 4px !important;
    left: 12px !important;
    right: 12px !important;
    width: auto !important;
    min-width: auto !important;
  }
  
  /* 移动端下拉菜单改为单列 */
  .el-sub-menu__popup .el-menu-item {
    width: 100% !important;
    padding: 0 20px 0 32px !important;
  }
  
  .el-sub-menu__popup .el-menu-item:hover {
    padding-left: 36px !important;
  }
}

.floating-buttons {
  position: fixed;
  top: 24px; /* 与顶栏保持一致的顶部间距 */
  right: calc((100% - 96%) / 2 + 20px); /* 相对于顶栏的右侧位置 */
  z-index: 200;
  display: flex;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.navbar-title {
  font-size: 22px;
  font-weight: bold;
  color: #fff;
  letter-spacing: 2px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nav-button {
  padding: 10px 24px;
  background: linear-gradient(135deg, #1e1e2e 0%, #23233a 100%);
  border: 1px solid rgba(35, 35, 58, 0.5);
  color: #e0e0e0;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.nav-button:hover {
  background: linear-gradient(135deg, #23233a 0%, #2a2a4a 100%);
  color: cornflowerblue;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  border-color: rgba(100, 149, 237, 0.3);
}

.main-content {
  margin-top: 80px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-content.fullscreen {
  margin-top: 80px;
  width: 100% !important;
}
.logo-section {
  margin-top: 30px;
  text-align: center;
  display: flex;
  flex-direction: column;

}
.slogan {
  margin-top: 0;
  font-size: 14px;
  color: #444444;
}

.library-logo {
  margin-top: 20px;
  width: 300px;
  height: auto;
}

.search-section {
  margin-top: 30px;
  width: 100%;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.search-tabs {
  display: flex;
}

.search-tab {
  float: left;
  padding: 5px 10px;
  background-color: #333;
  border: none;
  color: #fff;
  cursor: pointer;
}

.search-tab.active {
  background-color: #555;
}

.search-input {
  display: flex;
  margin-top: 10px;
}

.search-bar {
  flex: 1;
  padding: 10px 270px;
  background-color: #333;
  color: #fff;
  border: 1px solid #555;
}

.search-button {
  padding: 1px 50px;
  background-color: #333;
  border: none;
  color: #fff;
  cursor: pointer;
  white-space: nowrap;
}

.popular-section {
  width: 100%;
  max-width: 800px;
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.popular-title-container {
  width: 102%;
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
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 15px;
}

.book-cover {
  width: 23%;
  height: auto;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.book-cover:hover {
  transform: scale(1.1);
}

.login-wrapper {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  height: 310px;
  width: 380px;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  border-radius: 16px;
  box-shadow: none;
}

.register-wrapper {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.7s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
body {
  overflow-x: hidden;
}
</style>