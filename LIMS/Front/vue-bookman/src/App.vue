<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="slide">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
/* 隐藏所有页面的滚动条 */
::-webkit-scrollbar {
  display: none;
}

/* 清除页面边框 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 防止页面切换时出现白屏 */
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow-x: hidden;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
}

#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #08080c 0%, #1a1a2e 100%);
  color: #e0e0e0;
  font-family: Arial, sans-serif;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 添加过渡效果以减少白屏现象 */
.slide-enter-active {
  transition: all 0.7s ease;
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
</style>