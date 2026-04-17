# AI 模拟面试与能力提升软件

> 锐捷网络 · 教育行业解决方案

## 项目简介

本项目是一个面向计算机相关专业学生的 AI 模拟面试与能力提升平台，核心是构建一个能够模拟不同技术岗位面试场景、并进行多维度智能评估与反馈的 AI 教练。

### 核心功能

- **岗位化情景模拟**：针对后端开发、前端开发、测试工程师、算法工程师等岗位构建专属面试题库和评估模型
- **多模态互动面试**：支持语音和文字输入，与 AI 面试官进行多轮沉浸式对话
- **语音情绪分析**：基于阿里云 DashScope 大模型分析面试者情绪状态和自信程度
- **音频智能分析**：集成阿里云 Paraformer 语音识别，实现高精度语音转文字
- **PDF 简历解析**：支持上传 PDF 简历，自动提取个人信息和技能标签
- **RAG 知识增强**：基于向量检索的专业知识库，提升评估准确性
- **能力评估与深度反馈**：从技术正确性、逻辑严谨性、沟通能力等多维度量化评分
- **个性化提升路径**：根据面试记录分析能力短板，智能推荐学习资源

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                      前端界面                            │
│  (HTML5 + CSS3 + JavaScript + Web Speech API)           │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   Spring Boot 3.2                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Controller 层                       │   │
│  │ UserController │ PositionController │ InterviewController │
│  │ AudioController │ SpeechEvaluationController │ RagController │
│  └─────────────────────────────────────────────────┘   │
│                            ↓                            │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Service 层                          │   │
│  │ UserService │ PositionService │ InterviewService │
│  │ LlmService │ RagService │ VectorStoreService │
│  │ AudioAnalysisService │ EmbeddingService │
│  │ PdfResumeParser │ ResumeService │
│  └─────────────────────────────────────────────────┘   │
│                            ↓                            │
│  ┌─────────────────┐         ┌─────────────────┐       │
│  │  SQLite Database │         │  阿里云 DashScope  │   │
│  │  (interview.db)  │         │  (LLM + ASR + Embedding) │
│  └─────────────────┘         └─────────────────┘       │
└─────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- 现代浏览器（Chrome/Edge 90+，用于语音输入功能）

### 2. 获取 API Key

本项目使用**阿里云 DashScope（通义千问）**作为大模型和语音服务提供商：

1. 访问 [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/)
2. 注册/登录阿里云账号
3. 创建 API Key
4. 免费额度：qwen-turbo 每日 200 万 tokens 免费

### 3. 配置项目

编辑 `src/main/resources/application.yml`，将 `api-key` 替换为您的实际 API Key：

```yaml
app:
  llm:
    provider: dashscope
    api-key: sk-your-actual-api-key-here  # 替换为您的 API Key
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen3.6-plus
```

### 4. 编译运行

```bash
# 编译项目
mvn clean package -DskipTests

# 运行项目
mvn spring-boot:run

# 或者在 IDEA 中直接运行 Application.java
```

### 5. 访问应用

打开浏览器访问：`http://localhost:8080/api/`

## 项目结构

