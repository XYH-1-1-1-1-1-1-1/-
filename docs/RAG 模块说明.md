# RAG (检索增强生成) 模块说明

## 一、模块概述

### 1.1 什么是 RAG

RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合信息检索和文本生成的技术。它通过以下步骤增强大模型的输出质量：

1. **检索**：从知识库中检索与当前问题相关的信息
2. **增强**：将检索到的信息作为上下文提供给大模型
3. **生成**：大模型基于增强的上下文生成更准确的回答

### 1.2 本项目 RAG 模块的作用

在 AI 模拟面试系统中，RAG 模块主要用于：

- **岗位知识库管理**：存储各岗位的技术栈、面试考点、优秀回答范例
- **智能检索**：根据面试问题检索相关知识
- **评估增强**：为 AI 评估提供专业知识支持，使评估更准确
- **学习推荐**：基于用户薄弱环节推荐相关知识

## 二、架构设计

### 2.1 模块结构

```
com.ruijie.interview.service
├── RagService.java          # RAG 服务核心类
│   ├── KnowledgeItem        # 知识条目内部类
│   ├── loadKnowledgeBase()  # 加载知识库
│   ├── retrieveKnowledge()  # 检索知识
│   ├── buildRagContext()    # 构建 RAG 上下文
│   └── chatWithRag()        # RAG 增强的对话
```

### 2.2 数据流

```
用户问题
    ↓
[关键词提取]
    ↓
[知识检索] ←→ [知识库 JSON 文件]
    ↓
[上下文构建]
    ↓
[大模型调用] → [增强的回答/评估]
```

## 三、知识库结构

### 3.1 文件组织

知识库采用 JSON 文件格式存储，便于用户自行下载和更新：

```
data/knowledge-base/
├── backend.json      # 后端开发工程师知识库
├── frontend.json     # 前端开发工程师知识库
├── qa.json          # 测试工程师知识库
└── algorithm.json   # 算法工程师知识库
```

### 3.2 JSON 文件格式

```json
{
  "positionId": "backend",
  "positionName": "后端开发工程师",
  "categories": [
    {
      "name": "Java 基础",
      "items": [
        {
          "id": "1",
          "title": "HashMap 原理",
          "content": "HashMap 内部使用数组 + 链表 + 红黑树的结构...",
          "tags": "哈希冲突、负载因子、扩容机制",
          "importance": 5
        }
      ]
    }
  ]
}
```

### 3.3 知识条目字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 唯一标识 |
| title | String | 知识点标题 |
| content | String | 详细内容描述 |
| tags | String | 关键词标签 |
| importance | Integer | 重要程度 (1-5) |

## 四、核心功能实现

### 4.1 知识库加载

```java
@PostConstruct
public void init() {
    loadKnowledgeBase();
}

public void loadKnowledgeBase() {
    // 1. 检查知识库目录是否存在
    Path kbPath = Paths.get(KNOWLEDGE_BASE_PATH);
    if (!Files.exists(kbPath)) {
        Files.createDirectories(kbPath);
        createDefaultKnowledgeBase();  // 创建默认知识库
    }
    
    // 2. 加载所有 JSON 文件
    Files.list(kbPath)
        .filter(path -> path.toString().endsWith(".json"))
        .forEach(this::loadKnowledgeFile);
}
```

### 4.2 知识检索算法

```java
public List<KnowledgeItem> retrieveKnowledge(String positionId, String query, int limit) {
    // 1. 获取岗位相关知识
    Map<String, List<KnowledgeItem>> positionKnowledge = knowledgeBase.get(positionId);
    
    // 2. 收集所有知识条目
    List<KnowledgeItem> allItems = new ArrayList<>();
    positionKnowledge.values().forEach(allItems::addAll);
    
    // 3. 基于关键词匹配排序
    String lowerQuery = query.toLowerCase();
    allItems.sort((a, b) -> {
        int scoreA = calculateRelevanceScore(a, lowerQuery);
        int scoreB = calculateRelevanceScore(b, lowerQuery);
        return scoreB - scoreA;  // 降序排列
    });
    
    // 4. 返回最相关的 N 条
    return allItems.stream().limit(limit).toList();
}

private int calculateRelevanceScore(KnowledgeItem item, String query) {
    int score = 0;
    
    // 标题匹配权重最高
    if (item.title.toLowerCase().contains(query)) score += 10;
    
    // 内容匹配
    if (item.content.toLowerCase().contains(query)) score += 5;
    
    // 标签匹配
    if (item.tags.toLowerCase().contains(query)) score += 8;
    
    // 重要程度加分
    score += item.importance;
    
    return score;
}
```

### 4.3 RAG 上下文构建

```java
public String buildRagContext(String positionId, String query) {
    List<KnowledgeItem> relevantKnowledge = retrieveKnowledge(positionId, query, 5);
    
    if (relevantKnowledge.isEmpty()) {
        return "";
    }
    
    StringBuilder context = new StringBuilder();
    context.append("【相关知识库】\n");
    for (KnowledgeItem item : relevantKnowledge) {
        context.append(String.format("- %s: %s\n", item.title, item.content));
    }
    
    return context.toString();
}
```

### 4.4 RAG 增强对话

```java
public String chatWithRag(String positionId, String query, String systemPrompt) {
    // 1. 检索相关知识
    String ragContext = buildRagContext(positionId, query);
    
    // 2. 构建增强的提示
    String enhancedPrompt = query;
    if (!ragContext.isEmpty()) {
        enhancedPrompt = ragContext + "\n\n【问题】\n" + query;
    }
    
    // 3. 调用大模型
    return llmService.simpleChat(enhancedPrompt, systemPrompt);
}
```

## 五、使用示例

### 5.1 在面试评估中使用 RAG

