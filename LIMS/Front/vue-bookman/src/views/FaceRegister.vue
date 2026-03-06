<template>
  <Teleport to="body">
    <div 
      class="face-register-wrapper" 
      v-if="visible"
      @click="handleOverlayClick"
    >
      <div class="register-overlay"></div>
      <div class="register-container">
        <button class="close-btn" @click.stop="closeRegister">✖</button>
        <div class="form-header">
          <h2>人脸注册</h2>
          <p>请确保您已登录系统</p>
        </div>
        
        <div class="user-info" v-if="currentUser">
          <p>当前用户: {{ currentUser.username }}</p>
        </div>
        
        <div class="camera-container" v-if="currentUser && !isRegisterSuccess">
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
              @click.stop="startCamera" 
              class="submit-btn"
              :disabled="isLoading"
            >
              {{ isLoading ? '加载中...' : '开始摄像头' }}
            </button>
            
            <button 
              v-if="isCapturing && !isProcessing" 
              @click.stop="captureAndRegister" 
              class="submit-btn face-submit"
            >
              注册人脸
            </button>
            
            <button 
              v-if="isCapturing" 
              @click.stop="stopCamera" 
              class="submit-btn danger-btn"
            >
              停止摄像头
            </button>
          </div>
        </div>
        
        <div class="message-container">
          <div class="error-message" v-if="errorMsg">{{ errorMsg }}</div>
          <div class="success-message" v-if="successMsg">{{ successMsg }}</div>
        </div>
        
        <div class="form-footer" v-if="isRegisterSuccess">
          <button @click.stop="closeRegister" class="submit-btn face-submit">完成</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { loadFaceApiModels, startCamera as startCam, stopCamera as stopCam, captureImage, faceRegister } from '@/utils/face'
import { ElMessage } from 'element-plus'

// 定义组件属性
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

// 定义组件事件
const emit = defineEmits(['close', 'register-success'])

const router = useRouter()
const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

// 状态变量
const isCapturing = ref(false)
const isProcessing = ref(false)
const isLoading = ref(false)
const isRegisterSuccess = ref(false)

const successMsg = ref('')
const errorMsg = ref('')

// 用户信息
const currentUser = ref<any>(null)

let stream: MediaStream | null = null

// 监听visible属性变化，当组件显示时加载模型和用户信息
watch(() => props.visible, (newVal) => {
  if (newVal) {
    getCurrentUser()
    loadFaceApiModelsWrapper()
  } else {
    // 组件隐藏时清理资源
    resetState()
  }
})

// 获取当前用户信息
const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      currentUser.value = JSON.parse(userStr)
    } catch (e) {
      console.error('解析用户信息失败:', e)
      errorMessage('获取用户信息失败')
      closeRegister()
    }
  } else {
    errorMessage('用户未登录')
    closeRegister()
  }
}

// 重置组件状态
const resetState = () => {
  stopCamera()
  isCapturing.value = false
  isProcessing.value = false
  isLoading.value = false
  isRegisterSuccess.value = false
  successMsg.value = ''
  errorMsg.value = ''
}

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
  if (!props.visible) return // 如果组件不可见，不加载模型
  
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

// 拍照并注册
const captureAndRegister = async () => {
  if (!videoRef.value || !canvasRef.value || !currentUser.value) return
  
  isProcessing.value = true
  
  try {
    // 获取图像数据
    const base64Data = captureImage(videoRef.value, canvasRef.value)
    
    // 发送到后端进行人脸注册
    const res = await faceRegister(currentUser.value.username, base64Data)
    
    if (res.code === 200) {
      // 注册成功
      isProcessing.value = false
      isRegisterSuccess.value = true
      successMessage('人脸注册成功')
      emit('register-success')
    } else {
      isProcessing.value = false
      errorMessage(res.message || '人脸注册失败')
    }
  } catch (error: any) {
    isProcessing.value = false
    console.error('人脸注册过程中出错:', error)
    errorMessage(error.message || '注册过程出现错误，请重试')
  }
}

