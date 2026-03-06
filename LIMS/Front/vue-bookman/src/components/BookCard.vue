<!-- BookCard.vue -->
<template>
  <li class="book-item">
    <img
      :src="book.cover || 'https://via.placeholder.com/110'"
      alt="Book Cover"
      class="book-cover"
    />
    <div class="book-details">
      <h3>{{ book.title }}</h3>
      <p>作者: {{ book.author }}</p>
      <p>出版社: {{ book.publisher }}</p>
      <p>类别: {{ book.category }}</p>
    </div>

    <!-- 爱心点赞 -->
    <div class="love-section">
      <span class="love-count">{{ book.love }}</span>
      <button
        class="love-button"
        :class="{ 'liked': book.liked }"
        @click="toggleLike"
      >
        ❤
      </button>
    </div>
  </li>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  book: {
    id: number
    title: string
    author: string
    publisher: string
    category: string
    cover?: string
    love: number
    liked: boolean
  }
}>()

const emit = defineEmits(['update:book'])

// 点赞/取消点赞
const toggleLike = () => {
  const updatedBook = { ...props.book }
  updatedBook.liked = !updatedBook.liked
  updatedBook.love += updatedBook.liked ? 1 : -1
  emit('update:book', updatedBook)
}
</script>

<style scoped>
.book-item {
  display: flex;
  align-items: center;
  background: #1a1a1a;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 10px;
  position: relative;
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
  color: #ffffff;
}

.love-section {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.love-count {
  color: #fff;
  font-size: 14px;
}

.love-button {
  background: transparent;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #ffffff;
  opacity: 0.5;
  transition: opacity 0.3s, color 0.3s;
}

.love-button.liked {
  opacity: 1;
  color: red;
}
</style>
