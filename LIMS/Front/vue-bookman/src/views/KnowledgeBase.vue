<template>
  <div class="knowledge-base-container">
    <!-- 左侧侧边栏 - 现代化设计 -->
    <aside class="sidebar">
      <!-- 搜索框 -->
      <div class="search-box">
        <i class="search-icon">🔍</i>
        <input 
          type="text" 
          placeholder="搜索实体…"
          class="search-input"
          v-model="searchQuery"
          @keyup.enter="searchEntities"
        >
      </div>
      
      <!-- 实体类型筛选 -->
      <div class="filter-section">
        <h3>实体类型</h3>
        <div class="entity-type-buttons">
          <button 
            class="entity-type-btn"
            :class="{ active: selectedEntityTypes.Book }"
            @click="toggleEntityType('Book')"
          >
            图书
          </button>
          <button 
            class="entity-type-btn"
            :class="{ active: selectedEntityTypes.Author }"
            @click="toggleEntityType('Author')"
          >
            作者
          </button>
          <button 
            class="entity-type-btn"
            :class="{ active: selectedEntityTypes.Publisher }"
            @click="toggleEntityType('Publisher')"
          >
            出版社
          </button>
          <button 
            class="entity-type-btn"
            :class="{ active: selectedEntityTypes.Category }"
            @click="toggleEntityType('Category')"
          >
            分类
          </button>
        </div>
      </div>
      
      <!-- 关系深度 -->
      <div class="filter-section">
        <h3>关系深度</h3>
        <div class="depth-buttons">
          <button 
            class="depth-btn" 
            :class="{ active: selectedDepth === 1 }"
            @click="selectedDepth = 1; updateGraph()"
          >1</button>
          <button 
            class="depth-btn" 
            :class="{ active: selectedDepth === 2 }"
            @click="selectedDepth = 2; updateGraph()"
          >2</button>
          <button 
            class="depth-btn" 
            :class="{ active: selectedDepth === 3 }"
            @click="selectedDepth = 3; updateGraph()"
          >3</button>
        </div>
      </div>
      
      <!-- 数据库连接状态 -->
      <div class="connection-status" :class="{ connected: isConnected }">
        <span>数据库: {{ isConnected ? '已连接' : '未连接' }}</span>
      </div>
      
      <!-- 文件上传功能 -->
      <div class="upload-section">
        <h3>更新知识图谱</h3>
        
        <div class="file-type-selector">
          <select v-model="selectedFileType" class="file-type-select">
            <option value="">选择文件类型</option>
            <option value="books">图书数据</option>
            <option value="authors">作者数据</option>
            <option value="publishers">出版社数据</option>
            <option value="categories">分类数据</option>
            <option value="relations">关系数据</option>
          </select>
        </div>
        
        <div class="file-upload-area">
          <input 
            type="file" 
            ref="fileInput" 
            accept=".xlsx" 
            @change="handleFileChange"
            class="file-input"
          >
          <button 
            class="upload-btn"
            @click="triggerFileUpload"
            :disabled="!selectedFileType || isUploading"
          >
            {{ selectedFileName || '选择Excel文件' }}
          </button>
        </div>
        
        <div v-if="selectedFileName" class="file-info">
          <span>{{ selectedFileName }}</span>
          <button class="clear-btn" @click="clearFile">×</button>
        </div>
        
        <button 
          class="submit-upload-btn"
          @click="uploadFile"
          :disabled="!selectedFile || isUploading"
        >
          {{ isUploading ? '上传中...' : '上传更新' }}
        </button>
        
        <button 
          class="reset-graph-btn"
          @click="confirmResetGraph"
          :disabled="isUploading"
        >
          重置图谱
        </button>
        
        <div v-if="uploadMessage" :class="['upload-message', uploadSuccess ? 'success' : 'error']">
          {{ uploadMessage }}
        </div>
      </div>
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部标题与统计 -->
      <div class="header-section">
        <h1>图书知识图谱</h1>
        <div class="stats-container">
          <div class="stat-card">
            <span class="stat-number">{{ totalNodes }}</span>
            <span class="stat-label">实体</span>
          </div>
          <div class="stat-card">
            <span class="stat-number">{{ totalEdges }}</span>
            <span class="stat-label">关系</span>
          </div>
          <div v-if="loading" class="loading-indicator">
            <div class="spinner"></div>
            <span>加载中...</span>
          </div>
        </div>
      </div>
      
      <!-- 图谱可视化区域 -->
      <div class="graph-container">
        <!-- 图谱控制按钮 -->
        <div class="graph-controls">
          <button class="control-btn" @click="resetZoom" title="重置视图">
            <i class="control-icon">🔄</i>
            重置
          </button>
          <button class="control-btn" @click="updateGraph" title="刷新图谱">
            <i class="control-icon">🔃</i>
            刷新
          </button>
          <button class="control-btn" @click="exportGraph" title="导出图谱">
            <i class="control-icon">📥</i>
            导出
          </button>
        </div>
        
        <!-- 图谱可视化区域 -->
        <div 
          class="graph-visualization" 
          ref="networkContainer"
        ></div>
      </div>
      
      <!-- 详情面板 -->
      <div class="detail-panel" v-if="selectedNode">
        <div class="detail-header">
          <h2>{{ selectedNode.label }}</h2>
          <span class="node-type">{{ selectedNode.type }}</span>
          <button class="close-btn" @click="selectedNode = null">×</button>
        </div>
        
        <div class="node-properties">
          <div v-if="selectedNode.properties" class="properties-grid">
            <div v-for="(value, key) in selectedNode.properties" :key="key" class="property-item">
              <span class="property-key">{{ formatPropertyName(String(key)) }}:</span>
              <span class="property-value">{{ formatPropertyValue(value) }}</span>
            </div>
          </div>
          <p v-else class="no-properties">暂无属性信息</p>
        </div>
        
        <div v-if="relatedNodes.length > 0" class="related-nodes">
          <h3>相关实体</h3>
          <div class="related-nodes-grid">
            <button 
              v-for="node in relatedNodes.slice(0, 8)" 
              :key="node.id"
              class="related-node-btn"
              @click="focusOnNode(node.id)"
              :title="node.label"
            >
              {{ truncateText(node.label, 15) }}
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import neo4j from 'neo4j-driver';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';

