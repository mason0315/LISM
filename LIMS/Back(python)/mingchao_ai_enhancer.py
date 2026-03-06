import logging
import json
import traceback
import os
import re

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger('mingchao_ai_enhancer')

# 确保logs目录存在
log_dir = 'logs'
if not os.path.exists(log_dir):
    os.makedirs(log_dir)

# 创建文件处理器
file_handler = logging.FileHandler(os.path.join(log_dir, 'ai_enhancer.log'), encoding='utf-8')
file_handler.setLevel(logging.INFO)

# 设置日志格式
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
file_handler.setFormatter(formatter)

# 添加处理器到logger
logger.addHandler(file_handler)

# 添加控制台输出
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.INFO)
console_handler.setFormatter(formatter)
logger.addHandler(console_handler)

def enhance_llm_with_comprehensive_graphrag(question):
    """
    使用知识图谱增强LLM回答的核心函数
    
    参数:
        question: 用户的问题
    
    返回:
        包含知识图谱增强上下文的字典
    """
    try:
        logger.info(f"开始处理问题: {question}")
        
        # 执行Neo4j查询
        graph_result = comprehensive_neo4j_query_for_mingchao(question)
        
        if graph_result and graph_result.get("success", False):
            graph_info = graph_result.get("graph_info", "")
            logger.info(f"知识图谱查询成功，获取到图数据")
            
            # 直接返回知识图谱增强上下文，不进行额外处理
            return {
                "success": True,
                "enhanced_context": graph_info,
                "graph_results": graph_result,
                "question": question
            }
        else:
            error_msg = graph_result.get("error", "知识图谱查询失败") if graph_result else "知识图谱查询失败"
            logger.error(f"知识图谱查询返回失败: {error_msg}")
            return {"success": False, "error": error_msg, "question": question}
            
    except Exception as e:
        logger.error(f"知识图谱增强处理异常: {str(e)}")
        logger.error(traceback.format_exc())
        return {"success": False, "error": f"处理异常: {str(e)}", "question": question}

def comprehensive_neo4j_query_for_mingchao(question):
    """
    全面查询《明朝那些事儿》相关信息
    采用多种查询策略获取所有可能的关系，包括直接关系和所有层级的扩展关系
    """
    logger.info(f"执行Neo4j全面查询，问题: {question}")
    
    try:
        # 尝试导入Neo4j处理器
        try:
            from neo4j_handler import init_neo4j_handler, get_neo4j_handler
        except ImportError:
            logger.error("无法导入neo4j_handler模块")
            return {"success": False, "error": "neo4j_handler模块不可用"}
        
        # 初始化Neo4j处理器
        handler = init_neo4j_handler("neo4j://127.0.0.1:7687", "neo4j", "12345678")
        
        # 验证连接
        if not handler.driver:
            logger.error("无法连接到Neo4j数据库")
            return {"success": False, "error": "无法连接到Neo4j数据库"}
        
        # 提取关键词 - 使用neo4j_handler的extract_keywords方法
        keywords = handler.extract_keywords(question)
        logger.info(f"从问题中提取的关键词: {keywords}")
        
        # 添加《明朝那些事儿》作为核心关键词
        if "明朝那些事儿" not in keywords:
            keywords.insert(0, "明朝那些事儿")
        
        # 1. 执行最大深度的扩展查询
        logger.info("执行最大深度的扩展查询...")
        result = handler.query_expanded_context(keywords, depth=4)
        logger.info(f"深度4扩展查询结果: 实体数={len(result.get('entities', []))}, 关系数={len(result.get('relationships', []))}")
        
        # 2. 执行直接Cypher查询，获取所有可能的关系
        direct_relationships_result = []
        ming_dynasty_relationships_result = []
        all_paths_result = []
        author_relationships_result = []
        related_books_result = []
        
        try:
            with handler.driver.session() as session:
                # 2.1 查找与《明朝那些事儿》直接相关的所有关系
                direct_relationships_query = """
                MATCH (n)-[r]-(m)
                WHERE n.name CONTAINS '明朝那些事儿' OR m.name CONTAINS '明朝那些事儿'
                RETURN n, r, m
                """
                direct_relationships_result = session.run(direct_relationships_query).data()
                logger.info(f"直接Cypher查询找到{len(direct_relationships_result)}条直接关系")
                
                # 2.2 查找与明朝相关的扩展关系
                ming_dynasty_relationships_query = """
                MATCH (n)-[r*1..4]-(m)
                WHERE (n.name CONTAINS '明朝' OR m.name CONTAINS '明朝')
                  AND (n.name CONTAINS '明朝那些事儿' OR m.name CONTAINS '明朝那些事儿')
                RETURN n, r, m
                """
                ming_dynasty_relationships_result = session.run(ming_dynasty_relationships_query).data()
                logger.info(f"明朝相关扩展关系查询找到{len(ming_dynasty_relationships_result)}条关系")
        except Exception as e:
            logger.warning(f"Cypher查询部分失败: {str(e)}")
        
        # 构建全面的综合上下文
        comprehensive_context = build_comprehensive_context(
            result, 
            direct_relationships_result, 
            ming_dynasty_relationships_result,
            all_paths_result,
            author_relationships_result,
            related_books_result
        )
        
        logger.info(f"全面查询完成")
        
        return {
            "success": True,
            "graph_info": comprehensive_context,
            "raw_result": result,
            "direct_relationships_count": len(direct_relationships_result),
            "ming_dynasty_relationships_count": len(ming_dynasty_relationships_result)
        }
        
    except Exception as e:
        logger.error(f"Neo4j查询失败: {str(e)}")
        logger.error(f"错误堆栈: {traceback.format_exc()}")
        return {
            "success": False,
            "error": str(e)
        }
    finally:
        # 确保资源被正确释放
        if 'handler' in locals() and handler.driver:
            try:
                handler.close()
            except:
                pass

