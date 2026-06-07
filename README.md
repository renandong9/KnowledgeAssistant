# Knowledge Assistant

Knowledge Assistant 是一个面向论文和科研文章的 AI 科研知识助手。项目目标是让用户上传一篇 PDF、DOCX、TXT 或 Markdown 文档后，系统自动完成文档解析、Chunk 切分、AI 分析、相关论文推荐，并支持围绕同一篇文章创建多个独立主题问答窗口。

## 当前功能

- 文档上传：支持 PDF、DOCX、TXT、Markdown。
- 文档解析：保存文档元数据，提取文本并切分为 DocumentChunk。
- OCR：已接入 Tess4J 配置，默认关闭；普通文本 PDF 不依赖 OCR。
- AI 分析：通过本地 Ollama 生成研究背景、问题定义、核心方法、实验结果、创新点、优缺点和一句话总结。
- 多主题问答：每篇 Document 可以创建多个 ChatSession，每个窗口保存独立 ChatMessage 历史。
- RAG 问答：限定当前 Document 检索，结合 ChatSession 标题辅助召回，回答基于原文 Chunk。
- 相关论文推荐：接入 Semantic Scholar，失败时不影响主流程。
- 前端工作区：初始上传卡片，上传后进入 Document Workspace，展示分析结果、推荐论文和多主题问答区。

## 技术栈

后端：

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- PDFBox
- Apache POI
- Tess4J
- Ollama

前端：

- Vue 3
- Vite
- Axios

## 环境要求

- MySQL 已启动，并创建数据库 `knowledge_assistant`。
- Ollama 已启动，默认地址为 `http://localhost:11434`。
- 本地已拉取模型 `deepseek-r1:1.5b`。
- 如需扫描版 PDF OCR，需要额外安装 Tesseract 和对应语言包。

## 配置

后端敏感配置通过环境变量读取，不要提交真实账号密码。

```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_mysql_password"
$env:OLLAMA_BASE_URL="http://localhost:11434"
$env:OLLAMA_MODEL="deepseek-r1:1.5b"
```

后端端口默认 `8080`，可通过环境变量覆盖：

```powershell
$env:SERVER_PORT="18080"
```

前端代理默认指向 `http://localhost:8080`，可通过环境变量覆盖：

```powershell
$env:VITE_API_TARGET="http://localhost:18080"
```

可参考：

- `Backend/src/main/resources/application-example.yml`
- `Frontend/.env.example`

## 数据库初始化

```powershell
mysql -u root -p knowledge_assistant < Backend/src/main/resources/schema.sql
```

核心表：

- `documents`
- `document_chunks`
- `paper_analysis`
- `paper_recommendations`
- `chat_sessions`
- `chat_messages`

## 启动后端

使用默认端口 `8080`：

```powershell
.\scripts\start-backend.ps1
```

如果 `8080` 被占用，可临时使用 `18080`：

```powershell
.\scripts\start-backend.ps1 -Port 18080 -MysqlPassword your_mysql_password
```

## 启动前端

后端使用默认 `8080` 时：

```powershell
.\scripts\start-frontend.ps1
```

后端使用 `18080` 时：

```powershell
.\scripts\start-frontend.ps1 -BackendUrl http://localhost:18080
```

前端访问地址：

```text
http://localhost:5173
```

## 常用验证

后端编译：

```powershell
cd Backend
mvn clean compile
```

前端构建：

```powershell
cd Frontend
npm run build
```

## 配置安全

- `application.yml` 使用环境变量占位，不提交真实密码或 API Key。
- `Frontend/.env` 已加入忽略列表，只提交 `Frontend/.env.example`。
- 外部推荐服务或官方 API Key 统一通过环境变量读取。