// Neo4j连接配置
const NEO4J_URI = "neo4j://127.0.0.1:7687";
const NEO4J_USER = "neo4j";
const NEO4J_PASSWORD = "12345678";

// 状态变量
const networkContainer = ref<HTMLElement | null>(null);
const network = ref<Network | null>(null);
const nodes = ref<DataSet<any>>(new DataSet());
const edges = ref<DataSet<any>>(new DataSet());
const selectedDepth = ref(2);
const selectedEntityTypes = ref({
  Book: true,
  Author: true,
  Publisher: true,
  Category: true
});
const searchQuery = ref('');
const isConnected = ref(false);
const loading = ref(false);
const totalNodes = ref(0);
const totalEdges = ref(0);
const selectedNode = ref<any>(null);
const relatedNodes = ref<any[]>([]);
let driver: any = null;

// 文件上传相关状态
const selectedFileType = ref('');
const selectedFile = ref<File | null>(null);
const selectedFileName = ref('');
const isUploading = ref(false);
const uploadMessage = ref('');
const uploadSuccess = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const API_BASE_URL = 'http://localhost:5000/api';

// 节点样式配置
const nodeStyles = {
  Book: {
    color: {
      background: '#6366f1',
      border: '#4f46e5',
      highlight: {
        background: '#818cf8',
        border: '#4f46e5'
      }
    },
    shape: 'box',
    font: {
      color: '#fff',
      size: 12,
      bold: true
    },
    borderWidth: 2,
    shadow: true
  },
  Author: {
    color: {
      background: '#ec4899',
      border: '#db2777',
      highlight: {
        background: '#f472b6',
        border: '#db2777'
      }
    },
    shape: 'circle',
    font: {
      color: '#fff',
      size: 12,
      bold: true
    },
    borderWidth: 2,
    shadow: true
  },
  Publisher: {
    color: {
      background: '#14b8a6',
      border: '#0d9488',
      highlight: {
        background: '#2dd4bf',
        border: '#0d9488'
      }
    },
    shape: 'box',
    font: {
      color: '#fff',
      size: 12,
      bold: true
    },
    borderWidth: 2,
    shadow: true
  },
  Category: {
    color: {
      background: '#f59e0b',
      border: '#d97706',
      highlight: {
        background: '#fbbf24',
        border: '#d97706'
      }
    },
    shape: 'box',
    font: {
      color: '#fff',
      size: 12,
      bold: true
    },
    borderWidth: 2,
    shadow: true
  }
};