def is_answer_enhanced(answer, graph_info):
    """
    检查回答是否有效利用了知识图谱中的关键信息
    """
    try:
        # 从图信息中提取关键实体
        key_entities = []
        lines = graph_info.split('\n')
        
        # 提取实体信息
        for line in lines:
            if '实体:' in line or '关系:' in line:
                # 提取可能的实体名称
                entities = re.findall(r'[《》「」"“”]([^《》「」"“”]+)[《》「」"“”]', line)
                key_entities.extend(entities)
                # 提取没有引号的实体
                if ':' in line:
                    parts = line.split(':')
                    if len(parts) > 1:
                        key_entities.append(parts[1].strip())
        
        # 去除重复实体
        key_entities = list(set(key_entities))
        
        # 检查回答中是否包含至少一个关键实体
        for entity in key_entities:
            if len(entity) > 2 and entity in answer:
                return True
        
        return False
    except Exception as e:
        logger.error(f"检查回答增强状态失败: {str(e)}")
        return False

def add_graph_knowledge_prompt(answer):
    """
    如果回答没有有效利用图信息，添加提示
    """
    enhanced_answer = answer + "\n\n【增强提示】: 本回答可以结合《明朝那些事儿》知识图谱中的实体关系信息进行进一步优化。"
    return enhanced_answer

