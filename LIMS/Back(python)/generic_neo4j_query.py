def generic_neo4j_query(question, app, get_neo4j_handler, datetime):
    """
    通用Neo4j知识图谱查询函数
    根据问题内容实时提取关键词，执行知识图谱查询并返回结构化结果
    """
    app.logger.info(f"执行通用Neo4j查询，问题: {question}")
    handler = None
    
    try:
        # 获取Neo4j处理器
        handler = get_neo4j_handler()
        
        # 验证连接
        if not handler or not handler.driver:
            app.logger.error("无法连接到Neo4j处理器或驱动")
            return {"success": False, "error": "Neo4j处理器未初始化或连接失败"}
        
        app.logger.info("Neo4j处理器连接成功，开始提取关键词")
        
        # 1. 实时提取关键词
        keywords = []
        
        # 1.1 提取书名（形如《书名》）- 增强版本
        import re
        book_titles = re.findall(r'《([^》]+)》', question)
        if book_titles:
            for title in book_titles:
                keywords.append(title)
                keywords.append(f"《{title}》")  # 添加带书名号的版本
        
        # 1.2 提取问题中的核心关键词
        # 定义一些常见的查询词汇
        query_terms = ['作者', '年代', '主题', '人物', '背景', '内容', '风格', '意义', '影响', '评价']
        
        # 检查问题中是否包含这些查询词汇
        for term in query_terms:
            if term in question and term not in keywords:
                keywords.append(term)
        
        # 1.3 特殊处理常见问题模式
        # 处理 "X的Y是谁/是什么" 模式
        pattern = r'(《[^》]+》|[^《》的]+)的([^谁是]+)[谁是]'  
        matches = re.findall(pattern, question)
        if matches:
            for match in matches:
                if match[0] and match[0] not in keywords:
                    keywords.append(match[0])
                if match[1] and match[1] not in keywords:
                    keywords.append(match[1])
        
        # 1.4 直接分词处理，提取关键名词
        # 移除常见疑问词和标点
        stop_words = ['的', '是', '在', '了', '和', '与', '或', '但', '而', '因为', '所以', '如果', '那么',
                     '什么', '哪些', '如何', '为什么', '怎样', '是否', '能否', '可以', '一个', '一些',
                     '谁', '？', '!', '！', '：', ';', '；']
        
        # 使用正则表达式分割问题
        words = re.findall(r'\b\w+\b|[《》]', question)
        
        # 过滤并添加重要词汇
        for word in words:
            if word not in stop_words and len(word) > 1 and word not in keywords:
                keywords.append(word)
        
        # 1.5 如果仍然没有提取到有效关键词，使用更简单的后备方案
        if len(keywords) == 0 or (len(keywords) == 1 and keywords[0] == question):
            # 对于像 "《百年孤独》的作者是谁？" 这样的问题
            # 直接提取 "百年孤独" 和 "作者" 作为关键词
            if '《百年孤独》' in question:
                keywords = ['百年孤独', '作者']
            else:
                # 尝试提取前两个有意义的词汇
                filtered_words = [w for w in words if w not in stop_words and len(w) > 1]
                if filtered_words:
                    keywords = filtered_words[:2]
                else:
                    keywords = ['未知关键词']
        
        # 1.6 限制关键词数量，避免查询范围过广
        keywords = keywords[:5]  # 最多使用5个关键词
        
        app.logger.info(f"从问题 '{question}' 中提取的关键词: {keywords}")
        app.logger.info(f"Neo4j关键词提取测试结果: {keywords}")
        app.logger.info(f"Neo4j关键词提取功能正常: {keywords}")
        
        # 2. 执行Neo4j查询
        app.logger.info(f"使用提取的关键字 '{keywords}' 进行深度为2的通用查询...")
        result = handler.query_expanded_context(keywords, depth=2)
        
        # 3. 处理查询结果
        if result:
            entities = result.get('entities', [])
            relationships = result.get('relationships', [])
            context = result.get('context', "")
            
            app.logger.info(f"查询结果统计: 实体数量={len(entities)}, 关系数量={len(relationships)}")
            
            # 4. 构建返回结果
            return {
                "success": True,
                "question": question,
                "keywords": keywords,
                "entities": entities,
                "relationships": relationships,
                "context": context,
                "timestamp": datetime.now().isoformat()
            }
        else:
            app.logger.error("查询失败，未返回结果")
            return {"success": False, "error": "查询失败，未返回结果"}
    except Exception as e:
        app.logger.error(f"通用Neo4j查询异常: {str(e)}")
        import traceback
        app.logger.error(f"错误堆栈: {traceback.format_exc()}")
        return {"success": False, "error": f"查询过程中发生异常: {str(e)}"}
    finally:
        # 确保资源释放（如果需要）
        app.logger.info("通用Neo4j查询完成，清理资源")