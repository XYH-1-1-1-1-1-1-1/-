# AI 模拟面试与能力提升软件

> 锐捷网络 · 教育行业解决方案

## 项目简介

本项目是一个面向计算机相关专业学生的 AI 模拟面试与能力提升平台，核心是构建一个能够模拟不同技术岗位面试场景、并进行多维度智能评估与反馈的 AI 教练。

### 核心功能

- **岗位化情景模拟**：针对后端开发、前端开发、测试工程师、算法工程师等岗位构建专属面试题库和评估模型
- **多模态互动面试**：支持语音和文字输入，与 AI 面试官进行多轮沉浸式对话
- **能力评估与深度反馈**：从技术正确性、逻辑严谨性、沟通能力等多维度量化评分
- **个性化提升路径**：根据面试记录分析能力短板，智能推荐学习资源

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                      前端界面                            │
│  (HTML5 + CSS3 + JavaScript / Web Speech API)           │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   Spring Boot 3.2                        │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │ UserController │  │ PositionController │  │ InterviewController │   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
│                            ↓                            │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │ UserService │  │ PositionService │  │ InterviewService │  │ RagService │   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
│                            ↓                            │
│  ┌─────────────────┐         ┌─────────────────┐       │
│  │  SQLite Database │         │  LLM API (OpenRouter) │ │
│  └─────────────────┘         └─────────────────┘       │
└─────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- 现代浏览器（Chrome/Edge 90+）

### 2. 获取 API Key

本项目使用 OpenRouter 作为大模型 API 提供商（支持多个免费模型）：

1. 访问 [OpenRouter](https://openrouter.ai/)
2. 注册账号并获取 API Key
3. 设置环境变量：
   ```bash
   export LLM_API_KEY=your-api-key-here
   ```

### 3. 编译运行

```bash
# 编译项目
mvn clean package -DskipTests

# 运行项目
mvn spring-boot:run
```

### 4. 访问应用

打开浏览器访问：`http://localhost:8080/api/`

## 项目结构

```
InterviewSimulation/
├── src/main/java/com/ruijie/interview/
│   ├── Application.java          # 主程序入口
│   ├── config/                    # 配置类
│   │   └── LlmConfig.java        # LLM 配置
│   ├── entity/                    # 实体类
│   │   ├── User.java
│   │   ├── Position.java
│   │   ├── Question.java
│   │   ├── InterviewSession.java
│   │   ├── Answer.java
│   │   ├── EvaluationReport.java
│   │   └── LearningResource.java
│   ├── repository/                # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── PositionRepository.java
│   │   ├── QuestionRepository.java
│   │   ├── InterviewSessionRepository.java
│   │   ├── AnswerRepository.java
│   │   ├── EvaluationReportRepository.java
│   │   └── LearningResourceRepository.java
│   ├── service/                   # 业务逻辑层
│   │   ├── LlmService.java       # 大模型调用服务
│   │   ├── RagService.java       # RAG 检索服务
│   │   ├── UserService.java
│   │   ├── PositionService.java
│   │   ├── QuestionService.java
│   │   └── InterviewService.java
│   └── controller/                # 控制器层
│       ├── UserController.java
│       ├── PositionController.java
│       └── InterviewController.java
├── src/main/resources/
│   ├── application.yml           # 应用配置
│   └── static/
│       └── index.html            # 前端页面
├── data/                         # 数据目录（运行时创建）
│   ├── interview.db              # SQLite 数据库
│   └── knowledge-base/           # RAG 知识库
└── docs/                         # 文档
    ├── 环境配置说明.md
    └── RAG 模块说明.md
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
- `POST /api/user/login` - 用户登录
- `POST /api/user/register` - 用户注册
- `GET /api/user/{id}` - 获取用户信息
- `PUT /api/user/{id}` - 更新用户信息

### 岗位接口
- `GET /api/position/list` - 获取岗位列表
- `GET /api/position/{code}` - 获取岗位详情
- `GET /api/position/{code}/questions` - 获取岗位题库
- `GET /api/position/{code}/knowledge` - 获取岗位知识库

### 面试接口
- `POST /api/interview/start` - 开始面试
- `GET /api/interview/{id}` - 获取面试会话
- `POST /api/interview/{sessionId}/answer` - 提交回答
- `GET /api/interview/{sessionId}/report` - 获取评估报告

## RAG 模块

本项目包含完整的 RAG（检索增强生成）实现，用于：
- 存储各岗位的技术知识点和面试考点
- 在评估时检索相关知识，增强 AI 评估的准确性
- 支持用户自行下载和更新知识库

详细文档请参阅：[docs/RAG 模块说明.md](docs/RAG 模块说明.md)

## 配置说明

### 大模型配置

在 `application.yml` 中配置：

```yaml
app:
  llm:
    provider: openrouter
    api-key: ${LLM_API_KEY:your-api-key-here}
    base-url: https://openrouter.ai/api/v1
    model: google/gemma-2-9b-it:free
    fallback-models:
      - meta-llama/llama-3-8b-instruct:free
      - mistralai/mistral-7b-instruct:free
```

### 数据库配置

默认使用 SQLite 数据库：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/interview.db
    driver-class-name: org.sqlite.JDBC
```

详细配置说明请参阅：[docs/环境配置说明.md](docs/环境配置说明.md)

## 功能演示

### 1. 登录注册
- 简洁的登录注册界面
- 支持用户名密码认证

### 2. 岗位选择
- 卡片式展示各岗位信息
- 显示岗位难度、题量、预计时长

### 3. 在线面试
- 文字输入回答
- 支持语音输入（Web Speech API）
- 实时显示面试进度

### 4. 评估报告
- 多维度评分（技术能力、沟通能力、逻辑思维等）
- 亮点与不足分析
- 个性化改进建议

### 5. 历史记录
- 查看历次面试记录
- 追踪能力成长曲线

## 技术特点

1. **无需本地部署模型**：使用 OpenRouter 等免费 API，降低使用门槛
2. **RAG 增强**：专业知识库支持，使评估更准确
3. **多模态交互**：支持文字和语音输入
4. **可扩展架构**：知识库可独立更新，支持新增岗位

## 开发计划

- [ ] 添加更多岗位类别
- [ ] 实现向量检索优化
- [ ] 增加学习资源推荐
- [ ] 能力成长可视化
- [ ] 支持追问功能

## 许可证

本项目为锐捷网络教育行业解决方案演示版本。

## 联系方式

锐捷网络 - 教育行业解决方案团队

---

© 2024 锐捷网络 | AI 模拟面试与能力提升软件