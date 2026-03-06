<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus' // 引入Element弹窗组件
import BookList from './BookList.vue'
import request from '@/utils/request'
import MessageDetail from "./MessageDetail.vue";
import { useLogout } from '@/utils/user'
import FaceRegister from './FaceRegister.vue'
const { handleLogout } = useLogout();

const currentSection = ref('个人信息')
const router = useRouter()

const switchSection = (section: string) => {
  currentSection.value = section
}

// 控制人脸注册浮窗的显示状态
const showFaceRegister = ref(false);

// 显示人脸注册浮窗
const goToFaceRegister = () => {
  showFaceRegister.value = true;
}

// 处理人脸注册成功事件
const handleRegisterSuccess = () => {
  ElMessage.success('人脸注册成功！');
}

interface User {
  user_id: number;
  email: string;
  username: string;
  phone: string;
}

// 状态管理
const userInfo = ref<User | null>(null);
const loading = ref(true);
const errorMsg = ref('');
const isEditing = ref(false); // 控制是否处于编辑模式
const showPasswordFields = ref(false); // 控制是否显示密码输入框

// 编辑时的临时数据存储
const editForm = reactive({
  user_id: 0,
  email: '',
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: ''
});
onMounted(() => {
  fetchUserInfo()
})
// 获取当前登录用户信息
const fetchUserInfo = async () => {
  try {
    // 1. 从localStorage获取用户信息
    const userStr = localStorage.getItem('user');
    if (!userStr) {
      router.push('/'); // 无数据则跳转登录
      return;
    }
    const user = JSON.parse(userStr);

    // 2. 验证存储的用户信息是否包含username
    if (!user.username) {
      ElMessage.error('用户信息异常，缺少用户名');
      localStorage.removeItem('user'); // 清除无效数据
      router.push('/');
      return;
    }

    // 3. 调用后端接口获取完整用户信息
    const res = await request.get(`/users/current?username=${user.username}`);
    console.log('后端返回的用户信息:', res.data); // 关键：确认res.data有email、username、phone

    // 4. 验证后端返回的数据是否有效
    if (!res.data || !res.data.username) {
      ElMessage.error('后端未返回有效的用户信息');
      return;
    }

    // 5. 赋值给userInfo（用于页面显示）
    userInfo.value = res.data;

    // 6. 初始化编辑表单（含userId）
    editForm.user_id = res.data.user_id;
    editForm.username = res.data.username;
    editForm.email = res.data.email;
    editForm.phone = res.data.phone;

    console.log('editForm初始化后:', editForm); // 验证userId是否存在
  } catch (error) {
    console.error('获取用户信息失败:', error);
    errorMsg.value = '获取用户信息失败，请重试';
  } finally {
    loading.value = false;
  }
};

// 检查用户名是否已存在
const checkUsernameExists = async (username: string): Promise<boolean> => {
  try {
    const res = await request.get(`/users/checkUsername?username=${username}`)
    return res.data.exists
  } catch (error) {
    console.error('检查用户名失败', error)
    return false
  }
}

// 切换到编辑模式
const handleEdit = () => {
  console.log("确认修改按钮被点击了");
  isEditing.value = true
  showPasswordFields.value = true // 显示密码输入框
}

