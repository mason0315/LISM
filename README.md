# LIMS-Pro: 智能化图书管理与知识服务系统 (全栈/演进式架构)

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023-blue.svg)](https://spring.io/projects/spring-cloud)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)](https://vuejs.org/)
[![Python](https://img.shields.io/badge/Python-3.9+-blue.svg)](https://www.python.org/)
[![Neo4j](https://img.shields.io/badge/Neo4j-GraphDB-blueviolet.svg)](https://neo4j.com/)

**LIMS-Pro** 是一个涵盖了**单体应用**与**分布式微服务**双重架构实现的现代化图书知识管理系统。项目不仅实现了业务的深度解耦，还集成了 **Neo4j 知识图谱**、**LLM (RAG) 智能问答**以及**人脸识别**等前沿 AI 技术，是一个极具工程实践价值的“全栈+AI”项目。

---

## 🏗️ 架构演进与设计

本项目最大的特色在于其**演进式架构设计**，仓库中同时保留并优化了两套后端实现：

1.  **单体架构 (`Back/bookmanagement`)**：基于 Spring Boot 3.4.3 的标准分层架构，适合快速部署和中小规模应用。
2.  **微服务架构 (`Back/microservices`)**：基于 Spring Cloud Alibaba 的分布式系统，实现了用户、图书、借阅、人脸识别等 10+ 个微服务的独立拆分。



---

## 🌟 技术亮点

### 1. 异构语言协同 (Java + Python)
* **Java (Core Business)**：处理核心借阅逻辑、事务控制与权限治理。
* **Python (AI & Graph)**：利用 Flask 封装智能问答引擎，对接 AnythingLLM API，并通过 Neo4j 实现图书关系的深度拓扑检索。

### 2. 知识图谱与 RAG 增强
* **数据治理**：在 `LISM Knowledge Graph/` 下包含完整的图谱构建脚本（Excel 导入、数据清洗、关系构建）。
* **RAG 实现**：结合向量检索与图谱路径查询，提供比传统 SQL 检索更精准的知识问答体验。

### 3. 分布式治理实践
* **网关/限流**：Gateway + JWT 统一鉴权，Sentinel 核心接口防雪崩。
* **一致性保障**：引入 Seata AT 模式，确保跨服务调用时的分布式事务一致性。

### 4. 生物识别安全
* **前端检测**：`face-api.js` 在浏览器端完成面部捕捉。
* **后端比对**：Java 模块对接百度 AI，实现毫秒级的人脸登录验证。

---

## 🛠️ 技术栈

| 领域 | 核心选型 |
| :--- | :--- |
| **Java 后端** | Spring Boot 3.4, Spring Cloud 2023, MyBatis-Plus, Nacos, Seata, Sentinel |
| **Python 后端** | Flask, Neo4j-Handler, LangChain/AnythingLLM API |
| **前端应用** | Vue 3.5, TypeScript, Vite, Pinia, Element Plus, face-api.js |
| **数据层** | MySQL 8.0, Redis (多级缓存), Neo4j (知识图谱) |

---

## 📂 目录结构说明

```text
LIMS/
├── Back/                      # Java 后端主目录
│   ├── bookmanagement/        # [单体版] Spring Boot 核心业务
│   └── microservices/         # [微服务版] Spring Cloud 分布式群
│       ├── gateway-service/   # 统一入口网关
│       ├── auth-service/      # 认证授权中心
│       ├── face-service/      # 人脸识别专用微服务
│       └── ...                # 其他业务微服务 (图书、借阅、留言等)
├── Back(python)/              # Python AI 引擎 (Flask + LLM + Neo4j 处理器)
├── Front/vue-bookman/         # Vue 3 前端工程 (TS 版)
├── LISM Knowledge Graph/      # 知识图谱初始化模块 (Excel 数据与导入脚本)
└── README/                    # 深度技术文档 (架构分析、性能优化、亮点解析)
