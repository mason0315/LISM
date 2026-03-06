<template>
  <div class="login-container">
    <button class="close-btn" @click="closeLogin">✖</button>
    
    <!-- 主内容容器 -->
    <div class="login-content-wrapper">
      <!-- 主内容区域 -->
      <div class="login-main-content">
        <div class="form-header">
          <h2>用户登录</h2>
        </div>
        
        <!-- 登录方式切换标签 -->
        <div class="login-tabs">
          <button 
            class="tab-btn" 
            :class="{ active: loginType === 'password' }"
            @click="loginType = 'password'"
          >
            账号密码登录
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: loginType === 'face' }"
            @click="loginType = 'face'"
          >
            人脸识别登录
          </button>
        </div>
        
        <!-- 账号密码登录表单 -->
        <form v-if="loginType === 'password'" @submit.prevent="handleLogin" class="floating-form">
          <div class="input-group">
            <input id="username" v-model.trim="loginForm.username" type="text" autocomplete="off" @input="validateInput" required />
            <label for="username">用户名</label>
            <span class="highlight"></span>
          </div>
          <div class="input-group">
            <input id="password" v-model.trim="loginForm.password" type="password" autocomplete="off" @input="validateInput" required />
            <label for="password">密码</label>
            <span class="highlight"></span>
          </div>
          <div class="error-message" v-if="errorMsg">{{ errorMsg }}</div>
          <div class="success-message" v-if="successMsg">{{ successMsg }}</div>
          <button type="submit" class="submit-btn" :disabled="!isFormValid">
            <span>登录</span>
            <i class="arrow-icon"></i>
          </button>
        </form>
        
        <!-- 人脸识别登录表单 -->
        <div v-else-if="loginType === 'face'" class="face-login-form">
          <div class="camera-container">
            <video ref="video" autoplay muted playsinline></video>
            <canvas ref="canvas" style="display: none;"></canvas>
            
            <div class="camera-controls">
              <button 
                v-if="!isCapturing && !isLoading" 
                @click="startCamera" 
                class="submit-btn"
                style="background: #4CAF50;"
              >
                开始自动识别人脸
              </button>
              
              <button 
                v-if="isLoading && !isCapturing"
                disabled
                class="submit-btn"
                style="background: #666;"
              >
                {{ isLoading ? '加载中...' : '准备中...' }}
              </button>
              
              <!-- 自动检测状态显示 -->
              <div v-if="isCapturing" class="detection-status">
                <div class="status-indicator" :class="{ 'detected': isFaceDetected, 'detecting': isAutoDetecting }"></div>
                <span class="status-text">
                  {{ isAutoDetecting ? (isFaceDetected ? `检测到人脸 (${detectedFaceCount}/3)` : '正在检测...') : '准备中' }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="face-login-actions">
            <button 
              type="button" 
              class="capture-btn" 
              @click="captureFace"
              :disabled="!isCapturing || isProcessing"
            >
              <i class="camera-icon"></i>
              拍摄人脸
            </button>
            
            <button 
              type="button" 
              class="submit-btn face-submit" 
              @click="handleFaceLogin" 
              :disabled="!faceImageCaptured || isProcessing"
            >
              {{ isProcessing ? '识别中...' : '人脸识别登录' }}
            </button>
            
            <button 
              v-if="isCapturing" 
              type="button" 
              class="submit-btn" 
              @click="stopCamera"
              style="background: #f44336;"
              :disabled="isProcessing"
            >
              停止摄像头
            </button>
          </div>
          
          <div class="error-message" v-if="errorMsg">{{ errorMsg }}</div>
          <div class="success-message" v-if="successMsg">{{ successMsg }}</div>
        </div>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
const router = useRouter()
import { ElMessage } from 'element-plus'
// 导入人脸识别相关工具
import { loadFaceApiModels, startCamera as startCam, stopCamera as stopCam, captureImage, faceLogin } from '@/utils/face'

// 登录类型：password - 账号密码登录，face - 人脸识别登录
const loginType = ref('password')

// 表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

const successMsg = ref('')
const errorMsg = ref('')
const isFormValid = ref(false)
const loading = ref(false) // 加载状态

// 人脸识别相关
const video = ref<HTMLVideoElement>()
const canvas = ref<HTMLCanvasElement>()
const faceImageSrc = ref('')
const faceImageCaptured = ref(false)
let stream: MediaStream | null = null
let detectionInterval: number | null = null
const detectedFaceCount = ref(0)
const isAutoDetecting = ref(false)
const isFaceDetected = ref(false)

// 新增人脸识别状态管理
const isCapturing = ref(false)
const isProcessing = ref(false)
const isLoading = ref(false)
const isLoginSuccess = ref(false)

// 输入验证
const validateInput = () => {
  if (loginForm.username.trim() && loginForm.password.trim()) {
    isFormValid.value = true
    errorMsg.value = ''
  } else {
    isFormValid.value = false
  }
}

// 账号密码登录处理
const handleLogin = async () => {
  if (!isFormValid.value) return

  try {
    loading.value = true;
    const res = await request.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password
    });

    if (res.code !== 200 || !res.data?.user) { // 确保返回用户对象
      ElMessage.error('登录失败：' + (res.message || '用户信息缺失'));
      return;
    }

    // 只存储后端返回的用户对象（含username、userId等）
    const userData = res.data.user;
    localStorage.setItem('user', JSON.stringify(userData));
    // 存储token（如果需要）
    localStorage.setItem('token', res.data.token);

    // 验证存储的用户信息是否正确
    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    if (!storedUser.username) {
      ElMessage.warning('用户信息不完整，请重新登录');
      return;
    }
    successMessage('登录成功，正在跳转...');

    // 200ms后关闭浮窗
    setTimeout(() => {
      // 跳转到对应的管理员页面
      const role = storedUser.role;
      if (role === 1) {
        router.push('/adminuser');
      } else if (role === 2) {
        router.push('/adminbook');
      } else if (role === 3) {
        router.push('/recordctrl');
      } else if (role === 4) {
        router.push('/suggestionctrl');
      } else if (role === 5) {
        router.push('/shelve');
      } else {
        // 普通用户跳转到首页或个人中心
        router.push('/');
      }
      closeLogin(); // 触发关闭浮窗
    }, 1100);
  } catch (error) {
    errorMessage('登录失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

// 人脸识别登录处理
const handleFaceLogin = async () => {
  if (!faceImageCaptured.value) return

  try {
    isProcessing.value = true;
    loading.value = true;
    
    // 使用工具函数进行人脸识别登录，适配face.ts中的API路径
    // 注意：faceLogin函数期望的是纯base64数据，需要处理faceImageSrc
    const base64Data = faceImageSrc.value.split(',')[1]; // 移除data:image/jpeg;base64,前缀
    const res = await faceLogin('', base64Data)

    if (res.code !== 200 || !res.data?.user) {
      ElMessage.error('人脸识别失败：' + (res.message || '无法识别到有效用户'));
      return;
    }

    isLoginSuccess.value = true
    
    // 存储用户信息和token
    const userData = res.data.user;
    localStorage.setItem('user', JSON.stringify(userData));
    localStorage.setItem('token', res.data.token);

    successMessage('人脸识别成功，正在跳转...');

    setTimeout(() => {
      const role = userData.role;
      if (role === 1) {
        router.push('/adminuser');
      } else if (role === 2) {
        router.push('/adminbook');
      } else if (role === 3) {
        router.push('/recordctrl');
      } else if (role === 4) {
        router.push('/suggestionctrl');
      } else if (role === 5) {
        router.push('/shelve');
      } else {
        router.push('/');
      }
      closeLogin();
    }, 1100);
  } catch (error: any) {
    console.error('人脸识别过程中出错:', error)
    errorMessage(error.message || '人脸识别失败，请稍后重试')
  } finally {
    isProcessing.value = false;
    loading.value = false;
  }
}

// 拍摄人脸
const captureFace = () => {
  if (!canvas.value || !video.value) return;

  const ctx = canvas.value.getContext('2d');
  if (!ctx) return;

  // 设置canvas尺寸与video一致
  canvas.value.width = video.value.videoWidth;
  canvas.value.height = video.value.videoHeight;
  
  // 绘制视频帧到canvas
  ctx.drawImage(video.value, 0, 0, canvas.value.width, canvas.value.height);
  
  // 转换为base64格式
  faceImageSrc.value = canvas.value.toDataURL('image/jpeg');
  faceImageCaptured.value = true;
  successMessage('人脸拍摄完成')
}

// 自动检测人脸并登录
const startFaceDetection = () => {
  if (!canvas.value || !video.value || isProcessing.value) return;
  
  isAutoDetecting.value = true;
  detectedFaceCount.value = 0;
  isFaceDetected.value = false;
  
  // 停止之前可能存在的检测
  if (detectionInterval) {
    clearInterval(detectionInterval);
  }
  
  // 开始定期检测
  detectionInterval = window.setInterval(async () => {
    if (!isCapturing.value || isProcessing.value) {
      if (detectionInterval) {
        clearInterval(detectionInterval);
      }
      return;
    }
    
    try {
      // 捕获当前视频帧
      const ctx = canvas.value?.getContext('2d');
      if (!ctx || !canvas.value || !video.value) return;
      
      canvas.value.width = video.value.videoWidth;
      canvas.value.height = video.value.videoHeight;
      ctx.drawImage(video.value, 0, 0, canvas.value.width, canvas.value.height);
      
      // 转换为base64格式
      const currentImageSrc = canvas.value.toDataURL('image/jpeg');
      faceImageSrc.value = currentImageSrc;
      
      // 使用工具函数进行人脸识别登录
      const base64Data = currentImageSrc.split(',')[1];
      const res = await faceLogin('', base64Data);
      
      if (res.code === 200 && res.data?.user) {
        detectedFaceCount.value++;
        isFaceDetected.value = true;
        
        // 连续检测到3次人脸才执行登录，避免误识别
        if (detectedFaceCount.value >= 3) {
          if (detectionInterval) {
            clearInterval(detectionInterval);
            detectionInterval = null;
          }
          isAutoDetecting.value = false;
          
          // 执行登录逻辑
          isLoginSuccess.value = true;
          const userData = res.data.user;
          localStorage.setItem('user', JSON.stringify(userData));
          localStorage.setItem('token', res.data.token);
          
          successMessage('人脸识别成功，正在跳转...');
          
          setTimeout(() => {
            const role = userData.role;
            if (role === 1) {
              router.push('/adminuser');
            } else if (role === 2) {
              router.push('/adminbook');
            } else if (role === 3) {
              router.push('/recordctrl');
            } else if (role === 4) {
              router.push('/suggestionctrl');
            } else if (role === 5) {
              router.push('/shelve');
            } else {
              router.push('/');
            }
            closeLogin();
          }, 1100);
        }
      } else {
        // 重置检测计数
        if (detectedFaceCount.value > 0) {
          detectedFaceCount.value--;
        }
        if (detectedFaceCount.value <= 0) {
          isFaceDetected.value = false;
        }
      }
    } catch (error) {
      // 忽略单次检测错误，继续检测
      console.log('人脸检测中...');
    }
  }, 1000); // 每秒检测一次
}

// 初始化face-api模型
const loadFaceApiModelsWrapper = async () => {
  if (loginType.value !== 'face') return;
  
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
  try {
    if (!video.value) return;
    
    isLoading.value = true
    
    // 使用工具函数启动摄像头，适配现有的face.ts文件
    stream = await startCam(video.value)
    if (stream) {
      isCapturing.value = true
      isLoading.value = false
      successMessage('摄像头已启动，开始自动识别人脸...')
      
      // 启动自动人脸识别
      setTimeout(() => {
        startFaceDetection();
      }, 1000); // 延迟1秒开始检测，让摄像头稳定
    } else {
      isLoading.value = false
      errorMessage('启动摄像头失败')
    }
  } catch (err: any) {
    console.error('摄像头访问失败:', err);
    isLoading.value = false
    errorMessage(err.message || '无法访问摄像头，请确保已授予权限');
  }
}

// 停止摄像头
const stopCamera = () => {
  // 停止人脸检测
  if (detectionInterval) {
    clearInterval(detectionInterval);
    detectionInterval = null;
  }
  
  // 使用工具函数停止摄像头
  stopCam(stream)
  stream = null
  isCapturing.value = false
  isAutoDetecting.value = false
  isFaceDetected.value = false
  detectedFaceCount.value = 0
  
  // 原生API方式
  // if (stream) {
  //   stream.getTracks().forEach(track => track.stop());
  //   stream = null;
  // }
}

// 监听登录类型变化
loginType.value = 'password';
const watchLoginType = () => {
  // 当切换到人脸登录时，先加载模型再启动摄像头
  if (loginType.value === 'face') {
    nextTick(() => {
      loadFaceApiModelsWrapper().then(() => {
        startCamera();
      });
    });
  } else {
    // 切换到密码登录时，停止摄像头
    stopCamera();
    // 重置人脸预览和状态
    faceImageSrc.value = '';
    faceImageCaptured.value = false;
    isCapturing.value = false;
    isProcessing.value = false;
    isLoading.value = false;
    isLoginSuccess.value = false;
  }
}

// 监听loginType的变化
loginType.value = 'password';
const unwatch = () => {
  // 在组件卸载时清理
  stopCamera();
};

// 组件挂载时执行
onMounted(() => {
  // 当首次加载时，如果默认是人脸登录则加载模型并启动摄像头
  if (loginType.value === 'face') {
    nextTick(() => {
      loadFaceApiModelsWrapper().then(() => {
        startCamera();
      });
    });
  }
});

// 组件卸载时清理
import { onUnmounted } from 'vue';
onUnmounted(() => {
  // 确保停止检测
  if (detectionInterval) {
    clearInterval(detectionInterval);
    detectionInterval = null;
  }
  stopCamera();
});

// 监听loginType变化
import { watch } from 'vue';
watch(loginType, () => {
  watchLoginType();
});

const successMessage = (text: string) => {
  successMsg.value = text
  setTimeout(() => {
    successMsg.value = ''
  }, 3000)
}

// 错误提示
const errorMessage = (text: string) => {
  errorMsg.value = text
  setTimeout(() => {
    errorMsg.value = ''
  }, 3000)
}

// 关闭弹窗
const emit = defineEmits(['close']);
const closeLogin = () => {
  stopCamera();
  emit('close');
}
</script>


<style scoped>
.login-container {
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

/* 登录方式切换标签 */
.login-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #444;
}
.tab-btn {
  flex: 1;
  padding: 10px;
  background: transparent;
  border: none;
  color: #ccc;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}
.tab-btn:hover {
  color: #fff;
}
.tab-btn.active {
  color: #409eff;
  font-weight: 600;
}
.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 20%;
  right: 20%;
  height: 3px;
  background: linear-gradient(90deg, #8b5cf6 0%, #a78bfa 100%);
  border-radius: 3px;
}

/* 账号密码登录表单 */
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

/* 人脸识别登录样式 */
.face-login-form {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

/* 主内容包装器 - 实现左右布局 */
.login-content-wrapper {
  display: flex;
  gap: 24px;
  width: 100%;
  align-items: flex-start;
}

/* 主内容区域 */
.login-main-content {
  flex: 1;
  min-width: 0;
}

.camera-container {
  width: 280px;
  max-width: 320px;
  height: 160px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  overflow: hidden;
  background-color: #111;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.camera-controls {
  position: absolute;
  bottom: 10px;
  width: 90%;
  display: flex;
  justify-content: center;
}

/* 自动检测状态样式 */
.detection-status {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.7);
  padding: 6px 12px;
  border-radius: 20px;
  backdrop-filter: blur(5px);
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #666;
  transition: all 0.3s ease;
  animation: pulse 2s infinite;
}

.status-indicator.detecting {
  background-color: #ff9800;
  animation: pulse 1.5s infinite;
}

.status-indicator.detected {
  background-color: #4caf50;
  animation: pulse-success 1s infinite;
}

.status-text {
  font-size: 12px;
  color: #fff;
  white-space: nowrap;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes pulse-success {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.9;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
.camera-container video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.face-login-actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 15px;
}

/* 自动识别模式下隐藏手动操作按钮 */
.face-login-actions .capture-btn,
.face-login-actions .face-submit {
  display: none;
}
.capture-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
}
.capture-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #5daf34 0%, #67c23a 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.35);
}
.camera-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid white;
  border-radius: 50%;
  position: relative;
}
.camera-icon::before {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 8px;
  height: 4px;
  background: white;
  border-radius: 2px;
}

/* 通用按钮样式 */
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
.face-submit {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}
.face-submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.35);
}
.arrow-icon {
  border: solid white;
  border-width: 0 2px 2px 0;
  display: inline-block;
  padding: 3px;
  transform: rotate(-45deg);
}

