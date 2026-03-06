<template>
  <div class="register-container">
    <button class="close-btn" @click="closeRegister">✖</button>
    
    <!-- 主内容容器 -->
    <div class="register-content-wrapper">
      <!-- 主内容区域 -->
      <div class="register-main-content">
        <div class="form-header">
          <h2>创建账户</h2>
        </div>
        
        <form @submit.prevent="handleRegister" class="floating-form">
          <div class="input-group">
            <input id="username" v-model.trim="registerForm.username" type="text" autocomplete="off" @input="validateInput" required />
            <label for="username">用户名</label>
            <span class="highlight"></span>
          </div>
          <div class="input-group">
            <input id="email" v-model.trim="registerForm.email" type="email" autocomplete="off" @input="validateInput" required />
            <label for="email">邮箱</label>
            <span class="highlight"></span>
          </div>
          <div class="input-group">
            <input id="password" v-model.trim="registerForm.password" type="password" autocomplete="off" @input="validateInput" required />
            <label for="password">密码</label>
            <span class="highlight"></span>
          </div>
          <div class="input-group">
            <input id="confirmPassword" v-model.trim="registerForm.confirmPassword" type="password" autocomplete="off" @input="validateInput" required />
            <label for="confirmPassword">确认密码</label>
            <span class="highlight"></span>
          </div>
          <div class="error-message" v-if="errorMsg">{{ errorMsg }}</div>
          <div class="success-message" v-if="successMsg">{{ successMsg }}</div>
          <button type="submit" class="submit-btn">
            <span>注册</span>
            <i class="arrow-icon"></i>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/utils/request'
const router = useRouter()

// 表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const errorMsg = ref('')
const successMsg = ref('')
const isFormValid = ref(false)

// 输入验证
const validateInput = () => {
  // 基本验证
  const { username, email, password, confirmPassword } = registerForm

  // 检查是否为空
  if (!username || !email || !password || !confirmPassword) {
    isFormValid.value = false
    return
  }

  // 邮箱格式验证
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(email)) {
    errorMsg.value = '请输入有效的邮箱地址'
    isFormValid.value = false
    return
  }

  // 密码长度验证
  if (password.length < 6) {
    errorMsg.value = '密码长度至少为6位'
    isFormValid.value = false
    return
  }

  // 密码匹配验证
  if (password !== confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    isFormValid.value = false
    return
  }

  // XSS攻击字符过滤
  const xssPattern = /(~|\{|\}|"|'|<|>|\?)/g
  if (xssPattern.test(username) || xssPattern.test(email)) {
    errorMsg.value = '输入内容包含非法字符'
    isFormValid.value = false
    return
  }

  // 验证通过
  isFormValid.value = true
  errorMsg.value = ''
}

// 注册处理
const handleRegister = async () => {
  try {
    const { username, email, password, confirmPassword } = registerForm

    // 重新验证表单，确保用户能看到具体的错误提示
    validateInput();
    
    // 前端验证通过后再发起请求
    if (!isFormValid.value) {
      // 错误信息已经在validateInput中设置
      return
    }

    // 调用后端注册接口
    const response = await register({
      username,
      email,
      password,
      phone: '' // 如果后端要求手机号且当前表单没有，请补充逻辑或提示用户输入
    })

    // 处理注册成功响应
    if (response.code === 200) {
      showSuccessMessage('注册成功！')
      setTimeout(() => {
        emit('register-success');
      }, 300);
      return;
    }else {
      errorMsg.value = response.message || '注册失败，请稍后重试'
    }
  } catch (error: any) {
    // 处理网络错误或其他异常
    errorMsg.value = error.response?.data?.message || '注册失败，请稍后重试'
  }
}


// 显示成功消息
const showSuccessMessage = (text: string) => {
  successMsg.value = text
  setTimeout(() => {
    successMsg.value = ''
  }, 3000)
}

// 关闭弹窗
const emit = defineEmits(['close', 'register-success']);
const closeRegister = () => {
  emit('close');
}

onMounted(() => {
  validateInput()
})
</script>

<style scoped>
.register-container {
  width: 100%;
  max-width: 400px;
  background: linear-gradient(135deg, rgba(35, 35, 58, 0.95) 0%, rgba(26, 26, 46, 0.95) 100%);
  border-radius: 24px;
  padding: 24px;
  padding-right: 44px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
  color: #fff;
  position: relative;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.form-header {
  text-align: center;
  margin-bottom: 20px;
}
.form-header h2 {
  font-size: 18px;
  margin-bottom: 10px;
  font-weight: 700;
}

/* 主内容包装器 */
.register-content-wrapper {
  display: flex;
  gap: 24px;
  width: 100%;
  align-items: flex-start;
}

/* 主内容区域 */
.register-main-content {
  flex: 1;
  min-width: 0;
}

/* 浮动表单样式 */
.floating-form .input-group {
  position: relative;
  margin-bottom: 15px;
}
.input-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  backdrop-filter: blur(5px);
}
.input-group label {
  position: absolute;
  left: 10px;
  top: 8px;
  background: rgba(255, 255, 255, 0.1);
  padding: 0 5px;
  color: #fff;
  font-size: 12px;
  transition: all 0.3s ease;
  pointer-events: none;
}
.input-group input:focus,
.input-group input:valid {
  border-color: #a78bfa;
  box-shadow: 0 0 0 2px rgba(167, 139, 250, 0.2);
}
.input-group input:focus + label,
.input-group input:valid + label {
  top: 0;
  font-size: 10px;
  color: #3498db;
}

/* 提交按钮样式 */
.submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #6a82fb 0%, #4f8cff 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 4px 12px rgba(106, 130, 251, 0.2);
}
.submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #4f8cff 0%, #6a82fb 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(106, 130, 251, 0.35);
}
.submit-btn:disabled {
  background: #666;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.arrow-icon {
  border: solid white;
  border-width: 0 2px 2px 0;
  display: inline-block;
  padding: 3px;
  transform: rotate(-45deg);
}

/* 消息提示样式 */
.error-message {
  color: #f56c6c;
  font-size: 12px;
  text-align: center;
  margin-bottom: 10px;
  width: 100%;
}
.success-message {
  color: #67c23a; /* Element Plus 成功绿色 */
  font-size: 12px;
  text-align: center;
  margin-bottom: 10px;
  font-weight: bold;
  width: 100%;
}

/* 关闭按钮 */
.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  color: #ccc;
  z-index: 10;
  transition: all 0.3s ease;
}
.close-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-container {
    padding: 20px;
    padding-right: 40px;
    max-width: 350px;
    border-radius: 20px;
  }
  .form-header h2 {
    font-size: 16px;
  }
  .input-group input,
  .submit-btn {
    padding: 10px;
  }
  
  /* 移动端恢复为垂直布局 */
  .register-content-wrapper {
    flex-direction: column;
    align-items: center;
  }
}
@media (max-width: 320px) {
  .register-container {
    padding: 16px;
    padding-right: 36px;
    max-width: 280px;
    border-radius: 16px;
  }
  .form-header h2 {
    font-size: 14px;
  }
}
</style>