// 返回首页
const goToDashboard = () => {
  emit('close')
  router.push('/')
}

// 关闭弹窗
const closeRegister = () => {
  resetState()
  emit('close')
}

// 点击遮罩层关闭弹窗
const handleOverlayClick = (event: MouseEvent) => {
  if (event.target === event.currentTarget) {
    closeRegister()
  }
}

// 组件卸载时清理资源
onUnmounted(() => {
  resetState()
})
</script>

<style scoped>
/* 浮窗容器和背景遮罩 */
.face-register-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}

.register-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(5px);
}

/* 注册容器样式 */
.register-container {
  position: relative;
  width: 100%;
  max-width: 380px;
  background: rgba(15, 15, 26, 0.85);
  border-radius: 20px;
  padding: 30px;
  padding-right: 40px;
  box-shadow: 
    0 10px 25px rgba(0, 0, 0, 0.3),
    0 0 20px rgba(100, 149, 237, 0.2);
  color: #fff;
  backdrop-filter: blur(12px);
  border: 1px solid rgba(100, 149, 237, 0.3);
  animation: slideUp 0.4s ease-out;
  z-index: 1001;
}

/* 表单头部 */
.form-header {
  text-align: center;
  margin-bottom: 25px;
}

.form-header h2 {
  font-size: 22px;
  margin-bottom: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #a78bfa 0%, #c4b5fd 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.form-header p {
  font-size: 14px;
  margin-bottom: 20px;
  color: #ccc;
}

/* 用户信息显示 */
.user-info {
  text-align: center;
  margin-bottom: 20px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info p {
  font-size: 15px;
  color: #e0e0e0;
  margin: 0;
}

/* 摄像头容器 */
.camera-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 15px;
}

.camera-video {
  border-radius: 12px;
  background: #000;
  width: 100%;
  max-width: 320px;
  height: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

/* 控制按钮组 */
.camera-controls {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}

/* 按钮样式 */
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 4px 12px rgba(106, 130, 251, 0.2);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(106, 130, 251, 0.35);
  background: linear-gradient(135deg, #4f8cff 0%, #6a82fb 100%);
}

.submit-btn:disabled {
  background: #666;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 人脸注册按钮特定样式 */
.face-submit {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.face-submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.35);
}

/* 危险按钮样式 */
.danger-btn {
  background: linear-gradient(135deg, #d9480f 0%, #ff4d4f 100%);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.danger-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff4d4f 0%, #d9480f 100%);
  box-shadow: 0 6px 20px rgba(255, 77, 79, 0.35);
}

/* 消息容器 */
.message-container {
  margin: 20px 0;
  min-height: 20px;
}

.error-message {
  color: #f56c6c;
  font-size: 13px;
  text-align: center;
  margin-bottom: 10px;
  padding: 8px;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(245, 108, 108, 0.2);
}

.success-message {
  color: #67c23a;
  font-size: 13px;
  text-align: center;
  margin-bottom: 10px;
  font-weight: bold;
  padding: 8px;
  background: rgba(103, 194, 58, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(103, 194, 58, 0.2);
}

/* 表单底部 */
.form-footer {
  text-align: center;
  margin-top: 25px;
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
  transform: scale(1.1);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-container {
    padding: 25px;
    padding-right: 35px;
    margin: 0 20px;
    border-radius: 16px;
  }
  
  .form-header h2 {
    font-size: 20px;
  }
  
  .form-header p {
    font-size: 13px;
  }
  
  .submit-btn {
    padding: 11px;
    font-size: 15px;
  }
  
  .camera-video {
    max-width: 280px;
  }
}

@media (max-width: 320px) {
  .register-container {
    padding: 20px;
    padding-right: 30px;
    margin: 0 15px;
    border-radius: 14px;
  }
  
  .form-header h2 {
    font-size: 18px;
  }
  
  .form-header p {
    font-size: 12px;
  }
  
  .submit-btn {
    padding: 10px;
    font-size: 14px;
  }
  
  .camera-video {
    max-width: 240px;
  }
}
</style>