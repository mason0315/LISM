<template>
  <div class="logo-and-search">
    <!-- Logo 和标语 -->
    <div class="logo-section">
      <img src="@/assets/logoLIMS.png" alt="Library Logo" class="library-logo" />
      <p class="slogan">过去的一切精华尽在书中！</p>
    </div>

    <!-- 搜索框 -->
    <div class="search-section">
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
    </div>
  </div>
</template>

<script>
import { searchBooksByKeyword } from '@/api/search'

export default {
  name: 'LogoAndSearch',
  data() {
    return {
      searchQuery: ''
    }
  },
  methods: {
    async handleSearchClick() {
      const query = this.searchQuery?.trim()
      if (!query) return

      try {
        const result = await searchBooksByKeyword(query, 1, 10)
        const books = result?.data?.list ?? []

        if (books.length > 0) {
          this.$router.push(`/search?keyword=${encodeURIComponent(query)}`)
        } else {
          alert('未找到相关书籍')
        }
      } catch (error) {
        console.error('搜索失败:', error)
        alert('搜索过程中发生错误')
      }
    },
    handleKeyPress(e) {
      if (e.key === 'Enter') {
        this.handleSearchClick()
      }
    }
  }
}
</script>

<style scoped>
.logo-section {
  text-align: center;
  margin-top: 0;
}

.slogan {
  margin-top: 10px;
  font-size: 14px;
  color: #444444;
}

.library-logo {
  width: 280px;
  height: auto;
}

.search-section {
  margin-top: 80px;
  display: flex;
  justify-content: center;
}

.search-input {
  display: flex;
  width: 80%;
  max-width: 800px;
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
</style>