// 关系样式配置
const edgeStyle = {
  width: 2,
  color: {
    color: '#888',
    highlight: '#555',
    hover: '#333'
  },
  arrows: {
    to: {
      enabled: true,
      scaleFactor: 0.6
    }
  },
  smooth: {
    enabled: true,
    type: 'cubicBezier',
    forceDirection: 'horizontal',
    roundness: 0.4
  },
  font: {
    size: 11,
    color: '#666',
    strokeWidth: 0,
    face: 'Arial'
  }
};

// 初始化Neo4j连接
async function initDatabaseConnection() {
  try {
    driver = neo4j.driver(NEO4J_URI, neo4j.auth.basic(NEO4J_USER, NEO4J_PASSWORD));
    await driver.verifyConnectivity();
    isConnected.value = true;
    console.log('Neo4j连接成功');
    return true;
  } catch (error) {
    console.error('Neo4j连接失败:', error);
    isConnected.value = false;
    return false;
  }
}

// 切换实体类型
function toggleEntityType(type: string) {
  selectedEntityTypes.value[type as keyof typeof selectedEntityTypes.value] = 
    !selectedEntityTypes.value[type as keyof typeof selectedEntityTypes.value];
  updateGraph();
}

// 构建Cypher查询
function buildCypherQuery(depth: number, entityTypes: any, startNodeId?: string) {
  const typeFilters = Object.entries(entityTypes)
    .filter(([_, enabled]) => enabled)
    .map(([type]) => `n:${type}`)
    .join(' OR ');

  // 获取所有启用的实体类型
  const enabledTypes = Object.keys(entityTypes).filter(t => entityTypes[t]);
  if (enabledTypes.length === 0) {
    // 如果没有启用的实体类型，返回一个不会匹配任何结果的查询
    return 'MATCH (n) WHERE false RETURN n LIMIT 0';
  }
  const enabledTypesStr = enabledTypes.map(t => `'${t}'`).join(',');

  if (startNodeId) {
    // 从指定节点展开，获取更多关系和节点
    return `
      MATCH (start)-[r*1..${depth}]-(n)
      WHERE ID(start) = toInteger(${startNodeId}) AND (${typeFilters})
      UNWIND r AS rel
      RETURN DISTINCT n, labels(n) AS labels, type(rel) AS relType, start
      LIMIT 150
    `;
  } else if (searchQuery.value) {
    // 搜索查询
    const query = searchQuery.value.toLowerCase();
    return `
      MATCH (n)
      WHERE (${typeFilters}) AND (
        toLower(n.name) CONTAINS '${query}' OR 
        toLower(n.title) CONTAINS '${query}' OR
        toLower(n.id) CONTAINS '${query}' OR
        toLower(n.isbn) CONTAINS '${query}'
      )
      WITH n, labels(n) AS labels
      OPTIONAL MATCH (n)-[r]-(related)
      WHERE labels(related)[0] IN [${enabledTypesStr}]
      RETURN DISTINCT n, labels, collect(DISTINCT {related: related, relType: type(r)}) AS relatedWithRel
      LIMIT 150
    `;
  } else {
    // 默认查询，增加限制以显示更多数据
    return `
      MATCH (n)
      WHERE (${typeFilters})
      WITH n, labels(n) AS labels
      OPTIONAL MATCH (n)-[r]-(related)
      WHERE labels(related)[0] IN [${enabledTypesStr}]
      WITH n, labels, collect(DISTINCT {related: related, relType: type(r)}) AS relatedWithRel
      ORDER BY size(relatedWithRel) DESC
      RETURN n, labels, relatedWithRel
      LIMIT 150
    `;
  }
}

