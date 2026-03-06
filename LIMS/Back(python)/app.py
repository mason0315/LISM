from flask import Flask, jsonify, request
from flask_cors import CORS
import os
import requests
import json
import uuid
import time
import re
import logging
from datetime import datetime, timedelta
from dotenv import load_dotenv
# 导入Neo4j处理模块
NEO4J_ENABLED = False
try:
    # 检查neo4j_handler.py文件是否存在
    import os
    module_path = os.path.join(os.path.dirname(__file__), 'neo4j_handler.py')
    print(f"检查neo4j_handler.py文件路径: {module_path}")
    print(f"文件是否存在: {os.path.exists(module_path)}")
    
    # 尝试导入模块
    from neo4j_handler import init_neo4j_handler, get_neo4j_handler
    NEO4J_ENABLED = True
    print("成功导入neo4j_handler模块")
except ImportError as e:
    print(f"警告: 无法导入neo4j_handler模块，Neo4j知识图谱功能不可用")
    print(f"导入错误详情: {str(e)}")
except Exception as e:
    print(f"警告: 导入neo4j_handler模块时发生未知错误: {str(e)}")

# 确保logs目录存在
if not os.path.exists('logs'):
    os.makedirs('logs')

# 配置日志 - 确保app.logger也被正确配置

# 创建Flask应用后才配置app.logger
# 先配置通用logger
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# 清除现有的处理器（防止重复日志）
for handler in logger.handlers[:]:
    logger.removeHandler(handler)

# 配置文件处理器
file_handler = logging.FileHandler('logs/ai_enhancer.log', encoding='utf-8')
file_handler.setLevel(logging.INFO)
file_formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
file_handler.setFormatter(file_formatter)

# 配置控制台处理器
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.INFO)
console_formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
console_handler.setFormatter(console_formatter)

# 添加处理器到logger
logger.addHandler(file_handler)
logger.addHandler(console_handler)
# 导入docx处理模块
try:
    from process_docx import extract_text_from_base64_docx
except ImportError:
    print("警告: 无法导入process_docx模块，Word文档处理功能可能不可用")
    def extract_text_from_base64_docx(base64_data):
        return "[Word文档内容，需要python-docx库来解析]"

# 知识图谱增强函数在文件后续部分已实现

# 导入优化版的通用Neo4j查询函数
try:
    from generic_neo4j_query import generic_neo4j_query as external_generic_neo4j_query
    NEO4J_OPTIMIZED = True
except ImportError:
    print("警告: 无法导入优化版的generic_neo4j_query模块")
    NEO4J_OPTIMIZED = False

# 加载环境变量
load_dotenv()

# 创建Flask应用
app = Flask(__name__)
# 配置更宽松的CORS支持，确保前端可以正常访问
CORS(app, resources={r"/api/*": {"origins": ["http://localhost:8080", "http://localhost:5173", "http://127.0.0.1:8080", "http://127.0.0.1:5173", "*"], "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"], "allow_headers": ["*"], "supports_credentials": True}})