// 确认修改
const confirmEdit = async () => {



  // 简单验证
  if (!editForm.username || !editForm.email || !editForm.phone) {
    ElMessageBox.alert('请输入完整信息','提示', {
      confirmButtonText: 'OK',
      customClass: 'small-ppopup',
    })
    return
  }

  // 密码验证（如果填写了密码）
  if (editForm.newPassword) {
    if (editForm.newPassword.length < 6) {
      ElMessageBox.alert('密码长度不能少于6位','提示', {
        confirmButtonText: 'OK',
        customClass: 'small-ppopup',
      })
      return
    }
    if (editForm.newPassword !== editForm.confirmPassword) {
      ElMessageBox.alert('两次输入的密码不一致','提示', {
        confirmButtonText: 'OK',
        customClass: 'small-ppopup',
      })
      return
    }
  }


  // 检查用户名是否重复（如果用户名有修改）
  if (editForm.username !== userInfo.value?.username) {
    const exists = await checkUsernameExists(editForm.username)
    if (exists) {
      // 使用Element弹窗提示用户名已存在
      ElMessageBox.alert('用户名已存在，请更换', '提示', {
        confirmButtonText: '确定',
        customClass: 'small-ppopup',
        type: 'error'
      })
      return
    }
  }
  console.log('提交的更新参数：', editForm); // 重点看userId和各字段值
  try {

    // 调用后端更新用户信息的接口
    await request.post('/users/updateUsers', { ...editForm })

    // 4. 若填写了新密码，先调用密码更新接口
    if (editForm.newPassword) {
      console.log('提交密码更新参数:', {
        user_id: editForm.user_id,
        password: editForm.newPassword  // 假设后端用password字段接收新密码
      });
      // 调用密码更新接口
      await request.post('/users/updatepa', {
        user_id: editForm.user_id,
        password: editForm.newPassword  // 与后端Users类的password字段匹配
      });
      ElMessageBox.alert('密码更新成功','提示', {
        confirmButtonText: 'OK',
        customClass: 'small-ppopup',
      })
    }
    // 5. 调用基本信息更新接口（更新用户名、邮箱、手机号）
    await request.post('/users/updateUsers', {
      user_id: editForm.user_id,
      username: editForm.username,
      email: editForm.email,
      phone: editForm.phone
    });
    // 6. 全部更新成功后，刷新本地数据和状态
    ElMessageBox.alert('所有信息修改成功', '提示', {
      confirmButtonText: 'OK',
      customClass: 'small-ppopup',
      type: 'success' }).then(() => {
      // 更新userInfo
      userInfo.value = {
        user_id: editForm.user_id,
        username: editForm.username,
        email: editForm.email,
        phone: editForm.phone

      };

      // 更新localStorage（不存储密码）
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        user.username = editForm.username;
        user.email = editForm.email;
        user.phone = editForm.phone;
        localStorage.setItem('user', JSON.stringify(user));
      }
      // 退出编辑模式，清空密码输入框
      isEditing.value = false;
      showPasswordFields.value = false;
      editForm.newPassword = '';
      editForm.confirmPassword = '';
    });




    // 显示修改成功弹窗
    ElMessageBox.alert('修改成功', '提示', {
      confirmButtonText: '确定',
      customClass: 'small-ppopup',
      type: 'success'
    }).then(() => {
      // 更新本地用户信息
      userInfo.value = {
        user_id: Number(editForm.user_id),
        email: editForm.email,
        username: editForm.username,
        phone: editForm.phone
      }

      // 更新localStorage中的用户信息
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        user.username = editForm.username;
        user.email = editForm.email;
        user.phone = editForm.phone;
        localStorage.setItem('user', JSON.stringify(user));
      }

      // 退出编辑模式
      isEditing.value = false
      showPasswordFields.value = false
      // 清空密码输入框
      editForm.newPassword = ''
      editForm.confirmPassword = ''
    })
  } catch (error) {
    console.error('修改失败', error)
    ElMessageBox.alert('修改失败，请重试', '提示', {
      confirmButtonText: '确定',
      customClass: 'small-ppopup',
      type: 'error'
    })
  }
}

// 取消编辑
const cancelEdit = () => {
  console.log("取消按钮被点击了");

  // 恢复原始值
  if (userInfo.value) {
    editForm.email = userInfo.value.email
    editForm.username = userInfo.value.username
    editForm.phone = userInfo.value.phone
  }
  // 清空密码
  editForm.newPassword = ''
  editForm.confirmPassword = ''
  // 退出编辑模式
  isEditing.value = false
  showPasswordFields.value = false
}




const goBack = () => {
  router.back()
}



// 退出登录逻辑
// 将handleLogout函数内容提取到utils/user.ts中，并在此处导出

interface Message {
  title: string;
  date: string;
  content: string;
}
// 存储借阅记录
interface BorrowRecord {
  title: string;
  dueDate: string;
}
const today = new Date();
const messages = ref<Message[]>([
  {
    title: "阅读的最大理由是想摆脱平庸，早一天就多一份人生的精彩；迟一天就多一天平庸的困扰。📕",
    date: today.toLocaleDateString(),
    content: `欢迎各位光临我们的图书馆！😀 我们的图书馆是一个温馨、安静、学术氛围浓厚的学习场所📕。 我们拥有丰富的书籍资源，覆盖各个领域，能够满足读者多方面的需求💻。 同时，我们也有电子资源、学术期刊、文献检索等数据库，为读者提供便捷的查询服务🔍。 在这里，我们提供宽敞明亮、环境整洁的读者休息区和自修区，为读者提供最舒适的学习环境。 为了最大程度保护图书资源，同时提高读者的借阅质量，我们设置了严格的借阅规则和保管措施🍎.`
  }
]);

const borrowRecords = ref<BorrowRecord[]>([]);

