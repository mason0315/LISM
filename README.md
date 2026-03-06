🏗️ 核心架构设计项目采用 Spring Cloud Alibaba 体系进行微服务治理，实现了业务解耦与高并发支持。1. 服务拆分维度用户服务 (User Service)：负责 JWT 鉴权、RBAC 权限管理及人脸特征存储。图书服务 (Book Service)：核心业务逻辑，处理高频的借阅、归还及库存管理。AI 增强服务 (AI Service)：基于 Python Flask，封装 AnythingLLM API 与向量数据库，提供语义搜索。知识图谱服务 (Graph Service)：专门对接 Neo4j，维护图书-作者-学科的网状关联。2. 微服务治理网关中心：基于 Spring Cloud Gateway 实现统一入口、动态路由及黑名单过滤。配置/注册中心：使用 Nacos 进行服务发现与配置热更新。流量防护：集成 Sentinel 实现核心接口的熔断与降级，保障系统稳定性。分布式事务：采用 Seata (AT 模式) 解决跨服务借阅场景下的数据一致性问题。🌟 技术亮点与攻关🧠 智能 RAG 问答体系不同于传统的关键词搜索，本项目构建了 "知识图谱 + 大模型" 的双驱动模式：结构化检索：通过 Cypher 查询 Neo4j，精准定位学科层级关系。非结构化增强：利用 RAG (检索增强生成) 技术，将 PDF/Markdown 文档向量化，配合 LLM 提供专业回答。👤 生物识别与安全边缘计算提取：前端通过 face-api.js 在浏览器端完成人脸检测，减轻后端计算压力。多因子认证：结合 JWT 令牌与人脸特征双重校验，提升系统安全性。🚀 高性能优化实践多级缓存方案：利用 Redis 缓存热点书籍信息，配合 Caffeine 本地缓存，支撑高频查询。异步削峰：引入 RabbitMQ 处理逾期通知、日志记录等非核心链路，提升系统吞吐量。数据库调优：针对百万级数据完成索引优化，并实现读写分离架构。🛠️ 技术栈清单维度技术选型核心框架Spring Boot 3.x, Spring Cloud 2023, MyBatis-Plus中间件Nacos, Sentinel, Seata, RabbitMQ, RedisAI/算法Python 3.9, Flask, AnythingLLM, face-api.js数据库MySQL 8.0 (关系型), Neo4j (图数据库), ChromaDB (向量)前端Vue 3.5, TypeScript, Vite, Element Plus, Pinia运维Docker, Jenkins, Prometheus + Grafana📂 模块说明Plaintextlims-microservices/
├── lims-gateway/           # API 网关 [统一鉴权、限流]
├── lims-auth/              # 认证中心 [OAuth2 / JWT]
├── lims-service/           # 业务逻辑微服务群
│   ├── lims-service-user/  # 用户与人脸识别服务
│   ├── lims-service-book/  # 图书与借阅服务
│   └── lims-service-graph/ # 知识图谱服务
├── lims-ai-engine/         # Python AI 引擎 [LLM/RAG]
└── lims-common/            # 公共通用组件库
📈 性能表现根据压测报告（详见 docs/optimization.md）：吞吐量：核心借阅接口 QPS 提升了 300%。延迟：通过 Redis 预热，图书检索平均响应时间压低至 50ms 以内。一致性：Seata 成功处理了 99.9% 的分布式环境异常回滚。🚀 快速启动环境准备：确保本地已启动 MySQL, Redis, Nacos 和 Neo4j。配置 Nacos：将 config/ 下的配置文件导入 Nacos 配置中心。编译项目：Bashmvn clean install
启动服务：依次启动 Gateway、Auth、User、Book 等服务。AI 服务：Bashcd lims-ai-engine && pip install -r requirements.txt && python main.py
✉️ 联系与反馈如果你对这个项目的架构感兴趣，或者有任何建议，欢迎提交 Pull Request 或联系我。
