<template>
  <div class="book-list">
    <div v-if="books.length === 0" class="no-books">暂无借阅记录</div>
    <div class="book-item" v-for="book in books" :key="book.borrow_id">
      <!-- 显示书籍封面 -->
      <img
        :src="book.cover || 'https://via.placeholder.com/110'"
        alt="Book Cover"
        class="book-cover"
      />
      <div class="book-details">
        <h3>{{ book.title }}</h3>
        <p>出版社: {{ book.publisher }}</p>
        <p><span>作者:</span> {{ book.author }}</p>
        <p><span>借阅日期:</span> {{ formatDateString(book.borrowDate) }}</p>
        <p><span>应还日期:</span> {{ formatDateString(book.dueDate) }}</p>
        <p><strong>货架号:</strong> {{ book.shelveId || '无' }}</p>
        <!-- 删除记录按钮 -->
        <button class="return-button" @click="returnBook(book)" v-if="book.status !== 1">删除记录</button>
      </div>

      <!-- 借阅状态 -->
      <span
        v-if="book.status !== undefined"
        class="borrow-status"
        :class="{
    'status-returned': book.status === 'returned',
    'status-unreturned': book.status === 'borrowed',
    'status-reserved': book.status === 'reserved'
  }"
      >
  {{
          book.status === 'returned' ? '已归还' :
            book.status === 'borrowed' ? '借阅中' :
              book.status === 'reserved' ? '预约中' :
                '未知状态'
        }}
</span>


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
import { ref, onMounted, reactive } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

// 书籍列表数据
const books = ref<any[]>([])
// 当前页码
const pageNum = ref<number>(1)
// 每页大小
const pageSize = ref<number>(5)
// 总页数
const totalPages = ref<number>(1)
//获取当前用户id
interface User {
  user_id: number;
}
const router = useRouter()
// 状态管理
const userInfo = ref<User | null>(null);
// 编辑时的临时数据存储
const editForm = reactive({
  user_id: 0,
});
const shelveMap = ref<Record<string, number>>({});

onMounted(async () => {
  await fetchUserInfo();
  await fetchMyBooks();
});
// 获取当前登录用户信息
const fetchUserInfo = async () => {
  // 1. 从localStorage获取用户信息
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    router.push('/'); // 无数据则跳转登录
    return;
  }
  const user = JSON.parse(userStr);
  // 3. 调用后端接口获取完整用户信息
  const res = await request.get(`/users/current?username=${user.username}`);
  // 5. 赋值给userInfo（用于页面显示）
  userInfo.value = res.data;
  // 6. 初始化编辑表单（含userId）
  editForm.user_id = res.data.user_id;
};
// 获取当前用户借阅的书籍列表
const fetchMyBooks = async () => {
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
    const userId = editForm.user_id; // 手动指定测试用的 userId
    const response = await request.get('/borrowrecord/myrecord', {
      params: {
        title: '',
        userId,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    });

    if (response.code === 200) {
      const rawBooks = response.data.list || [];
      const detailedBooks = await Promise.all(rawBooks.map(async (book: any) => {
        const detailedResponse = await request.get(`/book/findByTitle/${book.title}`);
        if (detailedResponse.code === 200) {
          const detailedInfo = detailedResponse.data;
          return { ...book, ...detailedInfo, shelveId: shelveMap.value[book.title] };
        } else {
          return { ...book, shelveId: shelveMap.value[book.title] };
        }
      }));

      books.value = detailedBooks;
      totalPages.value = response.data.pages || 1;
      console.log('Books data:', books.value);
    } else {
      console.error(response.message);
    }
  } catch (error) {
    console.error('获取借阅记录失败:', error);
  }
}

// 格式化日期字符串
const formatDateString = (dateString: string): string => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString()
}
// 归还书籍
const returnBook = async (book: any) => {
  try {
    const response = await request.delete('/borrowrecord/deleteRecord', {
      params: {
        recordId: book.recordId // 注意字段名要与后端匹配（原为 record_id）
      }
    })

    if (response.code === 200) {
      alert('删除记录成功')
      fetchMyBooks() // 刷新数据
    } else {
      alert('删除记录失败: ' + response.message)
    }
  } catch (error) {
    console.error('删除记录失败:', error)
    alert('网络错误，请重试')
  }
}
// 上一页
const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
    fetchMyBooks()
  }
}

// 下一页
const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
    fetchMyBooks()
  }
}

onMounted(() => {
  fetchMyBooks()
})
</script>

<style scoped>
.book-list {
  width: 95%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
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
  color: #ccc;
}

.borrow-status {
  position: absolute;
  bottom: 10px;
  right: 15px;
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 4px;
  color: #fff;
}

.status-returned {
  background-color: #27a103; /* 绿色 */
}

.status-unreturned {
  background-color: #bd0606; /* 红色 */
}

.status-reserved {
  background-color: #caa500; /* 黄色 */
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  font-size: 14px;
  color: #fff3f3;
}

.pagination button {
  padding: 6px 12px;
  color: #fdfcfc;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  background-color: #655f5f;
  cursor: not-allowed;
}
.return-button {
  position: absolute;
  bottom: 10px;
  right: 80px;
  padding: 5px 12px;
  font-size: 12px;
  color: #fff;
  background-color: #122370;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.return-button:hover {
  background-color: #8f8fe1;
}
</style>