// 获取当前用户借阅记录
const fetchBorrowRecords = async () => {
  try {
    const userId = editForm.user_id;
    const response = await request.get('/borrowrecord/myrecord', {
      params: {
        title: '',
        userId,
        pageNum: 1,
        pageSize: 10000
      }
    });
    if (response.code === 200) {
      borrowRecords.value = response.data.list || [];
    } else {
      console.error('获取借阅记录失败:', response.message);
    }
  } catch (error) {
    console.error('请求借阅记录出错:', error);
  }
};

// 生成归还提醒
const generateReturnReminder = () => {
  const today = new Date();
  const todayStr = today.toLocaleDateString();
  const messagesToAdd: Message[] = [];

  borrowRecords.value.forEach(record => {
    const dueDate = new Date(record.dueDate);
    const diffDays = Math.floor((dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays <= 1) {
      const dueDateStr = dueDate.toLocaleDateString();
      messagesToAdd.push({
        title: `归还提醒：你的书籍《${record.title}》 应在 ${dueDateStr} 前归还`,
        date: todayStr,
        content: `尊敬的用户，您好！温馨提醒您，此前借阅的书籍《${record.title}》将在 ${dueDateStr} 到期，想必这本书在这段时间里给您带来了不少收获与感悟吧。为了不影响您的借阅信誉，也方便其他读者借阅，请您留意时间并及时归还。如果您还未读完，希望继续品味书中的精彩内容，可在到期前通过图书馆官网、APP 或前往服务台办理续借手续（部分特殊书籍可能有续借限制，具体可咨询工作人员）。若您在归还过程中有任何疑问，欢迎随时联系我们的服务热线或到馆咨询，我们将竭诚为您提供帮助。最后，祝您学业精进，生活愉快，在书海中持续探索更多美好！`
      });
    }
  });

  // 调用 addMessages 方法添加新消息，保证状态同步
  addMessages(messagesToAdd);
};
//删除消息
const deleteMessage = (index: number) => {
  messages.value.splice(index, 1);
  messageExpanded.value = [...messages.value.map(() => false)];
};

// 安全地添加消息
const addMessages = (newMessages: Message[]) => {
  messages.value.push(...newMessages);
  // 更新 messageExpanded 状态数组
  messageExpanded.value = Array(messages.value.length).fill(false);
};

// 记录每条消息是否展开
const messageExpanded = ref<boolean[]>(Array(messages.value.length).fill(false));

// 切换展开状态的方法
const toggleMessageExpand = (index: number) => {
  if (index >= 0 && index < messageExpanded.value.length) {
    messageExpanded.value = messageExpanded.value.map((_, i) =>
      i === index ? !messageExpanded.value?.[i] : false
    );
  }
};

// 页面加载时初始化
onMounted(async () => {
  await fetchUserInfo();
  await fetchBorrowRecords();
  generateReturnReminder();
});
// 获取用户角色
const userRole = ref<number | null>(null)

onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    userRole.value = user.role // 假设用户信息中包含 role 字段
  }

  await fetchUserInfo()
  await fetchBorrowRecords()
  generateReturnReminder()
})

// 管理员入口跳转逻辑
const goToAdmin = () => {
  const role = userRole.value
  if (role === 0) return // role=0 不显示按钮

  let targetRoute = ''
  if (role === 1) {
    targetRoute = '/adminuser'
  } else if (role === 2) {
    targetRoute = '/adminbook'
  } else if (role === 3) {
    targetRoute = '/recordctrl'
  } else if (role === 4) {
    targetRoute = '/suggestionctrl'
  } else {
    targetRoute = '/shelve'
  }

  if (targetRoute) {
    router.push(targetRoute)
  }
}

</script>