/* 人脸预览样式 */
.face-preview {
  width: 100%;
  text-align: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.face-preview h4 {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #a78bfa;
}
.face-preview img {
  width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
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
  .login-container {
    padding: 20px;
    padding-right: 40px;
    max-width: 350px;
    border-radius: 20px;
  }
  .form-header h2 {
    font-size: 16px;
  }
  .input-group input,
  .submit-btn,
  .capture-btn {
    padding: 10px;
  }
  
  /* 移动端恢复为垂直布局 */
  .login-content-wrapper {
    flex-direction: column;
    align-items: center;
  }
  
  .camera-container {
    max-width: 280px;
    height: 220px;
  }
  
  .face-preview-sidebar {
    width: 100%;
    max-width: 280px;
    margin-top: 16px;
  }
  
  .face-preview {
    width: 100%;
    padding: 12px;
  }
}
@media (max-width: 320px) {
  .login-container {
    padding: 16px;
    padding-right: 36px;
    max-width: 280px;
    border-radius: 16px;
  }
  .form-header h2 {
    font-size: 14px;
  }
  .tab-btn {
    font-size: 12px;
    padding: 8px;
  }
  .camera-container {
    max-width: 240px;
    height: 200px;
  }
  
  .face-preview {
    width: 100%;
    max-width: 240px;
  }
}
</style>