// 加载图谱数据
async function loadGraphData() {
  if (!isConnected.value || !driver) {
    console.error('数据库未连接');
    return;
  }

  loading.value = true;
  try {
    const session = driver.session();
    const query = buildCypherQuery(selectedDepth.value, selectedEntityTypes.value);
    
    const result = await session.run(query);
    await session.close();

    // 处理结果
    const newNodes: any[] = [];
    const newEdges: any[] = [];
    const nodeMap = new Map();

    result.records.forEach((record: any) => {
      const mainNode = record.get('n');
      const labels = record.get('labels');
      const type = labels[0];
      const nodeId = mainNode.identity.toString();
      
      // 添加主节点
      const nodeData = {
        id: nodeId,
        label: mainNode.properties.name || mainNode.properties.title || '未知',
        type: type,
        properties: mainNode.properties
      };
      
      if (!nodeMap.has(nodeId)) {
        nodeMap.set(nodeId, nodeData);
        
        // 使用节点样式
        newNodes.push({
          id: nodeId,
          label: nodeData.label,
          type: type,
          properties: nodeData.properties,
          ...nodeStyles[type as keyof typeof nodeStyles]
        });
      }

      // 处理相关节点和关系
      const relatedWithRel = record.get('relatedWithRel');
      if (relatedWithRel && Array.isArray(relatedWithRel)) {
        relatedWithRel.forEach((item: any) => {
          if (!item || !item.related) return;
          
          const relatedNode = item.related;
          const relType = item.relType;
          
          const relatedId = relatedNode.identity.toString();
          const relatedLabels = relatedNode.labels;
          const relatedType = relatedLabels[0];
          
          // 添加相关节点
          if (!nodeMap.has(relatedId)) {
            const relatedData = {
              id: relatedId,
              label: relatedNode.properties.name || relatedNode.properties.title || '未知',
              type: relatedType,
              properties: relatedNode.properties
            };
            nodeMap.set(relatedId, relatedData);
            
            // 使用节点样式
            newNodes.push({
              id: relatedId,
              label: relatedData.label,
              type: relatedType,
              properties: relatedData.properties,
              ...nodeStyles[relatedType as keyof typeof nodeStyles]
            });
          }
          
          // 添加关系
          const edgeId = `${nodeId}-${relatedId}-${relType}`;
          const reverseEdgeId = `${relatedId}-${nodeId}-${relType}`;
          
          if (!newEdges.find(e => e.id === edgeId || e.id === reverseEdgeId)) {
            newEdges.push({
              id: edgeId,
              from: nodeId,
              to: relatedId,
              label: relType,
              ...edgeStyle
            });
          }
        });
      }
    });

    // 更新节点和边
    nodes.value.clear();
    edges.value.clear();
    nodes.value.add(newNodes);
    edges.value.add(newEdges);
    
    totalNodes.value = newNodes.length;
    totalEdges.value = newEdges.length;
    
    // 如果是首次加载，默认选中第一个节点
    if (newNodes.length > 0 && !selectedNode.value) {
      const firstNodeId = newNodes[0].id;
      selectedNode.value = nodeMap.get(firstNodeId);
      updateRelatedNodes(firstNodeId);
    }

  } catch (error) {
    console.error('加载图谱数据失败:', error);
  } finally {
    loading.value = false;
  }
}

