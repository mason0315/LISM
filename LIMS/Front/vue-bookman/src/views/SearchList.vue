<template>
  <div class="book-list-container">
    <!-- 使用复用组件 -->
    <LogoAndSearch />
  <div class="book-list">
    <div v-if="books.length === 0" class="no-books">暂无搜索结果</div>

    <!-- 使用 router-link 包裹整个 book-item -->
    <router-link
      :to="{ name: 'Borrowing', params: { bookTitle: encodeURIComponent(book.title) } }"
      v-for="(book, index) in books"
      :key="index"
      class="book-item"
    >
      <!-- 显示书籍封面 -->
      <img :src="book.cover || 'src/assets/book7.jpg'" alt="Book Cover" class="book-cover" />
      <div class="book-details">
        <h3>{{ book.title }}</h3>
        <p>出版社: {{ book.publisher }}</p>
        <p><span>作者:</span> {{ book.author }}</p>
        <p><span>库存:</span> {{ book.stock }}</p>
        <p><span>可借阅量:</span> {{ book.avaBooks }}</p>
        <p><strong>货架号:</strong> {{ book.shelveId || '无' }}</p>
      </div>
      <div class="book-details1">
        <p><span>种类:</span> {{ book.category }}</p>
        <p><span>语言:</span> {{ book.language }}</p>
      </div>
    </router-link>
   </div>
    <!-- 分页组件 -->
    <div class="pagination">
      <button @click="prevPage" :disabled="pageNum === 1">上一页</button>
      <span>第 {{ pageNum }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="pageNum === totalPages">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import LogoAndSearch from '@/components/LogoAndSearch.vue'

const books = ref<any[]>([])
const keyword = ref<string>('')
const pageNum = ref<number>(1)
const pageSize = ref<number>(5)
const totalPages = ref<number>(1)
const shelveMap = ref<Record<string, number>>({});

const route = useRoute()
const router = useRouter()

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
    } else {
      console.error(response.message)
    }
  } catch (error) {
    console.error('搜索失败:', error)
  }
}

// 监听路由变化
watch(
  () => route.query.keyword,
  (newKeyword) => {
    if (newKeyword) {
      keyword.value = decodeURIComponent(newKeyword as string)
      pageNum.value = 1
      fetchSearchResults()
    }
  }
)

onMounted(() => {
  keyword.value = decodeURIComponent((route.query.keyword as string) || '')
  if (keyword.value) {
    fetchSearchResults()
  }
})

// 分页逻辑
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
.book-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 80px;
}

.no-books {
  text-align: center;
  font-size: 16px;
  color: #aaa;
  padding: 20px;
}

.book-item {
  display: flex;
  align-items: center;
  background: #1a1a1a;
  padding: 10px;
  border-radius: 8px;
  position: relative; /* 父容器设为相对定位 */
  color: inherit;
  text-decoration: none;
  min-height: 130px;
}

.book-item:hover {
  background-color: #2c2c2c; /* 可选：添加悬停背景色 */
}

.book-cover {
  width: 110px;
  height: auto;
  object-fit: cover;
  margin-right: 20px;
  border-radius: 4px;
}

.book-details {
  flex: 1;
}

.book-details h3 {
  margin: 0 0 8px;
  font-size: 16px;
}


.book-details p {
  margin: 4px 0;
  font-size: 14px;
  color: #ccc;
}


.book-details1 {
  position: absolute;
  bottom: 50px;
  right: 400px;
  font-size: 14px;
  color: #ccc;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  font-size: 14px;
  color: #ccc;
}

.pagination button {
  padding: 6px 12px;
  background-color: #2c2c2c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  background-color: #555;
  cursor: not-allowed;
}
.book-list-container {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0; /* 显式设置为 0 */
}

.back-button i {
  margin-right: 8px;
  font-size: 25px;
}
</style>