def build_comprehensive_context(
    main_result, 
    direct_relationships_result, 
    ming_dynasty_relationships_result,
    all_paths_result,
    author_relationships_result,
    related_books_result
):
    """
    构建全面的综合上下文信息
    """
    try:
        # 基础结果格式化
        formatted_context = []
        
        # 添加查询统计信息
        entities_count = len(main_result.get('entities', []))
        relationships_count = len(main_result.get('relationships', []))
        formatted_context.append(f"### 《明朝那些事儿》知识图谱查询结果 ###")
        formatted_context.append(f"查询结果统计: 共找到 {entities_count} 个相关实体，{relationships_count} 条关系")
        formatted_context.append("")
        
        # 提取直接相关实体
        direct_entities = []
        entity_relationships = []
        
        # 处理main_result中的实体和关系
        for entity in main_result.get('entities', [])[:20]:  # 限制显示数量
            entity_name = entity.get('name', '未知实体')
            entity_type = entity.get('type', '未知类型')
            direct_entities.append(f"- {entity_name} (类型: {entity_type})")
        
        # 处理关系
        relationship_types = {}
        for rel in main_result.get('relationships', [])[:30]:  # 限制显示数量
            source = rel.get('source', {}).get('name', '未知源')
            target = rel.get('target', {}).get('name', '未知目标')
            relationship_type = rel.get('type', '未知关系')
            
            entity_relationships.append(f"- {source} -{relationship_type}-> {target}")
            
            # 统计关系类型
            if relationship_type in relationship_types:
                relationship_types[relationship_type] += 1
            else:
                relationship_types[relationship_type] = 1
        
        # 添加直接实体列表
        if direct_entities:
            formatted_context.append(f"### 直接相关实体 (前{len(direct_entities)}个) ###")
            formatted_context.extend(sorted(direct_entities))
            formatted_context.append("")
        
        # 添加直接关系类型统计
        if relationship_types:
            formatted_context.append("### 关系类型分布 ###")
            sorted_relationships = sorted(relationship_types.items(), key=lambda x: x[1], reverse=True)
            for rel_type, count in sorted_relationships:
                formatted_context.append(f"- {rel_type}: {count}条")
            formatted_context.append("")
        
        # 添加关系列表
        if entity_relationships:
            formatted_context.append(f"### 主要关系 (前{len(entity_relationships)}个) ###")
            formatted_context.extend(entity_relationships)
            formatted_context.append("")
        
        # 明朝相关扩展关系分析
        ming_related_count = len(ming_dynasty_relationships_result)
        if ming_related_count > 0:
            formatted_context.append(f"### 明朝相关扩展关系 ###")
            formatted_context.append(f"找到 {ming_related_count} 条与明朝相关的扩展关系")
            
            # 分析这些关系中的实体
            ming_entities = set()
            ming_rel_types = {}
            
            for record in ming_dynasty_relationships_result[:10]:  # 分析前10条
                # 提取实体
                if 'n' in record:
                    ming_entities.add(record['n'].get('name', '未知实体'))
                if 'm' in record:
                    ming_entities.add(record['m'].get('name', '未知实体'))
                
                # 提取关系类型
                if 'r' in record:
                    if isinstance(record['r'], list):
                        for rel in record['r']:
                            rel_type = rel.type if hasattr(rel, 'type') else '未知关系'
                            ming_rel_types[rel_type] = ming_rel_types.get(rel_type, 0) + 1
                    else:
                        rel_type = record['r'].type if hasattr(record['r'], 'type') else '未知关系'
                        ming_rel_types[rel_type] = ming_rel_types.get(rel_type, 0) + 1
            
            formatted_context.append(f"相关实体数: {len(ming_entities)}")
            formatted_context.append("主要关系类型:")
            for rel_type, count in sorted(ming_rel_types.items(), key=lambda x: x[1], reverse=True)[:5]:
                formatted_context.append(f"  - {rel_type}: {count}条")
            formatted_context.append("")
        
        # 作者相关关系分析
        if author_relationships_result:
            formatted_context.append("### 作者相关关系 ###")
            author_entities = set()
            for record in author_relationships_result[:10]:
                if 'n' in record:
                    author_entities.add(record['n'].get('name', '未知实体'))
                if 'm' in record:
                    author_entities.add(record['m'].get('name', '未知实体'))
            formatted_context.append(f"找到 {len(author_entities)} 个与作者相关的实体")
            formatted_context.append("")
        
        # 相关书籍分析
        if related_books_result:
            formatted_context.append("### 相关书籍 ###")
            books = []
            for record in related_books_result[:10]:
                if 'm' in record and 'name' in record['m']:
                    books.append(record['m']['name'])
            if books:
                formatted_context.append(f"找到 {len(books)} 本相关书籍:")
                for book in books:
                    formatted_context.append(f"  - {book}")
            formatted_context.append("")
        
        # 上下文长度优化
        context_str = '\n'.join(formatted_context)
        
        # 如果上下文过长，进行截断
        if len(context_str) > 5000:
            # 保留重要部分
            lines = context_str.split('\n')
            truncated_lines = []
            current_length = 0
            
            # 优先保留统计信息和直接相关实体
            for i, line in enumerate(lines):
                if i < 20:  # 保留前20行统计和直接实体信息
                    truncated_lines.append(line)
                    current_length += len(line) + 1  # +1 for newline
                elif current_length < 4000:  # 保留其他内容直到达到4000字符
                    truncated_lines.append(line)
                    current_length += len(line) + 1
                else:
                    break
            
            truncated_lines.append("\n### 结果已截断 ###")
            truncated_lines.append("由于内容过长，部分结果已被省略。")
            context_str = '\n'.join(truncated_lines)
        
        return context_str
        
    except Exception as e:
        logger.error(f"构建综合上下文失败: {str(e)}")
        # 返回降级的简单上下文
        return "### 知识图谱查询结果 ###\n\n[由于处理错误，无法提供详细的知识图谱信息]"

# 测试函数，便于调试
def test_enhancer():
    test_question = "朱元璋和朱棣是什么关系？"
    result = enhance_llm_with_comprehensive_graphrag(test_question)
    print(f"测试结果: {json.dumps(result, ensure_ascii=False, indent=2)}")

if __name__ == "__main__":
    test_enhancer()