// 更新相关节点
function updateRelatedNodes(nodeId: string) {
  const nodeEdges = edges.value.get();
  const relatedNodeIds = new Set<string>();
  
  nodeEdges.forEach(edge => {
    if (edge.from === nodeId) {
      relatedNodeIds.add(edge.to);
    } else if (edge.to === nodeId) {
      relatedNodeIds.add(edge.from);
    }
  });
  
  relatedNodes.value = Array.from(relatedNodeIds).map(id => {
    const node = nodes.value.get(id);
    return node ? { ...node, id } : null;
  }).filter(Boolean);
}

// 创建图谱可视化
function createNetwork() {
  if (!networkContainer.value) return;

  const data = {
    nodes: nodes.value,
    edges: edges.value
  };

  // 图谱选项配置 - 优化性能版
    const options = {
        nodes: {
          borderWidthSelected: 3,
          chosen: true,
          shape: 'box',
          size: 20, // 减小节点大小
          margin: {
            top: 5,
            right: 5,
            bottom: 5,
            left: 5
          }
        },
        edges: {
          arrows: {
            to: {
              enabled: true,
              scaleFactor: 0.4 // 减小箭头大小
            }
          },
          smooth: {
            type: 'continuous', // 更简单的平滑算法
            forceDirection: 'none', // 不强制方向，提高性能
            roundness: 0.2
          }
        },
        layout: {
          improvedLayout: true
        },
        physics: {
          enabled: true,
          barnesHut: {
            gravitationalConstant: -2000, // 降低引力
            centralGravity: 0.3, // 增加中心引力
            springLength: 150, // 增加弹簧长度
            springConstant: 0.08, // 增加弹簧强度
            damping: 0.4, // 增加阻尼
            avoidOverlap: 0
          },
          solver: 'barnesHut',
          stabilization: {
            enabled: true,
            iterations: 500, // 大幅减少迭代次数
            updateInterval: 50,
            onlyDynamicEdges: true, // 仅动态边参与稳定化
            fit: true,
            earlyStop: true // 提前停止稳定化
          },
          maxVelocity: 50, // 限制最大速度
          minVelocity: 0.1
        },
        interaction: {
          dragNodes: true,
          dragView: true,
          hover: false, // 禁用悬停效果
          hoverConnectedEdges: false,
          keyboard: {
            enabled: false, // 禁用键盘交互
            speed: {x: 10, y: 10, zoom: 0.02},
            bindToWindow: true
          },
          multiselect: false, // 禁用多选
          selectable: true,
          selectConnectedEdges: true,
          tooltipDelay: 500,
          zoomView: true,
          navigationButtons: false
        },
        configure: {
          enabled: false
        }
      } as any;

  // 创建或更新网络
  if (network.value) {
    network.value.setData(data);
    network.value.setOptions(options);
  } else {
    network.value = new Network(networkContainer.value, data, options);
    
    // 添加事件监听器
    network.value.on('click', (params) => {
      if (params.nodes.length > 0) {
        const nodeId = params.nodes[0];
        const node = nodes.value.get(nodeId);
        if (node) {
          const nodeData = nodes.value.get(nodeId) as any;
          selectedNode.value = {
            id: nodeId,
            label: nodeData?.label || '未知',
            type: nodeData?.type || '未知',
            properties: nodeData?.properties || {}
          };
          updateRelatedNodes(nodeId);
        }
      }
    });

    network.value.on('stabilizationProgress', (params) => {
      const progress = Math.round(params.iterations / params.total * 100);
      console.log(`布局进度: ${progress}%`);
    });

    network.value.on('stabilizationIterationsDone', () => {
      console.log('布局完成');
    });
  }
}

// 更新图谱
function updateGraph() {
  loadGraphData().then(() => {
    nextTick(() => {
      createNetwork();
    });
  });
}

