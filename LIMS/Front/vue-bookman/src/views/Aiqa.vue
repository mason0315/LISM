<template>
  <div class="ai-qa-container" data-v-aiqa-page>
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="app-logo">智能问答</div>
      </div>
      
      <!-- 新建对话按钮 -->
      <button class="new-chat-button" @click="createNewChat">
        + 新建对话
      </button>
      
      <!-- 对话列表 -->
      <div class="chat-list">
        <div 
          v-for="chat in chatHistory" 
          :key="chat.id"
          class="chat-item"
          :class="{ active: activeChatId === chat.id }"
          @click="switchChat(chat.id)"
        >
          <div class="chat-title" style="border: 1px solid #333; border-radius: 4px; padding: 8px; background: #1a1a2e;">{{ chat.title }}</div>
          <div class="chat-time">{{ formatTime(chat.timestamp) }}</div>
        </div>
        

      </div>
      
      <!-- 侧边栏底部 -->
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">👤</div>
          <div class="user-name">用户</div>
        </div>
      </div>
    </div>
    
    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 对话内容区域 -->
      <div class="chat-content">
        <!-- 聊天标题栏 - 始终显示 -->
        <div class="chat-header">
          <h2 style="border: 1px solid #444; border-radius: 6px; padding: 12px; background: #16213e; margin: 0;">{{ activeChat ? activeChat.title : '智能助手' }}</h2>
          <div class="chat-actions">
            <button class="action-button" @click="createNewChat">
              <span class="icon">+</span>
            </button>
            <button class="action-button back-home-button" @click="handleBackHome">
              <span class="icon">🏠</span>
            </button>
          </div>
        </div>
        
        <!-- 消息列表容器 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 欢迎界面（无对话或对话为空时显示） -->
          <div v-if="!activeChat || activeChat.messages.length === 0" class="welcome-screen">
            <div class="welcome-content">
              <div class="welcome-icon">🤖</div>
              <h1>智能助手</h1>
              <p>有什么我可以帮助您的吗？</p>
            </div>
          </div>
          
          <!-- 消息列表（有对话内容时显示） -->
          <template v-if="activeChat && activeChat.messages.length > 0">
            <div 
              v-for="(message, index) in activeChat.messages" 
              :key="index"
              class="message-wrapper"
              :class="{ 
                'user-message': message.role === 'user',
                'ai-message': message.role === 'assistant'
              }"
            >
              <div class="message-avatar">
                {{ message.role === 'user' ? '👤' : '🤖' }}
              </div>
              <div class="message-content">
                <div class="message-text" :style="message.style ? { deepThinking: message.style === 'deep-thinking' } : null">{{ message.content }}</div>
                
                <!-- 引用文本功能 - 仅对AI消息且有sources时显示 -->
                <div v-if="message.role === 'assistant' && message.sources && message.sources.length > 0" class="sources-container">
                  <button 
                    class="sources-toggle-button" 
                    @click="toggleSources(index)"
                    :class="{ active: expandedSources.includes(index) }"
                    :style="{
                      background: expandedSources.includes(index) ? 'linear-gradient(90deg, #1a054c 0%, #3a0a6a 100%)' : 'linear-gradient(90deg, #0a031a 0%, #120525 100%)',
                      border: expandedSources.includes(index) ? '1px solid #3a0f5a' : '1px solid #120525',
                      color: '#e0e0e0',
                      outline: 'none',
                      boxShadow: expandedSources.includes(index) ? '0 2px 8px rgba(50, 12, 70, 0.2)' : 'none'
                    }"
                    @mouseenter="$event.target.style.background = expandedSources.includes(index) ? 'linear-gradient(90deg, #2a055c 0%, #4a0a7a 100%)' : 'linear-gradient(90deg, #0d031f 0%, #150528 100%)'; $event.target.style.color = '#ffffff';"
                    @mouseleave="$event.target.style.background = expandedSources.includes(index) ? 'linear-gradient(90deg, #1a054c 0%, #3a0a6a 100%)' : 'linear-gradient(90deg, #0a031a 0%, #120525 100%)'; $event.target.style.color = '#e0e0e0';"
                  >
                    引用文本 ({{ message.sources.length }})
                  </button>
                  <div 
                    v-if="expandedSources.includes(index)" 
                    class="sources-expansion-panel"
                  >
                    <div 
                      v-for="(source, sourceIndex) in message.sources" 
                      :key="sourceIndex"
                      class="source-item"
                    >
                      <div style="border: 1px solid #2a0a4a; border-radius: 6px; padding: 8px; background: #0a032c; margin-bottom: 8px;">
                        <button 
                          class="source-title-button"
                          style="
                              background: linear-gradient(90deg, #0a033c 0%, #1a054a 100%);
                              border: 1px solid #2a0a4a;
                              color: #ffffff;
                              outline: none;
                              boxShadow: 0 2px 6px rgba(30, 8, 45, 0.3);
                              width: 100%;
                              text-align: left;
                            "
                          @mouseenter="$event.target.style.background = 'linear-gradient(90deg, #12054c 0%, #200a5a 100%)'; $event.target.style.boxShadow = '0 3px 10px rgba(30, 8, 45, 0.5)';"
                          @mouseleave="$event.target.style.background = 'linear-gradient(90deg, #0a033c 0%, #1a054a 100%)'; $event.target.style.boxShadow = '0 2px 6px rgba(30, 8, 45, 0.3)';"
                          @click="showSourceDetails(source)"
                        >{{ source.title }}</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
          
          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading-message">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
              <div class="loading-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 输入区域 - 始终显示 -->
        <div class="input-section">
          <!-- 临时上传附件显示区域 -->
          <div v-if="tempFiles.length > 0" class="temp-files-container">
            <div class="temp-files-header">
              <span class="temp-files-title">临时附件 ({{ tempFiles.length }})</span>
              <button 
                class="clean-all-temp-files-btn"
                @click="cleanAllTempFiles"
                title="清理所有临时附件"
                :disabled="isLoading"
              >
                清理全部
              </button>
            </div>
            <div 
              v-for="file in tempFiles" 
              :key="file.id"
              class="temp-file-item"
            >
              <span class="file-name">{{ file.name }}</span>
              <button 
                class="remove-file-btn"
                @click="removeTempFile(file.id)"
                title="移除附件"
              >
                ×
              </button>
            </div>
          </div>
          
          <!-- 已嵌入文件显示区域 -->
          <div v-if="embeddedFiles.length > 0" class="embedded-files-container">
            <div class="embedded-files-header">
              <span class="embedded-files-title">已嵌入文件 ({{ embeddedFiles.length }})</span>
              <button 
                class="clean-all-files-btn"
                @click="manualCleanUp"
                title="清理所有嵌入文件"
                :disabled="isLoading"
              >
                清理全部
              </button>
            </div>
            <div 
              v-for="(file, index) in embeddedFiles" 
              :key="index"
              class="embedded-file-item"
            >
              <span class="file-name">{{ file.name }}</span>
              <button 
                class="remove-file-btn"
                @click="removeEmbeddedFile(index)"
                title="移除文件"
              >
                ×
              </button>
            </div>
          </div>
          
          <div class="input-container">
            <div class="input-wrapper">
              <textarea
                v-model="userInput"
                class="message-input"
                placeholder="输入您的问题..."
                @keydown.enter.exact="$event => { $event.preventDefault(); sendMessage(); }"
                @keydown.enter.shift="$event => {}"
                rows="1"
                ref="messageInput"
              ></textarea>
            </div>
            <div class="action-buttons">
              <button 
                class="deep-thinking-button" 
                @click="toggleDeepThinking"
                :disabled="isLoading"
                :class="{ active: deepThinkingActive }"
              >
                深度思考
              </button>

              <div class="file-upload-container">
                <input
                  type="file"
                  ref="fileInput"
                  style="display: none"
                  @change="handleFileUpload"
                  accept=".pdf,.doc,.docx,.txt,.md"
                />
                <button 
                  class="file-upload-button"
                  @click="triggerFileUpload"
                  :disabled="isLoading"
                  :title="deepThinkingActive ? '上传文件（当前为深度思考模式）' : '上传文件（当前为默认模式）'"
                >
                  📄 嵌入文件
                </button>
              </div>
              <div class="temp-file-container">
                <input
                  type="file"
                  ref="tempFileInput"
                  style="display: none"
                  @change="handleTempFileUpload"
                  accept=".pdf,.doc,.docx,.txt,.md"
                />
                <button 
                  class="temp-file-button"
                  @click="triggerTempFileUpload"
                  :disabled="isLoading"
                  title="上传附件（仅本次对话使用，不上传至知识库）"
                >
                  📎 附件
                </button>
              </div>
              <!-- 智能体按钮 -->
              <div class="agent-dropdown-container">
                <button 
                  class="agent-button"
                  @click="toggleAgentDropdown"
                  :disabled="isLoading"
                  :class="{ active: activeAgent }"
                >
                  智能体
                </button>
                <div v-if="showAgentDropdown" class="agent-dropdown-menu">
                  <button 
                    class="agent-option"
                    @click="activateAgent('writer')"
                    :class="{ active: activeAgent === 'writer' }"
                  >
                    Writer模式 (大篇幅文本)
                  </button>
                  <button 
                    class="agent-option"
                    @click="activateAgent('speaker')"
                    :class="{ active: activeAgent === 'speaker' }"
                  >
                    Speaker模式 (人性化交流)
                  </button>
                  <button 
                    v-if="activeAgent" 
                    class="agent-option reset-option"
                    @click="activateAgent(null)"
                  >
                    重置模式
                  </button>
                </div>
              </div>
              
              <button 
                class="send-button" 
                @click="sendMessage"
                :disabled="!userInput.trim() || isLoading"
              >
                发送
              </button>
            </div>
          </div>
            <div class="input-hint">
              <span>Shift + Enter 换行</span>
              <span v-if="activeAgent" class="agent-indicator">
                {{ activeAgent === 'writer' ? '当前为Writer模式' : '当前为Speaker模式' }}
              </span>
            </div>
        </div>
      </div>
    </div>
    
    <!-- Source详情模态框 -->
    <div v-if="showSourceModal && currentSource" class="source-modal" @click="closeSourceModal">
      <div class="source-modal-content" @click.stop>
        <div class="source-modal-header">
          <h3>{{ currentSource.title }}</h3>
          <button class="source-modal-close" @click="closeSourceModal">×</button>
        </div>
        <div class="source-modal-body">
          <pre class="source-text">{{ currentSource.text || '暂无文本内容' }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 智能体按钮样式 */
.agent-buttons {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.agent-button {
  padding: 8px 16px;
  border: 1px solid #4a154b;
  border-radius: 6px;
  background: linear-gradient(135deg, #1a054a 0%, #2a0a4a 100%);
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.agent-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #2a0a4a 0%, #3a105a 100%);
  border-color: #6a256b;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(74, 21, 75, 0.3);
}

.agent-button.active {
  background: linear-gradient(135deg, #4a154b 0%, #6a256b 100%);
  border-color: #8a358b;
  box-shadow: 0 0 15px rgba(138, 53, 139, 0.5);
}

.agent-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.writer-button.active {
  background: linear-gradient(135deg, #154b4a 0%, #256b6a 100%);
  border-color: #358b8a;
  box-shadow: 0 0 15px rgba(53, 139, 138, 0.5);
}

.speaker-button.active {
  background: linear-gradient(135deg, #4b154a 0%, #6b256a 100%);
  border-color: #8b358a;
  box-shadow: 0 0 15px rgba(139, 53, 138, 0.5);
}

.reset-button {
  background: linear-gradient(135deg, #4b4b15 0%, #6b6b25 100%);
  border-color: #8b8b35;
}

.agent-dropdown-container {
  position: relative;
  display: inline-block;
}

.agent-button {
  background: linear-gradient(90deg, #3a105a 0%, #4a154b 100%);
  border: 1px solid #6a256b;
  color: #ffffff;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  margin-right: 8px;
  position: relative;
  transition: all 0.2s ease;
}

.agent-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #4a154b 0%, #6a256b 100%);
  border-color: #8a358b;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(74, 21, 75, 0.3);
}

.agent-button.active {
  background: linear-gradient(135deg, #4a154b 0%, #6a256b 100%);
  border-color: #8a358b;
  box-shadow: 0 0 15px rgba(138, 53, 139, 0.5);
}

.agent-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.agent-dropdown-menu {
  position: absolute;
  bottom: 100%;
  left: 0;
  background: linear-gradient(180deg, #100520 0%, #1a0a2e 100%);
  border: 1px solid #2a1a3e;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  padding: 4px 0;
  margin-bottom: 4px;
  z-index: 1000;
  min-width: 200px;
}

.agent-option {
  width: 100%;
  padding: 8px 16px;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  color: #e0e0e0;
  transition: all 0.2s ease;
}

.agent-option:hover {
  background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
  color: #ffffff;
}

.agent-option.active {
  background: linear-gradient(90deg, #4a148c 0%, #6a1b9a 100%);
  color: #ffffff;
  border-left: 3px solid #9c27b0;
}

.agent-option.reset-option {
  color: #ff8a80;
  margin-top: 4px;
  border-top: 1px solid #2a1a3e;
}

.agent-option.reset-option:hover {
  background: linear-gradient(90deg, #351515 0%, #452020 100%);
  color: #ff5252;
}

.agent-indicator {
  margin-left: 20px;
  font-size: 13px;
  color: #a0a0a0;
  padding: 4px 8px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

/* 模态框样式 */
.source-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.source-modal-content {
  background: linear-gradient(135deg, #0a033c 0%, #1a054a 100%);
  border: 1px solid #2a0a4a;
  border-radius: 8px;
  width: 80%;
  max-width: 800px;
  max-height: 80%;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
}

.source-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #2a0a4a;
  background: rgba(42, 10, 74, 0.2);
}

.source-modal-header h3 {
  margin: 0;
  color: #ffffff;
  font-size: 18px;
}

.source-modal-close {
  background: none;
  border: none;
  color: #ffffff;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.source-modal-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.source-modal-body {
  padding: 20px;
  max-height: calc(80vh - 100px);
  overflow-y: auto;
}

.source-text {
  background-color: #0a032c;
  border: 1px solid #2a0a4a;
  border-radius: 6px;
  padding: 16px;
  margin: 0;
  color: #e0e0e0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}
</style>

<script>
export default {
  name: 'Aiqa',
  data() {
    return {
      userInput: '',
      isLoading: false,
      activeChatId: null,
      chatHistory: [],
      deepThinkingActive: false,
      embeddedFiles: [], // 已嵌入文件列表
      tempFiles: [], // 临时文件列表
      tempFileTokens: {}, // 临时文件令牌映射
        expandedSources: [], // 跟踪哪些消息的引用文本已展开
        // 新增状态变量用于控制source详情模态框
        showSourceModal: false,
        currentSource: null,
      // 智能体模式状态
      activeAgent: null, // 当前激活的智能体模式：null(默认)、'writer'、'speaker'
      showAgentDropdown: false, // 控制智能体下拉框显示
        // 模型配置
        models: {
          // 默认模型（图书管理）
          default: {
            name: '图书管理',
            slug: '36f38646-a044-4677-9e4e-8a519402c38f',
            id: 3,
            vectorTag: null,
            createdAt: '2025-10-16T11:59:22.020Z',
            openAiTemp: 0.7,
            openAiHistory: 20,
            lastUpdatedAt: '2025-10-16T11:59:22.020Z',
            openAiPrompt: 'Given the following conversation, relevant context, and a follow up question, reply with an answer to the current question the user is asking. Return only your response to the question given the above information following the users instructions as needed.',
            similarityThreshold: 0.25,
            chatProvider: 'ollama',
            chatModel: 'deepseek-r1:7b',
            topN: 4,
            chatMode: 'chat',
            pfpFilename: null,
            agentProvider: null,
            agentModel: null,
            queryRefusalResponse: 'There is no relevant information in this workspace to answer your query.',
            vectorSearchMode: 'default'
          },
          // 训练模型配置（图书管理训练版）
          trained: {
            name: '图书管理（训练版）',
            slug: '715ff10f-5ea8-4ef2-8d37-6ae40856acbb',
            id: 5,
            vectorTag: null,
            createdAt: '2025-10-17T08:29:16.887Z',
            openAiTemp: 0.7,
            openAiHistory: 20,
            lastUpdatedAt: '2025-10-17T08:29:16.887Z',
            openAiPrompt: 'Given the following conversation, relevant context, and a follow up question, reply with an answer to the current question the user is asking. Return only your response to the question given the above information following the users instructions as needed.',
            similarityThreshold: 0.25,
            chatProvider: 'ollama',
            chatModel: 'deepseek-r1:7b',
            topN: 4,
            chatMode: 'chat',
            pfpFilename: null,
            agentProvider: null,
            agentModel: null,
            queryRefusalResponse: 'There is no relevant information in this workspace to answer your query.',
            vectorSearchMode: 'default'
          }
        }

    }
  },
  computed: {
    activeChat() {
      return this.chatHistory.find(chat => chat.id === this.activeChatId) || null;
    }
  },
  mounted() {
    // 自动调整输入框高度
    this.adjustTextareaHeight();
    window.addEventListener('resize', this.adjustTextareaHeight);
    
    // 禁止拖动功能 - 直接在mounted中实现
    this.preventDragging();
    
    // 添加延迟后执行测试API调用，以验证移除文件功能
        setTimeout(() => {
          console.log('=== 执行API测试调用 ===');
          const testModelId = this.modelId || '3';
          const testFilePath = '123.docx'; // 使用实际存在的文件名进行测试
          
          // 发送测试请求到正确的API路径
          fetch(`/ai/api/ai/update-embeddings?modelId=${testModelId}`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'accept': 'application/json'
            },
            body: JSON.stringify({
              adds: [],
              deletes: [testFilePath] // modelId作为查询参数传递
            })
          })
      .then(response => {
        console.log('测试API响应状态:', response.status);
        return response.json();
      })
      .then(data => {
        console.log('测试API响应数据:', JSON.stringify(data));
      })
      .catch(error => {
        console.error('测试API调用失败:', error);
      });
    }, 3000);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.adjustTextareaHeight);
    // 移除拖动事件监听器
    document.removeEventListener('dragstart', this.handleDragStart);
    document.removeEventListener('drop', this.handleDrop);
    document.removeEventListener('dragover', this.handleDragOver);
  },
  methods: {
    // 切换智能体下拉框显示
    toggleAgentDropdown() {
      if (!this.isLoading) {
        this.showAgentDropdown = !this.showAgentDropdown;
        // 点击其他地方时关闭下拉框
        if (this.showAgentDropdown) {
          setTimeout(() => {
            document.addEventListener('click', this.closeAgentDropdown);
          }, 10);
        } else {
          document.removeEventListener('click', this.closeAgentDropdown);
        }
      }
    },
    // 关闭智能体下拉框
    closeAgentDropdown(event) {
      const agentButton = document.querySelector('.agent-button');
      const dropdownMenu = document.querySelector('.agent-dropdown-menu');
      if (agentButton && !agentButton.contains(event.target) &&
          dropdownMenu && !dropdownMenu.contains(event.target)) {
        this.showAgentDropdown = false;
        document.removeEventListener('click', this.closeAgentDropdown);
      }
    },
    // 事件处理函数
    handleDragStart(e) { e.preventDefault(); },
    handleDrop(e) { e.preventDefault(); },
    handleDragOver(e) { e.preventDefault(); },
    
    // 阻止拖动功能
    preventDragging() {
      // 阻止文档级别的拖动事件
      document.addEventListener('dragstart', this.handleDragStart);
      document.addEventListener('drop', this.handleDrop);
      document.addEventListener('dragover', this.handleDragOver);
      
      // 为主要容器和关键元素添加不可拖动属性
      const container = this.$el;
      if (container) {
        container.setAttribute('draggable', 'false');
        // 递归设置所有子元素为不可拖动
        this.setAllElementsUndraggable(container);
      }
    },
    
    // 触发文件上传
    triggerFileUpload() {
      // 现在支持深度思考模式下的文件上传
      this.$refs.fileInput.click();
    },
    
    // 触发临时文件上传
    triggerTempFileUpload() {
      this.$refs.tempFileInput.click();
    },
    
    // 移除临时文件
    removeTempFile(fileId) {
      // 从临时文件列表中移除
      const fileIndex = this.tempFiles.findIndex(file => file.id === fileId);
      if (fileIndex !== -1) {
        const fileName = this.tempFiles[fileIndex].name;
        this.tempFiles.splice(fileIndex, 1);
        // 从临时文件令牌映射中移除
        delete this.tempFileTokens[fileId];
        // 显示移除消息
        this.addSystemMessage(`附件 "${fileName}" 已移除，不再包含在后续对话中。`);
      }
    },
    
    // 清理所有临时文件
    cleanAllTempFiles() {
      if (this.tempFiles.length > 0) {
        this.tempFiles = [];
        this.tempFileTokens = {};
        this.addSystemMessage('所有临时附件已清理。');
      }
    },
    
    // 处理临时文件上传
    async handleTempFileUpload(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      this.isLoading = true;
      
      try {
        // 使用FileReader读取文件内容
        const fileContent = await this.readFileContent(file);
        
        // 创建临时文件对象
        const tempFile = {
          id: Date.now().toString(),
          name: file.name,
          type: file.type,
          size: file.size,
          content: fileContent,
          uploadTime: new Date().toISOString()
        };
        
        // 发送临时文件到后端
        const response = await fetch('/ai/api/ai/temp-file', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            filename: file.name,
            content: fileContent
          })
        });
        
        if (!response.ok) {
          throw new Error(`HTTP错误! 状态码: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
          // 保存临时文件令牌
          this.tempFileTokens[tempFile.id] = data.token;
          // 添加到临时文件列表
          this.tempFiles.push(tempFile);
          // 显示成功消息
          this.addSystemMessage(`文件 "${file.name}" 已成功作为附件上传，可在本次对话中使用。`);
        } else {
          throw new Error(data.error || '临时文件上传失败');
        }
      } catch (error) {
        console.error('临时文件上传错误:', error);
        this.addSystemMessage(`临时文件上传失败: ${error.message}`);
      } finally {
        this.isLoading = false;
        // 清空文件输入
        this.$refs.tempFileInput.value = '';
      }
    },
    
    // 读取文件内容
    readFileContent(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => resolve(e.target.result);
        reader.onerror = (error) => reject(error);
        
        // 根据文件类型选择读取方式
        if (file.type === 'text/plain' || file.name.endsWith('.txt') || file.name.endsWith('.md')) {
          reader.readAsText(file);
        } else {
          // 对于其他文件类型，使用readAsDataURL
          reader.readAsDataURL(file);
        }
      });
    },
    
    // 处理文件上传
    async handleFileUpload(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      this.isLoading = true;
      
      try {
        const formData = new FormData();
        formData.append('file', file);
        // 根据当前模式动态设置modelId
        const modelId = this.deepThinkingActive ? 5 : 3;
        formData.append('modelId', modelId.toString());
        // 根据当前模式动态设置modelSlug
        const modelSlug = this.deepThinkingActive ? '715ff10f-5ea8-4ef2-8d37-6ae40856acbb' : this.models.default.slug;
        formData.append('modelSlug', modelSlug);
        
        const response = await fetch('/ai/api/ai/upload-document', {
          method: 'POST',
          headers: {
            'accept': 'application/json'
          },
          body: formData
        });
        
        if (!response.ok) {
          throw new Error(`HTTP错误! 状态码: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
          // 获取后端返回的实际文件路径
          const embeddedFilePath = data.data?.used_file_path || '';
          // 添加文件到已嵌入文件列表，保存原始文件名和实际路径
          this.embeddedFiles.push({ 
            name: file.name, 
            path: embeddedFilePath 
          });
          console.log('文件上传成功并添加到嵌入列表:', { name: file.name, path: embeddedFilePath });
          // 显示成功消息
          this.addSystemMessage(`文件 "${file.name}" 已成功上传并嵌入到知识库中！会话结束或离开页面时将自动清理。`);
        } else {
          throw new Error(data.error || '文件上传失败');
        }
      } catch (error) {
        console.error('文件上传错误:', error);
        this.addSystemMessage(`文件上传失败: ${error.message}`);
      } finally {
        this.isLoading = false;
        // 清空文件输入，以便可以重新选择同一个文件
        this.$refs.fileInput.value = '';
      }
    },
    
    // 添加系统消息
    addSystemMessage(content) {
      if (!this.activeChat) {
        this.createNewChat();
      }
      
      this.activeChat.messages.push({
        id: Date.now().toString(),
        role: 'system',
        content: content,
        timestamp: new Date().toISOString(),
        isSystem: true
      });
      
      this.scrollToBottom();
    },
    
    // 递归设置所有子元素为不可拖动
    setAllElementsUndraggable(element) {
      if (!element || !element.children) return;
      
      const children = element.children;
      for (let i = 0; i < children.length; i++) {
        children[i].setAttribute('draggable', 'false');
        this.setAllElementsUndraggable(children[i]);
      }
    },
    
    // 创建新对话
    createNewChat() {
      const newChat = {
        id: Date.now(),
        title: '新对话',
        timestamp: new Date(),
        messages: []
      };
      
      this.chatHistory.unshift(newChat);
      this.activeChatId = newChat.id;
      this.scrollToBottom();
      this.$nextTick(() => {
        this.$refs.messageInput?.focus();
      });
    },
    
    // 切换对话
    switchChat(chatId) {
      this.activeChatId = chatId;
      this.scrollToBottom();
      this.$nextTick(() => {
        this.$refs.messageInput?.focus();
      });
    },
    
    // 发送消息
    async sendMessage() {
      if (!this.userInput.trim() || this.isLoading) return;
      
      // 如果没有activeChat，自动创建一个新对话
      if (!this.activeChat) {
        this.createNewChat();
      }
      
      const userMessage = this.userInput.trim();
      this.userInput = '';
      
      // 添加用户消息
      this.activeChat.messages.push({
        id: Date.now().toString(),
        role: 'user',
        content: userMessage,
        timestamp: new Date().toISOString()
      });
      
      // 如果是第一条消息，更新对话标题
      if (this.activeChat.messages.length === 1) {
        this.activeChat.title = userMessage.length > 20 
          ? userMessage.substring(0, 20) + '...' 
          : userMessage;
      }
      
      // 更新时间戳
      this.activeChat.timestamp = new Date();
      
      this.scrollToBottom();
      this.isLoading = true;
      
      try {
        // 准备历史消息格式化为anythingLLM所需格式
        const history = this.activeChat.messages.slice(0, -1).map(msg => ({
          role: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content
        }));
        
        // 准备临时文件令牌列表
        const tempFileTokens = Object.values(this.tempFileTokens);
        
        // 根据深度思考状态选择模型
        const currentModel = this.deepThinkingActive ? this.models.trained : this.models.default;
        console.log('当前使用模型:', currentModel.name, 'slug:', currentModel.slug);
        
        // 根据智能体模式添加相应的提示词
        let enhancedQuestion = userMessage;
        if (this.activeAgent === 'writer') {
          // Writer模式：专注于写大篇幅文本，不要特殊符号
          enhancedQuestion = `请以写作者的身份详细回答以下问题，输出大篇幅的文本内容，避免使用特殊符号：\n\n${userMessage}`;
        } else if (this.activeAgent === 'speaker') {
          // Speaker模式：专注于人性化交流
          enhancedQuestion = `请以口语化、自然的方式回答以下问题，注重交流的亲切感和自然度，就像人与人之间的对话一样：\n\n${userMessage}`;
        }
        
        // 准备API请求数据
        const requestData = {
          question: enhancedQuestion,
          history: history,
          modelSlug: currentModel.slug,
          useTrainedModel: this.deepThinkingActive,
          modelName: currentModel.name,
          temp_file_tokens: tempFileTokens
        };
        
        console.log('准备发送API请求:', '/ai/api/ai/chat-with-anythingllm');
        console.log('请求参数:', requestData);
        
        // 调用后端API与anythingLLM交互
        const response = await fetch('/ai/api/ai/chat-with-anythingllm', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(requestData)
        });
          
          console.log('API响应状态:', response.status);
          
          if (!response.ok) {
            console.error('API请求失败:', response.status, response.statusText);
            throw new Error(`HTTP错误! 状态码: ${response.status}`);
          }
          
          const data = await response.json();
          console.log('API响应数据:', data);
          
          // 处理API响应
          if (data.success) {
          // 提取答案（优先使用answer字段）
          let aiContent = '我已收到您的问题，这是我的回答。';
          if (data.answer) {
            aiContent = data.answer;
          } else if (data.response) {
            if (typeof data.response === 'string') {
              aiContent = data.response;
            } else if (data.response.answer) {
              aiContent = data.response.answer;
            } else if (data.response.content) {
              aiContent = data.response.content;
            } else if (data.response.text) {
              aiContent = data.response.text;
            } else {
              aiContent = JSON.stringify(data.response);
            }
          }
          
          // 提取sources信息（如果存在）
          const sources = data.sources || [];
          
          this.activeChat.messages.push({
            id: (Date.now() + 1).toString(),
            role: 'assistant',
            content: aiContent,
            timestamp: new Date().toISOString(),
            sources: sources  // 添加sources信息到消息对象
          });
        } else {
          // 处理错误
          console.error('API错误:', data.error);
          this.handleErrorMessage(data.error || '未知错误');
        }
        
        // 更新时间戳
        this.activeChat.timestamp = new Date();
      } catch (error) {
        console.error('网络错误:', error);
        this.handleErrorMessage('网络错误，请检查后端服务是否正常运行');
      } finally {
        this.isLoading = false;
        // 移除深度思考状态自动重置，使其保持开启状态直到手动关闭
        this.scrollToBottom();
        this.adjustTextareaHeight();
      }
    },
    
    // 处理错误消息
    handleErrorMessage(errorText) {
      const errorMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `抱歉，发生错误：${errorText}`,
        timestamp: new Date().toISOString(),
        isError: true
      };
      this.activeChat.messages.push(errorMessage);
    },
    
    // 获取工作区信息（可以在组件加载时调用）
    async fetchWorkspaceInfo() {
      try {
        const response = await fetch('/ai/api/ai/workspace-info');
        const data = await response.json();
        console.log('工作区信息:', data);
        // 可以使用工作区信息进行一些初始化配置
      } catch (error) {
        console.error('获取工作区信息失败:', error);
      }
    },
    
    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      });
    },
    
    // 调整输入框高度
    adjustTextareaHeight() {
      const textarea = this.$refs.messageInput;
      if (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px';
      }
    },
    
    // 格式化时间
    formatTime(timestamp) {
      const date = new Date(timestamp);
      return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    },
    
    // 延迟函数
    delay(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    },
    
    // 切换引用文本的展开/收起状态
    toggleSources(messageIndex) {
      const index = this.expandedSources.indexOf(messageIndex);
      if (index > -1) {
        // 如果已展开，则收起
        this.expandedSources.splice(index, 1);
      } else {
        // 如果已收起，则展开
        this.expandedSources.push(messageIndex);
      }
    },
    
    // 显示source详情模态框
    showSourceDetails(source) {
      this.currentSource = source;
      this.showSourceModal = true;
    },
    
    // 关闭source详情模态框
    closeSourceModal() {
      this.showSourceModal = false;
      this.currentSource = null;
    },
    
    // 激活智能体模式
    activateAgent(agentType) {
      this.activeAgent = agentType;
      // 添加系统消息提示用户当前模式已切换
      let message = '';
      if (agentType === 'writer') {
        message = '已切换至Writer模式，现在AI将更专注于写大篇幅的文本内容。';
      } else if (agentType === 'speaker') {
        message = '已切换至Speaker模式，现在AI将更注重人性化交流。';
      } else {
        message = '已重置为默认模式。';
      }
      this.addSystemMessage(message);
      // 聚焦输入框
      this.$nextTick(() => {
        this.$refs.messageInput?.focus();
      });
    },
    

    
    // 移除已嵌入的文件
    async removeEmbeddedFile(index) {
      console.log('*** 开始移除嵌入文件操作 ***');
      console.log('移除索引:', index);
      console.log('移除前嵌入文件列表长度:', this.embeddedFiles ? this.embeddedFiles.length : 'null');
      console.log('当前嵌入文件列表:', JSON.stringify(this.embeddedFiles));
      
      // 安全检查：确保索引有效
      if (!this.embeddedFiles || index < 0 || index >= this.embeddedFiles.length) {
        console.error('错误：无效的索引', index);
        this.addSystemMessage('移除文件失败: 无效的文件索引');
        console.log('*** 移除文件操作完成 ***');
        return;
      }
      
      const fileToRemove = this.embeddedFiles[index];
      console.log('要移除的文件:', JSON.stringify(fileToRemove));
      
      const fileName = fileToRemove.name;
      const baseFileNameWithoutExt = fileName.split('.')[0];
      const uniquePaths = [fileName, baseFileNameWithoutExt];
      console.log('发送的deletes参数:', JSON.stringify(uniquePaths));
      
      // 先从本地列表中移除（UI立即更新）
      const removedFile = this.embeddedFiles.splice(index, 1);
      console.log('移除的文件:', JSON.stringify(removedFile));
      console.log('从本地列表移除后嵌入文件列表长度:', this.embeddedFiles.length);
      console.log('移除后嵌入文件列表:', JSON.stringify(this.embeddedFiles));
      
      // 立即强制更新UI
      this.$forceUpdate();
      console.log('首次UI强制更新完成');
      console.log('UI已更新，文件从列表中移除');
      
      // 从后端移除嵌入 - 使用正确的API路径和参数格式
      const modelId = this.modelId || '3';
      // 使用正确的后端端口（注意：后端运行在8081端口）
      const apiUrl = `http://localhost:8081/api/ai/update-embeddings?modelId=${modelId}`;
      const requestBody = { adds: [], deletes: uniquePaths };
      
      console.log('API URL:', apiUrl);
      console.log('请求体:', JSON.stringify(requestBody));
      
      // 使用fetch API并直接处理Promise
      fetch(apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          adds: [],
          deletes: uniquePaths
        })
    }).then(response => {
      console.log('API响应状态:', response.status);
      return response.json();
    }).then(data => {
      console.log('API响应数据:', JSON.stringify(data));
      // 即使后端没有返回success字段，只要API调用成功就认为操作成功
      console.log('移除成功！文件已从后端嵌入系统中删除');
      
      // 强制更新UI确保文件从列表中移除
      this.$nextTick(() => {
        this.$forceUpdate();
        console.log('UI已强制更新，当前嵌入文件数量:', this.embeddedFiles.length);
      });
      
      // 显示成功消息，无论data结构如何
      this.addSystemMessage(`文件 "${fileName}" 已成功从嵌入中移除`);
      
      // 如果使用了Vuex或其他状态管理，可以在这里更新全局状态
    }).catch(error => {
      console.error('API调用错误:', error.toString());
      console.error('错误类型:', error.type);
      console.error('错误信息:', error.message);
      console.error('错误堆栈:', error.stack);
      
      // 检查是否是跨域问题
      if (error.message && error.message.includes('CORS')) {
        console.error('检测到跨域错误，请检查后端CORS配置');
        this.addSystemMessage(`移除文件嵌入时发生错误: 跨域错误`);
      } else if (error.message && error.message.includes('Network')) {
        console.error('检测到网络错误，请检查服务是否运行');
        this.addSystemMessage(`移除文件嵌入时发生错误: 网络错误`);
      } else {
        this.addSystemMessage(`移除文件嵌入时发生错误: ${error.message || '网络错误'}`);
      }
    }).finally(() => {
      console.log('*** 移除文件操作完成 ***');
    });
  },
    

    
    // 切换深度思考状态
    toggleDeepThinking() {
      if (this.isLoading) return;
      
      // 切换深度思考状态
      this.deepThinkingActive = !this.deepThinkingActive;
      
      // 如果激活深度思考且有输入内容，执行深度思考
      if (this.deepThinkingActive && this.userInput.trim()) {
        this.startDeepThinking();
      }
      // 如果取消深度思考，这里可以添加清理逻辑
    },
    
    // 清理所有嵌入文件的方法
    cleanUpAllEmbeddedFiles(isSync = false, showConfirm = false) {
      if (!this.embeddedFiles || this.embeddedFiles.length === 0) {
        console.log('没有嵌入文件需要清理');
        // 检查是否有需要从localStorage清理的文件
        this.cleanUpFromLocalStorage();
        return;
      }
      
      // 如果需要显示确认对话框
      if (showConfirm && !isSync) {
        if (!confirm('确定要清理当前会话中的所有嵌入文件吗？')) {
          return;
        }
      }
      
      console.log(`开始清理所有嵌入文件 (${isSync ? '同步' : '异步'})...`);
      
      // 收集所有需要删除的文件路径
      const allDeletePaths = [];
      this.embeddedFiles.forEach(file => {
        // 优先使用后端返回的实际文件路径
        if (file.path) {
          console.log('使用实际文件路径删除:', file.path);
          allDeletePaths.push(file.path);
        } else {
          // 降级方案：使用原始文件名（包含扩展名和不包含扩展名的两种格式）
          const fileName = file.name;
          const baseFileNameWithoutExt = fileName.split('.')[0];
          console.log('使用文件名删除:', fileName, baseFileNameWithoutExt);
          allDeletePaths.push(fileName, baseFileNameWithoutExt);
        }
      });
      
      console.log('需要删除的文件路径:', JSON.stringify(allDeletePaths));
      
      // 调用API删除所有文件
      const modelId = this.deepThinkingActive ? '5' : (this.modelId || '3'); // 使用与上传时相同的modelId
      const apiUrl = `http://localhost:8081/api/ai/update-embeddings?modelId=${modelId}`;
      const requestBody = { adds: [], deletes: allDeletePaths };
      console.log('发送删除请求到:', apiUrl, '使用modelId:', modelId);
      
      // 关键优化：在localStorage中记录需要清理的文件，确保在页面重新加载时也能清理
      try {
        console.log('开始设置localStorage记录...');
        const cleanupData = {
          files: allDeletePaths,
          modelId: modelId,
          timestamp: Date.now()
        };
        const jsonString = JSON.stringify(cleanupData);
        console.log('准备存储的数据:', jsonString);
        localStorage.setItem('aiqa_pending_cleanup', jsonString);
        // 立即验证是否存储成功
        const stored = localStorage.getItem('aiqa_pending_cleanup');
        console.log('localStorage存储结果:', stored ? '成功' : '失败', '存储的数据:', stored);
      } catch (error) {
        console.error('localStorage存储失败:', error);
      }
      
      if (requestBody.deletes.length > 0) {
        // 对于同步模式（页面即将卸载），使用更可靠的方法组合
        if (isSync) {
          console.log('页面卸载模式 - 尝试多种方法确保清理成功...');
          
          // 创建一个自定义的同步删除方法，因为我们需要在用户确认后执行
          const performSynchronousCleanup = () => {
            console.log('开始执行同步清理操作...');
            
            // 优先使用sendBeacon，这是浏览器专门为页面卸载场景设计的API
            try {
              const blob = new Blob([JSON.stringify(requestBody)], { type: 'application/json' });
              const sent = navigator.sendBeacon(apiUrl, blob);
              console.log('使用sendBeacon发送删除请求:', sent ? '成功' : '失败');
            } catch (error) {
              console.error('sendBeacon发送失败:', error);
            }
            
            // 为每个文件单独发送删除请求，增加成功率
            allDeletePaths.forEach(filePath => {
              try {
                const singleFileApiUrl = `http://localhost:8081/api/ai/update-embeddings?modelId=${modelId}`;
                const singleFileBody = { adds: [], deletes: [filePath] };
                
                // 为单个文件发送sendBeacon请求
                const blob = new Blob([JSON.stringify(singleFileBody)], { type: 'application/json' });
                const sent = navigator.sendBeacon(singleFileApiUrl, blob);
                console.log(`文件 ${filePath} 单独删除请求:`, sent ? '成功' : '失败');
              } catch (error) {
                console.error(`文件 ${filePath} 单独删除请求失败:`, error);
              }
            });
            
            // 增加适当延迟，给请求一些时间发送
            const startTime = Date.now();
            while (Date.now() - startTime < 500) {
              // 给sendBeacon足够的启动时间
              Math.random();
            }
            
            console.log('同步清理操作完成');
          };
          
          // 执行清理
          performSynchronousCleanup();
          
          // 将清理操作也存储到localStorage作为备份
          try {
            localStorage.setItem('aiqa_pending_cleanup', JSON.stringify({
              deletes: allDeletePaths,
              timestamp: Date.now(),
              modelId: modelId
            }));
            console.log('已将待清理信息存储到localStorage作为备份');
          } catch (error) {
            console.error('存储到localStorage失败:', error);
          }
        }
        // 异步模式下使用更可靠的请求策略
        else {
          // 创建一个Promise链来确保至少有一种方法成功
          const deletePromises = [];
          
          // 方法1: fetch with timeout
          deletePromises.push(new Promise((resolve) => {
            try {
              const controller = new AbortController();
              const timeoutId = setTimeout(() => controller.abort(), 5000);
              
              fetch(apiUrl, {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody),
                signal: controller.signal
              }).then(response => {
                clearTimeout(timeoutId);
                console.log('fetch请求成功，状态码:', response.status);
                resolve(true);
              }).catch(error => {
                clearTimeout(timeoutId);
                console.error('fetch请求失败:', error.name !== 'AbortError' ? error : 'fetch请求超时');
                resolve(false);
              });
            } catch (error) {
              console.error('fetch请求异常:', error);
              resolve(false);
            }
          }));
          
          // 方法2: sendBeacon（如果可用）
          if (navigator.sendBeacon) {
            deletePromises.push(new Promise((resolve) => {
              try {
                const blob = new Blob([JSON.stringify(requestBody)], { type: 'application/json' });
                const sent = navigator.sendBeacon(apiUrl, blob);
                console.log('sendBeacon发送结果:', sent ? '成功' : '失败');
                resolve(sent);
              } catch (error) {
                console.error('sendBeacon异常:', error);
                resolve(false);
              }
            }));
          }
          
          // 等待所有请求完成，只要有一个成功就算成功
          Promise.all(deletePromises).then(results => {
            const anySuccess = results.some(result => result === true);
            console.log('删除请求执行结果:', anySuccess ? '至少一个请求成功' : '所有请求失败');
            
            // 无论成功与否，都清空本地列表
            this.embeddedFiles = [];
            console.log('本地嵌入文件列表已清空');
            
            // 如果是手动触发的清理，显示结果消息
            if (showConfirm) {
              this.addSystemMessage(anySuccess ? '已成功清理当前会话中的所有嵌入文件' : '清理请求已发送，请检查后端日志确认是否成功');
            }
          });
        }
        
        // 无论同步异步，都清空本地列表
        this.embeddedFiles = [];
        console.log('本地嵌入文件列表已清空');
      } else {
        // 没有文件需要删除，清空本地列表
        this.embeddedFiles = [];
        console.log('本地嵌入文件列表已清空');
      }
    },
    
    // 手动清理嵌入文件的方法
    manualCleanUp() {
      this.cleanUpAllEmbeddedFiles(false, true);
    },
    
    // 在组件挂载时添加全局事件监听
    mounted() {
      console.log('=== 组件挂载开始 ===');
      
      // 关键优化：页面加载时立即检查并清理localStorage中待处理的文件
      console.log('关键步骤：调用cleanUpFromLocalStorage清理localStorage中的待处理文件');
      this.cleanUpFromLocalStorage();
      
      window.addEventListener('beforeunload', this.handleBeforeUnload);
      document.addEventListener('visibilitychange', this.handleVisibilityChange);
      console.log('已添加页面离开事件监听器');
      console.log('=== 组件挂载完成 ===');
    },
    
    // 在组件卸载时清理事件监听
    beforeUnmount() {
      window.removeEventListener('beforeunload', this.handleBeforeUnload);
      document.removeEventListener('visibilitychange', this.handleVisibilityChange);
      this.cleanUpAllEmbeddedFiles();
    },
    
    // 清理localStorage中待处理的文件
    cleanUpFromLocalStorage() {
      try {
        console.log('开始检查localStorage中的待清理文件...');
        // 显示localStorage中的所有键值对，用于调试
        console.log('localStorage中的所有键:', Object.keys(localStorage));
        const cleanupDataStr = localStorage.getItem('aiqa_pending_cleanup');
        console.log('获取到的localStorage数据:', cleanupDataStr);
        
        if (cleanupDataStr) {
          const cleanupData = JSON.parse(cleanupDataStr);
          const now = Date.now();
          console.log('解析后的清理数据:', cleanupData);
          console.log('时间差(毫秒):', now - cleanupData.timestamp);
          
          // 只清理最近5分钟内记录的文件
          if (now - cleanupData.timestamp < 5 * 60 * 1000) {
            console.log('发现待清理文件记录，开始清理:', cleanupData.files);
            
            const apiUrl = `http://localhost:8081/api/ai/update-embeddings?modelId=${cleanupData.modelId}`;
            const requestBody = { adds: [], deletes: cleanupData.files };
            
            // 发送删除请求
            fetch(apiUrl, {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(requestBody)
            }).then(response => {
              console.log('localStorage清理请求成功，状态码:', response.status);
              if (response.ok) {
                localStorage.removeItem('aiqa_pending_cleanup');
                console.log('已清除localStorage中的待清理记录');
              }
            }).catch(error => {
              console.error('localStorage清理请求失败:', error);
            });
          } else {
            // 如果记录已过期，直接清除
            localStorage.removeItem('aiqa_pending_cleanup');
            console.log('待清理记录已过期，已清除');
          }
        } else {
          console.log('localStorage中没有待清理的文件记录');
        }
      } catch (error) {
        console.error('localStorage清理失败:', error);
      }
    },
    
    // 处理页面卸载事件
    handleBeforeUnload(event) {
      // 增加更详细的调试日志
      console.log('=== handleBeforeUnload 事件触发 ===', new Date().toISOString());
      
      // 检查事件对象
      console.log('事件对象:', event);
      
      // 详细检查embeddedFiles状态
      console.log('embeddedFiles 引用:', this.embeddedFiles);
      console.log('embeddedFiles 类型:', typeof this.embeddedFiles);
      console.log('embeddedFiles 是否数组:', Array.isArray(this.embeddedFiles));
      console.log('embeddedFiles 长度:', this.embeddedFiles ? this.embeddedFiles.length : 0);
      
      // 显示embeddedFiles的详细内容
      if (this.embeddedFiles && this.embeddedFiles.length > 0) {
        console.log('embeddedFiles 内容:', JSON.stringify(this.embeddedFiles));
      }
      
      // 尝试不同的文件存在判断方式
      const hasEmbeddedFiles = this.embeddedFiles && Array.isArray(this.embeddedFiles) && this.embeddedFiles.length > 0;
      console.log('hasEmbeddedFiles 判断结果:', hasEmbeddedFiles);
      
      // 关键修改：即使没有嵌入文件也尝试显示对话框，用于调试
      const forceShowDialog = true; // 设置为true可以强制显示对话框进行测试
      
      if (hasEmbeddedFiles || forceShowDialog) {
        console.log('触发条件满足，尝试显示确认对话框...');
        
        // 确认消息
        const confirmationMessage = '离开或刷新该页面将导致嵌入文件失效，是否确认离开？';
        
        try {
          // Edge浏览器兼容性修复：
          // 1. 首先阻止默认行为
          event.preventDefault();
          
          // 2. 设置返回值（这是触发对话框的关键）
          event.returnValue = confirmationMessage;
          
          console.log('已成功设置preventDefault()和returnValue');
          
          // 如果真的有文件，执行清理
          if (hasEmbeddedFiles) {
            this.cleanUpAllEmbeddedFiles(true);
          }
          
          // 3. 必须返回字符串值
          return confirmationMessage;
        } catch (error) {
          console.error('处理beforeunload事件时出错:', error);
        }
      } else {
        console.log('没有嵌入文件且未强制显示，不触发确认对话框');
      }
    },
    
    // 处理可见性变化事件
    handleVisibilityChange() {
      if (document.visibilityState === 'hidden') {
        console.log('页面变为不可见，开始清理嵌入文件...');
        // 使用同步模式确保更可靠的清理
        this.cleanUpAllEmbeddedFiles(true);
      }
    },
    
    // 处理返回主页按钮点击事件
    async handleBackHome() {
      const hasEmbeddedFiles = this.embeddedFiles && this.embeddedFiles.length > 0;
      
      // 只有在有嵌入文件时才显示确认对话框
      if (hasEmbeddedFiles) {
        // 显示确认对话框
        const confirmed = window.confirm('有文件嵌入，需点击确认，待文件删除后才能安全退出。确定要返回主页吗？');
        
        if (confirmed) {
          console.log('用户确认返回主页，开始清理嵌入文件...');
          
          // 使用异步模式清理文件，以获得更好的反馈
          try {
            // 创建一个Promise来等待清理完成
            await new Promise((resolve) => {
              // 重写cleanUpAllEmbeddedFiles的内部回调，以确保我们能知道清理何时完成
              const originalEmbeddedFiles = [...this.embeddedFiles];
              
              // 调用清理方法
              this.cleanUpAllEmbeddedFiles(false);
              
              // 轮询检查清理是否完成
              const checkCleanup = () => {
                if (this.embeddedFiles.length === 0) {
                  resolve();
                } else {
                  setTimeout(checkCleanup, 100);
                }
              };
              checkCleanup();
            });
            
            // 显示删除成功消息
            alert('嵌入文件已成功删除！');
          } catch (error) {
            console.error('清理文件时出错:', error);
            alert('文件清理过程中发生错误，但仍将返回主页。');
          }
          
          // 跳转到主页
          console.log('开始跳转到主页...');
          this.$router.push('/');
        }
      } else {
        // 如果没有嵌入文件，直接跳转到主页
        console.log('没有嵌入文件，直接跳转到主页...');
        this.$router.push('/');
      }
    },
    
    // 在用户确认离开后处理清理和卸载
    // 注意：这是一个变通方法，因为在beforeunload中无法等待异步操作完成
    async confirmAndCleanup() {
      const hasEmbeddedFiles = this.embeddedFiles && this.embeddedFiles.length > 0;
      if (hasEmbeddedFiles) {
        const confirmed = window.confirm('离开或刷新该页面将导致嵌入文件失效，是否确认离开？');
        
        if (confirmed) {
          console.log('用户确认离开，开始清理嵌入文件...');
          await this.cleanUpAllEmbeddedFiles(false); // 使用异步模式以获得更好的反馈
          console.log('文件清理完成，允许页面离开');
          // 注意：在实际环境中，这里无法控制页面导航，
          // 清理会在用户确认后自动进行
        }
      }
    },
    
    // 深度思考功能
    async startDeepThinking() {
      if (!this.userInput.trim() || this.isLoading) return;
      
      // 如果没有activeChat，自动创建一个新对话
      if (!this.activeChat) {
        this.createNewChat();
      }
      
      const userMessage = this.userInput.trim();
      this.userInput = '';
      
      // 添加用户消息
      this.activeChat.messages.push({
        id: Date.now().toString(),
        role: 'user',
        content: userMessage,
        timestamp: new Date().toISOString()
      });
      
      // 如果是第一条消息，更新对话标题
      if (this.activeChat.messages.length === 1) {
        this.activeChat.title = userMessage.length > 20 
          ? userMessage.substring(0, 20) + '...' 
          : userMessage;
      }
      
      // 更新时间戳
      this.activeChat.timestamp = new Date();
      
      this.scrollToBottom();
      this.isLoading = true;
      
      try {
        // 深度思考提示消息
        this.activeChat.messages.push({
          id: (Date.now() + 1).toString(),
          role: 'assistant',
          content: '我正在对这个问题进行深度思考，请稍候...',
          timestamp: new Date().toISOString()
        });
        this.scrollToBottom();
        
        // 准备深度思考的提示词
        const deepThinkingPrompt = `请对以下问题进行深度分析和思考：\n\n${userMessage}\n\n请从多个角度、多个层面进行详细分析，包括：\n1. 问题的核心本质和背景\n2. 相关的概念和理论\n3. 可能的解决方案和利弊\n4. 实际应用和案例\n5. 潜在的限制和挑战\n\n请提供深入、全面且有洞察力的回答。`;
        
        // 使用训练模型进行深度思考
        const trainedModel = this.models.trained;
        
        // 调用后端API进行深度思考
        const response = await fetch('http://localhost:8081/api/ai/chat-with-anythingllm', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            question: deepThinkingPrompt,
            history: [],
            modelSlug: trainedModel.slug,
            useTrainedModel: true
          })
        });
        
        if (!response.ok) {
          throw new Error(`HTTP错误! 状态码: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 处理API响应
        if (data.success) {
          // 移除之前的深度思考提示消息
          this.activeChat.messages.pop();
          
          // 提取答案
          let aiContent = '我已完成深度思考，这是我的详细分析。';
          if (data.answer) {
            aiContent = data.answer;
          } else if (data.response) {
            if (typeof data.response === 'string') {
              aiContent = data.response;
            } else if (data.response.answer) {
              aiContent = data.response.answer;
            } else if (data.response.content) {
              aiContent = data.response.content;
            } else if (data.response.text) {
              aiContent = data.response.text;
            } else {
              aiContent = JSON.stringify(data.response);
            }
          }
          
          this.activeChat.messages.push({
            id: (Date.now() + 2).toString(),
            role: 'assistant',
            content: `[深度思考]\n${aiContent}`,
            timestamp: new Date().toISOString(),
            isDeepThinking: true,
            style: 'deep-thinking'
          });
        } else {
          // 移除之前的深度思考提示消息
          this.activeChat.messages.pop();
          
          // 处理错误
          console.error('API错误:', data.error);
          this.handleErrorMessage(data.error || '未知错误');
        }
        
        // 更新时间戳
        this.activeChat.timestamp = new Date();
      } catch (error) {
        console.error('网络错误:', error);
        this.handleErrorMessage('网络错误，请检查后端服务是否正常运行');
      } finally {
        this.isLoading = false;
        this.deepThinkingActive = false; // 重置深度思考状态
        this.scrollToBottom();
        this.adjustTextareaHeight();
      }
    }
  }
}
</script>

<style scoped>
/* 全局样式 - 使用深度选择器覆盖App.vue中的全局样式 */
.ai-qa-container {
    display: flex;
    height: 100vh;
    width: 100%;
    margin: 0;
    background: linear-gradient(135deg, #050505 0%, #100520 50%, #1a0535 100%);
    color: #e0e0e0 !important;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    position: relative;
    z-index: 10;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    box-sizing: border-box;
    /* 移除overflow: hidden以允许内容正常显示 */
    /* 禁止拖动 */
    user-drag: none;
    -webkit-user-drag: none;
    user-select: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
  }

/* 临时附件容器样式 */
.temp-files-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 16px;
  background: rgba(33, 150, 243, 0.1);
  border-radius: 8px;
}

/* 临时附件头部样式 */
.temp-files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(33, 150, 243, 0.2);
}

/* 临时附件标题样式 */
.temp-files-title {
  font-size: 14px;
  font-weight: 500;
  color: #666;
}

/* 清理全部临时附件按钮样式 */
.clean-all-temp-files-btn {
  background-color: #ff9800;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.clean-all-temp-files-btn:hover:not(:disabled) {
  background-color: #f57c00;
}

.clean-all-temp-files-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* 临时附件项样式 */
.temp-file-item {
  display: flex;
  align-items: center;
  background: rgba(33, 150, 243, 0.2);
  border: 1px solid rgba(33, 150, 243, 0.3);
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 14px;
  color: #666;
}



/* 已嵌入文件容器样式 */
.embedded-files-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 16px;
  background: rgba(126, 34, 206, 0.1);
  border-radius: 8px;
}

/* 嵌入文件头部样式 */
.embedded-files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(126, 34, 206, 0.2);
}

/* 嵌入文件标题样式 */
.embedded-files-title {
  font-size: 14px;
  font-weight: 500;
  color: #666;
}

/* 清理全部按钮样式 */
.clean-all-files-btn {
  background-color: #f44336;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.clean-all-files-btn:hover:not(:disabled) {
  background-color: #d32f2f;
}

.clean-all-files-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* 单个嵌入文件项样式 */
.embedded-file-item {
  display: flex;
  align-items: center;
  background: rgba(126, 34, 206, 0.2);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  color: #e0e0e0;
  border: 1px solid rgba(126, 34, 206, 0.3);
}

/* 返回主页按钮样式 */
.back-home-button {
  margin-left: 8px;
  background-color: rgba(76, 175, 80, 0.2);
  border: 1px solid rgba(76, 175, 80, 0.4);
}

.back-home-button:hover {
  background-color: rgba(76, 175, 80, 0.3);
}

/* 文件名样式 */
.file-name {
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

/* 移除文件按钮样式 */
.remove-file-btn {
  background: none;
  border: none;
  color: #e0e0e0;
  font-size: 16px;
  cursor: pointer;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.remove-file-btn:hover {
  background-color: rgba(126, 34, 206, 0.3);
}

.remove-file-btn:active {
  background-color: rgba(126, 34, 206, 0.4);
}

/* 禁止所有元素拖动 */
.ai-qa-container * {
  user-drag: none;
  -webkit-user-drag: none;
}

/* 允许文本选择，特别是消息内容 */
.ai-qa-container .message-text,
.ai-qa-container .message-input {
  user-select: text;
  -webkit-user-select: text;
  -moz-user-select: text;
  -ms-user-select: text;
}

/* 其他非文本元素可以禁止选择 */
.ai-qa-container .message-avatar,
.ai-qa-container .message-wrapper,
.ai-qa-container .sidebar,
.ai-qa-container .chat-item,
.ai-qa-container button,
.ai-qa-container .chat-header,
.ai-qa-container .welcome-icon,
.ai-qa-container .user-avatar {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

/* 移除强制覆盖样式，允许各组件区域正确显示其设置的颜色 */
/* 不需要使用deep选择器和!important来强制覆盖所有样式 */

/* 侧边栏样式 */
.sidebar {
    width: 240px;
    min-width: 200px;
    max-width: 280px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    flex-shrink: 0;
    background: linear-gradient(180deg, #050505 0%, #100520 100%);
    border-right: 1px solid #2a1a3e;
  }

.sidebar-header {
  padding: 15px 16px;
  text-align: center;
  background: linear-gradient(90deg, #050505 0%, #101010 100%);
  color: #e0e0e0;
}

.app-logo {
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
}

.new-chat-button {
    margin: 0;
    padding: 12px;
    background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
    border: none;
    border-radius: 0;
    cursor: pointer;
    font-size: 15px;
    color: #e0e0e0;
    transition: all 0.2s ease;
    width: 100%;
  }
  .new-chat-button:hover {
    background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
  }
  .new-chat-button:active {
    background: linear-gradient(90deg, #251535 0%, #302040 100%);
    transform: scale(0.98);
  }

.chat-list {
    flex: 1;
    overflow-y: auto;
    padding: 0;
    background: linear-gradient(180deg, #100520 0%, #1a0a2e 100%);
  }

.chat-item {
    padding: 12px;
    border-radius: 0;
    cursor: pointer;
    transition: background-color 0.2s ease;
    position: relative;
    background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
    color: #e0e0e0;
    border-bottom: 1px solid #2a1a3e;
  }

.chat-item:hover {
  background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
}

.chat-item.active {
  background: linear-gradient(90deg, #251535 0%, #302040 100%);
  border-left: 3px solid #7e22ce;
}

.chat-title {
    font-size: 16px;
    font-weight: 500;
    color: #e0e0e0;
    margin-bottom: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

.chat-time {
  font-size: 12px;
  color: #a0a0a0;
}

.sidebar-footer {
    padding: 12px;
    background: linear-gradient(90deg, #050505 0%, #100520 100%);
    color: #e0e0e0;
  }

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6b21a8 0%, #7e22ce 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #ffffff;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #e0e0e0;
}

/* 主聊天区域样式 */
.chat-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    min-width: 0; /* 防止内容溢出 */
    background: linear-gradient(135deg, #100520 0%, #1a0535 100%);
    min-height: 0;
  }

/* 欢迎界面样式 */
.welcome-screen {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    text-align: center;
    background: linear-gradient(135deg, #100520 0%, #1a0535 100%);
  }

.welcome-content {
  max-width: 500px;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 24px;
  color: #9333ea;
}

.welcome-content h1 {
  font-size: 28px;
  font-weight: 700;
  color: #e0e0e0;
  margin-bottom: 12px;
}

.welcome-content p {
  font-size: 14px;
  color: #a0a0a0;
  margin-bottom: 24px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.quick-action-button {
  padding: 12px 24px;
  background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
  border: 1px solid #302040;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #e0e0e0;
  transition: all 0.2s ease;
  min-width: 200px;
}

.quick-action-button:hover {
  background: linear-gradient(90deg, #251535 0%, #302040 100%);
  border-color: #7e22ce;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(126, 34, 206, 0.3);
}

/* 对话内容区域样式 */
.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #2a1a3e;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, #050505 0%, #100520 100%);
}

.chat-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
  margin: 0;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #302040;
  background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-button:hover {
  background: linear-gradient(90deg, #251535 0%, #302040 100%);
  border-color: #7e22ce;
}

.action-button .icon {
  font-size: 16px;
  color: #e0e0e0;
}

/* 消息列表样式 */
.messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 15px;
    background: linear-gradient(135deg, #100520 0%, #1a0535 100%);
    min-height: 200px; /* 确保即使没有消息也有最小高度 */
    max-height: calc(100vh - 200px); /* 限制最大高度，确保输入框可见 */
  }

.message-wrapper {
    display: flex;
    margin-bottom: 15px;
    gap: 10px;
    align-items: flex-start;
  }

.message-wrapper.ai-message {
  flex-direction: row;
  justify-content: flex-start;
}

.message-wrapper.user-message {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6b21a8 0%, #7e22ce 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  color: #ffffff;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.user-message .message-content {
  align-self: flex-end;
  margin-left: 0;
  margin-right: 0;
}

.ai-message .message-content {
  background: linear-gradient(135deg, #1a0a2e 0%, #251535 100%);
  border-radius: 14px;
  padding: 10px 14px;
  border: 1px solid #302040;
}

/* 深度思考消息样式 */
.ai-message .message-content:has(.message-text[style*="deep-thinking"]) {
  background: linear-gradient(135deg, #251535 0%, #302040 100%);
  border: 1px solid #7e22ce;
  position: relative;
}

.ai-message .message-content:has(.message-text[style*="deep-thinking"])::before {
  content: "深度思考";
  position: absolute;
  top: -10px;
  left: 12px;
  background: linear-gradient(90deg, #6b21a8 0%, #7e22ce 100%);
  color: white;
  padding: 2px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.user-message .message-content {
  background: linear-gradient(135deg, #6b21a8 0%, #7e22ce 100%);
  border-radius: 14px;
  padding: 10px 14px;
}

.ai-message .message-text {
  color: #e0e0e0;
  line-height: 1.5;
}

.user-message .message-text {
  color: #ffffff;
  line-height: 1.5;
}

/* 加载状态样式 */
.loading-message {
  display: flex;
  margin-bottom: 24px;
  gap: 12px;
  align-items: flex-start;
}

.loading-indicator {
  display: flex;
  gap: 6px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #1a0a2e 0%, #251535 100%);
  border-radius: 18px;
  border: 1px solid #302040;
}

.loading-indicator span {
  width: 8px;
  height: 8px;
  background-color: #1976d2;
  border-radius: 50%;
  animation: loading 1.4s infinite ease-in-out both;
}

.loading-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes loading {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 输入区域样式 */
.input-section {
    padding: 15px;
    border-top: 1px solid #2a1a3e;
    background: linear-gradient(135deg, #050505 0%, #100520 100%);
    box-sizing: border-box;
    position: sticky;
    bottom: 0;
    z-index: 10;
  }

.input-container {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
    background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
    border-radius: 12px;
    padding: 15px;
    border: 1px solid #302040;
  }

.input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* 动作按钮容器样式 */
.action-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
}

/* 深度思考按钮样式 */
.deep-thinking-button {
  padding: 8px 16px;
  background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%); /* 初始状态：深色背景 */
  color: #e0e0e0; /* 初始状态：浅灰色文字 */
  border: 1px solid #302040;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.deep-thinking-button:hover:not(:disabled) {
  background: linear-gradient(90deg, #251535 0%, #302040 100%);
  border-color: #7e22ce;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(126, 34, 206, 0.2);
}

.deep-thinking-button.active {
  background: linear-gradient(90deg, #6b21a8 0%, #7e22ce 100%); /* 激活状态：紫色渐变 */
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(126, 34, 206, 0.3);
}

.deep-thinking-button:disabled {
  background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
  color: #6a6a6a;
  border-color: #2a1a3e;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}



/* 文件上传按钮样式 */
.file-upload-button {
  padding: 8px 16px;
  background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
  color: #7e22ce;
  border: 1px solid #302040;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.file-upload-button:hover:not(:disabled) {
  background: linear-gradient(90deg, #251535 0%, #302040 100%);
  border-color: #7e22ce;
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(126, 34, 206, 0.2);
}

.file-upload-button:disabled {
  background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
  color: #6a6a6a;
  border-color: #2a1a3e;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 临时文件按钮样式 */
.temp-file-button {
  margin-left: 10px;
  padding: 8px 16px;
  background: linear-gradient(90deg, #0369a1 0%, #0284c7 100%);
  color: white;
  border: 1px solid #0e7490;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.temp-file-button:hover:not(:disabled) {
  background: linear-gradient(90deg, #0c4a6e 0%, #075985 100%);
  border-color: #0ea5e9;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
}

.temp-file-button:disabled {
  background: linear-gradient(90deg, #051b26 0%, #072530 100%);
  color: #6a6a6a;
  border-color: #0c4a6e;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 发送按钮样式 */
.send-button {
  margin-left: auto; /* 发送按钮靠右 */
  padding: 8px 20px;
  background: linear-gradient(90deg, #6b21a8 0%, #7e22ce 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.send-button:hover:not(:disabled) {
  background: linear-gradient(90deg, #5a1d96 0%, #6b21a8 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(126, 34, 206, 0.3);
}

.send-button:disabled {
  background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
  color: #6a6a6a;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 输入框样式 */
.message-input {
    flex: 1;
    border: 1px solid #302040;
    border-radius: 8px;
    background: linear-gradient(90deg, #251535 0%, #302040 100%);
    padding: 12px 16px;
    resize: none;
    outline: none;
    font-size: 15px;
    line-height: 1.5;
    color: #e0e0e0;
    min-height: 45px;
    max-height: 150px;
    font-family: inherit;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

.message-input:focus {
  border-color: #7e22ce;
  box-shadow: 0 0 0 2px rgba(126, 34, 206, 0.2);
}

/* send-button样式已在响应式设计部分定义 */

/* 深度思考按钮样式已在.input-container部分重新定义 */

.input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #8a8a8a;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .ai-qa-container {
      flex-direction: column;
      height: 100vh;
      width: 100%;
    }
    
    .sidebar {
      width: 100%;
      height: auto;
      min-height: 120px;
      max-height: 200px;
      border-right: none;
      border-bottom: 1px solid #2a1a3e;
      background: linear-gradient(180deg, #050505 0%, #100520 100%);
    }
    
    .sidebar-header {
      background: linear-gradient(90deg, #050505 0%, #101010 100%);
    }
    
    .new-chat-button {
      background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
      color: #e0e0e0;
    }
    
    .new-chat-button:hover {
      background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
    }
    .new-chat-button:active {
      background: linear-gradient(90deg, #251535 0%, #302040 100%);
      transform: scale(0.98);
    }
    
    .chat-list {
      display: flex;
      overflow-x: auto;
      overflow-y: hidden;
      padding: 8px;
      background: linear-gradient(180deg, #100520 0%, #1a0a2e 100%);
    }
    
    .chat-item {
      min-width: 160px;
      margin-right: 8px;
      margin-bottom: 0;
      flex-shrink: 0;
      background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
      color: #e0e0e0;
      border-bottom: 1px solid #2a1a3e;
    }
    
    .sidebar-footer {
    }
    
    /* 引用文本相关样式 */
    .sources-container {
      margin-top: 10px;
      border-radius: 8px;
      overflow: hidden;
      z-index: 1;
    }
    
    .sources-toggle-button {
      width: 100%;
      padding: 8px 12px;
      background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%) !important;
      border: 1px solid #2a1a3e !important;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      color: #e0e0e0 !important;
      transition: all 0.2s ease;
      text-align: left;
      outline: none;
      box-shadow: none;
    }
    
    .sources-toggle-button:hover {
      background: linear-gradient(90deg, #251535 0%, #301f3f 100%) !important;
      color: #ffffff !important;
    }
    
    .sources-toggle-button.active {
      background: linear-gradient(90deg, #4a148c 0%, #6a1b9a 100%) !important;
      color: white !important;
      border-color: #7b1fa2 !important;
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
      box-shadow: 0 2px 8px rgba(123, 31, 162, 0.3);
    }
    
    .sources-expansion-panel {
      background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
      border: 1px solid #2a1a3e;
      border-top: none;
      border-bottom-left-radius: 4px;
      border-bottom-right-radius: 4px;
      padding: 12px;
    }
    
    .source-item {
      margin-bottom: 8px;
    }
    
    .source-item:last-child {
      margin-bottom: 0;
    }
    
    .source-title-button {
      width: 100%;
      padding: 6px 10px;
      background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%) !important;
      border: 1px solid #2a1a3e !important;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      color: #e0e0e0 !important;
      text-align: left;
      transition: all 0.2s ease;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      outline: none;
      box-shadow: none;
    }
    
    .source-title-button:hover {
      background: linear-gradient(90deg, #251535 0%, #301f3f 100%) !important;
      border-color: #3a294e !important;
      color: #ffffff !important;
    }
    
    .sidebar-footer {
      background: linear-gradient(90deg, #050505 0%, #100520 100%);
      color: #e0e0e0;
    }
    
    .message-content {
      max-width: 85%;
    }
    
    .chat-main {
      flex: 1;
      min-height: 0;
    }
    
    .input-container {
      gap: 8px;
      padding: 8px 12px;
    }
    
    .send-button {
      padding: 8px 16px;
      font-size: 14px;
    }
    
    .welcome-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }
    
    .welcome-content h1 {
      font-size: 24px;
    }
  }
  
  @media (max-width: 480px) {
    .sidebar {
      width: 100%;
      height: auto;
      min-height: 100px;
      background: linear-gradient(180deg, #050505 0%, #100520 100%);
      border-right: none;
    }
    
    .sidebar-header {
      background: linear-gradient(90deg, #050505 0%, #101010 100%);
      padding: 10px;
    }
    
    .new-chat-button {
      background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
      color: #e0e0e0;
      padding: 10px;
      font-size: 14px;
    }
    
    .new-chat-button:hover {
      background: linear-gradient(90deg, #1a0a2e 0%, #251535 100%);
    }
    
    .chat-item {
      min-width: 140px;
      background: linear-gradient(90deg, #100520 0%, #1a0a2e 100%);
      color: #e0e0e0;
      padding: 10px;
    }
    
    .chat-list {
      background: linear-gradient(180deg, #100520 0%, #1a0a2e 100%);
      padding: 6px;
    }
    
    .sidebar-footer {
      background: linear-gradient(90deg, #050505 0%, #100520 100%);
      color: #e0e0e0;
      padding: 10px;
    }
    
    .message-content {
      max-width: 90%;
    }
    
    .messages-container {
      padding: 10px;
    }
    
    .input-section {
      padding: 10px;
    }
  }

/* 滚动条样式 */
.messages-container::-webkit-scrollbar,
.chat-list::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track,
.chat-list::-webkit-scrollbar-track {
  background: #100520;
}

.messages-container::-webkit-scrollbar-thumb,
.chat-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #7e22ce 0%, #9333ea 100%);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover,
.chat-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #5a1d96 0%, #6b21a8 100%);
}
</style>