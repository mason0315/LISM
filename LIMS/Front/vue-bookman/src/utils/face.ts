import * as faceapi from 'face-api.js'
import request from './request'

// 加载人脸检测模型
export const loadFaceApiModels = async (): Promise<boolean> => {
  try {
    // 加载模型 - 注意根据实际模型文件位置调整路径
    await faceapi.nets.tinyFaceDetector.loadFromUri('/models')
    await faceapi.nets.faceLandmark68Net.loadFromUri('/models')
    await faceapi.nets.faceRecognitionNet.loadFromUri('/models')
    console.log('人脸模型加载成功')
    return true
  } catch (error) {
    console.error('人脸模型加载失败:', error)
    console.warn('模型加载失败，系统将尝试直接使用摄像头功能')
    // 返回true继续运行，即使模型加载失败
    return true
  }
}

// 启动摄像头
export const startCamera = async (videoRef: HTMLVideoElement | null): Promise<MediaStream | null> => {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    throw new Error('浏览器不支持摄像头访问')
  }

  if (!videoRef) {
    throw new Error('视频元素未找到')
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ 
      video: { width: 300, height: 300 } 
    })
    
    videoRef.srcObject = stream
    return stream
  } catch (error) {
    console.error('无法访问摄像头:', error)
    throw new Error('无法访问摄像头，请检查权限设置')
  }
}

// 停止摄像头
export const stopCamera = (stream: MediaStream | null) => {
  if (stream) {
    const tracks = stream.getTracks()
    tracks.forEach(track => track.stop())
  }
}

// 拍照并获取base64数据
export const captureImage = (videoRef: HTMLVideoElement | null, canvasRef: HTMLCanvasElement | null): string => {
  if (!videoRef || !canvasRef) {
    throw new Error('视频或画布元素未找到')
  }

  const canvas = canvasRef
  const ctx = canvas.getContext('2d')
  
  if (!ctx) {
    throw new Error('无法获取画布上下文')
  }
  
  // 绘制当前视频帧到canvas
  ctx.drawImage(videoRef, 0, 0, canvas.width, canvas.height)
  
  // 获取图像数据
  const imageData = canvas.toDataURL('image/jpeg')
  // 返回纯base64数据（移除前缀）
  return imageData.split(',')[1]
}

// 人脸识别登录
export const faceLogin = async (username: string, faceData: string) => {
  try {
    const res = await request.post('/auth/face/login', {
      username,
      faceData
    })
    
    return res
  } catch (error) {
    console.error('人脸识别登录失败:', error)
    throw error
  }
}

// 人脸注册
export const faceRegister = async (username: string, faceData: string) => {
  try {
    const res = await request.post('/auth/face/register', {
      username,
      faceData
    })
    
    return res
  } catch (error) {
    console.error('人脸注册失败:', error)
    throw error
  }
}

// 人脸数据更新
export const faceUpdate = async (username: string, faceData: string) => {
  try {
    const res = await request.post('/auth/face/update', {
      username,
      faceData
    })
    
    return res
  } catch (error) {
    console.error('人脸数据更新失败:', error)
    throw error
  }
}

// 清除人脸数据
export const faceClear = async (userId: number) => {
  try {
    const res = await request.delete(`/auth/face/clear/${userId}`)
    
    return res
  } catch (error) {
    console.error('清除人脸数据失败:', error)
    throw error
  }
}

// 检测人脸
export const detectFace = async (imageElement: HTMLImageElement | HTMLVideoElement) => {
  try {
    // 使用tinyFaceDetector进行人脸检测
    const detections = await faceapi.detectSingleFace(
      imageElement, 
      new faceapi.TinyFaceDetectorOptions()
    ).withFaceLandmarks().withFaceDescriptor()
    
    return detections
  } catch (error) {
    console.error('人脸检测失败:', error)
    console.warn('人脸检测功能不可用，将跳过前端检测，直接使用后端百度AI进行识别')
    // 不抛出错误，允许继续流程
    return null
  }
}