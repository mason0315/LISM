import re
import logging
from neo4j import GraphDatabase
from typing import Dict, List, Tuple, Set, Optional

# 配置日志
logger = logging.getLogger(__name__)

class Neo4jHandler:
    def __init__(self, uri: str, user: str, password: str):
        """初始化Neo4j连接"""
        self.uri = uri
        self.user = user
        self.password = password
        self.driver = None
        
    def connect(self):
        """建立数据库连接"""
        try:
            self.driver = GraphDatabase.driver(self.uri, auth=(self.user, self.password))
            # 测试连接
            self.driver.verify_connectivity()
            logger.info("成功连接到Neo4j数据库")
            return True
        except Exception as e:
            logger.error(f"连接Neo4j数据库失败: {str(e)}")
            return False
    
    def close(self):
        """关闭数据库连接"""
        if self.driver:
            self.driver.close()
            logger.info("已关闭Neo4j数据库连接")
    
    def extract_keywords(self, question: str) -> List[str]:
        """
        从问题中提取关键词
        简单实现：移除停用词，提取可能的实体词
        """
        # 基本停用词列表
        stop_words = {'的', '了', '和', '是', '在', '有', '我', '你', '他', '她', '它', 
                     '这', '那', '们', '之', '与', '而', '或', '一个', '一些', '这个', 
                     '那个', '吗', '呢', '啊', '呀', '吧', '嗨', '哦', '嗯', '哼',
                     '请', '请问', '可以', '能否', '如何', '怎样', '什么', '哪里', '何时'}
        
        # 移除标点符号
        question = re.sub(r'[\s+\-*/(),.!?:;"]', ' ', question)
        
        # 分词并过滤停用词
        words = [word.strip() for word in question.split() if word.strip() and word.strip() not in stop_words]
        
        # 返回非停用词的关键词，优先选择较长的词
        keywords = sorted(words, key=len, reverse=True)[:5]  # 取最长的5个词作为关键词
        logger.info(f"从问题 '{question}' 中提取的关键词: {keywords}")
        return keywords
    
    def query_entities_and_relationships(self, keywords: List[str]) -> Dict:
        """
        根据关键词查询Neo4j数据库中的实体、关系和属性
        返回结构化的查询结果
        """
        if not self.driver:
            logger.error("未连接到Neo4j数据库")
            return {}
        
        result = {
            'entities': [],
            'relationships': [],
            'context': ""
        }
        
        try:
            with self.driver.session() as session:
                # 使用关键词搜索相关节点和关系
                entity_ids = set()
                
                for keyword in keywords:
                    # 搜索包含关键词的节点
                    node_query = """
                    MATCH (n)
                    WHERE ANY(prop IN keys(n) WHERE toString(n[prop]) CONTAINS $keyword)
                    OR n.name CONTAINS $keyword OR n.title CONTAINS $keyword
              
        RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                    """
                    nodes = session.run(node_query, keyword=keyword).data()
                    
                    for record in nodes:
                        node_id = str(record['id'])
                        if node_id not in entity_ids:
                            entity_ids.add(node_id)
                            entity_info = {
                                'elementId': node_id,
                                'labels': record['labels'],
                                'properties': record['properties']
                            }
                            result['entities'].append(entity_info)
                    
                    # 搜索与关键词相关的关系
                    rel_query = """
                    MATCH (n)-[r]->(m)
                    WHERE (ANY(prop IN keys(n) WHERE toString(n[prop]) CONTAINS $keyword)
                    OR ANY(prop IN keys(m) WHERE toString(m[prop]) CONTAINS $keyword))
                    RETURN elementId(n) as n_id, labels(n) as n_labels, properties(n) as n_props,
                           type(r) as r_type, elementId(r) as r_id, properties(r) as r_props,
                           elementId(m) as m_id, labels(m) as m_labels, properties(m) as m_props
                    """
                    relationships = session.run(rel_query, keyword=keyword).data()
                    
                    for record in relationships:
                        # 处理起始节点n
                        n_id = str(record['n_id'])
                        if n_id not in entity_ids:
                            entity_ids.add(n_id)
                            entity_info = {
                                'elementId': n_id,
                                'labels': record['n_labels'],
                                'properties': record['n_props']
                            }
                            result['entities'].append(entity_info)
                        
                        # 处理目标节点m
                        m_id = str(record['m_id'])
                        if m_id not in entity_ids:
                            entity_ids.add(m_id)
                            entity_info = {
                                'elementId': m_id,
                                'labels': record['m_labels'],
                                'properties': record['m_props']
                            }
                            result['entities'].append(entity_info)
                        
                        # 记录关系信息
                        rel_info = {
                            'type': record['r_type'],
                            'start_node': n_id,
                            'end_node': m_id,
                            'properties': record['r_props']
                        }
                        result['relationships'].append(rel_info)
                
                # 生成结构化上下文
                context_parts = []
                context_parts.append("## 知识图谱信息")
                
                # 添加实体信息
                if result['entities']:
                    context_parts.append("\n### 实体信息")
                    for entity in result['entities']:
                        entity_name = entity['properties'].get('name', entity['properties'].get('title', f"实体_{entity['elementId'][:8]}"))
                        labels = ", ".join(entity['labels'])
                        context_parts.append(f"- **{entity_name}** [{labels}]")
                        
                        # 添加关键属性
                        important_props = []
                        for key, value in entity['properties'].items():
                            if key.lower() not in ['id', 'name', 'title', 'label']:
                                if isinstance(value, str) and len(value) > 100:
                                    value = value[:100] + "..."
                                important_props.append(f"{key}: {value}")
                        
                        if important_props:
                            context_parts.append(f"  属性: {', '.join(important_props)}")
                
                # 添加关系信息
                if result['relationships']:
                    context_parts.append("\n### 关系信息")
                    for rel in result['relationships']:
                        # 获取相关节点名称
                        start_node = next((e for e in result['entities'] if e['elementId'] == rel['start_node']), None)
                        end_node = next((e for e in result['entities'] if e['elementId'] == rel['end_node']), None)
                        
                        start_name = start_node['properties'].get('name', start_node['properties'].get('title', f"实体_{start_node['elementId'][:8]}")) if start_node else "未知节点"
                        end_name = end_node['properties'].get('name', end_node['properties'].get('title', f"实体_{end_node['elementId'][:8]}")) if end_node else "未知节点"
                        
                        rel_desc = f"{start_name} -{rel['type']}-> {end_name}"
                        if rel['properties']:
                            props_str = ", ".join([f"{k}: {v}" for k, v in rel['properties'].items()])
                            rel_desc += f" [属性: {props_str}]"
                        
                        context_parts.append(f"- {rel_desc}")
                
                result['context'] = "\n".join(context_parts)
                logger.info(f"Neo4j查询完成，找到 {len(result['entities'])} 个实体和 {len(result['relationships'])} 个关系")
                
        except Exception as e:
            logger.error(f"Neo4j查询失败: {str(e)}")
            result['context'] = f"【警告：知识图谱查询失败，错误信息：{str(e)}】"
        
        return result
    
    def query_expanded_context(self, keywords: List[str], depth: int = 3) -> Dict:
        """
        增强的扩展查询方法 - 确保返回统一格式的完整关系数据
        优化关系查询逻辑，确保all_relationships包含所有必要的节点和关系信息
        """
        if not self.driver:
            logger.error("未连接到Neo4j数据库")
            return {'entities': [], 'relationships': [], 'all_relationships': [], 'context': "数据库未连接"}
        
        result = {
            'entities': [],
            'relationships': [],
            'all_relationships': [],  # 存储完整关系信息
            'context': "",
            'debug_info': {},  # 添加调试信息
            'stats': {}  # 添加统计信息
        }
        
        try:
            with self.driver.session() as session:
                # 1. 查找初始节点 - 使用多种查询策略
                initial_nodes = []
                all_initial_ids = []
                
                for keyword in keywords:
                    if not keyword or not isinstance(keyword, str):
                        continue
                    
                    logger.info(f"搜索关键词: {keyword}")
                    
                    # 多种查询策略，确保找到相关节点
                    queries = [
                        # 1. 精确匹配名称、标题或其他常用属性
                        """
                        MATCH (n)
                        WHERE n.name = $keyword OR n.title = $keyword OR n.书名 = $keyword OR n.label = $keyword
                        RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                        LIMIT 15
                        """,
                        # 2. 包含匹配
                        """
                        MATCH (n)
                        WHERE n.name CONTAINS $keyword OR n.title CONTAINS $keyword OR n.书名 CONTAINS $keyword OR n.label CONTAINS $keyword
                        RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                        LIMIT 15
                        """,
                        # 3. 任意属性匹配
                        """
                        MATCH (n)
                        WHERE ANY(prop IN keys(n) WHERE toString(n[prop]) CONTAINS $keyword)
                        RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                        LIMIT 10
                        """
                    ]
                    
                    for i, query in enumerate(queries):
                        try:
                            nodes = session.run(query, keyword=keyword).data()
                            logger.info(f"查询策略 {i+1} 找到 {len(nodes)} 个节点")
                            
                            for record in nodes:
                                node_id = str(record['id'])
                                if node_id not in all_initial_ids:
                                    all_initial_ids.append(node_id)
                                    initial_nodes.append({
                                        'elementId': node_id,
                                        'labels': record['labels'],
                                        'properties': record['properties']
                                    })
                        except Exception as e:
                            logger.warning(f"查询策略 {i+1} 失败: {str(e)}")
                            continue
                
                result['debug_info']['initial_nodes'] = len(initial_nodes)
                result['debug_info']['initial_ids'] = all_initial_ids
                result['stats']['initial_nodes_count'] = len(initial_nodes)
                
                if not initial_nodes:
                    logger.warning(f"未找到包含关键词 {keywords} 的初始节点")
                    result['context'] = f"未找到与关键词 '{', '.join(keywords)}' 相关的节点"
                    return result
                
                logger.info(f"找到 {len(initial_nodes)} 个初始节点，开始深度 {depth} 查询")
                
                # 2. 执行深度查询 - 优化查询策略
                all_relationships_data = []
                unique_relationship_keys = set()
                
                # 方法1: 使用单跳查询，更简单可靠
                for d in range(1, min(depth, 3) + 1):  # 限制最大深度为3
                    # 对于不同深度使用不同的查询策略
                    if d == 1:
                        # 单跳查询 - 简单直接
                        query = """
                        MATCH (start)-[r]-(end)
                        WHERE elementId(start) IN $initial_ids
                        RETURN 
                            elementId(start) as start_id,
                            labels(start) as start_labels,
                            properties(start) as start_props,
                            type(r) as rel_type,
                            elementId(r) as rel_id,
                            properties(r) as rel_props,
                            elementId(end) as end_id,
                            labels(end) as end_labels,
                            properties(end) as end_props
                        LIMIT 100
                        """
                    else:
                        # 多跳查询 - 递归查找
                        query = """
                        MATCH path = (start)-[*1..%d]-(end)
                        WHERE elementId(start) IN $initial_ids
                        WITH relationships(path) as rels
                        UNWIND rels as r
                        RETURN DISTINCT 
                            elementId(startNode(r)) as start_id,
                            labels(startNode(r)) as start_labels,
                            properties(startNode(r)) as start_props,
                            type(r) as rel_type,
                            elementId(r) as rel_id,
                            properties(r) as rel_props,
                            elementId(endNode(r)) as end_id,
                            labels(endNode(r)) as end_labels,
                            properties(endNode(r)) as end_props
                        LIMIT 100
                        """ % d
                    
                    try:
                        relationships = session.run(query, initial_ids=all_initial_ids).data()
                        logger.info(f"深度 {d} 查询找到 {len(relationships)} 个关系")
                        
                        for rel in relationships:
                            # 创建关系唯一键
                            rel_key = (rel['start_id'], rel['end_id'], rel['rel_id'], rel['rel_type'])
                            if rel_key not in unique_relationship_keys:
                                unique_relationship_keys.add(rel_key)
                                all_relationships_data.append(rel)
                    except Exception as e:
                        logger.warning(f"深度 {d} 查询失败: {str(e)}")
                        continue
                
                # 如果还是没找到关系，尝试更简单的查询
                if not all_relationships_data:
                    logger.info("尝试使用更简单的查询...")
                    simple_query = """
                    MATCH (n)-[r]-(m)
                    WHERE elementId(n) IN $initial_ids OR elementId(m) IN $initial_ids
                    RETURN 
                        elementId(n) as start_id,
                        labels(n) as start_labels,
                        properties(n) as start_props,
                        type(r) as rel_type,
                        elementId(r) as rel_id,
                        properties(r) as rel_props,
                        elementId(m) as end_id,
                        labels(m) as end_labels,
                        properties(m) as end_props
                    LIMIT 50
                    """
                    
                    try:
                        relationships = session.run(simple_query, initial_ids=all_initial_ids).data()
                        logger.info(f"简单查询找到 {len(relationships)} 个关系")
                        
                        for rel in relationships:
                            rel_key = (rel['start_id'], rel['end_id'], rel['rel_id'], rel['rel_type'])
                            if rel_key not in unique_relationship_keys:
                                unique_relationship_keys.add(rel_key)
                                all_relationships_data.append(rel)
                    except Exception as e:
                        logger.error(f"简单查询也失败: {str(e)}")
                
                # 3. 处理查询结果
                entity_dict = {}
                # 添加初始节点
                for node in initial_nodes:
                    entity_dict[node['elementId']] = node
                
                # 处理关系并构建完整关系信息
                relationship_dict = {}
                all_relationships_list = []
                
                for record in all_relationships_data:
                    # 处理起始节点
                    start_id = str(record['start_id'])
                    if start_id not in entity_dict:
                        entity_dict[start_id] = {
                            'elementId': start_id,
                            'labels': record['start_labels'],
                            'properties': record['start_props']
                        }
                    
                    # 处理目标节点
                    end_id = str(record['end_id'])
                    if end_id not in entity_dict:
                        entity_dict[end_id] = {
                            'elementId': end_id,
                            'labels': record['end_labels'],
                            'properties': record['end_props']
                        }
                    
                    # 构建标准关系格式（简单格式）
                    rel_id = str(record['rel_id'])
                    if rel_id not in relationship_dict:
                        relationship_dict[rel_id] = {
                            'type': record['rel_type'],
                            'start_node': start_id,
                            'end_node': end_id,
                            'properties': record['rel_props']
                        }
                    
                    # 构建完整关系信息，包含完整节点数据
                    rel_detail = {
                        'elementId': rel_id,
                        'type': record['rel_type'],
                        'start_node': {
                            'elementId': start_id,
                            'labels': record['start_labels'],
                            'properties': record['start_props']
                        },
                        'end_node': {
                            'elementId': end_id,
                            'labels': record['end_labels'],
                            'properties': record['end_props']
                        },
                        'properties': record['rel_props'],
                        # 添加额外的方便使用的字段
                        'source': record['start_props'].get('name', record['start_props'].get('title', f"实体_{start_id[:8]}")),
                        'target': record['end_props'].get('name', record['end_props'].get('title', f"实体_{end_id[:8]}"))
                    }
                    all_relationships_list.append(rel_detail)
                
                # 更新结果
                result['entities'] = list(entity_dict.values())
                result['relationships'] = list(relationship_dict.values())
                result['all_relationships'] = all_relationships_list  # 确保始终包含完整关系信息
                
                # 更新统计信息
                result['debug_info']['entity_count'] = len(result['entities'])
                result['debug_info']['relationship_count'] = len(result['relationships'])
                result['debug_info']['all_relationships_count'] = len(result['all_relationships'])
                result['stats']['entity_count'] = len(result['entities'])
                result['stats']['relationship_count'] = len(result['relationships'])
                result['stats']['all_relationships_count'] = len(result['all_relationships'])
                
                logger.info(f"扩展查询完成，找到 {len(result['entities'])} 个实体和 {len(result['relationships'])} 个关系")
                
                # 4. 生成上下文
                result['context'] = self._generate_context(result)
                
        except Exception as e:
            error_msg = str(e)
            logger.error(f"Neo4j扩展查询失败: {error_msg}")
            import traceback
            logger.error(f"错误堆栈: {traceback.format_exc()}")
            
            result['context'] = f"【警告：知识图谱查询失败，错误信息：{error_msg}】"
            result['debug_info']['error'] = error_msg
        
        return result
    
    def _generate_context(self, result: Dict) -> str:
        """
        生成上下文信息的辅助方法
        """
        context_parts = []
        context_parts.append("## 知识图谱信息")
        
        # 添加实体信息
        if result['entities']:
            context_parts.append("\n### 实体信息")
            # 限制显示数量，避免上下文过长
            for entity in result['entities'][:50]:
                entity_name = entity['properties'].get('name', 
                             entity['properties'].get('title', 
                             f"实体_{entity['elementId'][:8]}"))
                labels = ", ".join(entity['labels'])
                context_parts.append(f"- **{entity_name}** [{labels}]")
                
                # 添加关键属性
                important_props = []
                for key, value in entity['properties'].items():
                    if key.lower() not in ['id', 'elementid', 'name', 'title', 'label']:
                        if isinstance(value, str) and len(value) > 100:
                            value = value[:100] + "..."
                        important_props.append(f"{key}: {value}")
                
                if important_props:
                    context_parts.append(f"  属性: {', '.join(important_props[:5])}")  # 限制属性数量
            
            if len(result['entities']) > 50:
                context_parts.append(f"  ... 还有 {len(result['entities']) - 50} 个实体未显示")
        
        # 添加关系信息
        if result['relationships']:
            context_parts.append("\n### 关系信息")
            # 限制显示数量，避免上下文过长
            displayed_relations = 0
            max_relations = 100
            
            for rel in result['relationships'][:max_relations]:
                # 获取相关节点名称
                start_node = next((e for e in result['entities'] if e['elementId'] == rel['start_node']), None)
                end_node = next((e for e in result['entities'] if e['elementId'] == rel['end_node']), None)
                
                start_name = start_node['properties'].get('name', 
                            start_node['properties'].get('title', 
                            f"实体_{start_node['elementId'][:8]}")) if start_node else "未知节点"
                end_name = end_node['properties'].get('name', 
                          end_node['properties'].get('title', 
                          f"实体_{end_node['elementId'][:8]}")) if end_node else "未知节点"
                
                rel_desc = f"{start_name} -{rel['type']}-> {end_name}"
                if rel['properties']:
                        # 只显示部分重要属性
                        props_str = ", ".join([f"{k}: {v}" for k, v in list(rel['properties'].items())[:3]])
                        rel_desc += f" [属性: {props_str}]"
                
                context_parts.append(f"- {rel_desc}")
                displayed_relations += 1
            
            if len(result['relationships']) > max_relations:
                context_parts.append(f"  ... 还有 {len(result['relationships']) - max_relations} 个关系未显示")
        
        # 添加统计信息
        context_parts.append(f"\n## 查询统计")
        context_parts.append(f"- 总实体数量: {len(result['entities'])}")
        context_parts.append(f"- 总关系数量: {len(result['relationships'])}")
        
        return "\n".join(context_parts)
    
    def comprehensive_neo4j_query_for_mingchao(self, keywords: List[str], depth: int = 2) -> Dict:
        """
        执行全面的Neo4j查询，获取深度为2的所有相关实体和关系
        包括多种查询类型，确保捕获所有可能的关联信息
        """
        if not self.driver:
            logger.error("未连接到Neo4j数据库")
            return {'entities': [], 'relationships': [], 'all_relationships': [], 'stats': {}}
        
        result = {
            'entities': set(),  # 使用集合避免重复
            'relationships': set(),  # 使用集合避免重复
            'all_relationships': [],  # 存储所有关系详情
            'stats': {}
        }
        
        try:
            with self.driver.session() as session:
                # 1. 精确匹配关键词的节点
                exact_matches = []
                for keyword in keywords:
                    query = """
                    MATCH (n)
                    WHERE ANY(prop IN keys(n) WHERE toString(n[prop]) CONTAINS $keyword)
                    OR n.name CONTAINS $keyword OR n.title CONTAINS $keyword
                    RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                    """
                    matches = session.run(query, keyword=keyword).data()
                    exact_matches.extend(matches)
                
                # 提取初始节点ID
                initial_node_ids = [str(match['id']) for match in exact_matches]
                result['stats']['initial_nodes_count'] = len(initial_node_ids)
                
                if not initial_node_ids:
                    logger.info(f"未找到与关键词 {keywords} 匹配的节点")
                    return result
                
                # 2. 深度扩展查询 - 获取指定深度的所有节点和关系
                # 使用更直接的方式获取所有深度路径的关系
                query = """
                MATCH path = (n)-[*1..%d]-(m)
                WHERE elementId(n) IN $node_ids
                UNWIND relationships(path) as r
                WITH DISTINCT r, startNode(r) as a, endNode(r) as b
                RETURN type(r) as r_type, elementId(r) as r_id,
                       elementId(a) as a_id, labels(a) as a_labels, properties(a) as a_props,
                       elementId(b) as b_id, labels(b) as b_labels, properties(b) as b_props,
                       properties(r) as r_props
                """ % depth
                
                params = {'node_ids': initial_node_ids}
                    
                relationships = session.run(query, **params).data()
                
                for record in relationships:
                    # 处理起始节点a
                    a_id = str(record['a_id'])
                    result['entities'].add(a_id)
                    
                    # 处理目标节点b
                    b_id = str(record['b_id'])
                    result['entities'].add(b_id)
                    
                    # 记录关系
                    rel_id = str(record['r_id'])
                    result['relationships'].add(rel_id)
                    
                    # 存储关系详情（包含完整节点信息）
                    rel_detail = {
                    'elementId': rel_id,
                    'type': record['r_type'],
                    'start_node': {
                        'elementId': a_id,
                        'labels': record['a_labels'],
                        'properties': record['a_props']
                    },
                    'end_node': {
                        'elementId': b_id,
                        'labels': record['b_labels'],
                        'properties': record['b_props']
                    },
                    'properties': record['r_props']
                }
                    result['all_relationships'].append(rel_detail)
                
                logger.info(f"深度查询完成，共找到 {len(relationships)} 个关系")
                
                # 3. 额外查询 - 确保捕获所有类型的关系
                # 查找与关键词相关的所有类型的关系
                for keyword in keywords:
                    query = """
                    MATCH (n)-[r]->(m)
                        WHERE (n.name CONTAINS $keyword OR n.title CONTAINS $keyword OR 
                               m.name CONTAINS $keyword OR m.title CONTAINS $keyword)
                        RETURN DISTINCT type(r) as rel_type, count(r) as count
                    """
                    rel_types = session.run(query, keyword=keyword).data()
                    
                    for rel_type_info in rel_types:
                        rel_type = rel_type_info['rel_type']
                        # 针对每种关系类型进行专门查询
                        specific_query = """
                        MATCH (n)-[r:%s]->(m)
                        WHERE (n.name CONTAINS $keyword OR n.title CONTAINS $keyword OR 
                               m.name CONTAINS $keyword OR m.title CONTAINS $keyword)
                        RETURN elementId(n) as n_id, labels(n) as n_labels, properties(n) as n_props,
                               elementId(r) as r_id, properties(r) as r_props,
                               elementId(m) as m_id, labels(m) as m_labels, properties(m) as m_props
                        """ % rel_type
                        
                        specific_rels = session.run(specific_query, keyword=keyword).data()
                        
                        for record in specific_rels:
                            # 处理节点和关系
                            n_id = str(record['n_id'])
                            result['entities'].add(n_id)
                            
                            m_id = str(record['m_id'])
                            result['entities'].add(m_id)
                            
                            rel_id = str(record['r_id'])
                            result['relationships'].add(rel_id)
                            
                            # 检查关系是否已存在
                            existing_rel = next(
                                (r for r in result['all_relationships'] if r['elementId'] == rel_id),
                                None
                            )
                            
                            if not existing_rel:
                                rel_detail = {
                                'elementId': rel_id,
                                'type': rel_type,
                                'start_node': {
                                    'elementId': n_id,
                                    'labels': record['n_labels'],
                                    'properties': record['n_props']
                                },
                                'end_node': {
                                    'elementId': m_id,
                                    'labels': record['m_labels'],
                                    'properties': record['m_props']
                                },
                                'properties': record['r_props']
                            }
                                result['all_relationships'].append(rel_detail)
                
                # 4. 增强查询：查找具有相同属性值的其他节点
                # 这对于查找"同一出版社"等场景特别有用
                additional_entities = []
                
                # 首先找到初始节点的关键属性值
                key_attributes = ['publisher', '出版社', '作者', 'author', '类型', 'category']
                for node_id in initial_node_ids[:5]:  # 限制处理前5个初始节点
                    query = """
                    MATCH (n)
                    WHERE elementId(n) = $node_id
                    RETURN properties(n) as properties
                    """
                    node_props = session.run(query, node_id=node_id).data()
                    
                    if node_props and node_props[0]['properties']:
                        props = node_props[0]['properties']
                        # 查找具有相同关键属性值的其他节点
                        for attr in key_attributes:
                            if attr in props and props[attr]:
                                attr_value = props[attr]
                                # 查找具有相同属性值的其他节点
                                same_attr_query = """
                                MATCH (n)
                                WHERE n[$attr] = $value AND elementId(n) <> $node_id
                                RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                                LIMIT 50  # 限制返回数量
                                """
                                same_attr_nodes = session.run(
                                    same_attr_query, 
                                    attr=attr, 
                                    value=attr_value,
                                    node_id=node_id
                                ).data()
                                
                                # 添加找到的节点到结果中
                                for node in same_attr_nodes:
                                    node_id_str = str(node['id'])
                                    if node_id_str not in result['entities']:
                                        result['entities'].add(node_id_str)
                                        additional_entities.append(node)
                                        
                                        # 同时查找这些新节点与已有节点之间的关系
                                        relationship_query = """
                                        MATCH (a)-[r]-(b)
                                        WHERE elementId(a) = $new_node_id AND elementId(b) IN $existing_node_ids
                                        RETURN type(r) as r_type, elementId(r) as r_id,
                                               elementId(a) as a_id, labels(a) as a_labels, properties(a) as a_props,
                                               elementId(b) as b_id, labels(b) as b_labels, properties(b) as b_props,
                                               properties(r) as r_props
                                        """
                                        existing_ids = list(result['entities'])[:100]  # 限制已有节点数量
                                        new_relationships = session.run(
                                            relationship_query,
                                            new_node_id=node_id_str,
                                            existing_node_ids=existing_ids
                                        ).data()
                                        
                                        # 添加新关系到结果中
                                        for rel_record in new_relationships:
                                            rel_id = str(rel_record['r_id'])
                                            if rel_id not in result['relationships']:
                                                result['relationships'].add(rel_id)
                                                rel_detail = {
                                                    'elementId': rel_id,
                                                    'type': rel_record['r_type'],
                                                    'start_node': {
                                                        'elementId': str(rel_record['a_id']),
                                                        'labels': rel_record['a_labels'],
                                                        'properties': rel_record['a_props']
                                                    },
                                                    'end_node': {
                                                        'elementId': str(rel_record['b_id']),
                                                        'labels': rel_record['b_labels'],
                                                        'properties': rel_record['b_props']
                                                    },
                                                    'properties': rel_record['r_props']
                                                }
                                                result['all_relationships'].append(rel_detail)
                
                logger.info(f"增强查询完成，找到 {len(additional_entities)} 个额外实体")
                
                # 5. 查询所有节点的详细属性
                entity_list = list(result['entities'])
                detailed_entities = []
                
                for i in range(0, len(entity_list), 50):  # 分批查询
                    batch_ids = entity_list[i:i+50]
                    query = """
                    MATCH (n)
                    WHERE elementId(n) IN $ids
                    RETURN elementId(n) as id, labels(n) as labels, properties(n) as properties
                    """
                    entities = session.run(query, ids=batch_ids).data()
                    detailed_entities.extend(entities)
                
                # 合并额外找到的实体
                for add_entity in additional_entities:
                    if not any(e['id'] == add_entity['id'] for e in detailed_entities):
                        detailed_entities.append(add_entity)
                
                # 更新统计信息
                result['stats'] = {
                    'total_entities': len(result['entities']),
                    'total_relationships': len(result['relationships']),
                    'initial_nodes_count': len(initial_node_ids),
                    'relationship_types': {}
                }
                
                # 统计关系类型
                for rel in result['all_relationships']:
                    rel_type = rel['type']
                    if rel_type not in result['stats']['relationship_types']:
                        result['stats']['relationship_types'][rel_type] = 0
                    result['stats']['relationship_types'][rel_type] += 1
                
                logger.info(f"全面查询完成，共找到 {result['stats']['total_entities']} 个实体和 {result['stats']['total_relationships']} 个关系")
                
        except Exception as e:
            logger.error(f"Neo4j全面查询失败: {str(e)}")
        
        # 使用详细实体信息替换节点ID列表，确保前端正确显示
        entity_ids_list = list(result['entities'])  # 先保存ID列表
        result['entities'] = detailed_entities  # 使用详细实体信息
        result['relationships'] = list(result['relationships'])
        result['raw_entity_ids'] = entity_ids_list  # 保留原始ID列表以便其他用途
        
        return result
    
    def execute_query(self, query: str, parameters: Dict = None) -> List[Dict]:
        """
        执行Cypher查询并返回结果
        """
        if not self.driver:
            logger.error("未连接到Neo4j数据库")
            return []
        
        try:
            with self.driver.session() as session:
                result = session.run(query, parameters or {})
                return [record.data() for record in result]
        except Exception as e:
            logger.error(f"执行查询失败: {str(e)}")
            return []
    
    def format_neo4j_results(self, query_result: Dict, max_relations: int = 500, max_entities: int = 500) -> Dict:
        """
        格式化Neo4j查询结果，完整提取所有实体和关系信息
        """
        formatted_result = {
            'entity_count': query_result['stats'].get('total_entities', 0),
            'relationship_count': query_result['stats'].get('total_relationships', 0),
            'relationship_types': query_result['stats'].get('relationship_types', {}),
            'key_entities': [],
            'key_relationships': []
        }
        
        # 提取关键实体（优先选择名称包含关键词的实体）
        entities_with_names = []
        for rel in query_result['all_relationships']:
            # 处理起始节点
            start_node = rel['start_node']
            entity_name = start_node['properties'].get('name', start_node['properties'].get('title', f"实体_{start_node['elementId'][:8]}"))
            entities_with_names.append({
                'elementId': start_node.get('elementId', start_node.get('id')),
                'name': entity_name,
                'labels': start_node['labels'],
                'properties': start_node['properties']
            })
            
            # 处理目标节点
            end_node = rel['end_node']
            entity_name = end_node['properties'].get('name', end_node['properties'].get('title', f"实体_{end_node['elementId'][:8]}"))
            entities_with_names.append({
                'elementId': end_node['elementId'],
                'name': entity_name,
                'labels': end_node['labels'],
                'properties': end_node['properties']
            })
        
        # 去重并按名称长度排序
        unique_entities = {}
        for entity in entities_with_names:
            if entity['elementId'] not in unique_entities:
                    unique_entities[entity['elementId']] = entity
        
        sorted_entities = sorted(unique_entities.values(), key=lambda x: len(x['name']), reverse=True)
        formatted_result['key_entities'] = sorted_entities[:max_entities]  # 取指定数量的关键实体
        
        # 处理关系，截断过长的关系列表
        relationships = query_result['all_relationships']
        if len(relationships) > max_relations:
            # 优先保留不同类型的关系
            relationship_types = {}
            selected_relationships = []
            
            for rel in relationships:
                rel_type = rel['type']
                if rel_type not in relationship_types:
                    relationship_types[rel_type] = []
                relationship_types[rel_type].append(rel)
            
            # 从每种类型中选择一定数量的关系
            for rel_type, rel_list in relationship_types.items():
                count = min(5, len(rel_list))  # 每种类型最多选5个
                selected_relationships.extend(rel_list[:count])
            
            # 如果还不够，补充更多关系
            remaining_count = max_relations - len(selected_relationships)
            if remaining_count > 0:
                # 过滤掉已选择的关系
                selected_ids = {rel['elementId'] for rel in selected_relationships}
                remaining_rels = [rel for rel in relationships if rel['elementId'] not in selected_ids]
                selected_relationships.extend(remaining_rels[:remaining_count])
            
            formatted_result['key_relationships'] = selected_relationships
        else:
            formatted_result['key_relationships'] = relationships
        
        return formatted_result
    
    def build_comprehensive_context(self, formatted_results: Dict, question: str) -> str:
        """
        构建综合上下文，整合所有查询结果信息
        """
        context_parts = []
        
        # 1. 统计信息
        context_parts.append("## 知识图谱查询统计")
        context_parts.append(f"- 查询到 {formatted_results['entity_count']} 个相关实体")
        context_parts.append(f"- 查询到 {formatted_results['relationship_count']} 个相关关系")
        
        if formatted_results['relationship_types']:
            context_parts.append("- 关系类型分布:")
            sorted_types = sorted(
                formatted_results['relationship_types'].items(), 
                key=lambda x: x[1], 
                reverse=True
            )
            for rel_type, count in sorted_types[:10]:  # 显示前10种关系类型
                context_parts.append(f"  - {rel_type}: {count} 个")
        
        # 2. 关键实体信息
        if formatted_results['key_entities']:
            context_parts.append("\n## 关键实体信息")
            for entity in formatted_results['key_entities'][:15]:  # 最多显示15个实体
                entity_name = entity['name']
                labels = ", ".join(entity['labels'])
                context_parts.append(f"### {entity_name} [{labels}]")
                
                # 添加重要属性
                important_props = []
                for key, value in entity['properties'].items():
                    if key.lower() not in ['id', 'name', 'title']:
                        if isinstance(value, str) and len(value) > 80:
                            value = value[:80] + "..."
                        important_props.append(f"{key}: {value}")
                
                if important_props:
                    context_parts.append("属性:")
                    for prop in important_props[:5]:  # 每个实体最多显示5个属性
                        context_parts.append(f"  - {prop}")
        
        # 3. 关系信息
        if formatted_results['key_relationships']:
            context_parts.append("\n## 实体关系信息")
            context_parts.append("以下是查询到的主要关系:")
            
            # 按关系类型分组
            rels_by_type = {}
            for rel in formatted_results['key_relationships']:
                rel_type = rel['type']
                if rel_type not in rels_by_type:
                    rels_by_type[rel_type] = []
                rels_by_type[rel_type].append(rel)
            
            # 显示每种类型的关系
            for rel_type, rels in rels_by_type.items():
                context_parts.append(f"\n### {rel_type} 关系:")
                
                for rel in rels[:10]:  # 每种类型最多显示10个关系
                    start_name = rel['start_node']['properties'].get('name', 
                                 rel['start_node']['properties'].get('title', 
                                 f"实体_{rel['start_node']['elementId'][:8]}"))
                    end_name = rel['end_node']['properties'].get('name', 
                               rel['end_node']['properties'].get('title', 
                               f"实体_{rel['end_node']['elementId'][:8]}"))
                    
                    rel_desc = f"- {start_name} -> {end_name}"
                    
                    # 添加关系属性
                    if rel['properties']:
                        props_str = ", ".join([f"{k}: {v}" for k, v in rel['properties'].items() if len(str(v)) < 30])
                        if props_str:
                            rel_desc += f" [属性: {props_str}]"
                    
                    context_parts.append(rel_desc)
        
        # 4. 查询问题相关说明
        context_parts.append("\n## 查询说明")
        context_parts.append(f"- 查询基于用户问题: '{question}'")
        context_parts.append(f"- 查询深度: 3")
        context_parts.append("- 以上信息来自知识图谱数据库")
        
        # 连接所有部分
        context = "\n".join(context_parts)
        
        # 如果上下文太长，进行适当截断
        if len(context) > 10000:  # 限制在10000字符以内
            context = context[:9500] + "\n\n[内容过长，已截断]"
        
        return context

# 创建全局Neo4j处理器实例
neo4j_handler = None

def init_neo4j_handler(uri: str, user: str, password: str) -> Neo4jHandler:
    """初始化全局Neo4j处理器"""
    global neo4j_handler
    if not neo4j_handler:
        neo4j_handler = Neo4jHandler(uri, user, password)
        neo4j_handler.connect()
    return neo4j_handler

def get_neo4j_handler() -> Neo4jHandler:
    """获取Neo4j处理器实例"""
    return neo4j_handler

# 示例用法
def test_neo4j_connection():
    """测试Neo4j连接"""
    handler = Neo4jHandler("neo4j://127.0.0.1:7687", "neo4j", "12345678")
    if handler.connect():
        keywords = handler.extract_keywords("图书管理系统的主要功能有哪些？")
        result = handler.query_entities_and_relationships(keywords)
        print(result['context'])
        handler.close()

if __name__ == "__main__":
    test_neo4j_connection()