<template>
  <div class="personal-center">
    <!-- 顶栏 -->
    <div class="top-bar">
      <button @click="goBack" class="back-button">
        <ArrowLeftOutlined />
      </button>
      <button @click="switchSection('我的书籍')">我的书籍</button>
      <button @click="switchSection('个人信息')" :class="{ active: currentSection === '个人信息' }">个人信息</button>
      <button @click="switchSection('我的消息')">我的消息</button>
      <!-- 管理员入口按钮 -->
      <button v-if="userRole !== null && userRole !== 0" @click="goToAdmin" class="admin-entry">
        管理员入口
      </button>
    </div>

    <!-- 内容区域 -->
    <transition name="slide" mode="out-in">
      <div :key="currentSection" class="content-wrapper">
        <!-- 我的书籍板块 -->
        <div v-show="currentSection === '我的书籍'" class="my-books">
          <h2>我的书籍</h2>
          <BookList />
        </div>

        <div v-show="currentSection === '个人信息'" class="personal-info">
          <h2>个人信息</h2>
          <form class="info-form">
            <div v-if="userInfo" class="user-info">
              <div class="info-item">
                <span class="label">邮箱：</span>
                <span class="value" v-if="!isEditing">{{ userInfo.email }}</span>
                <el-input
                  v-if="isEditing"
                  v-model="editForm.email"
                  type="text"
                  placeholder="请输入邮箱"
                  class="edit-input"
                />
              </div>
              <div class="info-item">
                <span class="label">用户名：</span>
                <span class="value" v-if="!isEditing">{{ userInfo.username }}</span>
                <el-input
                  v-if="isEditing"
                  v-model="editForm.username"
                  type="text"
                  placeholder="请输入用户名"
                  class="edit-input"
                />
              </div>
              <div class="info-item">
                <span class="label">手机号：</span>
                <span class="value" v-if="!isEditing">{{ userInfo.phone }}</span>
                <el-input
                  v-if="isEditing"
                  v-model="editForm.phone"
                  type="text"
                  placeholder="请输入手机号"
                  class="edit-input"
                />
              </div>
            </div>

            <!-- 编辑/确认按钮 -->
            <div class="button-group">
              <el-button
                type="primary"
                @click="handleEdit"
                v-if="!isEditing"
              >
                修改
              </el-button>

              <div v-if="isEditing" class="edit-buttons">
                <el-button type="primary" @click="confirmEdit">
                  确认修改
                </el-button>
                <el-button type="primary" @click="cancelEdit">
                  取消
                </el-button>
              </div>
              
              <!-- 人脸注册按钮 -->
              <div class="button-group">
                <el-button
                  type="primary"
                  @click="goToFaceRegister">
                  人脸注册
                </el-button>
              </div>
              
              <!-- 退出登录按钮（建议放在个人信息底部） -->
              <div class="button-group">
                <el-button
                  type="danger"
                  @click="handleLogout">
                  退出登录
                </el-button>
              </div>
            </div>
          </form>

          <hr />

          <!-- 密码修改区域，仅在编辑模式显示 -->
          <form class="password-form" v-if="showPasswordFields">
            <div class="form-group">
              <label for="new-password">新密码</label>
              <el-input
                type="password"
                id="new-password"
                v-model="editForm.newPassword"
                placeholder="不修改密码请留空"
                class="password-input"
              />
            </div>
            <div class="form-group">
              <label for="confirm-password">确认新密码</label>
              <el-input
                type="password"
                id="confirm-password"
                v-model="editForm.confirmPassword"
                placeholder="再次输入新密码"
                class="password-input"
              />
            </div>
          </form>
        </div>

        <!-- 借阅记录板块 -->
        <div v-show="currentSection === '借阅记录'" class="borrow-history">
          <h2>借阅记录</h2>
          <BookHistory />
        </div>

        <!-- 我的消息板块 -->
        <div v-show="currentSection === '我的消息'" class="my-messages">
          <h2>我的消息</h2>
          <div v-for="(msg, index) in messages" :key="index" class="message-item">
            <div class="message-header" @click="toggleMessageExpand(index)">
              <h3>{{ msg.title }}</h3>
              <p class="date">{{ msg.date }}</p>
            </div>
            <div class="message-preview">
              {{ msg.content.slice(0, 100) }}...
            </div>
            <!-- 删除按钮 -->
            <button class="button-delete" @click.stop="deleteMessage(index)">删除</button>
            <!-- 每个消息都有自己的 MessageDetail 组件 -->
            <transition name="slide1">
              <MessageDetail
                v-if="messageExpanded[index]"
                :messages="messages"
                :currentMessageIndex="index"
              />
            </transition>
          </div>
      </div>
      </div>
    </transition>
    
    <!-- 人脸注册浮窗组件 -->
    <FaceRegister 
      :visible="showFaceRegister"
      @close="showFaceRegister = false"
      @register-success="handleRegisterSuccess"
    />
  </div>
</template>


