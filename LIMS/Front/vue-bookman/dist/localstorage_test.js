/**
 * localStorage文件清理功能测试脚本
 * 使用方法：在浏览器控制台中执行以下命令加载此脚本
 * loadLocalStorageTest();
 */

// 自动注册加载函数到window对象
window.loadLocalStorageTest = function() {
  console.log('=== localStorage文件清理功能测试脚本加载成功 ===');
  
  // 创建测试函数
  window.testLocalStorage = {
    // 测试localStorage基本功能
    testBasicOperations: function() {
      console.log('\n--- 测试localStorage基本操作 ---');
      
      try {
        // 测试存储
        const testKey = 'test_aiqa_key';
        const testValue = JSON.stringify({test: 'value', timestamp: Date.now()});
        console.log(`尝试存储数据: ${testKey} = ${testValue}`);
        localStorage.setItem(testKey, testValue);
        
        // 测试读取
        const storedValue = localStorage.getItem(testKey);
        console.log(`存储结果: ${storedValue ? '成功' : '失败'}`);
        console.log(`读取的值: ${storedValue}`);
        
        // 测试删除
        localStorage.removeItem(testKey);
        console.log(`删除后检查: ${localStorage.getItem(testKey) === null ? '成功' : '失败'}`);
        
        // 列出所有localStorage键
        console.log('\n当前localStorage中的所有键:');
        for(let i = 0; i < localStorage.length; i++) {
          const key = localStorage.key(i);
          console.log(`  - ${key}`);
        }
        
        return true;
      } catch (error) {
        console.error('localStorage基本操作测试失败:', error);
        return false;
      }
    },
    
    // 模拟创建待清理文件记录
    simulateCleanupRecord: function(files = ['test_doc1.md', 'test_doc2.pdf']) {
      console.log('\n--- 模拟创建清理记录 ---');
      
      try {
        const cleanupData = {
          files: files,
          modelId: 3,
          timestamp: Date.now()
        };
        
        console.log('准备存储的清理数据:', cleanupData);
        localStorage.setItem('aiqa_pending_cleanup', JSON.stringify(cleanupData));
        
        // 立即验证
        const stored = localStorage.getItem('aiqa_pending_cleanup');
        console.log('存储结果:', stored ? '成功' : '失败');
        if (stored) {
          console.log('存储的数据:', JSON.parse(stored));
        }
        
        return true;
      } catch (error) {
        console.error('创建清理记录失败:', error);
        return false;
      }
    },
    
    // 检查并模拟清理localStorage中的记录
    checkAndCleanupRecords: function() {
      console.log('\n--- 检查并清理记录 ---');
      
      try {
        const cleanupDataStr = localStorage.getItem('aiqa_pending_cleanup');
        
        if (cleanupDataStr) {
          console.log('发现待清理记录:', cleanupDataStr);
          const cleanupData = JSON.parse(cleanupDataStr);
          
          console.log('解析后的清理数据:');
          console.log('- 文件列表:', cleanupData.files);
          console.log('- 模型ID:', cleanupData.modelId);
          console.log('- 时间戳:', cleanupData.timestamp);
          console.log('- 时间差(秒):', Math.floor((Date.now() - cleanupData.timestamp) / 1000));
          
          // 模拟发送清理请求
          console.log('\n模拟发送清理请求...');
          const mockApiUrl = `http://localhost:8081/api/ai/update-embeddings?modelId=${cleanupData.modelId}`;
          console.log('目标API:', mockApiUrl);
          console.log('删除文件:', cleanupData.files);
          
          // 模拟删除localStorage记录
          console.log('\n模拟清理完成，删除localStorage记录...');
          localStorage.removeItem('aiqa_pending_cleanup');
          console.log('删除结果:', localStorage.getItem('aiqa_pending_cleanup') === null ? '成功' : '失败');
          
          return true;
        } else {
          console.log('未发现待清理记录');
          return false;
        }
      } catch (error) {
        console.error('检查并清理记录失败:', error);
        return false;
      }
    },
    
    // 测试页面刷新场景
    testPageRefreshScenario: function() {
      console.log('\n--- 测试页面刷新场景 ---');
      
      // 步骤1: 模拟创建记录
      this.simulateCleanupRecord(['doc1.txt', 'doc2.txt']);
      
      // 步骤2: 显示当前状态
      console.log('\n刷新前状态检查:');
      console.log('localStorage中是否有待清理记录:', localStorage.getItem('aiqa_pending_cleanup') !== null);
      
      // 步骤3: 提示用户
      console.log('\n请手动刷新页面，然后在刷新后的页面控制台执行:');
      console.log('window.testLocalStorage.checkAndCleanupRecords()');
    },
    
    // 直接调用Vue组件的清理方法(如果可访问)
    callVueCleanupMethod: function() {
      console.log('\n--- 尝试调用Vue组件清理方法 ---');
      
      try {
        // 尝试各种可能的Vue实例访问方式
        const possibleVueInstances = [
          window.app,
          window.vueApp,
          document.querySelector('#app').__vue__
        ];
        
        let vueInstance = null;
        for (const instance of possibleVueInstances) {
          if (instance && typeof instance.cleanUpFromLocalStorage === 'function') {
            vueInstance = instance;
            break;
          }
        }
        
        if (vueInstance) {
          console.log('找到Vue实例，调用cleanUpFromLocalStorage方法...');
          vueInstance.cleanUpFromLocalStorage();
          return true;
        } else {
          console.log('未找到可访问的Vue实例或cleanUpFromLocalStorage方法');
          console.log('请尝试直接在Vue组件内执行: this.cleanUpFromLocalStorage()');
          return false;
        }
      } catch (error) {
        console.error('调用Vue清理方法失败:', error);
        return false;
      }
    },
    
    // 完整测试流程
    runFullTest: function() {
      console.log('\n=== 开始完整测试流程 ===');
      
      // 1. 测试基本功能
      const basicTestResult = this.testBasicOperations();
      console.log('基本功能测试结果:', basicTestResult ? '通过' : '失败');
      
      if (!basicTestResult) {
        console.log('基本功能测试失败，localStorage可能被禁用或有限制');
        return;
      }
      
      // 2. 模拟清理记录
      this.simulateCleanupRecord(['test_file_for_cleanup.md']);
      
      // 3. 检查记录
      this.checkAndCleanupRecords();
      
      // 4. 提示用户进行后续测试
      console.log('\n=== 测试完成 ===');
      console.log('请继续以下测试:');
      console.log('1. 执行 window.testLocalStorage.testPageRefreshScenario() 测试页面刷新场景');
      console.log('2. 在AI问答页面嵌入文件后，检查控制台是否有localStorage相关日志');
      console.log('3. 刷新页面后，检查localStorage中是否有aiqa_pending_cleanup记录');
    }
  };
  
  // 显示使用说明
  console.log('\n使用说明:');
  console.log('1. 执行基本测试: window.testLocalStorage.testBasicOperations()');
  console.log('2. 模拟创建清理记录: window.testLocalStorage.simulateCleanupRecord()');
  console.log('3. 检查并清理记录: window.testLocalStorage.checkAndCleanupRecords()');
  console.log('4. 测试页面刷新场景: window.testLocalStorage.testPageRefreshScenario()');
  console.log('5. 尝试调用Vue清理方法: window.testLocalStorage.callVueCleanupMethod()');
  console.log('6. 运行完整测试: window.testLocalStorage.runFullTest()');
  
  // 默认运行完整测试
  setTimeout(() => {
    window.testLocalStorage.runFullTest();
  }, 1000);
};

// 如果脚本直接在控制台执行，自动初始化
if (typeof window !== 'undefined') {
  window.loadLocalStorageTest();
}