```
InterviewSimulation/
├── src/main/java/com/ruijie/interview/
│   ├── Application.java          # 主程序入口
│   ├── config/                   # 配置类
│   │   └── LlmConfig.java        # LLM 配置
│   ├── entity/                   # 实体类
│   │   ├── User.java
│   │   ├── Position.java
│   │   ├── Question.java
│   │   ├── InterviewSession.java
│   │   ├── Answer.java
│   │   ├── EvaluationReport.java
│   │   ├── LearningResource.java
│   │   └── Resume.java           # 简历实体
│   ├── repository/               # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── PositionRepository.java
│   │   ├── QuestionRepository.java
│   │   ├── InterviewSessionRepository.java
│   │   ├── AnswerRepository.java
│   │   ├── EvaluationReportRepository.java
│   │   ├── LearningResourceRepository.java
│   │   └── ResumeRepository.java
│   └── service/                  # 业务逻辑层
│       ├── LlmService.java       # 大模型调用服务
│       ├── RagService.java       # RAG 检索服务
│       ├── VectorStoreService.java  # 向量存储服务
│       ├── EmbeddingService.java    # 文本向量化服务
│       ├── UserService.java
│       ├── PositionService.java
│       ├── QuestionService.java
│       ├── InterviewService.java    # 面试流程服务
│       ├── AudioAnalysisService.java # 音频分析服务
│       ├── SpeechEvaluationService.java # 语音评估服务
│       ├── PdfResumeParser.java   # PDF 简历解析
│       └── ResumeService.java     # 简历管理服务
├── src/main/java/com/ruijie/interview/controller/
│   ├── UserController.java
│   ├── PositionController.java
│   ├── InterviewController.java
│   ├── AudioController.java       # 音频分析接口
│   ├── SpeechEvaluationController.java # 语音评估接口
│   └── RagController.java         # RAG 搜索接口
├── src/main/resources/
│   ├── application.yml           # 应用配置
│   └── static/
│       └── index.html            # 前端页面
├── data/                         # 数据目录（运行时创建）
│   ├── interview.db              # SQLite 数据库
│   ├── knowledge-base/           # RAG 知识库
│   └── resumes/                  # 上传的简历文件
└── docs/                         # 文档
    ├── 快速开始.md
    ├── 环境配置说明.md
    ├── RAG 模块说明.md
    ├── 音频分析实现详解.md
    ├── 语音情绪分析功能说明.md
    └── PDF 简历解析功能说明.md
```

## 支持的岗位

| 岗位 | 编码 | 技术栈 |
|------|------|--------|
| 后端开发工程师 | backend | Java, Spring Boot, MySQL, Redis |
| 前端开发工程师 | frontend | JavaScript, Vue/React, CSS |
| 测试工程师 | qa | 测试理论，Selenium, JMeter |
| 算法工程师 | algorithm | Python, PyTorch, 机器学习 |

## API 接口

### 用户接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/login | 用户登录 |
| POST | /api/user/register | 用户注册 |
| GET | /api/user/{id} | 获取用户信息 |
| PUT | /api/user/{id} | 更新用户信息 |

### 岗位接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/position/list | 获取岗位列表 |
| GET | /api/position/{code} | 获取岗位详情 |
| GET | /api/position/{code}/questions | 获取岗位题库 |
| GET | /api/position/{code}/knowledge | 获取岗位知识库 |

### 面试接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/interview/start | 开始面试 |
| GET | /api/interview/{id} | 获取面试会话 |
| GET | /api/interview/history/{userId} | 获取面试历史 |
| POST | /api/interview/{sessionId}/answer | 提交回答 |
| GET | /api/interview/{sessionId}/report | 获取评估报告 |

### 音频分析接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/audio/analyze | 音频文件分析（语音识别 + 情绪分析） |
| POST | /api/audio/analyze/base64 | Base64 音频分析 |
| POST | /api/speech/evaluate | 语音评估（真实面试模式） |

### RAG 接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/rag/search | 智能搜索（混合检索） |
| POST | /api/rag/search/vector | 向量搜索 |
| POST | /api/rag/search/keyword | 关键词搜索 |
| POST | /api/rag/chat | RAG 增强对话 |
| POST | /api/rag/reload | 重新加载知识库 |
| GET | /api/rag/vector/status | 获取向量化状态 |

## 核心功能详解

### 1. 语音情绪分析

系统使用阿里云 DashScope 的 Qwen-max 大模型对面试者的语音回答进行情绪识别：

- **支持的情绪类型**：CONFIDENT（自信）、NERVOUS（紧张）、CALM（从容）、ANXIOUS（焦虑）、EXCITED（兴奋）、NORMAL（平静）、FOCUSED（专注）
- **评估维度**：技术能力、沟通能力、逻辑思维、知识深度、综合评分
- **自信程度**：1-5 星级评估

