<template>
  <div class="login-container">
    <button class="close-btn" @click="closeLogin">✖</button>
    <div class="form-header">
      <h2>人脸登录</h2>
    </div>
    
    <div class="camera-container" v-if="!isLoginSuccess">
      <video 
        ref="videoRef" 
        autoplay 
        playsinline 
        muted
        width="300" 
        height="300"
        class="camera-video"
      ></video>
      <canvas ref="canvasRef" width="300" height="300" style="display: none;"></canvas>
      
      <div class="camera-controls">
        <button 
          v-if="!isCapturing" 
          @click="startCamera" 
          class="submit-btn"
          :disabled="isLoading"
        >
          {{ isLoading ? '加载中...' : '开始摄像头' }}
        </button>
        
        <button 
          v-if="isCapturing && !isProcessing" 
          @click="captureAndLogin" 
          class="submit-btn"
          style="background: cornflowerblue;"
        >
          识别登录
        </button>
        
        <button 
          v-if="isCapturing" 
          @click="stopCamera" 
          class="submit-btn"
          style="background: #f44336;"
        >
          停止摄像头
        </button>
      </div>
    </div>
    
    <div class="message-container">
      <div class="error-message" v-if="errorMsg">{{ errorMsg }}</div>
      <div class="success-message" v-if="successMsg">{{ successMsg }}</div>
    </div>
    
    <div class="form-footer">
      <p>或者使用 <a href="#" @click.prevent="switchToPasswordLogin">密码登录</a></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { loadFaceApiModels, startCamera as startCam, stopCamera as stopCam, captureImage, faceLogin } from '@/utils/face'
import { ElMessage } from 'element-plus'

const router = useRouter()
const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

// 状态变量
const isCapturing = ref(false)
const isProcessing = ref(false)
const isLoading = ref(false)
const isLoginSuccess = ref(false)

const successMsg = ref('')
const errorMsg = ref('')

let stream: MediaStream | null = null

// 显示成功消息
const successMessage = (text: string) => {
  successMsg.value = text
  setTimeout(() => {
    successMsg.value = ''
  }, 3000)
}

// 显示错误消息
const errorMessage = (text: string) => {
  errorMsg.value = text
  setTimeout(() => {
    errorMsg.value = ''
  }, 3000)
}

// 初始化face-api模型
const loadFaceApiModelsWrapper = async () => {
  isLoading.value = true
  
  try {
    const success = await loadFaceApiModels()
    
    if (success) {
      isLoading.value = false
      successMessage('模型加载完成')
    } else {
      isLoading.value = false
      errorMessage('模型加载失败，请检查网络连接')
    }
  } catch (error) {
    console.error('模型加载失败:', error)
    isLoading.value = false
    errorMessage('模型加载失败，请检查网络连接')
  }
}

// 启动摄像头
const startCamera = async () => {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    errorMessage('浏览器不支持摄像头访问')
    return
  }

  try {
    isLoading.value = true
    
    stream = await startCam(videoRef.value)
    isCapturing.value = true
    isLoading.value = false
    successMessage('摄像头已启动')
  } catch (error: any) {
    console.error('无法访问摄像头:', error)
    isLoading.value = false
    errorMessage(error.message || '无法访问摄像头，请检查权限设置')
  }
}

// 停止摄像头
const stopCamera = () => {
  stopCam(stream)
  stream = null
  isCapturing.value = false
}

// 拍照并登录
const captureAndLogin = async () => {
  if (!videoRef.value || !canvasRef.value) return
  
  isProcessing.value = true
  
  try {
    // 获取图像数据
    const base64Data = captureImage(videoRef.value, canvasRef.value)
    
    // 发送到后端进行人脸识别登录
    const res = await faceLogin('', base64Data)
    
    if (res.code === 200 && res.data?.user) {
      // 登录成功
      isProcessing.value = false
      isLoginSuccess.value = true
      
      // 存储用户信息和token
      const userData = res.data.user
      localStorage.setItem('user', JSON.stringify(userData))
      localStorage.setItem('token', res.data.token)
      
      successMessage('登录成功，正在跳转...')
      
      // 延迟跳转
      setTimeout(() => {
        // 根据角色跳转到对应页面
        const role = userData.role
        if (role === 1) {
          router.push('/adminuser')
        } else if (role === 2) {
          router.push('/adminbook')
        } else if (role === 3) {
          router.push('/recordctrl')
        } else if (role === 4) {
          router.push('/suggestionctrl')
        } else if (role === 5) {
          router.push('/shelve')
        } else {
          // 普通用户跳转到首页
          router.push('/')
        }
        
        closeLogin()
      }, 1500)
    } else {
      isProcessing.value = false
      errorMessage(res.message || '人脸识别失败')
    }
  } catch (error: any) {
    isProcessing.value = false
    console.error('人脸识别过程中出错:', error)
    errorMessage(error.message || '识别过程出现错误，请重试')
  }
}

// 切换到密码登录
const switchToPasswordLogin = () => {
  stopCamera()
  router.push('/login')
}

// 关闭弹窗
const emit = defineEmits(['close'])
const closeLogin = () => {
  stopCamera()
  emit('close')
}

// 组件挂载时加载模型
loadFaceApiModelsWrapper()

// 组件卸载时清理资源
onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.login-container {
  width: 100%;
  max-width: 320px;
  background: rgba(23, 23, 23, 0.9);
  border-radius: 16px;
  padding: 20px;
  padding-right: 40px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
  color: #fff;
  position: relative;
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
.camera-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}
.camera-video {
  border-radius: 8px;
  background: #000;
  width: 100%;
  max-width: 300px;
  height: auto;
}
.camera-controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}
.submit-btn {
  width: 100%;
  padding: 10px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.message-container {
  margin: 15px 0;
  min-height: 20px;
}
.error-message {
  color: #f56c6c;
  font-size: 12px;
  text-align: center;
  margin-bottom: 10px;
}
.success-message {
  color: #67c23a;
  font-size: 12px;
  text-align: center;
  margin-bottom: 10px;
  font-weight: bold;
}
.form-footer {
  text-align: center;
  margin-top: 10px;
}
.form-footer a {
  color: #409eff;
  text-decoration: none;
}
.form-footer a:hover {
  text-decoration: underline;
}
.close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #999;
}
@media (max-width: 480px) {
  .login-container {
    padding: 15px;
  }
  .form-header h2 {
    font-size: 16px;
  }
  .submit-btn {
    padding: 8px;
    font-size: 13px;
  }
}
@media (max-width: 320px) {
  .login-container {
    padding: 10px;
  }
  .form-header h2 {
    font-size: 14px;
  }
  .submit-btn {
    padding: 6px;
    font-size: 12px;
  }
}
</style>