// 搜索实体
function searchEntities() {
  updateGraph();
}

// 聚焦到指定节点
function focusOnNode(nodeId: string) {
  if (network.value) {
    network.value.selectNodes([nodeId]);
    network.value.focus(nodeId, {
      scale: 1.5,
      animation: {
        duration: 1000,
        easingFunction: 'easeInOutQuad'
      }
    });
  }
}

// 重置缩放
function resetZoom() {
  if (network.value) {
    network.value.moveTo({
      scale: 1,
      animation: {
        duration: 1000,
        easingFunction: 'easeInOutQuad'
      }
    });
  }
}

// 导出图谱
function exportGraph() {
  if (network.value) {
    // 创建一个临时的 SVG 元素来导出
    const svgElement = document.querySelector('.vis-network > svg');
    if (svgElement) {
      const svgData = new XMLSerializer().serializeToString(svgElement);
      const blob = new Blob([svgData], {type: 'image/svg+xml;charset=utf-8'});
      const url = URL.createObjectURL(blob);
      
      // 创建下载链接
      const link = document.createElement('a');
      link.href = url;
      link.download = `knowledge-graph-${new Date().toISOString().slice(0, 10)}.svg`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }
  }
}

// 格式化属性名
function formatPropertyName(key: string): string {
  // 将驼峰命名转换为空格分隔的单词
  return key.replace(/([A-Z])/g, ' $1').trim().replace(/^./, str => str.toUpperCase());
}

// 格式化属性值
function formatPropertyValue(value: any): string {
  if (value === null || value === undefined) return '无';
  if (Array.isArray(value)) return value.join(', ');
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

// 截断文本
function truncateText(text: string, maxLength: number): string {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

// 监听搜索查询变化
watch(searchQuery, (newQuery) => {
  if (newQuery.trim() === '') {
    updateGraph();
  }
});

// 组件挂载时初始化
onMounted(async () => {
  await initDatabaseConnection();
  if (isConnected.value) {
    await loadGraphData();
    nextTick(() => {
      createNetwork();
    });
  }
});

// 触发文件选择
function triggerFileUpload() {
  fileInput.value?.click();
}

// 处理文件选择
function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    selectedFile.value = input.files[0];
    selectedFileName.value = input.files[0].name;
  }
}

// 清除选中的文件
function clearFile() {
  selectedFile.value = null;
  selectedFileName.value = '';
  if (fileInput.value) {
    fileInput.value.value = '';
  }
}

// 上传文件更新知识图谱
async function uploadFile() {
  if (!selectedFile.value || !selectedFileType.value) return;
  
  isUploading.value = true;
  uploadMessage.value = '';
  
  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    formData.append('file_type', selectedFileType.value);
    
    const response = await fetch(`${API_BASE_URL}/upload`, {
      method: 'POST',
      body: formData
    });
    
    const result = await response.json();
    
    if (result.success) {
      uploadMessage.value = result.message;
      uploadSuccess.value = true;
      // 上传成功后刷新图谱
      await updateGraph();
    } else {
      uploadMessage.value = result.message;
      uploadSuccess.value = false;
    }
  } catch (error) {
    uploadMessage.value = '文件上传失败，请确保后端服务已启动';
    uploadSuccess.value = false;
    console.error('文件上传错误:', error);
  } finally {
    isUploading.value = false;
    // 3秒后清除消息
    setTimeout(() => {
      uploadMessage.value = '';
    }, 3000);
  }
}

// 确认重置图谱
function confirmResetGraph() {
  if (confirm('确定要重置知识图谱吗？这将清除所有数据！')) {
    resetGraph();
  }
}

