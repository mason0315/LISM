<template>
  <div class="top-container">
    <div class="top-books">
      <h2>借阅量TOP20</h2>
      <ul>
        <router-link
          :to="{ name: 'Borrowing', params: { bookTitle: encodeURIComponent(book.title) } }"
          v-for="(book, index) in topBooks"
          :key="book.id"
          class="book-item-link"
        >
          <li class="book-item" :class="{ top3: index < 3 }">
            <span class="rank">T O P {{ index + 1 }}.</span>
            <img :src="book.cover || 'https://via.placeholder.com/110'" alt="Book Cover" class="book-cover" />
            <div class="book-details">
              <h3>{{ book.title }}</h3>
              <p>作者: {{ book.author }}</p>
              <p>出版社: {{ book.publisher }}</p>
              <p>类别: {{ book.category }}</p>
              <p>货架号: {{ book.shelveId || '无' }}</p>
            </div>
            <div class="love-section">
              <span class="love-count">{{ book.love }}</span>
              <button class="love-button" :class="{ liked: book.liked }" @click.stop="toggleLike(book)">❤</button>
            </div>
          </li>
        </router-link>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const topBooks = ref([])
const shelveMap = ref({})

const fetchTopBooks = async () => {
  try {
    // 批量获取所有书的货架号映射表
    if (Object.keys(shelveMap.value).length === 0) {
      const shelvesRes = await request.get('/shelves/getAllShelves', { params: { pageNum: 1, pageSize: 1000 } })
      if (shelvesRes.code === 200 && shelvesRes.data?.list) {
        shelveMap.value = {}
        shelvesRes.data.list.forEach((item) => {
          shelveMap.value[item.title] = item.shelveId
        })
      }
    }
    const response = await request.get('/book/AllBook', {
      params: { pageNum: 1, pageSize: 20 }
    })
    if (response.code === 200) {
      topBooks.value = response.data.list.map((book) => ({
        ...book,
        liked: false,
        shelveId: shelveMap.value[book.title]
      }))
    }
  } catch (error) {
    console.error('获取好书排行榜失败:', error)
  }
}

const toggleLike = (book) => {
  if (book.liked) book.love -= 1
  else book.love += 1
  book.liked = !book.liked
}

onMounted(fetchTopBooks)
</script>

<style scoped>
.top-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  padding: 20px;
  font-family: Arial, sans-serif;
}

.top-books {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  padding: 40px 0 0 0;
}

.top-books h2 {
  text-align: center;
  margin-bottom: 50px;
  font-size: 35px;
  color: cornflowerblue;
  letter-spacing: 2px;
}

.top-books ul {
  list-style: none;
  padding: 0;
  margin: 0 auto;
  max-width: 900px;
}

.book-item-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.book-item {
  display: flex;
  align-items: center;
  background: #1a1a1a;
  padding: 18px 20px;
  border-radius: 10px;
  margin-bottom: 18px;
  position: relative;
  color: #fff;
  box-shadow: 0 2px 8px #0002;
  transition: background 0.2s, transform 0.2s;
  cursor: pointer;
}

.book-item:hover {
  background: #23233a;
  transform: scale(1.01);
}

.book-item.top3 {
  background: linear-gradient(135deg, #f3e50b, #706844);
  color: #000;
}

.book-item.top3 .book-details h3,
.book-item.top3 .book-details p,
.book-item.top3 .rank {
  color: #000;
}

.rank {
  font-weight: bold;
  margin-right: 18px;
  color: cornflowerblue;
  font-size: 18px;
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

.love-section {
  position: absolute;
  bottom: 18px;
  right: 30px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.love-count {
  color: #fff;
  font-size: 16px;
}

.love-button {
  background: transparent;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #fff;
  opacity: 0.5;
  transition: opacity 0.3s, color 0.3s;
}

.love-button.liked {
  opacity: 1;
  color: red;
}

.book-item.top3 .love-count,
.book-item.top3 .love-button {
  color: #000;
}
</style>