<style scoped>
.personal-center {
  width: 100%;
  margin: 0 auto;
  padding: 40px;
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  color: #fff;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.top-bar {
  display: flex;
  align-items: center;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(42, 92, 170, 0.15);
  padding: 12px 24px;
  margin-bottom: 32px;
  gap: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
}
.top-bar button {
  margin: 0 8px;
  padding: 8px 18px;
  background: #22335b;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  font-size: 1.05em;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(34, 51, 91, 0.10);
}
.top-bar button.active,
.top-bar button:hover {
  background: #16213a;
  color: #fff;
  box-shadow: 0 2px 8px rgba(34, 51, 91, 0.18);
}
.admin-entry {
  background: linear-gradient(135deg, #22335b 0%, #16213a 100%) !important;
  color: #fff !important;
  font-weight: bold;
  border-radius: 8px !important;
}

.personal-info,
.my-messages {
  width: 100%;
  max-width: 800px;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(42, 92, 170, 0.10);
  padding: 32px 32px 24px 32px;
  margin: 0 auto;
  color: #fff;
  border: 1px solid rgba(255,255,255,0.1);
  backdrop-filter: blur(10px);
}

.info-form,
.password-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  align-items: flex-start;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  width: 100%;
}

.label {
  display: inline-block;
  width: 100px;
  color: #6fa8dc;
  font-weight: 500;
}

.value {
  flex: 1;
  padding: 5px;
  color: #fff;
}

.edit-input, .password-input, .el-input__inner {
  border-radius: 8px !important;
  border: 1.5px solid #22335b !important;
  background: rgba(34, 51, 91, 0.15) !important;
  color: #fff !important;
  padding: 8px 12px !important;
  font-size: 1em !important;
  transition: border 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(34, 51, 91, 0.08);
}
.edit-input:focus, .password-input:focus, .el-input__inner:focus {
  border: 2px solid #3a4667 !important;
  background: rgba(34, 51, 91, 0.25) !important;
  box-shadow: 0 0 0 2px #22335b33;
}
.el-button, .button-delete {
  border-radius: 8px !important;
  font-weight: 500;
  padding: 8px 24px !important;
  font-size: 1em !important;
  margin-right: 10px;
  transition: background 0.2s, color 0.2s;
  border: none !important;
  box-shadow: 0 1px 4px rgba(34, 51, 91, 0.10);
}
.el-button[type="primary"], .button-delete {
  background: #22335b !important;
  color: #fff !important;
}
.el-button[type="primary"]:hover, .button-delete:hover {
  background: #16213a !important;
  color: #fff !important;
}
.el-button[type="danger"] {
  background: linear-gradient(135deg, #3a4667 0%, #22335b 100%) !important;
  color: #fff !important;
}
.button-delete {
  position: absolute;
  right: 24px;
  top: 24px;
  background: #22335b !important;
  color: #fff !important;
  padding: 4px 18px !important;
  font-size: 0.95em !important;
  margin-right: 0 !important;
  border-radius: 8px !important;
}
.button-delete:hover {
  background: #16213a !important;
  color: #fff !important;
}

.form-group {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 70%;
}

.form-group label {
  flex: 0 0 100px;
  text-align: left;
  color: #6fa8dc;
  font-weight: 500;
}

.button-group {
  margin-top: 20px;
  align-self: flex-start;
}

.edit-buttons {
  display: flex;
  gap: 10px;
}

.back-button {
  background-color: transparent;
  color: #a78bfa;
  border: none;
  padding: 7px 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-size: 20px;
  border-radius: 8px;
}

/* 动画类 */
.slide-enter-active {
  transition: all 0.5s ease;
}
.slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from {
  transform: translateX(100%);
  opacity: 0;
}
.slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}
.slide1-enter-active {
  transition: all 0.5s ease;
}
.slide1-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}

.message-item {
  margin-bottom: 24px;
  padding: 18px 20px;
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(42, 92, 170, 0.10);
  transition: box-shadow 0.2s, transform 0.2s;
  color: #fff;
  position: relative;
  border: 1px solid rgba(255,255,255,0.1);
  backdrop-filter: blur(10px);
}
.message-item:hover {
  box-shadow: 0 6px 24px rgba(42, 92, 170, 0.18);
  transform: translateY(-2px) scale(1.01);
}

h2 {
  font-size: 1.6em;
  font-weight: bold;
  margin-bottom: 18px;
  color: #6fa8dc;
  letter-spacing: 1px;
}
hr {
  border: none;
  border-top: 1.5px solid #e0e7ef;
  margin: 24px 0;
}

.message-preview {
  margin-top: 10px;
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

@media (max-width: 900px) {
  .personal-info, .my-messages {
    max-width: 98vw;
    padding: 16px 4vw;
  }
  .top-bar {
    flex-wrap: wrap;
    padding: 8px 4vw;
  }
}
@media (max-width: 600px) {
  .personal-center {
    padding: 8px 2vw;
  }
  .top-bar button {
    font-size: 0.95em;
    padding: 6px 10px;
  }
}
</style>