// 重置知识图谱
async function resetGraph() {
  isUploading.value = true;
  uploadMessage.value = '';
  
  try {
    const response = await fetch(`${API_BASE_URL}/reset-graph`, {
      method: 'POST'
    });
    
    const result = await response.json();
    
    if (result.success) {
      uploadMessage.value = result.message;
      uploadSuccess.value = true;
      // 重置成功后刷新图谱
      await updateGraph();
    } else {
      uploadMessage.value = result.message;
      uploadSuccess.value = false;
    }
  } catch (error) {
    uploadMessage.value = '重置失败，请确保后端服务已启动';
    uploadSuccess.value = false;
    console.error('重置图谱错误:', error);
  } finally {
    isUploading.value = false;
    // 3秒后清除消息
    setTimeout(() => {
      uploadMessage.value = '';
    }, 3000);
  }
}

// 验证API连接
async function verifyApiConnection() {
  try {
    const response = await fetch(`${API_BASE_URL}/verify-connection`);
    const result = await response.json();
    return result.success;
  } catch (error) {
    console.error('API连接验证失败:', error);
    return false;
  }
}

// 组件卸载时清理
onUnmounted(() => {
  if (network.value) {
    network.value.destroy();
  }
  if (driver) {
    driver.close();
  }
});
</script>

<style scoped>
/* 全局样式 */
.knowledge-base-container {
  display: flex;
  height: 100vh;
  background: #121212;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  overflow: hidden;
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  background: #1e1e1e;
  padding: 20px;
  border-right: 1px solid #333;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(10px);
}

/* 搜索框样式 */
.search-box {
  position: relative;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 0 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #888;
}

.search-input {
  width: 100%;
  padding: 12px 12px 12px 36px;
  background: transparent;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  color: #e0e0e0;
  outline: none;
  transition: all 0.3s ease;
}

.search-input::placeholder {
  color: #888;
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.08);
}

/* 筛选区域样式 */
.filter-section {
  margin-bottom: 24px;
}

.filter-section h3 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #b0b0b0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 实体类型按钮 */
.entity-type-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entity-type-btn {
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: #e0e0e0;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.entity-type-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  transition: left 0.5s ease;
}

.entity-type-btn:hover::before {
  left: 100%;
}

.entity-type-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateX(4px);
}

.entity-type-btn.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(99, 102, 241, 0.1));
  border-color: rgba(99, 102, 241, 0.5);
  color: #a5b4fc;
  font-weight: 500;
}

/* 深度按钮 */
.depth-buttons {
  display: flex;
  gap: 10px;
}

.depth-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #e0e0e0;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.depth-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  transition: left 0.5s ease;
}

.depth-btn:hover::before {
  left: 100%;
}

.depth-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.depth-btn.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border-color: #6366f1;
  color: white;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
}

/* 连接状态 */
.connection-status {
  margin-top: auto;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  font-size: 13px;
  color: #fca5a5;
  text-align: center;
  transition: all 0.3s ease;
}