详细文档：[docs/语音情绪分析功能说明.md](docs/语音情绪分析功能说明.md)

### 2. 音频分析（ASR）

集成阿里云 Paraformer 实时语音识别模型：

- **高精度语音转文字**：支持中文普通话识别
- **自动标点**：自动添加标点符号
- **文本规范化**：数字、日期等自动转换为标准格式

详细文档：[docs/音频分析实现详解.md](docs/音频分析实现详解.md)

### 3. PDF 简历解析

支持上传 PDF 格式简历，自动解析：

- **个人信息提取**：姓名、联系方式、教育背景
- **技能标签识别**：自动提取技术栈关键词
- **经历分析**：工作/项目经历结构化处理

### 4. RAG 知识增强

基于向量检索的增强生成技术：

- **混合搜索**：向量相似度 (70%) + 关键词匹配 (30%)
- **文本向量化**：使用 DashScope text-embedding-v3 模型（1024 维）
- **岗位知识库**：每个岗位独立的知识库文件
- **智能推荐**：根据薄弱环节推荐学习资源

详细文档：[docs/RAG 模块说明.md](docs/RAG 模块说明.md)

## 配置说明

### 大模型配置

```yaml
app:
  llm:
    provider: dashscope              # API 提供商
    api-key: sk-xxxx                 # API Key
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen3.6-plus              # 默认模型
    enable-thinking: true            # 启用深度思考
    fallback-models:                 # 备选模型
      - qwen3.6-plus
      - qwen-plus
      - qwen-max
```

### RAG 配置

```yaml
app:
  rag:
    enable-vector-search: true       # 启用向量搜索
    vector-weight: 0.7               # 向量搜索权重
```

### PDF 解析配置

```yaml
app:
  pdf:
    parser:
      tessdata-path: ./tessdata      # Tesseract OCR 数据路径
      ocr-lang: chi_sim+eng          # 识别语言
```

详细配置说明：[docs/环境配置说明.md](docs/环境配置说明.md)

## 面试模式

| 功能 | 练习模式 (PRACTICE) | 真实面试 (REAL) |
|------|---------------------|-----------------|
| 输入方式 | 文字/语音可选 | 仅语音 |
| 情绪分析 | ❌ | ✅ |
| 自信程度评估 | ❌ | ✅ |
| 回答可修改 | ✅ | ❌ |
| 评估报告 | 基础评分 | 包含情绪分析 |

## 技术特点

1. **云端大模型**：使用阿里云 DashScope，无需本地部署模型
2. **RAG 增强**：专业知识库支持，使评估更准确
3. **多模态交互**：支持文字和语音输入
4. **语音情绪识别**：基于大模型的语音情绪分析
5. **向量检索**：混合搜索算法，提升检索准确性
6. **可扩展架构**：知识库可独立更新，支持新增岗位

## 开发计划

- [ ] 添加更多岗位类别（产品、运营、设计等）
- [ ] 实现面部表情识别（需摄像头权限）
- [ ] 增加语速、停顿等语音特征分析
- [ ] 能力成长可视化（雷达图、趋势图）
- [ ] 支持追问和反向提问功能
- [ ] 增加学习资源推荐系统

## 常见问题

### 端口被占用
修改 `application.yml` 中的 `server.port` 配置。

### API Key 无效
确保 API Key 正确配置，检查阿里云 DashScope 控制台。

### 语音输入不可用
- 使用 Chrome、Edge 等支持 Web Speech API 的浏览器
- 允许浏览器访问麦克风权限
- 使用 localhost 或 HTTPS 访问

### 数据库问题
SQLite 数据库文件位于 `./data/interview.db`，可删除后重新启动（会丢失数据）。

## 许可证

本项目为锐捷网络教育行业解决方案演示版本。

## 联系方式

锐捷网络 - 教育行业解决方案团队

---

© 2024-2026 锐捷网络 | AI 模拟面试与能力提升软件