# 初始化Neo4j处理器
print(f"准备初始化Neo4j处理器, NEO4J_ENABLED={NEO4J_ENABLED}")
if NEO4J_ENABLED:
    try:
        # 从环境变量获取Neo4j连接信息，如果没有则使用默认值
        NEO4J_URI = os.getenv('NEO4J_URI', 'neo4j://127.0.0.1:7687')
        NEO4J_USER = os.getenv('NEO4J_USER', 'neo4j')
        NEO4J_PASSWORD = os.getenv('NEO4J_PASSWORD', '12345678')
        
        print(f"正在初始化Neo4j处理器，连接信息: URI={NEO4J_URI}, USER={NEO4J_USER}")
        handler = init_neo4j_handler(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)
        print(f"Neo4j处理器初始化完成")
        
        # 尝试获取处理器并验证
        handler_instance = get_neo4j_handler()
        print(f"获取Neo4j处理器实例成功")
        
        # 进行简单的连接测试
        if handler_instance:
            # 测试关键词提取功能
            test_keywords = handler_instance.extract_keywords("《百年孤独》的作者是谁？")
            print(f"Neo4j关键词提取测试结果: {test_keywords}")
            app.logger.info(f"Neo4j关键词提取功能正常: {test_keywords}")
        
        app.logger.info("Neo4j知识图谱处理器初始化成功")
    except Exception as e:
        print(f"Neo4j知识图谱处理器初始化失败: {str(e)}")
        app.logger.error(f"Neo4j知识图谱处理器初始化失败: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        NEO4J_ENABLED = False
else:
    print("Neo4j功能未启用")

# 确保logs目录存在
log_dir = os.path.join(os.path.dirname(__file__), 'logs')
os.makedirs(log_dir, exist_ok=True)

# 清除现有的处理器以避免重复日志
if app.logger.handlers:
    for handler in app.logger.handlers[:]:
        app.logger.removeHandler(handler)

# 配置Flask应用日志器
app.logger.setLevel(logging.INFO)

# 添加文件处理器到app.logger
file_handler = logging.FileHandler(os.path.join(log_dir, 'ai_enhancer.log'), encoding='utf-8')
file_handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
file_handler.setFormatter(formatter)
app.logger.addHandler(file_handler)

# 添加控制台处理器
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
console_handler.setFormatter(formatter)
app.logger.addHandler(console_handler)

# AnythingLLM配置
ANYTHINGLLM_BASE_URL = os.getenv('ANYTHINGLLM_BASE_URL', 'http://localhost:3001')
ANYTHINGLLM_WORKSPACE_ID = os.getenv('ANYTHINGLLM_WORKSPACE_ID', '1c09e02c-5c1b-490f-b52c-36c977f79699')  # 默认workspace ID
ANYTHINGLLM_API_KEY = os.getenv('ANYTHINGLLM_API_KEY', '61M2HCQ-A4KM618-HG11G12-Z9QCXYP')  # 用户提供的API密钥

# 工作区Slug到UUID的映射
WORKSPACE_SLUG_TO_ID = {
    '36f38646-a044-4677-9e4e-8a519402c38f': '3',  # 图书管理
    '715ff10f-5ea8-4ef2-8d37-6ae40856acbb': '715ff10f-5ea8-4ef2-8d37-6ae40856acbb'  # 图书管理（训练版）
}

# 临时文件存储
TEMP_FILE_STORAGE = {}
TEMP_FILE_EXPIRY = 3600  # 临时文件过期时间（秒）

# 清理过期临时文件的函数
def cleanup_expired_temp_files():
    current_time = time.time()
    expired_tokens = []
    
    # 首先收集过期的token和文件名
    for token, file_info in TEMP_FILE_STORAGE.items():
        if current_time - file_info['timestamp'] > TEMP_FILE_EXPIRY:
            expired_tokens.append((token, file_info['filename']))
    
    # 然后删除过期文件
    for token, filename in expired_tokens:
        del TEMP_FILE_STORAGE[token]
        app.logger.info(f"清理过期临时文件: {filename}")
    
    if expired_tokens:
            app.logger.info(f"共清理 {len(expired_tokens)} 个过期临时文件")

def format_neo4j_results(result, max_entities=50, max_relationships=100):
    """
    增强的格式化函数，处理各种关系数据格式
    限制实体和关系数量，避免信息过载
    新增关系去重功能，优化实体查找方式，增强属性处理
    """
    if not result:
        return "未找到相关信息"
    
    formatted_text = "### 知识图谱信息 ###\n\n"
    
    # 创建实体ID到名称的映射，用于关系显示优化
    entity_id_to_name = {}
    
    # 格式化实体信息
    entities = result.get('entities', [])
    entity_count = len(entities)
    entities_to_display = entities[:max_entities]
    
    entities_text = "实体: \n"
    for entity in entities_to_display:
        # 获取实体属性
        properties = entity.get('properties', entity)  # 优先使用properties，否则使用实体本身
        # 处理properties可能是字符串的情况
        if isinstance(properties, str):
            try:
                # 尝试将字符串转换为字典
                import json
                properties = json.loads(properties.replace("'", '"'))
            except (json.JSONDecodeError, AttributeError):
                properties = {}
        
        # 获取实体名称，支持多种属性名
        entity_id = entity.get('id', entity.get('elementId', str(properties.get('id', 'unknown'))))
        entity_name = properties.get('name', 
                                    properties.get('title', 
                                    properties.get('书名', 
                                    f"实体_{str(entity_id)[:8]}")))
        
        # 存储实体ID到名称的映射
        entity_id_to_name[entity_id] = entity_name
        
        # 处理可能过长的名称
        if len(entity_name) > 30:
            entity_name = entity_name[:27] + "..."
        
        # 尝试获取实体标签，支持不同的键名和格式
        labels = entity.get('labels', entity.get('type', []))
        # 确保labels是可迭代的列表或集合
        if isinstance(labels, str):
            labels = [labels]
        # 如果没有标签，使用默认标签
        if not labels:
            labels = ['Unknown']
        labels_str = ", ".join(labels)
        
        # 收集并精简主要属性
        main_props = {}
        for key, value in properties.items():
            if key.lower() not in ['id', '_id', 'elementid', 'name', 'title'] and len(str(value)) > 0:
                # 对长文本属性进行截断
                prop_value = str(value)
                if len(prop_value) > 50:
                    prop_value = prop_value[:47] + "..."
                main_props[key] = prop_value
        
        entities_text += f"- {entity_name} [{labels_str}]: {main_props}\n"
    
    # 如果实体数量超过限制，添加提示
    if entity_count > max_entities:
        entities_text += f"- ... 还有 {entity_count - max_entities} 个实体未显示\n"
    
    # 统一关系数据源
    relationships = result.get('all_relationships', []) or result.get('relationships', [])
    relationship_count = len(relationships)
    
    # 统计关系类型
    relationship_types = {}
    for rel in relationships:
        if isinstance(rel, dict):
            rel_type = rel.get('type', 'UNKNOWN')
            relationship_types[rel_type] = relationship_types.get(rel_type, 0) + 1
        elif isinstance(rel, (list, tuple)) and len(rel) >= 3:
            try:
                rel_type = str(rel[1])
                relationship_types[rel_type] = relationship_types.get(rel_type, 0) + 1
            except:
                pass
    
    # 按关系频率排序，优先显示重要关系
    sorted_relationship_types = sorted(relationship_types.items(), key=lambda x: x[1], reverse=True)
    if app.logger:
        app.logger.info(f"关系类型统计: {sorted_relationship_types}")
    
    # 格式化所有关系，添加去重逻辑
    relationships_text = "关系: \n"
    relationships_text += "\n关系详情:\n"
    
    # 使用集合记录已处理的关系，避免重复
    processed_relationships = set()
    displayed_count = 0
    
    for rel in relationships:
        if displayed_count >= max_relationships:
            break
        
        # 处理不同的关系格式
        if isinstance(rel, dict):
            rel_type = rel.get('type', 'RELATES_TO')
            
            # 尝试多种方式获取起始节点和结束节点信息
            start_name = None
            end_name = None
            
            # 方式1：完整的节点对象
            if 'start_node' in rel and 'end_node' in rel:
                if isinstance(rel['start_node'], dict):
                    start_props = rel['start_node'].get('properties', {})
                    start_id = rel['start_node'].get('elementId', rel['start_node'].get('id'))
                    start_name = start_props.get('name', start_props.get('title', entity_id_to_name.get(start_id, f"实体_{start_id[:8] if start_id else 'unknown'}")))
                else:
                    # 如果start_node是ID而不是对象
                    start_id = rel['start_node']
                    start_name = entity_id_to_name.get(start_id, f"实体_{str(start_id)[:8]}")
                
                if isinstance(rel['end_node'], dict):
                    end_props = rel['end_node'].get('properties', {})
                    end_id = rel['end_node'].get('elementId', rel['end_node'].get('id'))
                    end_name = end_props.get('name', end_props.get('title', entity_id_to_name.get(end_id, f"实体_{end_id[:8] if end_id else 'unknown'}")))
                else:
                    # 如果end_node是ID而不是对象
                    end_id = rel['end_node']
                    end_name = entity_id_to_name.get(end_id, f"实体_{str(end_id)[:8]}")
            
            # 方式2：source和target字段
            elif 'source' in rel and 'target' in rel:
                start_name = rel.get('source', '未知节点')
                end_name = rel.get('target', '未知节点')
            
            # 方式3：节点ID字段
            elif 'start_node_id' in rel and 'end_node_id' in rel:
                start_id = rel.get('start_node_id')
                end_id = rel.get('end_node_id')
                start_name = entity_id_to_name.get(start_id, f"实体_{str(start_id)[:8]}")
                end_name = entity_id_to_name.get(end_id, f"实体_{str(end_id)[:8]}")
            elif 'source_id' in rel and 'target_id' in rel:
                start_id = rel.get('source_id')
                end_id = rel.get('target_id')
                start_name = entity_id_to_name.get(start_id, f"实体_{str(start_id)[:8]}")
                end_name = entity_id_to_name.get(end_id, f"实体_{str(end_id)[:8]}")
            
            if start_name and end_name:
                # 处理关系属性
                rel_props = rel.get('properties', {})
                prop_text = ""
                if rel_props and isinstance(rel_props, dict):
                    simplified_props = {}
                    for key, value in rel_props.items():
                        if key.lower() not in ['id', '_id', 'elementid', 'type']:
                            prop_value = str(value)
                            if len(prop_value) > 30:
                                prop_value = prop_value[:27] + "..."
                            simplified_props[key] = prop_value
                    if simplified_props:
                        prop_text = f" (属性: {simplified_props})"
                
                # 构建关系标识，用于去重
                rel_identifier = f"{start_name}-{rel_type}-{end_name}"
                if rel_identifier not in processed_relationships:
                    processed_relationships.add(rel_identifier)
                    relationships_text += f"- {start_name} -{rel_type}-> {end_name}{prop_text}\n"
                    displayed_count += 1
        
        elif isinstance(rel, (tuple, list)) and len(rel) >= 3:
            # 列表或元组格式
            try:
                source = str(rel[0]) if len(rel) > 0 else '未知节点'
                rel_type = str(rel[1]) if len(rel) > 1 else 'RELATES_TO'
                target = str(rel[2]) if len(rel) > 2 else '未知节点'
                
                # 尝试通过实体ID映射获取真实名称
                if source.startswith('节点_') and len(source) > 4:
                    source_id = source[4:]
                    source = entity_id_to_name.get(source_id, source)
                if target.startswith('节点_') and len(target) > 4:
                    target_id = target[4:]
                    target = entity_id_to_name.get(target_id, target)
                
                # 构建关系标识，用于去重
                rel_identifier = f"{source}-{rel_type}-{target}"
                if rel_identifier not in processed_relationships:
                    processed_relationships.add(rel_identifier)
                    relationships_text += f"- {source} -{rel_type}-> {target}\n"
                    displayed_count += 1
            except Exception as e:
                if app.logger:
                    app.logger.error(f"处理列表格式关系时出错: {str(e)}")
                # 简单处理异常情况
                relationships_text += f"- 关系格式错误: {str(rel)[:50]}\n"
                displayed_count += 1
        else:
            # 其他格式
            relationships_text += f"- 未知关系: {str(rel)[:50]}\n"
            displayed_count += 1
    
    # 如果还有未显示的关系，添加提示
    remaining_relationships = len(processed_relationships) - displayed_count
    if remaining_relationships > 0:
        relationships_text += f"- ... 还有 {remaining_relationships} 个关系未显示\n"
    
    formatted_text += entities_text + "\n"
    formatted_text += relationships_text + "\n"
    
    # 添加实体和关系统计
    formatted_text += f"统计: 共找到 {entity_count} 个实体, {relationship_count} 个原始关系, {len(processed_relationships)} 个去重后关系\n"
    
    # 添加关系类型分布
    if relationship_types:
        formatted_text += "\n关系类型分布:\n"
        for rel_type, count in sorted_relationship_types[:10]:  # 显示前10种关系类型
            formatted_text += f"- {rel_type}: {count} 条\n"
    
    return formatted_text

# 统一使用format_neo4j_results函数处理所有Neo4j查询结果

def generic_neo4j_query(question, depth=3):
    """
    通用Neo4j知识图谱查询函数
    根据问题内容实时提取关键词，执行知识图谱查询并返回结构化结果
    包含完整的关键词提取、数据库检索和结果处理逻辑
    """
    app.logger.info(f"执行通用Neo4j查询，问题: {question[:100]}...")
    handler = None
    
    try:
        # 1. 获取Neo4j处理器
        handler = get_neo4j_handler()
        
        # 2. 验证连接
        if not handler or not hasattr(handler, 'driver') or not handler.driver:
            app.logger.error("无法连接到Neo4j处理器或驱动")
            return {"success": False, "error": "Neo4j处理器未初始化或连接失败"}
        
        app.logger.info("Neo4j处理器连接成功，开始提取关键词")
        
        # 3. 增强的关键词提取逻辑
        keywords = []
        
        # 3.1 提取书名（形如《书名》）
        import re
        book_titles = re.findall(r'《([^》]+)》', question)
        if book_titles:
            for title in book_titles:
                keywords.append(title.strip())
                # 只添加不带书名号的版本，避免重复和无效格式
                # keywords.append(f"《{title}》")  # 移除带书名号的版本
        
        # 3.2 提取常见实体和主题词
        common_entities = {
            '人物': ['作者', '作家', '诗人', '画家', '科学家', '哲学家', '政治家', '军事家'],
            '时间': ['朝代', '时期', '年代', '世纪', '年', '月', '日'],
            '地点': ['国家', '城市', '地区', '地点', '地方'],
            '作品': ['小说', '诗歌', '散文', '戏剧', '著作', '作品'],
            '概念': ['理论', '思想', '主义', '概念', '方法', '技术']
        }
        
        # 检查问题中是否包含这些实体类型词，并尝试提取相关实体
        for entity_type, entity_words in common_entities.items():
            for word in entity_words:
                if word in question:
                    # 提取实体类型词附近的内容作为潜在实体
                    # 使用简单的方法查找实体类型词附近的可能实体
                    parts = question.split(word)
                    for i, part in enumerate(parts[:-1]):
                        # 从当前部分末尾提取可能的实体名称（向前查找）
                        end_part = part.strip()
                        if end_part:
                            # 尝试提取最后一个词或短语
                            potential_entity = ''.join([c for c in end_part.split()[-1] if c.isalnum() or c in '《》'])
                            if len(potential_entity) > 1 and potential_entity not in keywords:
                                keywords.append(potential_entity)
        
        # 3.3 从问题中提取核心名词
        # 移除常见停用词和标点符号
        stop_words = ['的', '是', '在', '了', '和', '与', '或', '但', '而', '因为', '所以', '如果', '那么', 
                     '什么', '哪些', '如何', '为什么', '怎样', '是否', '能否', '可以', '一个', '一些']
        question_clean = question
        for stop_word in stop_words:
            question_clean = question_clean.replace(stop_word, ' ')
        
        # 移除标点符号
        import string
        for char in string.punctuation + '，。！？；：""\'\'（）【】':
            question_clean = question_clean.replace(char, ' ')
        
        # 分词并提取可能的关键词
        potential_keywords = question_clean.split()
        for word in potential_keywords:
            # 过滤掉太短的词和纯数字
            if len(word) > 1 and not word.isdigit() and word not in keywords:
                # 优先考虑长度较长的词作为关键词
                if len(word) >= 2:
                    keywords.append(word)
        
        # 3.4 尝试使用jieba分词作为备选（如果已安装）
        if not keywords or len(keywords) < 2:
            try:
                import jieba
                # 使用精确模式分词
                jieba_words = jieba.cut(question, cut_all=False)
                jieba_keywords = [word for word in jieba_words if len(word) >= 2 and word not in stop_words]
                # 去重并添加到关键词列表
                for word in jieba_keywords[:10]:  # 最多添加10个
                    if word not in keywords:
                        keywords.append(word)
                app.logger.info(f"使用jieba分词增强关键词: {jieba_keywords[:5]}")
            except ImportError:
                app.logger.warning("jieba库未安装，跳过增强分词")
        
        # 3.5 优化关键词，确保质量
        # 过滤掉无效的关键词
        valid_keywords = []
        for kw in keywords:
            # 过滤掉太短或只包含标点符号的关键词
            clean_kw = kw.strip('《》"\'()[]{}，。！？；：')
            if len(clean_kw) >= 2 and not clean_kw.isspace():
                if clean_kw not in valid_keywords:
                    valid_keywords.append(clean_kw)
        
        keywords = valid_keywords[:5]  # 最多使用5个关键词
        app.logger.info(f"[关键词优化] 优化后的有效关键词: {keywords}")
        
        # 3.6 如果没有提取到关键词，提供默认行为
        if not keywords:
            # 使用整个问题的主要部分作为关键词
            main_question = ''.join([c for c in question if c.isalnum() or c in '《》'])
            keywords = [main_question[:10]]  # 取前10个字符作为关键词
            app.logger.warning(f"[关键词提取] 未能提取有效关键词，使用默认关键词: {keywords}")
        
        # 确保关键词日志总是显示
        print(f"[调试] 从问题中提取的关键词: {keywords}")  # 控制台输出用于调试
        app.logger.info(f"[关键词提取] 从问题 '{question[:20]}...' 中提取的关键词: {keywords}")
        app.logger.info(f"[关键词详情] 关键词数量: {len(keywords)}, 关键词列表: {keywords}")
        
        # 4. 增强的多策略查询执行
        app.logger.info(f"使用提取的关键词 '{keywords}' 进行深度为{depth}的查询...")
        
        # 检查问题是否包含特定关系查询模式（如同一出版社、同一作者等）
        specific_relation_patterns = [
            '同一出版社', '同出版社', '同一作者', '同作者', '同一个作者',
            '相同出版社', '相同作者', '同属', '共同', '一起出版'
        ]
        
        # 根据问题内容选择合适的查询策略
        needs_specific_relation_query = any(pattern in question for pattern in specific_relation_patterns)
        
        # 定义查询策略 - 添加查询深度配置和重试机制
        if needs_specific_relation_query and hasattr(handler, 'comprehensive_neo4j_query_for_mingchao'):
            # 对于特定关系查询，优先使用增强的综合查询
            app.logger.info(f"检测到特定关系查询模式，优先使用comprehensive_neo4j_query_for_mingchao")
            query_strategies = [
                {"name": "综合增强查询", "func": lambda: handler.comprehensive_neo4j_query_for_mingchao(keywords, depth=2)},
                {"name": "仅关键词查询(深度2)", "func": lambda: handler.query_expanded_context(keywords, depth=2)},
                {"name": "实体关系查询", "func": lambda: handler.query_entities_and_relationships(keywords)}
            ]
        else:
            # 普通查询策略 - 优先使用能返回完整关系的方法
            query_strategies = [
                {"name": "扩展上下文查询(深度2)", "func": lambda: handler.query_expanded_context(keywords, depth=2)},
                {"name": "实体关系查询", "func": lambda: handler.query_entities_and_relationships(keywords)},
                {"name": "仅实体查询", "func": lambda: handler.query_entities(keywords) if hasattr(handler, 'query_entities') else None}
            ]
        
        result = None
        success_strategy = None
        
        # 尝试所有查询策略
        for strategy in query_strategies:
            try:
                app.logger.info(f"尝试查询策略: {strategy['name']}，使用关键词: {keywords}")
                strategy_result = strategy['func']()
                
                # 更详细的结果检查
                if strategy_result and isinstance(strategy_result, dict):
                    entities_count = len(strategy_result.get('entities', []))
                    relationships_count = len(strategy_result.get('relationships', []))
                    app.logger.info(f"策略 '{strategy['name']}' 结果: {entities_count}个实体, {relationships_count}个关系")
                    
                    # 即使实体和关系数量为0，也标记为成功执行，以便记录关键词信息
                    result = strategy_result
                    success_strategy = strategy['name']
                    app.logger.info(f"查询策略 '{strategy['name']}' 成功执行")
                    
                    # 如果有实体或关系，立即返回
                    if entities_count > 0 or relationships_count > 0:
                        break
            except Exception as e:
                app.logger.warning(f"查询策略 '{strategy['name']}' 失败: {str(e)}")
                continue
        
        # 如果所有策略都失败，创建空结果
        if result is None:
            app.logger.error("所有查询策略均失败，返回空结果")
            result = {'entities': [], 'relationships': []}
        
        # 5. 增强的结果处理和转换
        app.logger.info(f"查询结果类型: {type(result)}")
        
        # 转换结果为标准格式
        if not isinstance(result, dict):
            app.logger.error(f"查询结果格式错误，期望字典类型，得到: {type(result)}")
            try:
                import json
                if isinstance(result, str):
                    result = json.loads(result)
                else:
                    result = {'entities': [], 'relationships': []}
            except:
                result = {'entities': [], 'relationships': []}
        
        # 6. 提取和验证实体和关系数据
        entities = result.get('entities', [])
        relationships = result.get('relationships', [])
        context = result.get('context', '')
        
        # 增强的类型转换和验证
        if isinstance(entities, str):
            try:
                import ast
                entities = ast.literal_eval(entities)
            except:
                app.logger.warning(f"实体数据解析失败，重置为空列表")
                entities = []
        
        if isinstance(relationships, str):
            try:
                import ast
                relationships = ast.literal_eval(relationships)
            except:
                app.logger.warning(f"关系数据解析失败，重置为空列表")
                relationships = []
        
        # 最终类型检查
        if not isinstance(entities, list):
            app.logger.warning(f"实体数据类型错误，期望列表，得到: {type(entities)}")
            entities = []
        
        if not isinstance(relationships, list):
            app.logger.warning(f"关系数据类型错误，期望列表，得到: {type(relationships)}")
            relationships = []
        
        # 7. 实体数据增强和标准化
        enhanced_entities = []
        for entity in entities:
            if isinstance(entity, dict):
                standardized_entity = {}
                
                # 处理不同名称的主键字段
                if 'name' in entity:
                    standardized_entity['name'] = entity['name']
                elif 'title' in entity:
                    standardized_entity['name'] = entity['title']
                elif 'id' in entity:
                    standardized_entity['name'] = str(entity['id'])
                elif 'label' in entity:
                    standardized_entity['name'] = entity['label']
                else:
                    first_value = next(iter(entity.values()), 'Unknown')
                    standardized_entity['name'] = str(first_value)
                
                # 添加类型和属性
                standardized_entity['type'] = entity.get('type', entity.get('labels', ['Unknown'])[0] if isinstance(entity.get('labels'), list) else 'Unknown')
                standardized_entity['properties'] = {k: v for k, v in entity.items() if k not in ['name', 'title', 'id', 'type', 'labels']}
                
                enhanced_entities.append(standardized_entity)
            elif isinstance(entity, str):
                enhanced_entities.append({'name': entity, 'type': 'Unknown', 'properties': {}})
        
        # 8. 关系数据标准化 - 修复版本
        enhanced_relationships = []
        all_relationships_raw = result.get('all_relationships', []) or result.get('relationships', [])

        for rel in all_relationships_raw:
            if isinstance(rel, dict):
                # 处理不同的关系格式
                if 'start_node' in rel and isinstance(rel['start_node'], dict):
                    # 完整格式的关系数据
                    start_node = rel['start_node']
                    end_node = rel['end_node']
                    
                    # 提取节点名称
                    start_props = start_node.get('properties', {})
                    end_props = end_node.get('properties', {})
                    
                    start_name = start_props.get('name', 
                                start_props.get('title', 
                                f"实体_{start_node.get('elementId', 'unknown')[:8]}"))
                    end_name = end_props.get('name', 
                              end_props.get('title', 
                              f"实体_{end_node.get('elementId', 'unknown')[:8]}"))
                    
                    standardized_rel = {
                        'source': start_name,
                        'target': end_name,
                        'type': rel.get('type', 'RELATED_TO'),
                        'properties': rel.get('properties', {}),
                        # 保留完整节点信息
                        'start_node': start_node,
                        'end_node': end_node,
                        'elementId': rel.get('elementId')
                    }
                else:
                    # 简单格式的关系数据
                    standardized_rel = {
                        'source': rel.get('source', rel.get('start_node', 'Unknown')),
                        'target': rel.get('target', rel.get('end_node', 'Unknown')),
                        'type': rel.get('type', 'RELATED_TO'),
                        'properties': rel.get('properties', {})
                    }
                
                enhanced_relationships.append(standardized_rel)
            elif isinstance(rel, (tuple, list)) and len(rel) >= 3:
                enhanced_relationships.append({
                    'source': rel[0],
                    'type': rel[1],
                    'target': rel[2],
                    'properties': {}
                })
        
        # 添加调试信息来验证关系数据
        app.logger.info(f"原始关系数据: {result.get('relationships', [])}")
        app.logger.info(f"完整关系数据: {result.get('all_relationships', [])}")
        app.logger.info(f"关系数据类型: {type(result.get('relationships'))}")
        
        # 检查关系数据的结构
        if result.get('relationships'):
            sample_rel = result['relationships'][0]
            if isinstance(sample_rel, dict):
                app.logger.info(f"关系样本结构: {list(sample_rel.keys())}")
                app.logger.info(f"关系样本内容: {sample_rel}")
            else:
                app.logger.info(f"关系样本类型: {type(sample_rel)}")
                app.logger.info(f"关系样本内容: {sample_rel}")
        
        # 详细记录查询结果信息
        app.logger.info(f"查询到 {len(enhanced_entities)} 个实体和 {len(enhanced_relationships)} 个关系")
        # 记录实体样本
        if enhanced_entities:
            entity_samples = [f"{e['name']}({e['type']})" for e in enhanced_entities[:3]]
            app.logger.info(f"实体样本: {', '.join(entity_samples)}")
        # 记录关系样本
        if enhanced_relationships:
            rel_samples = [f"{r['source']} -{r['type']}-> {r['target']}" for r in enhanced_relationships[:3]]
            app.logger.info(f"关系样本: {', '.join(rel_samples)}")
        
        # 9. 构建结构化返回结果
        final_result = {
            "success": True,
            "question": question,
            "keywords": keywords,
            "entities": enhanced_entities,
            "relationships": enhanced_relationships,
            "all_relationships": all_relationships_raw,  # 保留原始关系数据
            "context": context,
            "timestamp": datetime.now().isoformat(),
            "query_info": {
                "depth": depth,
                "keyword_count": len(keywords),
                "entity_count": len(enhanced_entities),
                "relationship_count": len(enhanced_relationships),
                "success_strategy": success_strategy,
                "result_quality": "high" if len(enhanced_entities) > 3 and len(enhanced_relationships) > 3 else "medium" if len(enhanced_entities) > 0 else "low"
            }
        }
        
        # 详细记录查询完成信息
        result_quality = final_result['query_info']['result_quality']
        app.logger.info(f"通用Neo4j查询完成，查询策略: {success_strategy}, 结果质量: {result_quality}")
        app.logger.info(f"返回结果包含 {len(enhanced_entities)} 个实体和 {len(enhanced_relationships)} 个关系")
        app.logger.info(f"查询深度: {depth}, 关键词数量: {len(keywords)}")
        return final_result
        
    except Exception as e:
        app.logger.error(f"通用Neo4j查询异常: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return {"success": False, "error": f"查询过程中发生异常: {str(e)}"}
    finally:
        # 确保日志记录查询结束
        app.logger.info("通用Neo4j查询流程结束")

# 已移除针对特定书籍的查询函数，全部使用通用查询逻辑

# 独立的enhance_llm_with_comprehensive_graphrag函数
def enhance_llm_with_comprehensive_graphrag(question, depth=2):
    """
    使用全面Neo4j查询结果增强大模型问答
    强化问答能力，充分利用所有关系信息
    支持可配置的查询深度和结果质量自适应
    """
    app.logger.info(f"使用全面GraphRAG信息增强大模型问答，查询深度: {depth}")
    
    try:
        # 1. 执行Neo4j查询获取图谱信息 - 使用新的通用查询函数
        app.logger.info(f"调用generic_neo4j_query获取知识图谱数据，问题: {question[:50]}...")
        query_result = generic_neo4j_query(question, depth)
        
        app.logger.info(f"generic_neo4j_query返回结果类型: {type(query_result)}")
        
        # 检查查询结果是否有效
        if not isinstance(query_result, dict) or not query_result.get('success'):
            error_msg = query_result.get('error', 'Neo4j查询失败') if isinstance(query_result, dict) else '查询结果格式错误'
            app.logger.error(f"Neo4j查询失败: {error_msg}")
            return {"success": False, "error": error_msg}
        
        # 2. 从查询结果中提取实体和关系数据
        entities = query_result.get('entities', [])
        relationships = query_result.get('relationships', [])
        context = query_result.get('context', '')
        keywords = query_result.get('keywords', [])
        query_info = query_result.get('query_info', {})
        success_strategy = query_info.get('success_strategy', 'unknown')
        result_quality = query_info.get('result_quality', 'unknown')
        
        # 详细记录提取的图谱数据信息
        app.logger.info(f"从查询结果中提取到 {len(entities)} 个实体和 {len(relationships)} 个关系，结果质量: {result_quality}")
        app.logger.info(f"使用查询策略: {success_strategy}，查询深度: {depth}")
        # 记录实体样本
        if entities:
            entity_names = [e.get('name', str(e)) for e in entities[:3]]
            app.logger.info(f"实体样本: {', '.join(entity_names)}")
        # 记录关系样本
        if relationships:
            rel_details = []
            for rel in relationships[:3]:
                source = rel.get('source', 'Unknown')
                rel_type = rel.get('type', 'RELATED')
                target = rel.get('target', 'Unknown')
                rel_details.append(f"{source} -{rel_type}-> {target}")
            app.logger.info(f"关系样本: {', '.join(rel_details)}")
        
        # 3. 根据结果质量调整处理策略
        # 动态调整显示限制
        max_entities = 50 if result_quality != 'low' else 20
        max_relationships = 100 if result_quality != 'low' else 30
        
        # 4. 构建标准格式的结果对象供format_neo4j_results使用
        result_for_formatting = {
            'entities': entities,
            'relationships': relationships
        }
        
        # 5. 使用format_neo4j_results函数进行格式化，设置合理的显示限制
        graph_info = format_neo4j_results(result_for_formatting, max_entities=max_entities, max_relationships=max_relationships)
        
        # 6. 如果有内置的context，也加入到图谱信息中
        if context and len(context) > 10:
            graph_info = f"{graph_info}\n\n### 原始上下文信息 ###\n{context[:2000] if len(context) > 2000 else context}"
        
        # 详细记录格式化后的图谱信息
        app.logger.info(f"构建完成的图谱信息长度: {len(graph_info)} 字符")
        # 统计图谱信息中的实体和关系数量
        entity_count_in_text = graph_info.count('- 实体_') if graph_info else 0
        relationship_count_in_text = graph_info.count('->') if graph_info else 0
        app.logger.info(f"图谱信息中包含约 {entity_count_in_text} 个实体描述和 {relationship_count_in_text} 个关系描述")
        if graph_info:
            # 显示图谱信息的前100和后100字符，以便更全面了解
            app.logger.info(f"图谱信息前100字符: {graph_info[:100]}...")
            if len(graph_info) > 200:
                app.logger.info(f"图谱信息后100字符: ...{graph_info[-100:]}")
        else:
            app.logger.warning("图谱信息为空")
        
        # 7. 根据结果质量构建不同的提示词模板
        if result_quality == 'high':
            # 高质量结果使用深度分析模板
            enhanced_prompt = f"""
### 任务说明 ###
你是一个基于深度知识图谱的专业问答专家。请严格基于以下从Neo4j知识图谱中提取的深度关系信息，全面、深入地回答用户的问题。

关键指令：
1. **强制使用知识图谱信息**：必须优先且充分利用提供的所有知识图谱关系信息回答问题
2. **层次化关系分析**：分析直接关系和多跳(最多{depth}层)关系，构建完整的知识网络
3. **深度关系推理**：基于{depth}层深度的关系网络进行合理推理，发掘实体间的间接联系和隐含关系
4. **精确引用**：明确指出你使用了哪些具体的实体和关系信息来支持你的回答
5. **结构化回答**：按照"核心答案-证据支持-关系网络分析-深度见解"的结构组织回答
6. **相关性评估**：评估不同实体和关系与问题的相关程度，优先使用最相关的信息
7. **完整性保证**：确保回答全面覆盖问题的各个方面，不遗漏重要信息

### 知识图谱查询信息 ###
- 查询关键词: {', '.join(keywords)}
- 结果质量: {result_quality}
- 使用策略: {success_strategy}
- 查询深度: {depth}

### 知识图谱深度关系信息 ###
{graph_info if graph_info else "未找到相关知识图谱信息"}

### 用户问题 ###
{question}

请基于上述深度知识图谱关系信息，提供一个准确、全面、有深度的专业回答。在回答中展示你对知识网络的深入理解和推理能力。
"""
        elif result_quality == 'medium':
            # 中等质量结果使用平衡模板
            enhanced_prompt = f"""
### 任务说明 ###
你是一个结合知识图谱和通用知识的问答专家。请基于以下知识图谱信息和你的通用知识，全面回答用户的问题。

关键指令：
1. **优先使用知识图谱**：首先尝试使用提供的知识图谱关系信息回答问题
2. **补充通用知识**：当知识图谱信息不足时，可合理利用你的通用知识进行补充
3. **清晰区分来源**：明确区分哪些信息来自知识图谱，哪些来自通用知识
4. **结构化回答**：按照"核心答案-证据支持-补充信息"的结构组织回答
5. **相关性分析**：分析知识图谱中实体和关系与问题的相关程度
6. **完整性保证**：确保回答覆盖问题的主要方面

### 知识图谱查询信息 ###
- 查询关键词: {', '.join(keywords)}
- 结果质量: {result_quality}
- 使用策略: {success_strategy}
- 查询深度: {depth}

### 知识图谱关系信息 ###
{graph_info if graph_info else "未找到相关知识图谱信息"}

### 用户问题 ###
{question}

请基于上述知识图谱关系信息和你的通用知识，提供一个准确、全面的回答。在回答中明确区分信息来源。
"""
        else:
            # 低质量结果使用通用知识为主的模板
            enhanced_prompt = f"""
### 任务说明 ###
你是一个基于通用知识的问答专家，同时会参考有限的知识图谱信息。请基于以下信息回答用户的问题。

关键指令：
1. **主要使用通用知识**：主要基于你的通用知识回答问题
2. **参考有限图谱信息**：如果提供的知识图谱信息与问题相关，可作为补充参考
3. **明确标注来源**：如果使用了知识图谱信息，请明确标注
4. **合理推理**：基于有限信息进行合理推理
5. **开放性说明**：如果知识图谱信息不足，请说明你主要基于通用知识回答

### 知识图谱查询信息 ###
- 查询关键词: {', '.join(keywords)}
- 结果质量: {result_quality}
- 查询深度: {depth}

### 有限的知识图谱信息 ###
{graph_info if graph_info else "未找到相关知识图谱信息"}

### 用户问题 ###
{question}

请基于你的通用知识和上述有限的知识图谱信息，提供一个准确、合理的回答。
"""
        
        # 8. 构建增强的返回结果
        # 记录增强上下文的详细信息
        app.logger.info(f"增强上下文构建完成，模板类型: {'高质量深度分析' if result_quality == 'high' else '中等质量平衡' if result_quality == 'medium' else '低质量通用知识'}")
        app.logger.info(f"增强上下文总长度: {len(enhanced_prompt)} 字符")
        
        # 添加原始查询结果，包含完整的节点和关系信息
        raw_results = {
            "entities": entities,  # 完整的实体列表
            "relationships": relationships,  # 完整的关系列表
            "keywords": keywords,  # 使用的关键词
            "query_depth": depth,  # 查询深度
            "result_quality": result_quality,  # 结果质量
            "success_strategy": success_strategy  # 成功的查询策略
        }
        
        return {
            "success": True,
            "enhanced_context": enhanced_prompt,
            "graph_info": graph_info,
            "query_result": query_result,
            "raw_results": raw_results,  # 添加原始查询结果
            "graph_info_detail": {
                "keyword_count": len(keywords),
                "entity_count": len(entities),
                "relationship_count": len(relationships),
                "result_quality": result_quality,
                "success_strategy": success_strategy,
                "query_depth": depth,
                "timestamp": query_result.get('timestamp', datetime.now().isoformat())
            },
            "raw_sample": {
                "keywords": keywords,
                "sample_entities": entities[:3] if len(entities) > 0 else [],
                "sample_relationships": relationships[:3] if len(relationships) > 0 else []
            }
        }
        
    except Exception as e:
        app.logger.error(f"增强函数处理异常: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return {
            "success": False, 
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }

# 临时文件上传API端点
@app.route('/api/ai/temp-file', methods=['POST'])
def upload_temp_file():
    try:
        # 先清理过期文件
        cleanup_expired_temp_files()
        
        data = request.json
        filename = data.get('filename')
        content = data.get('content')
        
        if not filename or not content:
            return jsonify({
                'success': False,
                'error': '缺少文件名或文件内容'
            }), 400
        
        # 生成唯一令牌
        token = str(uuid.uuid4())
        
        # 存储临时文件信息
        TEMP_FILE_STORAGE[token] = {
            'filename': filename,
            'content': content,
            'timestamp': time.time()
        }
        
        app.logger.info(f"临时文件上传成功: {filename}, 令牌: {token}")
        
        return jsonify({
            'success': True,
            'token': token,
            'message': '临时文件上传成功'
        })
    except Exception as e:
        app.logger.error(f"临时文件上传失败: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/')
def hello():
    return jsonify({
        'message': 'Welcome to AI Q&A Service',
        'endpoints': [
            '/api/ai/chat-with-anythingllm - AI问答接口',
            '/api/ai/upload-document - 文件上传接口（已优化，自动嵌入）',
            '/api/ai/embed-file - 手动嵌入单个文件接口',
            '/api/ai/test-embedding - 嵌入测试接口（用于调试）',
            '/api/ai/update-embeddings - 批量更新文档嵌入接口',
            '/api/ai/workspace-info - 获取工作区信息'
        ]
    })

# 调用AnythingLLM服务的函数
def call_anythingllm_ai(prompt, history=None, workspace_id=ANYTHINGLLM_WORKSPACE_ID, api_key=ANYTHINGLLM_API_KEY, model_slug=None, temp_file_tokens=None, depth=3):
    try:
        # 详细记录开始调用
        app.logger.info(f"===== 开始调用AnythingLLM =====")
        app.logger.info(f"使用配置: BASE_URL={ANYTHINGLLM_BASE_URL}, WORKSPACE_ID={workspace_id}")
        
        # 使用v1版本API路径和workspace ID
        url = f"{ANYTHINGLLM_BASE_URL}/api/v1/workspace/{workspace_id}/chat"
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "accept": "application/json"
        }
        
        # 构建基础prompt
        full_prompt = prompt
        
        # 在深度思考模式下（workspace_id为5或训练版工作区ID），使用Neo4j知识图谱增强上下文
        is_deep_thinking_mode = workspace_id == '5' or workspace_id == '715ff10f-5ea8-4ef2-8d37-6ae40856acbb'
        
        # 添加详细日志记录当前模式和Neo4j状态
        app.logger.info(f"当前workspace_id: {workspace_id}, 是否深度思考模式: {is_deep_thinking_mode}, NEO4J_ENABLED: {NEO4J_ENABLED}")
        
        if is_deep_thinking_mode and NEO4J_ENABLED:
            try:
                app.logger.info("===== 开始知识图谱增强(GraphRAG) =====")
                
                # 使用enhance_llm_with_comprehensive_graphrag函数进行知识图谱增强
                app.logger.info(f"调用enhance_llm_with_comprehensive_graphrag，问题: {prompt[:50]}..., 查询深度: {depth}")
                enhanced_result = enhance_llm_with_comprehensive_graphrag(prompt, depth=depth)
                
                app.logger.info(f"enhance_llm_with_comprehensive_graphrag返回结果类型: {type(enhanced_result)}")
                app.logger.info(f"enhance_llm_with_comprehensive_graphrag返回结果: {enhanced_result}")
                
                if isinstance(enhanced_result, dict) and enhanced_result.get("success", False):
                    # 获取增强后的上下文
                    enhanced_prompt_text = enhanced_result.get("enhanced_context", "")
                    graph_info_detail = enhanced_result.get("graph_info_detail", {})
                    
                    # 详细记录知识图谱增强结果
                    app.logger.info(f"知识图谱增强成功！增强上下文长度: {len(enhanced_prompt_text)} 字符")
                    app.logger.info(f"图谱数据统计 - 实体: {graph_info_detail.get('entity_count', 0)}, 关系: {graph_info_detail.get('relationship_count', 0)}")
                    app.logger.info(f"结果质量: {graph_info_detail.get('result_quality', 'unknown')}, 查询策略: {graph_info_detail.get('success_strategy', 'unknown')}")
                    app.logger.info(f"查询深度: {graph_info_detail.get('query_depth', depth)}, 关键词数量: {graph_info_detail.get('keyword_count', 0)}")
                    
                    # 记录增强上下文的关键部分
                    app.logger.info(f"增强上下文前100字符: {enhanced_prompt_text[:100]}...")
                    if len(enhanced_prompt_text) > 200:
                        app.logger.info(f"增强上下文后100字符: ...{enhanced_prompt_text[-100:]}")
                    
                    # 使用增强的prompt替换原始prompt
                    full_prompt = enhanced_prompt_text
                    app.logger.info("已使用知识图谱增强的prompt替换原始prompt，启用深度思考模式")
                else:
                    # 知识图谱查询失败，使用优化的降级prompt
                    app.logger.warning(f"知识图谱增强返回非预期结果或失败")
                    if isinstance(enhanced_result, dict):
                        app.logger.warning(f"成功状态: {enhanced_result.get('success', False)}, 错误: {enhanced_result.get('error', '未知')}")
                    else:
                        app.logger.warning(f"返回结果类型: {type(enhanced_result)}，内容: {str(enhanced_result)[:200]}...")
                    
                    full_prompt = f"### 深度思考模式(降级) ###\n请基于你已有的知识库，以深度思考的方式详细回答以下问题。尽管无法访问知识图谱增强数据，但仍请提供全面、有深度的分析和见解。\n\n{prompt}"
                    app.logger.info("已使用优化的降级模式prompt，未启用知识图谱增强")
                    
            except Exception as e:
                app.logger.error(f"===== 知识图谱增强(GraphRAG)异常 =====")
                app.logger.error(f"异常类型: {type(e).__name__}, 错误信息: {str(e)}")
                import traceback
                app.logger.error(f"错误堆栈: {traceback.format_exc()}")
                
                # 即使知识图谱处理失败，也继续使用原始prompt，并标记为深度思考模式
                full_prompt = f"### 深度思考模式(降级) ###\n请基于你已有的知识库，以深度思考的方式详细回答以下问题。尽管无法访问知识图谱增强数据，但仍请提供全面、有深度的分析和见解。\n\n{prompt}"
                app.logger.info("异常情况下已切换到降级模式，未使用知识图谱增强")
        else:
            # 非深度思考模式或Neo4j未启用，保持原始prompt
            app.logger.info(f"未使用知识图谱增强: 深度思考模式={is_deep_thinking_mode}, Neo4j启用={NEO4J_ENABLED}")
            full_prompt = prompt
        
        # 如果有临时文件令牌，添加文件内容到prompt
        if temp_file_tokens and isinstance(temp_file_tokens, list):
            # 先清理过期文件
            cleanup_expired_temp_files()
            
            file_contents = []
            for token in temp_file_tokens:
                if token in TEMP_FILE_STORAGE:
                    file_info = TEMP_FILE_STORAGE[token]
                    file_content = file_info['content']
                    filename = file_info['filename']
                    
                    # 更新文件访问时间
                    import time  # 确保在函数内部可以访问time模块
                    file_info['timestamp'] = time.time()
                    
                    # 处理不同类型的文件
                    processed_content = file_content
                    
                    # 检测Word文档
                    if (filename.lower().endswith('.docx') or 
                        (file_content.startswith('data:application/vnd.openxmlformats-officedocument.wordprocessingml.document;base64,'))):
                        try:
                            app.logger.info(f"处理Word文档: {filename}")
                            processed_content = extract_text_from_base64_docx(file_content)
                            app.logger.info(f"成功提取Word文档文本内容")
                        except Exception as e:
                            app.logger.error(f"解析Word文档失败: {str(e)}")
                            processed_content = f"[Word文档内容无法解析: {str(e)}]"
                    
                    # 限制文件内容长度，防止prompt过长
                    max_content_length = 5000
                    if len(processed_content) > max_content_length:
                        processed_content = processed_content[:max_content_length] + "...（内容过长，已截断）"
                    
                    file_contents.append(f"\n\n[附件文件: {filename}]\n{processed_content}")
            
            if file_contents:
                file_context = "\n".join(file_contents)
                full_prompt = f"用户上传的附件内容如下：{file_context}\n\n请基于上述附件内容回答用户的问题：{prompt}"
        
        # 请求数据包含message和mode字段
        data = {
            "message": full_prompt,
            "mode": "chat"
        }
        
        # 详细记录API调用信息
        app.logger.info(f"===== 准备调用AnythingLLM API =====")
        app.logger.info(f"URL: {url}")
        app.logger.info(f"工作区ID: {workspace_id}")
        app.logger.info(f"提示词长度: {len(full_prompt)} 字符")
        app.logger.info(f"用户问题前100字符: {prompt[:100]}...")
        app.logger.info(f"知识图谱查询深度: {depth if is_deep_thinking_mode else '未启用'}")
        
        import time
        start_time = time.time()
        
        # 发送POST请求
        app.logger.info(f"正在发送POST请求到AnythingLLM服务...")
        
        # 添加超时设置
        response = requests.post(url, headers=headers, json=data, timeout=30)  # 30秒超时
        
        end_time = time.time()
        response_time = round(end_time - start_time, 2)
        
        # 详细记录响应状态
        app.logger.info(f"===== 收到AnythingLLM响应 =====")
        app.logger.info(f"状态码: {response.status_code}")
        app.logger.info(f"响应时间: {response_time} 秒")
        app.logger.info(f"响应头: {dict(response.headers)}")
        
        # 检查响应内容
        try:
            response_text = response.text
            app.logger.info(f"响应内容长度: {len(response_text)} 字符")
            app.logger.info(f"响应内容前300字符: {response_text[:300]}...")
            if len(response_text) > 500:
                app.logger.info(f"响应内容后200字符: ...{response_text[-200:]}")
        except Exception as e:
            app.logger.error(f"获取响应内容时出错: {str(e)}")
        
        # 尝试解析JSON
        try:
            result = response.json()
            app.logger.info(f"成功解析JSON响应")
            app.logger.info(f"响应数据结构: {list(result.keys())}")
        except Exception as e:
            app.logger.error(f"解析JSON响应时出错: {str(e)}")
            raise Exception(f"解析AnythingLLM响应失败: {str(e)}")
        
        response.raise_for_status()
        app.logger.info(f"响应数据有效性验证通过")
        
        # 提取textResponse字段作为回答
        answer = result.get('textResponse', '')
        app.logger.info(f"===== 回答处理详情 =====")
        app.logger.info(f"原始回答长度: {len(answer)} 字符")
        
        # 处理可能的思考过程标记
        if answer:
            # 记录原始回答的开头部分
            app.logger.info(f"原始回答前100字符: {answer[:100]}...")
            
            # 处理思考过程标记
            if '</think>' in answer:
                app.logger.info("检测到思考过程标记，进行处理")
                parts = answer.split('</think>')
                if len(parts) > 1:
                    processed_answer = parts[1].strip()
                    app.logger.info(f"思考过程长度: {len(parts[0])} 字符")
                else:
                    processed_answer = answer.replace('</think>', '').strip()
            else:
                app.logger.info("未检测到思考过程标记")
                processed_answer = answer
            
            # 检查处理后的回答
            if not processed_answer:
                processed_answer = "抱歉，我暂时无法回答这个问题，请尝试提供更多上下文信息。"
                app.logger.warning("处理后的回答为空，使用默认回复")
            else:
                app.logger.info(f"处理后回答长度: {len(processed_answer)} 字符")
                app.logger.info(f"处理后回答前100字符: {processed_answer[:100]}...")
            
            # 记录完整处理结果
            app.logger.info(f"===== 回答处理完成 =====")
            app.logger.info(f"总响应时间: {response_time} 秒")
            app.logger.info(f"知识图谱增强: {'已启用' if is_deep_thinking_mode and NEO4J_ENABLED else '未启用'}")
            
            # 提取sources信息（如果存在）
            sources = []
            if 'sources' in result and isinstance(result['sources'], list):
                for source in result['sources']:
                    if isinstance(source, dict) and 'title' in source:
                        # 添加text字段以及所有其他可用字段
                        source_item = {
                            'title': source['title'],
                            'id': source.get('id', ''),
                            'url': source.get('url', ''),
                            'docAuthor': source.get('docAuthor', ''),
                            'published': source.get('published', ''),
                            'text': source.get('text', '')  # 添加text字段
                        }
                        # 添加其他可能存在的字段
                        for key, value in source.items():
                            if key not in source_item:
                                source_item[key] = value
                        sources.append(source_item)
            app.logger.info(f"提取到{len(sources)}个引用源，包含完整信息")
            
            # 返回处理后的回答、sources信息和Neo4j数据（如果有）
            return {
                "answer": processed_answer,
                "sources": sources,
                "neo4j_data": enhanced_result.get("raw_results", None) if is_deep_thinking_mode and NEO4J_ENABLED and 'enhanced_result' in locals() and isinstance(enhanced_result, dict) else None
            }
        else:
            app.logger.warning("AnythingLLM返回的textResponse为空")
            return "抱歉，我暂时无法回答这个问题，请尝试提供更多上下文信息。"
        
    except requests.exceptions.ConnectionError as ce:
        app.logger.error(f"===== 连接错误 =====")
        app.logger.error(f"连接AnythingLLM服务失败: {str(ce)}")
        app.logger.error(f"请确认: 1. AnythingLLM服务正在运行 2. 地址 {ANYTHINGLLM_BASE_URL} 正确 3. 网络连接正常")
        return f"抱歉，无法连接到AnythingLLM服务，请检查服务是否正常运行在 {ANYTHINGLLM_BASE_URL}。"
    except requests.exceptions.Timeout as te:
        app.logger.error(f"===== 超时错误 =====")
        app.logger.error(f"AnythingLLM服务请求超时: {str(te)}")
        app.logger.error(f"请求超时，请检查AnythingLLM服务负载和网络连接")
        return f"抱歉，AnythingLLM服务响应超时，请稍后再试。"
    except requests.exceptions.HTTPError as he:
        app.logger.error(f"===== HTTP错误 =====")
        app.logger.error(f"HTTP错误: {str(he)}")
        app.logger.error(f"状态码: {response.status_code if 'response' in locals() else '未知'}")
        return f"抱歉，AnythingLLM服务返回错误: HTTP {response.status_code if 'response' in locals() else '未知'}。"
    except Exception as e:
        app.logger.error(f"===== 未知错误 =====")
        app.logger.error(f"调用AnythingLLM时出错: {str(e)}")
        app.logger.error(f"错误类型: {type(e).__name__}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return f"抱歉，调用AnythingLLM时发生未知错误: {str(e)}。"

# AI问答接口 - 与AnythingLLM交互
@app.route('/api/ai/chat-with-anythingllm', methods=['POST'])
def chat_with_anythingllm():
    try:
        # 获取请求数据
        data = request.get_json()
        if not data:
            return jsonify({'success': False, 'error': '缺少请求参数'}), 400
        
        # 检查必要参数
        question = data.get('question', '').strip()
        if not question:
            return jsonify({'success': False, 'error': '问题不能为空'}), 400
        
        # 获取历史记录（可选）
        history = data.get('history', [])
        
        # 获取临时文件令牌（可选）
        temp_file_tokens = data.get('temp_file_tokens', [])
        
        # 获取查询深度参数（可选）
        depth = data.get('depth', 2)  # 默认查询深度为2，确保获取深度为2的路径上所有相关节点和关系
        if not isinstance(depth, int) or depth < 1 or depth > 5:
            app.logger.warning(f"无效的查询深度参数: {depth}，将使用默认值2")
            depth = 2
        
        # 获取模型相关参数（支持从前端切换模型）
        model_slug = data.get('modelSlug')  # 训练模型的slug
        model_name = data.get('modelName', '未指定')  # 模型名称
        use_trained_model = data.get('useTrainedModel', False)  # 是否使用训练模型
        
        # 如果使用训练模型且提供了model_slug，则使用该slug作为workspace_id
        if use_trained_model and model_slug:
            # 使用slug作为workspace_id
            workspace_id = model_slug
            app.logger.info(f"使用深度思考模型: {model_name}, workspace_id={workspace_id} (slug={model_slug})")
        else:
            # 使用slug作为workspace_id
            workspace_id = model_slug if model_slug else ANYTHINGLLM_WORKSPACE_ID
            app.logger.info(f"使用默认模型: {model_name}, workspace_id={workspace_id} (slug={model_slug})")
        
        # 记录收到的问题和历史记录
        app.logger.info(f"收到问题: {question}")
        app.logger.info(f"历史记录数量: {len(history)}")
        
        # 调用AnythingLLM进行回答
        answer = call_anythingllm_ai(question, history, workspace_id, temp_file_tokens=temp_file_tokens, depth=depth)
        
        # 处理调用结果，提取answer、neo4j_data和sources信息
        if isinstance(answer, dict):
            response_answer = answer.get('answer', answer)
            neo4j_data = answer.get('neo4j_data')
            sources = answer.get('sources', [])
        else:
            response_answer = answer
            neo4j_data = None
            sources = []
            
        # 返回结果，包含Neo4j数据库查询信息和引用来源
        return jsonify({
            'success': True,
            'answer': response_answer,
            'response': {
                'answer': response_answer,
                'content': response_answer
            },
            'neo4j_data': neo4j_data,  # 添加Neo4j数据库查询的详细信息
            'sources': sources  # 添加引用来源信息
        })
    except Exception as e:
        app.logger.error(f"AI问答API错误: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# 文件上传接口 - 调用AnythingLLM上传文档
@app.route('/api/ai/upload-document', methods=['POST'])
def upload_document():
    try:
        # 记录完整的请求信息，包括表单和文件信息
        app.logger.info(f"收到文件上传请求，请求表单: {list(request.form.keys())}, 文件: {list(request.files.keys())}")
        
        # 检查是否包含文件
        if 'file' not in request.files:
            app.logger.error("文件上传失败: 请求中没有'file'字段")
            return jsonify({'success': False, 'error': '未找到上传文件，需要使用file字段'}), 400
        
        file = request.files['file']
        app.logger.info(f"文件对象信息: 文件名={file.filename}, 内容类型={file.content_type}, 大小={file.content_length}")
        
        # 检查文件是否为空
        if file.filename == '':
            app.logger.error("文件上传失败: 文件名不能为空")
            return jsonify({'success': False, 'error': '未选择文件'}), 400
        
        # 检查文件大小
        file.seek(0, 2)  # 移动到文件末尾
        file_size = file.tell()  # 获取文件大小
        file.seek(0)  # 重置到文件开头
        
        if file_size == 0:
            app.logger.error(f"文件上传失败: 文件 {file.filename} 为空")
            return jsonify({'success': False, 'error': '上传的文件为空，请选择有效的文件'}), 400
            
        app.logger.info(f"文件大小检查通过: {file_size} 字节")
        
        # 获取模型信息
        model_id = request.form.get('modelId', '3')
        
        # 只支持id为3和5的模型的文件嵌入功能
        if model_id not in ['3', '5']:
            app.logger.warning(f"拒绝不支持的模型ID: {model_id}")
            return jsonify({'success': False, 'error': '当前只支持图书管理(模型ID:3)和图书管理（训练版）(模型ID:5)的文件嵌入功能'}), 403
        
        # 根据模型ID选择对应的工作区信息
        workspace_name = '图书管理'  # 默认使用图书管理工作区
        workspace_slug = '36f38646-a044-4677-9e4e-8a519402c38f'
        if model_id == '5':
            workspace_name = '图书管理（训练版）'  # 深度思考模式使用训练版工作区
            workspace_slug = '715ff10f-5ea8-4ef2-8d37-6ae40856acbb'
        
        app.logger.info(f"收到文件上传请求: 文件名={file.filename}, 模型ID={model_id}, 工作区={workspace_name}")
        
        # 重置文件流指针，确保从头开始读取
        file.stream.seek(0)
        
        # 构建文件上传数据
        files = {'file': (file.filename, file.stream, file.content_type)}
        data = {'addToWorkspaces': workspace_name}  # 添加到指定工作区
        headers = {
            "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
            "accept": "application/json"
        }
        
        # 使用单一的API端点，与参考文件保持一致
        upload_url = f"{ANYTHINGLLM_BASE_URL}/api/v1/document/upload"
        app.logger.info(f"准备上传文件到: {upload_url}")
        
        # 添加超时设置
        upload_response = requests.post(upload_url, headers=headers, files=files, data=data, timeout=30)
        app.logger.info(f"上传响应状态码: {upload_response.status_code}")
        
        # 检查响应状态
        if upload_response.status_code >= 400:
            app.logger.error(f"上传文档失败，状态码: {upload_response.status_code}")
            # 尝试解析错误响应
            try:
                error_data = upload_response.json()
                app.logger.error(f"错误数据: {error_data}")
                return jsonify({
                    'success': False,
                    'error': error_data.get('error', f'上传失败，状态码: {upload_response.status_code}'),
                    'details': error_data,
                    'status_code': upload_response.status_code
                }), upload_response.status_code
            except:
                error_text = upload_response.text
                app.logger.error(f"错误响应内容: {error_text}")
                return jsonify({
                    'success': False,
                    'error': f'上传失败，状态码: {upload_response.status_code}',
                    'response_text': error_text
                }), upload_response.status_code
        
        # 正常处理响应
        app.logger.info(f"文件上传成功，准备解析响应")
        
        # 解析响应
        result = upload_response.json()
        app.logger.info(f"AnythingLLM上传文档响应数据: {result}")
        
        # 自动调用update-embeddings确保文件被嵌入
        try:
            # 从上传结果中构建文件路径
            file_to_embed = None
            
            # 1. 尝试从documents数组中获取title来构建路径
            if 'documents' in result and isinstance(result['documents'], list) and len(result['documents']) > 0:
                document = result['documents'][0]
                if 'title' in document:
                    title = document['title']
                    # 构建custom-documents/title.json路径
                    base_name = os.path.splitext(title)[0]
                    # 使用参考文件中的格式
                    file_to_embed = f"custom-documents/{base_name}.json"
                    app.logger.info(f"从documents数组中提取title构建路径: {file_to_embed}")
            
            # 2. 如果构建失败，使用简化路径
            if not file_to_embed:
                # 从文件名生成简单路径
                base_name = "uploaded_file"
                if 'documents' in result and isinstance(result['documents'], list) and len(result['documents']) > 0:
                    document = result['documents'][0]
                    if 'title' in document:
                        base_name = os.path.splitext(document['title'])[0]
                
                file_to_embed = f"custom-documents/{base_name}.json"
                app.logger.warning(f"使用简化路径: {file_to_embed}")
            
            # 获取系统文件列表
            documents_url = f"{ANYTHINGLLM_BASE_URL}/api/v1/documents"
            documents_headers = {
                "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
                "Content-Type": "application/json",
                "accept": "application/json"
            }
            
            app.logger.info(f"开始获取系统文件列表: {documents_url}")
            documents_response = requests.get(documents_url, headers=documents_headers, timeout=10)
            documents_response.raise_for_status()
            documents_data = documents_response.json()
            
            # 提取custom-documents文件夹下的所有文件信息
            system_files = []
            try:
                # 递归提取文件信息
                def extract_files(items, base_path=""):
                    files = []
                    for item in items:
                        if item['type'] == 'file':
                            full_path = f"{base_path}/{item['name']}" if base_path else item['name']
                            files.append({
                                'path': full_path,
                                'name': item['name'],
                                'id': item.get('id')
                            })
                        elif item['type'] == 'folder' and 'items' in item:
                            folder_path = f"{base_path}/{item['name']}" if base_path else item['name']
                            files.extend(extract_files(item['items'], folder_path))
                    return files
                
                # 从localFiles开始提取
                if 'localFiles' in documents_data and documents_data['localFiles']['type'] == 'folder':
                    system_files = extract_files(documents_data['localFiles']['items'])
                
                app.logger.info(f"成功获取系统文件列表，共{len(system_files)}个文件")
            except Exception as e:
                app.logger.error(f"解析文件列表出错: {str(e)}")
            
            # 从系统文件列表中查找最匹配的文件
            final_file_path = file_to_embed
            if system_files:
                # 尝试根据文件名部分匹配
                file_name = os.path.basename(file_to_embed)
                if file_name.endswith('.json'):
                    base_name = file_name[:-5]  # 移除.json扩展名
                    
                    # 查找包含该基础名称的文件
                    for system_file in system_files:
                        if system_file['path'].startswith('custom-documents/') and system_file['path'].endswith('.json'):
                            if base_name in system_file['name']:
                                final_file_path = system_file['path']
                                app.logger.info(f"找到匹配的系统文件: {base_name} -> {system_file['path']}")
                                break
            
            # 调用嵌入API
            embed_url = f"{ANYTHINGLLM_BASE_URL}/api/v1/workspace/{workspace_slug}/update-embeddings"
            embed_payload = {
                "adds": [final_file_path],
                "deletes": []
            }
              
            app.logger.info(f"准备调用update-embeddings进行文件嵌入: {final_file_path}")
            try:
                embed_response = requests.post(
                    embed_url, 
                    headers={
                        "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
                        "Content-Type": "application/json",
                        "accept": "application/json"
                    },
                    json=embed_payload,
                    timeout=30
                )
                embed_response.raise_for_status()
                embed_result = embed_response.json()
                app.logger.info(f"文件嵌入成功: {embed_result}")
            except Exception as embed_api_error:
                app.logger.error(f"嵌入API调用失败: {str(embed_api_error)}")
                # 记录错误但不抛出异常，允许继续返回结果
                result['embedding_error'] = f"API调用失败: {str(embed_api_error)}"
                embed_result = {'status': 'failed', 'error': str(embed_api_error)}
            
            # 将嵌入结果添加到返回数据中
            result['embedding_info'] = embed_result
            result['used_file_path'] = final_file_path
        except Exception as embed_error:
            app.logger.error(f"自动嵌入文件失败: {str(embed_error)}")
            import traceback
            app.logger.error(f"嵌入错误堆栈: {traceback.format_exc()}")
            # 记录错误但不影响上传结果返回
            result['embedding_error'] = str(embed_error)
        
        return jsonify({
            'success': True,
            'message': '文件上传成功，已尝试自动嵌入',
            'data': result
        })
        
    except requests.exceptions.ConnectionError as ce:
        app.logger.error(f"连接AnythingLLM服务失败: {str(ce)}")
        return jsonify({
            'success': False,
            'error': '连接AnythingLLM服务失败，请检查服务是否正常运行'
        }), 500
    except requests.exceptions.HTTPError as he:
        app.logger.error(f"上传文档HTTP错误: {str(he)}")
        # 确保upload_response变量存在性检查
        if 'upload_response' in locals():
            try:
                error_data = upload_response.json()
                return jsonify({
                    'success': False,
                    'error': error_data.get('error', '上传失败'),
                    'details': error_data
                }), upload_response.status_code
            except:
                return jsonify({
                    'success': False,
                    'error': f'上传失败: {str(he)}'
                }), 500
        return jsonify({
            'success': False,
            'error': f'上传失败: {str(he)}'
        }), 500
    except Exception as e:
        app.logger.error(f"上传文档时出错: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return jsonify({
            'success': False,
            'error': f'上传失败: {str(e)}'
        }), 500

# 嵌入测试接口 - 用于快速测试嵌入功能
@app.route('/api/ai/test-embedding', methods=['POST'])
def test_embedding():
    try:
        # 获取请求数据
        data = request.get_json()
        if not data or 'file_path' not in data:
            return jsonify({'success': False, 'error': '请求体必须包含file_path字段'}), 400
        
        file_path = data.get('file_path')
        
        app.logger.info(f"收到嵌入测试请求: 文件路径={file_path}")
        
        # 直接调用update-embeddings接口
        embed_url = f"{ANYTHINGLLM_BASE_URL}/api/v1/workspace/图书管理/update-embeddings"
        headers = {
            "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "adds": [file_path],
            "deletes": []
        }
        
        app.logger.info(f"发送嵌入请求到: {embed_url}")
        response = requests.post(embed_url, headers=headers, json=payload)
        response.raise_for_status()
        
        return jsonify({
            'success': True,
            'message': '文件嵌入测试成功',
            'data': response.json()
        })
        
    except Exception as e:
        app.logger.error(f"嵌入测试失败: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return jsonify({
            'success': False,
            'error': str(e),
            'stack_trace': traceback.format_exc()
        }), 500

# 手动嵌入单个文件接口 - 直接指定文件路径进行嵌入
@app.route('/api/ai/embed-file', methods=['POST'])
def embed_file():
    try:
        # 获取请求数据
        data = request.get_json()
        if not data or 'file_path' not in data:
            return jsonify({'success': False, 'error': '请求体必须包含file_path字段'}), 400
        
        file_path = data.get('file_path')
        if not file_path:
            return jsonify({'success': False, 'error': 'file_path不能为空'}), 400
        
        # 只支持id为3和5的模型的文件嵌入功能
        model_id = request.args.get('modelId', '3')
        if model_id not in ['3', '5']:
            return jsonify({'success': False, 'error': '当前只支持图书管理(模型ID:3)和图书管理（训练版）(模型ID:5)的文件嵌入功能'}), 403
        
        # 根据模型ID选择对应的工作区slug
        workspace_slug = '36f38646-a044-4677-9e4e-8a519402c38f'  # 默认使用图书管理工作区
        if model_id == '5':
            workspace_slug = '715ff10f-5ea8-4ef2-8d37-6ae40856acbb'  # 深度思考模式使用训练版工作区
        
        app.logger.info(f"收到手动嵌入文件请求: 文件路径={file_path}, 工作区={workspace_slug}")
        
        # 构建嵌入请求
        embed_url = f"{ANYTHINGLLM_BASE_URL}/api/v1/workspace/{workspace_slug}/update-embeddings"
        headers = {
            "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
            "Content-Type": "application/json",
            "accept": "application/json"
        }
        
        # 构建请求体
        payload = {
            "adds": [file_path],
            "deletes": []
        }
        
        app.logger.info(f"准备调用update-embeddings进行文件嵌入")
        
        # 调用API进行嵌入
        response = requests.post(embed_url, headers=headers, json=payload)
        app.logger.info(f"文件嵌入响应状态码: {response.status_code}")
        response.raise_for_status()
        
        # 解析响应
        result = response.json()
        app.logger.info(f"文件嵌入成功: {result}")
        
        return jsonify({
            'success': True,
            'message': '文件嵌入成功',
            'data': result,
            'embedded_file': file_path
        })
        
    except requests.exceptions.ConnectionError as ce:
        app.logger.error(f"连接AnythingLLM服务失败: {str(ce)}")
        return jsonify({
            'success': False,
            'error': '连接AnythingLLM服务失败，请检查服务是否正常运行'
        }), 500
    except requests.exceptions.HTTPError as he:
        app.logger.error(f"文件嵌入HTTP错误: {str(he)}")
        try:
            error_data = response.json()
            return jsonify({
                'success': False,
                'error': error_data.get('error', '嵌入失败'),
                'details': error_data
            }), response.status_code
        except:
            return jsonify({
                'success': False,
                'error': f'嵌入失败: {str(he)}'
            }), 500
    except Exception as e:
        app.logger.error(f"文件嵌入时出错: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return jsonify({
            'success': False,
            'error': f'嵌入失败: {str(e)}'
        }), 500

# 更新工作区文档嵌入接口 - 调用AnythingLLM的update-embeddings API
@app.route('/api/ai/update-embeddings', methods=['POST'])
def update_embeddings():
    try:
        # 获取请求数据
        data = request.get_json()
        if not data:
            return jsonify({'success': False, 'error': '请求体不能为空，需要提供adds和deletes数组'}), 400
        
        # 验证必要的参数
        adds = data.get('adds', [])
        deletes = data.get('deletes', [])
        
        # 只支持id为3和5的模型的文档嵌入更新功能
        model_id = request.args.get('modelId', '3')
        if model_id not in ['3', '5']:
            return jsonify({'success': False, 'error': '当前只支持图书管理(模型ID:3)和图书管理（训练版）(模型ID:5)的文档嵌入更新功能'}), 403
        
        # 根据模型ID选择对应的工作区slug
        workspace_slug = '36f38646-a044-4677-9e4e-8a519402c38f'  # 默认使用图书管理工作区
        if model_id == '5':
            workspace_slug = '715ff10f-5ea8-4ef2-8d37-6ae40856acbb'  # 深度思考模式使用训练版工作区
        
        app.logger.info(f"收到更新嵌入请求: 添加文档数={len(adds)}, 删除文档数={len(deletes)}, 工作区={workspace_slug}")
        
        # 第一步：获取系统文件列表 - 按照正确流程
        documents_url = "http://localhost:3001/api/v1/documents"
        documents_headers = {
            "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
            "Content-Type": "application/json",
            "accept": "application/json"
        }
        
        app.logger.info(f"开始获取系统文件列表: {documents_url}")
        documents_response = requests.get(documents_url, headers=documents_headers)
        documents_response.raise_for_status()
        documents_data = documents_response.json()
        
        # 提取custom-documents文件夹下的所有文件信息
        system_files = []
        try:
            # 遍历文档树结构，提取custom-documents下的文件
            def extract_files(items, base_path=""):
                files = []
                for item in items:
                    if item['type'] == 'file':
                        # 构建完整路径: 文件夹名/文件名
                        full_path = f"{base_path}/{item['name']}" if base_path else item['name']
                        files.append({
                            'path': full_path,
                            'name': item['name'],
                            'id': item.get('id')
                        })
                    elif item['type'] == 'folder' and 'items' in item:
                        # 递归处理子文件夹
                        folder_path = f"{base_path}/{item['name']}" if base_path else item['name']
                        files.extend(extract_files(item['items'], folder_path))
                return files
            
            # 从localFiles开始提取
            if 'localFiles' in documents_data and documents_data['localFiles']['type'] == 'folder':
                system_files = extract_files(documents_data['localFiles']['items'])
            
            app.logger.info(f"成功获取系统文件列表，共{len(system_files)}个文件")
        except Exception as e:
            app.logger.error(f"解析文件列表出错: {str(e)}")
            # 继续执行，即使解析失败也尝试使用原始请求
        
        # 第二步：根据系统文件列表，构造正确的嵌入请求体
        # 这里需要确保adds和deletes中的文件路径格式正确
        # 格式应为: custom-documents/文件名-UUID.json
        
        # 构建正确格式的文件路径列表
        formatted_adds = []
        for file_path in adds:
            # 检查是否已经是正确格式
            if isinstance(file_path, str) and file_path.startswith('custom-documents/') and file_path.endswith('.json'):
                formatted_adds.append(file_path)
            else:
                # 尝试从系统文件列表中查找匹配的文件
                for system_file in system_files:
                    if system_file['path'].startswith('custom-documents/') and system_file['path'].endswith('.json'):
                        # 如果文件名部分匹配（忽略UUID）
                        original_name = file_path
                        if original_name.endswith('.json'):
                            original_name = original_name[:-5]  # 移除.json扩展名
                        
                        # 检查系统文件名是否包含原始文件名（在-UUID之前的部分）
                        if original_name in system_file['name']:
                            formatted_adds.append(system_file['path'])
                            app.logger.info(f"找到匹配的系统文件: {original_name} -> {system_file['path']}")
                            break
                
                # 如果没有找到匹配，保留原始路径但记录警告
                if file_path not in formatted_adds:
                    formatted_adds.append(file_path)
                    app.logger.warning(f"未在系统文件列表中找到匹配文件: {file_path}，将使用原始路径")
        
        # 对于deletes，也进行相同的格式检查
        formatted_deletes = []
        for file_path in deletes:
            if isinstance(file_path, str) and file_path.startswith('custom-documents/') and file_path.endswith('.json'):
                formatted_deletes.append(file_path)
            else:
                # 尝试从系统文件列表中查找匹配的文件
                for system_file in system_files:
                    if system_file['path'].startswith('custom-documents/') and system_file['path'].endswith('.json'):
                        original_name = file_path
                        if original_name.endswith('.json'):
                            original_name = original_name[:-5]  # 移除.json扩展名
                        
                        if original_name in system_file['name']:
                            formatted_deletes.append(system_file['path'])
                            app.logger.info(f"找到匹配的系统删除文件: {original_name} -> {system_file['path']}")
                            break
                
                if file_path not in formatted_deletes:
                    formatted_deletes.append(file_path)
                    app.logger.warning(f"未在系统文件列表中找到匹配删除文件: {file_path}，将使用原始路径")
        
        app.logger.info(f"格式化后的嵌入请求: adds={formatted_adds}, deletes={formatted_deletes}")
        
        # 构建AnythingLLM的update-embeddings API URL
        url = f"{ANYTHINGLLM_BASE_URL}/api/v1/workspace/{workspace_slug}/update-embeddings"
        headers = {
            "Authorization": f"Bearer {ANYTHINGLLM_API_KEY}",
            "Content-Type": "application/json",
            "accept": "application/json"
        }
        
        # 构建请求体
        payload = {
            "adds": formatted_adds,
            "deletes": formatted_deletes
        }
        
        app.logger.info(f"准备调用AnythingLLM更新嵌入: URL={url}")
        
        # 调用AnythingLLM更新嵌入
        response = requests.post(url, headers=headers, json=payload)
        app.logger.info(f"AnythingLLM更新嵌入响应状态码: {response.status_code}")
        response.raise_for_status()
        
        # 解析响应
        result = response.json()
        app.logger.info(f"AnythingLLM更新嵌入响应数据: {result}")
        
        return jsonify({
            'success': True,
            'message': '文档嵌入更新成功',
            'data': result,
            'system_files_count': len(system_files),
            'formatted_adds': formatted_adds,
            'formatted_deletes': formatted_deletes
        })
        
    except requests.exceptions.ConnectionError as ce:
        app.logger.error(f"连接AnythingLLM服务失败: {str(ce)}")
        return jsonify({
            'success': False,
            'error': '连接AnythingLLM服务失败，请检查服务是否正常运行'
        }), 500
    except requests.exceptions.HTTPError as he:
        app.logger.error(f"更新嵌入HTTP错误: {str(he)}")
        try:
            error_data = response.json()
            return jsonify({
                'success': False,
                'error': error_data.get('error', '更新失败'),
                'details': error_data
            }), response.status_code
        except:
            return jsonify({
                'success': False,
                'error': f'更新失败: {str(he)}'
            }), 500
    except Exception as e:
        app.logger.error(f"更新嵌入时出错: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return jsonify({
            'success': False,
            'error': f'更新失败: {str(e)}'
        }), 500