.connection-status.connected {
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  color: #86efac;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

/* 标题部分 */
.header-section {
  margin-bottom: 20px;
}

.header-section h1 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 16px;
  background: linear-gradient(90deg, #e0e0e0, #b0b0b0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-container {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 20px;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: #6366f1;
}

.stat-label {
  font-size: 14px;
  color: #b0b0b0;
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #888;
  font-size: 14px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top: 2px solid #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 图谱容器 - 全新现代设计 */
.graph-container {
  background: linear-gradient(135deg, rgba(30, 30, 30, 0.8), rgba(20, 20, 20, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 24px;
  position: relative;
  margin-bottom: 20px;
  flex: 1;
  backdrop-filter: blur(16px);
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.graph-container:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

/* 控制按钮区域 */
.graph-controls {
  position: absolute;
  top: 24px;
  right: 24px;
  display: flex;
  gap: 16px;
  z-index: 10;
  background: rgba(15, 15, 15, 0.7);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 控制按钮 - 全新设计 */
.control-btn {
  padding: 12px 20px;
  background: transparent;
  border: 2px solid transparent;
  border-radius: 12px;
  color: #e0e0e0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  overflow: hidden;
}

.control-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: left 0.6s ease;
}

.control-btn:hover::before {
  left: 100%;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.1);
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.control-btn:active {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
}

.control-icon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.control-btn:hover .control-icon {
  transform: scale(1.2);
}

/* 图谱可视化区域 - 增强设计 */
.graph-visualization {
  width: 100%;
  height: 100%;
  min-height: 600px;
  position: relative;
  border-radius: 16px;
  background: radial-gradient(circle at 50% 50%, rgba(30, 30, 30, 0.2), transparent 70%);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.03);
}

/* 添加装饰性元素 */
.graph-visualization::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: repeating-linear-gradient(
    45deg,
    rgba(99, 102, 241, 0.02),
    rgba(99, 102, 241, 0.02) 10px,
    rgba(99, 102, 241, 0.03) 10px,
    rgba(99, 102, 241, 0.03) 20px
  );
  opacity: 0.3;
  animation: movePattern 20s linear infinite;
  pointer-events: none;
}

@keyframes movePattern {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  100% {
    transform: translate(-50px, -50px) rotate(360deg);
  }
}

/* 详情面板 */
.detail-panel {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(10px);
  max-height: 300px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  position: relative;
}

.detail-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #e0e0e0;
}

.node-type {
  padding: 4px 10px;
  background: rgba(99, 102, 241, 0.2);
  border-radius: 12px;
  font-size: 12px;
  color: #a5b4fc;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.close-btn {
  position: absolute;
  right: 0;
  top: 0;
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #e0e0e0;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: scale(1.1);
}

/* 节点属性 */
.node-properties {
  margin-bottom: 20px;
}

.properties-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.property-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.property-key {
  font-size: 12px;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.property-value {
  font-size: 14px;
  color: #e0e0e0;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
  padding: 6px 10px;
  word-break: break-word;
}

.no-properties {
  color: #888;
  text-align: center;
  padding: 20px;
  font-style: italic;
}

/* 相关节点 */
.related-nodes h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #b0b0b0;
}

.related-nodes-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.related-node-btn {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: #e0e0e0;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-node-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .sidebar {
    width: 240px;
    padding: 16px;
  }
  
  .main-content {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .knowledge-base-container {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    height: auto;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 16px;
    padding: 12px;
  }
  
  .search-box {
    flex: 1;
    min-width: 200px;
    margin-bottom: 0;
  }
  
  .filter-section {
    margin-bottom: 0;
  }
  
  .connection-status {
    margin-top: 0;
  }
}

/* 文件上传样式 */
.upload-section {
  margin-top: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.upload-section h3 {
  color: #fff;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 600;
}

.file-type-selector {
  margin-bottom: 10px;
}

.file-type-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.file-type-select option {
  background: #1e293b;
  color: #fff;
}

.file-upload-area {
  position: relative;
  margin-bottom: 10px;
}

.file-input {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.upload-btn {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  z-index: 1;
}

.upload-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #4338ca, #4f46e5);
  transform: translateY(-1px);
}

.upload-btn:disabled {
  background: #475569;
  cursor: not-allowed;
  transform: none;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #e2e8f0;
}

.clear-btn {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.3s ease;
}

.clear-btn:hover {
  background: rgba(239, 68, 68, 0.2);
}

.submit-upload-btn {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 10px;
  transition: all 0.3s ease;
}

.submit-upload-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669, #047857);
  transform: translateY(-1px);
}

.submit-upload-btn:disabled {
  background: #475569;
  cursor: not-allowed;
  transform: none;
}

.reset-graph-btn {
  width: 100%;
  padding: 8px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s ease;
}

.reset-graph-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  transform: translateY(-1px);
}

.reset-graph-btn:disabled {
  background: #475569;
  cursor: not-allowed;
  transform: none;
}

.upload-message {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  text-align: center;
  animation: fadeIn 0.3s ease;
}

.upload-message.success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.upload-message.error {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>