```java
// 在 InterviewService 中生成评估报告时
@Transactional
public EvaluationReport generateEvaluationReport(InterviewSession session) {
    Position position = positionService.findByCode(session.getPositionId()).orElse(null);
    String positionName = position != null ? position.getName() : "技术岗位";
    
    // 使用 RAG 增强评估
    String evalJson = llmService.generateEvaluationReport(
        session.getConversationHistory(), 
        positionName
    );
    
    // ... 解析并保存报告
}
```

### 5.2 在 API 中暴露知识检索

```java
// PositionController 中
@GetMapping("/{code}/knowledge/search")
public ApiResponse<List<KnowledgeItem>> searchKnowledge(
        @PathVariable String code,
        @RequestParam String query) {
    List<KnowledgeItem> items = ragService.retrieveKnowledge(code, query, 20);
    return ApiResponse.success(items);
}
```

## 六、知识库扩展

### 6.1 添加新岗位知识库

1. 在 `data/knowledge-base/` 目录创建新的 JSON 文件
2. 按照标准格式填充内容
3. 重启应用自动加载

### 6.2 知识库内容来源

- **官方文档**：各技术栈的官方文档
- **面试题库**：整理自网络面经、技术博客
- **优秀回答**：来自资深工程师的分享
- **持续更新**：根据用户反馈和评估结果优化

### 6.3 知识库下载

用户可自行下载更新的知识库文件：

```bash
# 示例：从 Git 仓库拉取最新知识库
cd data/knowledge-base
git pull origin main
```

或在应用中调用：
```java
ragService.loadKnowledgeBase();  // 重新加载知识库
```

## 七、向量增强搜索

### 7.1 架构概述

当前 RAG 模块已升级为**向量增强搜索**架构，结合了传统的关键词匹配和先进的语义向量搜索：

```
┌──────────────┐     ┌───────────────┐     ┌───────────────┐
│   用户输入    │ ──→ │ EmbeddingSvc  │ ──→ │ VectorStore   │
│              │     │ 文本→向量转换  │     │ 向量相似度检索 │
└──────────────┘     └───────────────┘     └───────────────┘
         │                      │                      │
         │                      │                      ▼
         │              ┌───────────────┐     ┌───────────────┐
         └─────────────→│ Keyword Match │ ──→ │ 混合排序引擎   │
                        │ 关键词匹配     │     │ (加权融合)    │
                        └───────────────┘     └───────────────┘
                                                       │
                                                       ▼
                                              ┌───────────────┐
                                              │ 检索结果返回   │
                                              └───────────────┘
```

### 7.2 新增组件

| 组件 | 说明 |
|------|------|
| `EmbeddingService` | 调用 DashScope text-embedding-v3 模型将文本转换为 1024 维向量 |
| `VectorStoreService` | 基于 SQLite 的向量存储和检索服务，支持余弦相似度计算 |
| `RagController` | REST API 接口，提供搜索、向量化管理等功能 |

### 7.3 混合搜索算法

混合搜索将向量相似度和关键词匹配加权融合：

```
混合分数 = (向量权重 × 向量相似度) + ((1 - 向量权重) × 归一化关键词分数)
```

- **向量相似度**：使用余弦相似度计算，范围 0-1
- **关键词分数**：基于标题、内容、标签的匹配程度
- **默认权重**：向量占 70%，关键词占 30%（可通过配置调整）

### 7.4 配置项

在 `application.yml` 中可配置：

```yaml
app:
  rag:
    # 是否启用向量增强搜索（默认启用）
    enable-vector-search: true
    # 向量搜索在混合搜索中的权重 (0-1)
    vector-weight: 0.7
```

### 7.5 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/rag/search` | POST | 智能搜索（自动选择最佳检索方式） |
| `/api/rag/search/vector` | POST | 仅使用向量搜索 |
| `/api/rag/search/keyword` | POST | 仅使用关键词搜索 |
| `/api/rag/chat` | POST | RAG 增强对话 |
| `/api/rag/vector/status` | GET | 获取向量化状态 |
| `/api/rag/vector/vectorize` | POST | 手动触向量化 |
| `/api/rag/vector/weight` | POST | 设置向量搜索权重 |
| `/api/rag/reload` | POST | 重新加载知识库 |

### 7.6 向量化流程

1. **应用启动时**：自动异步向量化所有知识库条目
2. **知识库更新时**：调用 `/api/rag/reload` 重新加载并向量化
3. **降级策略**：向量搜索失败时自动回退到关键词匹配

### 7.7 缓存机制

`EmbeddingService` 内置内存缓存，避免重复调用 API 生成向量：

```java
// Embedding 缓存：文本内容 -> 向量
private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
```

## 八、与 LLM 服务集成

RAG 模块与 LLM 服务紧密配合：

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   用户输入   │ ──→ │  RagService │ ──→ │  LlmService │
│             │     │  检索知识    │     │  生成回答    │
└─────────────┘     └─────────────┘     └─────────────┘
                           ↓
                    ┌─────────────┐
                    │  知识库 JSON │
                    └─────────────┘
```

在 `LlmService` 中调用 RAG：

```java
// 评估回答时检索相关知识
String ragContext = ragService.buildRagContext(positionId, question);
String enhancedPrompt = ragContext + "\n\n问题：" + question + "\n回答：" + answer;
String evaluation = llmService.simpleChat(enhancedPrompt, evaluationSystemPrompt);
```

## 九、总结

RAG 模块通过以下方式提升系统能力：

1. **专业性**：提供岗位相关的专业知识支持
2. **准确性**：使 AI 评估和反馈更加准确
3. **可扩展**：知识库可独立更新，无需修改代码
4. **可解释**：检索到的知识可作为评估依据

---

© 2024 锐捷网络 | AI 模拟面试与能力提升软件