# 获取工作区信息接口
@app.route('/api/ai/workspace-info', methods=['GET'])
def get_workspace_info():
    try:
        # 返回所有可用工作区信息，支持正常模式和深度思考模式
        workspaces = [
            {
                "id": 3,
                "name": "图书管理",
                "slug": "36f38646-a044-4677-9e4e-8a519402c38f",
                "vectorTag": None,
                "createdAt": "2025-10-16T11:59:22.020Z",
                "openAiTemp": 0.7,
                "openAiHistory": 20,
                "lastUpdatedAt": "2025-10-16T11:59:22.020Z",
                "openAiPrompt": "Given the following conversation, relevant context, and a follow up question, reply with an answer to the current question the user is asking. Return only your response to the question given the above information following the users instructions as needed.",
                "similarityThreshold": 0.25,
                "chatProvider": "ollama",
                "chatModel": "deepseek-r1:7b",
                "topN": 4,
                "chatMode": "chat",
                "pfpFilename": None,
                "agentProvider": None,
                "agentModel": None,
                "queryRefusalResponse": "There is no relevant information in this workspace to answer your query.",
                "vectorSearchMode": "default",
                "threads": []
            },
            {
                "id": 5,
                "name": "图书管理（训练版）",
                "slug": "715ff10f-5ea8-4ef2-8d37-6ae40856acbb",
                "vectorTag": None,
                "createdAt": "2025-10-17T08:29:16.887Z",
                "openAiTemp": 0.7,
                "openAiHistory": 20,
                "lastUpdatedAt": "2025-10-17T08:29:16.887Z",
                "openAiPrompt": "Given the following conversation, relevant context, and a follow up question, reply with an answer to the current question the user is asking. Return only your response to the question given the above information following the users instructions as needed.",
                "similarityThreshold": 0.25,
                "chatProvider": "ollama",
                "chatModel": "deepseek-r1:7b",
                "topN": 4,
                "chatMode": "chat",
                "pfpFilename": None,
                "agentProvider": None,
                "agentModel": None,
                "queryRefusalResponse": "There is no relevant information in this workspace to answer your query.",
                "vectorSearchMode": "default",
                "threads": []
            }
        ]
        
        # 支持通过参数获取特定工作区信息
        model_id = request.args.get('modelId')
        if model_id:
            # 查找指定ID的工作区
            for workspace in workspaces:
                if str(workspace['id']) == model_id:
                    return jsonify({
                        'success': True,
                        'workspace': workspace
                    })
        
        # 默认返回所有工作区和默认工作区
        return jsonify({
            'success': True,
            'workspaces': workspaces,
            'workspace': workspaces[0]  # 默认返回第一个工作区
        })
    except Exception as e:
        app.logger.error(f"获取工作区信息错误: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

if __name__ == '__main__':
    # 启动Flask应用，监听在8081端口
    app.run(debug=True, host='0.0.0.0', port=8081)