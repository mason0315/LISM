<template>
  <div v-if="currentMessage" class="message-detail">
    <div class="message-header">
      <h3>{{ currentMessage.title }}</h3>
      <p class="date">{{ currentMessage.date }}</p>
    </div>
    <div class="message-content" v-html="currentMessage.content"></div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, computed } from 'vue';

interface Message {
  title: string;
  date: string;
  content: string;
}

const props = defineProps<{
  messages: Message[];
  currentMessageIndex: number | null;
}>();

const currentMessage = computed(() => {
  if (props.currentMessageIndex !== null) {
    return props.messages[props.currentMessageIndex];
  }
  return null;
});
</script>

<style scoped>
.message-detail {
  width: 100%;
  max-width: 800px;
  background-color: rgba(21, 21, 80, 0.17);
  border-radius: 8px;
  padding: 20px;
  margin-top: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.date {
  font-size: 0.9em;
  color: darkgrey;
}

.message-content {
  line-height: 1.6;
}

.message-content a {
  text-decoration: none;
}
</style>
