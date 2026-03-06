<template>
  <div class="bookclass-container">
    <button class="back-button" @click="$router.go(-1)">
      <i class="fas fa-arrow-left"></i> 返回
    </button>
    <div class="book-list-container">
      <LogoAndSearch />
      <div class="book-list">
        <div v-if="books.length === 0" class="no-books">暂无搜索结果</div>
        <router-link
          :to="{ name: 'Borrowing', params: { bookTitle: encodeURIComponent(book.title) } }"
          v-for="(book, index) in books"
          :key="index"
          class="book-item"
        >
          <img :src="book.cover || 'src/assets/book7.jpg'" alt="Book Cover" class="book-cover" />
          <div class="book-details">
            <h3>{{ book.title }}</h3>
            <p>出版社: {{ book.publisher }}</p>
            <p>作者: {{ book.author }}</p>
            <p>库存: {{ book.stock }}</p>
            <p>可借阅量: {{ book.avaBooks }}</p>
            <p>种类: {{ book.category }}</p>
            <p>语言: {{ book.language }}</p>
            <p><strong>货架号:</strong> {{ book.shelveId || '无' }}</p>
          </div>
        </router-link>
      </div>
      <div class="pagination">
        <button @click="prevPage" :disabled="pageNum === 1">上一页</button>
        <span>第 {{ pageNum }} 页 / 共 {{ totalPages }} 页</span>
        <button @click="nextPage" :disabled="pageNum === totalPages">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import LogoAndSearch from '@/components/LogoAndSearch.vue'

const books = ref<any[]>([])
const keyword = ref<string>('')
const pageNum = ref<number>(1)
const pageSize = ref<number>(5)
const totalPages = ref<number>(1)
const shelveMap = ref<Record<string, number>>({});

const route = useRoute()

const fetchSearchResults = async () => {
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
    const response = await request.get('/book/search', {
      params: {
        keyword: keyword.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (response.code === 200) {
      const rawBooks = response.data.list || []
      // 为每本书补全 shelveId
      const booksWithShelve = rawBooks.map((book: any) => {
        return { ...book, shelveId: shelveMap.value[book.title] };
      })
      books.value = booksWithShelve
      totalPages.value = response.data.pages || 1
    }
  } catch (error) {
    console.error('搜索失败:', error)
  }
}

watch(
  () => route.query.category,
  (newCategory) => {
    if (newCategory) {
      keyword.value = decodeURIComponent(newCategory as string)
      pageNum.value = 1
      fetchSearchResults()
    }
  }
)
onMounted(() => {
  keyword.value = decodeURIComponent((route.query.category as string) || '')
  if (keyword.value) {
    fetchSearchResults()
  }
})

const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
    fetchSearchResults()
  }
}
const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
    fetchSearchResults()
  }
}
</script>

<style scoped>
.bookclass-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  padding-top: 40px;
}
.book-list-container {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}
.back-button {
  position: absolute;
  top: 30px;
  left: 30px;
  background: #222;
  color: cornflowerblue;
  border: none;
  border-radius: 4px;
  padding: 8px 18px;
  font-size: 16px;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}
.back-button:hover {
  background: #333;
}
.book-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 60px;
}
.no-books {
  text-align: center;
  font-size: 18px;
  color: #aaa;
  padding: 30px;
}
.book-item {
  display: flex;
  align-items: center;
  background: #1a1a1a;
  padding: 18px 20px;
  border-radius: 10px;
  color: inherit;
  text-decoration: none;
  transition: background 0.2s, transform 0.2s;
  box-shadow: 0 2px 8px #0002;
}
.book-item:hover {
  background: #23233a;
  transform: scale(1.01);
}
.book-cover {
  width: 110px;
  height: 150px;
  object-fit: cover;
  margin-right: 30px;
  border-radius: 6px;
  box-shadow: 0 2px 8px #0004;
}
.book-details {
  flex: 1;
  color: #fff;
}
.book-details h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: cornflowerblue;
}
.book-details p {
  margin: 4px 0;
  font-size: 15px;
  color: #ccc;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 18px;
  margin: 30px 0 0 0;
  font-size: 15px;
  color: #ccc;
}
.pagination button {
  padding: 7px 22px;
  background-color: #333;
  color: cornflowerblue;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.pagination button:disabled {
  background-color: #555;
  color: #888;
  cursor: not-allowed;
}
</style>