# Knowledge Assistant

Knowledge Assistant 是一个个人知识库助手项目，用于上传和解析文档，基于文档内容进行检索、问答、学习复盘，并提供论文推荐入口。

## 功能概览

- 文档上传：支持 PDF、DOCX、Markdown、TXT 文件。
- 文档解析：提取文本内容并切分为知识片段。
- 知识检索：根据问题或关键词检索相关文档片段。
- 知识库问答：结合检索结果调用大模型生成回答。
- 学习复盘：基于文档内容生成总结、知识点、复习问题和改进建议。
- 论文推荐：根据研究主题检索 Semantic Scholar，失败时提供 arXiv 检索入口。

## 技术栈

### Backend

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- PDFBox
- Apache POI
- Ollama

### Frontend

- Vue 3
- Vite
- Axios

## 项目结构

```text
Knowledge Assistant/
├── Backend/                  # Spring Boot 后端
│   ├── src/main/java/        # 后端业务代码
│   ├── src/main/resources/   # 配置文件和数据库脚本
│   └── pom.xml
├── Frontend/                 # Vue 前端
│   ├── src/
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## 数据库

默认数据库名：

```sql
knowledge_assistant
```

初始化表结构：

```bash
mysql -u root -p knowledge_assistant < Backend/src/main/resources/schema.sql
```

核心表包括：

- `documents`
- `document_chunks`
- `chat_sessions`
- `chat_messages`
- `review_reports`
- `paper_recommendations`

## 环境变量

后端通过环境变量读取敏感配置：

```bash
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_mysql_password
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=deepseek-r1:1.5b
```

默认使用本地 Ollama，不需要 API Key。请先确认 Ollama 已启动，并且本地已拉取 `deepseek-r1:1.5b` 模型。

## 启动后端

```bash
cd Backend
mvn clean compile
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

## 启动前端

```bash
cd Frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

## 主要接口

- `POST /api/documents/upload`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `POST /api/ai/chat`
- `POST /api/search`
- `POST /api/chat`
- `POST /api/review/summary`
- `POST /api/papers/recommend`

## 当前状态

项目已经具备个人知识库助手的基础闭环：文档入库、文本切片、基础检索、RAG 问答、复盘报告和论文推荐。后续可以继续增强检索质量、聊天历史页面、用户体系、真实向量数据库和部署配置。
