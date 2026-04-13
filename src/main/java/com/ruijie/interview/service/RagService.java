package com.ruijie.interview.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * RAG (检索增强生成) 服务
 * 
 * 功能说明：
 * 1. 加载岗位相关知识库（技术栈、常见面试题、优秀回答范例）
 * 2. 基于关键词和语义相似度进行知识检索
 * 3. 将检索到的知识作为上下文提供给大模型，增强回答质量
 * 
 * 知识库存储结构：
 * - 使用 JSON 文件存储知识库数据（便于用户自行下载和更新）
 * - 支持内存索引，快速检索
 * - 可配置知识库路径
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    @Autowired
    private LlmService llmService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    // 是否启用向量增强搜索
    @Value("${app.rag.enable-vector-search:true}")
    private boolean enableVectorSearch;

    // 向量搜索权重（混合搜索时向量所占权重）
    @Value("${app.rag.vector-weight:0.7}")
    private double vectorWeight;

    // 异步任务线程池，用于向量化知识库
    private final ExecutorService vectorizationExecutor = Executors.newFixedThreadPool(2);

    // 知识库数据：positionId -> category -> List<KnowledgeItem>
    private final Map<String, Map<String, List<KnowledgeItem>>> knowledgeBase = new ConcurrentHashMap<>();

    // 知识库文件路径
    private static final String KNOWLEDGE_BASE_PATH = "./data/knowledge-base";

    /**
     * 知识库条目
     */
    public static class KnowledgeItem {
        public String id;
        public String positionId;
        public String category;
        public String title;
        public String content;
        public String tags;
        public String example;
        public int importance;

        public KnowledgeItem() {}

        public KnowledgeItem(String id, String positionId, String category, String title, 
                            String content, String tags, String example, int importance) {
            this.id = id;
            this.positionId = positionId;
            this.category = category;
            this.title = title;
            this.content = content;
            this.tags = tags;
            this.example = example;
            this.importance = importance;
        }
    }

    @PostConstruct
    public void init() {
        try {
            loadKnowledgeBase();
            // 异步向量化知识库
            if (enableVectorSearch) {
                vectorizationExecutor.submit(this::vectorizeKnowledgeBase);
            }
        } catch (Exception e) {
            log.warn("知识库加载失败（可能是文件损坏），将重新创建。错误：{}", e.getMessage());
        }
    }

    /**
     * 加载知识库
     */
    public void loadKnowledgeBase() {
        log.info("正在加载知识库...");
        knowledgeBase.clear();

        try {
            Path kbPath = Paths.get(KNOWLEDGE_BASE_PATH);
            if (!Files.exists(kbPath)) {
                Files.createDirectories(kbPath);
                log.info("创建知识库目录：{}", KNOWLEDGE_BASE_PATH);
                createDefaultKnowledgeBase();
            }

            // 加载所有岗位的知识库文件
            Files.list(kbPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadKnowledgeFile);

            log.info("知识库加载完成，共加载 {} 个岗位的知识", knowledgeBase.size());

        } catch (IOException e) {
            log.error("加载知识库失败", e);
        }
    }

    /**
     * 加载单个知识库文件
     */
    private void loadKnowledgeFile(Path filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            JSONObject json = JSON.parseObject(content.toString());
            String positionId = json.getString("positionId");
            
            Map<String, List<KnowledgeItem>> positionKnowledge = new HashMap<>();
            
            JSONArray categories = json.getJSONArray("categories");
            if (categories != null) {
                for (int i = 0; i < categories.size(); i++) {
                    JSONObject category = categories.getJSONObject(i);
                    String categoryName = category.getString("name");
                    List<KnowledgeItem> items = new ArrayList<>();
                    
                    JSONArray itemsArray = category.getJSONArray("items");
                    if (itemsArray != null) {
                        for (int j = 0; j < itemsArray.size(); j++) {
                            JSONObject itemJson = itemsArray.getJSONObject(j);
                            KnowledgeItem item = itemJson.toJavaObject(KnowledgeItem.class);
                            item.positionId = positionId;
                            item.category = categoryName;
                            items.add(item);
                        }
                    }
                    positionKnowledge.put(categoryName, items);
                }
            }
            
            knowledgeBase.put(positionId, positionKnowledge);
            log.info("加载岗位 [{}] 的知识，共 {} 个类别", positionId, positionKnowledge.size());

        } catch (IOException e) {
            log.error("加载知识库文件失败：{}", filePath, e);
        }
    }

    /**
     * 创建默认知识库文件
     */
    private void createDefaultKnowledgeBase() {
        // 后端开发工程师知识库
        createBackendKnowledgeBase();
        // 前端开发工程师知识库
        createFrontendKnowledgeBase();
        // 测试工程师知识库
        createQAKnowledgeBase();
        
        log.info("已创建默认知识库文件");
    }

    /**
     * 创建后端开发知识库
     */
    private void createBackendKnowledgeBase() {
        JSONObject backendKB = new JSONObject();
        backendKB.put("positionId", "backend");
        backendKB.put("positionName", "后端开发工程师");

        JSONArray categories = new JSONArray();

        // Java 基础
        JSONObject javaCategory = new JSONObject();
        javaCategory.put("name", "Java 基础");
        JSONArray javaItems = new JSONArray();
        
        javaItems.add(createKnowledgeItem("1", "Java 基础", "HashMap 原理", 
            "HashMap 是 Java 中最常用的集合类之一，基于哈希表实现。",
            "HashMap 内部使用数组 + 链表 + 红黑树（JDK8+）的结构。当发生哈希冲突时，使用链表法解决。" +
            "当链表长度超过 8 且数组长度超过 64 时，链表转换为红黑树以提高查找效率。" +
            "HashMap 不是线程安全的，多线程环境下应使用 ConcurrentHashMap。",
            "理解哈希冲突、负载因子、扩容机制", 5));
        
        javaItems.add(createKnowledgeItem("2", "Java 基础", "ConcurrentHashMap 原理",
            "ConcurrentHashMap 是线程安全的哈希表实现。",
            "JDK1.7 使用分段锁（Segment）机制，JDK1.8 改用 CAS+synchronized 锁单个桶。" +
            "锁粒度更细，并发性能更高。不支持 null 键和 null 值。",
            "掌握锁粒度优化思路", 5));

        javaItems.add(createKnowledgeItem("3", "Java 基础", "JVM 内存模型",
            "JVM 内存分为堆、栈、方法区等区域。",
            "堆：存储对象实例，是 GC 的主要区域。栈：存储局部变量和方法调用。方法区：存储类信息、常量池。" +
            "理解内存溢出和内存泄漏的区别。",
            "GC 根对象、可达性分析", 5));
        
        javaCategory.put("items", javaItems);
        categories.add(javaCategory);

        // 数据库
        JSONObject dbCategory = new JSONObject();
        dbCategory.put("name", "数据库");
        JSONArray dbItems = new JSONArray();
        
        dbItems.add(createKnowledgeItem("1", "数据库", "MySQL 索引原理",
            "MySQL 索引使用 B+ 树数据结构。",
            "B+ 树的特点：非叶子节点只存储索引不存储数据，叶子节点存储所有数据且形成链表。" +
            "优势：减少磁盘 IO，支持范围查询，查询效率稳定。",
            "聚簇索引、覆盖索引、最左前缀原则", 5));
        
        dbItems.add(createKnowledgeItem("2", "数据库", "事务隔离级别",
            "MySQL 支持四种事务隔离级别。",
            "读未提交、读已提交、可重复读（默认）、串行化。" +
            "InnoDB 通过 MVCC 和 Next-Key Lock 实现可重复读。",
            "脏读、幻读、不可重复读的区别", 5));
        
        dbCategory.put("items", dbItems);
        categories.add(dbCategory);

        // 框架
        JSONObject frameworkCategory = new JSONObject();
        frameworkCategory.put("name", "框架");
        JSONArray frameworkItems = new JSONArray();
        
        frameworkItems.add(createKnowledgeItem("1", "框架", "Spring Bean 生命周期",
            "Spring Bean 从创建到销毁经历多个阶段。",
            "实例化 -> 属性赋值 -> 初始化（各种 Aware 接口、BeanPostProcessor 前后置处理）-> 使用 -> 销毁。" +
            "理解各种扩展点对时机。",
            "BeanPostProcessor、InitializingBean、@PostConstruct", 4));
        
        frameworkItems.add(createKnowledgeItem("2", "框架", "Spring AOP 原理",
            "Spring AOP 基于动态代理实现。",
            "JDK 动态代理（接口）和 CGLIB 代理（类）。通过代理对象拦截目标方法调用，实现横切关注点。",
            "切面、切点、通知、织入概念", 4));
        
        frameworkCategory.put("items", frameworkItems);
        categories.add(frameworkCategory);

        // 系统设计
        JSONObject designCategory = new JSONObject();
        designCategory.put("name", "系统设计");
        JSONArray designItems = new JSONArray();
        
        designItems.add(createKnowledgeItem("1", "系统设计", "分布式锁实现",
            "分布式锁用于协调分布式系统中的资源访问。",
            "实现方式：Redis（SETNX+ 过期时间、Redlock）、ZooKeeper（临时顺序节点）、数据库（唯一索引）。" +
            "需要考虑锁超时、锁续期、可重入等问题。",
            "Redisson、Curator 等成熟方案", 4));
        
        designItems.add(createKnowledgeItem("2", "系统设计", "缓存穿透/击穿/雪崩",
            "缓存常见问题及解决方案。",
            "穿透：查询不存在的数据，解决：布隆过滤器、缓存空值。" +
            "击穿：热点 key 过期，解决：互斥锁、逻辑过期。" +
            "雪崩：大量 key 同时过期，解决：随机过期时间、高可用。",
            "理解三种问题的区别和应对策略", 4));
        
        designCategory.put("items", designItems);
        categories.add(designCategory);

        backendKB.put("categories", categories);
        saveKnowledgeFile("backend.json", backendKB);
    }

    /**
     * 创建前端开发知识库
     */
    private void createFrontendKnowledgeBase() {
        JSONObject frontendKB = new JSONObject();
        frontendKB.put("positionId", "frontend");
        frontendKB.put("positionName", "前端开发工程师");

        JSONArray categories = new JSONArray();

        // JavaScript
        JSONObject jsCategory = new JSONObject();
        jsCategory.put("name", "JavaScript");
        JSONArray jsItems = new JSONArray();
        
        jsItems.add(createKnowledgeItem("1", "JavaScript", "闭包原理",
            "闭包是函数和其词法环境的组合。",
            "闭包可以访问函数外部的变量，常用于数据私有化、函数工厂等场景。" +
            "注意闭包可能导致内存泄漏。",
            "作用域链、词法环境", 5));
        
        jsItems.add(createKnowledgeItem("2", "JavaScript", "原型链",
            "JavaScript 通过原型链实现继承。",
            "每个对象都有__proto__指向其构造函数的 prototype，形成原型链。" +
            "ES6 class 是语法糖，本质仍是原型继承。",
            "Object.prototype、instanceof 原理", 5));
        
        jsItems.add(createKnowledgeItem("3", "JavaScript", "事件循环",
            "JavaScript 使用事件循环处理异步任务。",
            "执行栈、宏任务队列、微任务队列。Promise.then 属于微任务，setTimeout 属于宏任务。" +
            "理解执行顺序对编写正确代码至关重要。",
            "宏任务 vs 微任务执行顺序", 5));
        
        jsCategory.put("items", jsItems);
        categories.add(jsCategory);

        // Vue/React
        JSONObject frameworkCategory = new JSONObject();
        frameworkCategory.put("name", "前端框架");
        JSONArray frameworkItems = new JSONArray();
        
        frameworkItems.add(createKnowledgeItem("1", "前端框架", "Vue 响应式原理",
            "Vue 通过数据劫持实现响应式。",
            "Vue2 使用 Object.defineProperty，Vue3 使用 Proxy。" +
            "依赖收集 + 发布通知机制实现视图更新。",
            "Dep、Watcher、Proxy", 5));
        
        frameworkItems.add(createKnowledgeItem("2", "前端框架", "React Hooks 原理",
            "Hooks 是 React 16.8 引入的新特性。",
            "useState、useEffect 等 Hook 函数让函数组件拥有状态和生命周期。" +
            "Hook 调用顺序必须一致，不能放在条件语句中。",
            "useState、useEffect、useMemo、useCallback", 4));
        
        frameworkCategory.put("items", frameworkItems);
        categories.add(frameworkCategory);

        // CSS
        JSONObject cssCategory = new JSONObject();
        cssCategory.put("name", "CSS");
        JSONArray cssItems = new JSONArray();
        
        cssItems.add(createKnowledgeItem("1", "CSS", "Flex 布局",
            "Flex 是弹性盒子布局模型。",
            "主轴、交叉轴概念。justify-content、align-items 等属性控制对齐方式。" +
            "适合一维布局场景。",
            "flex-direction、flex-wrap、flex-grow", 4));
        
        cssItems.add(createKnowledgeItem("2", "CSS", "BFC",
            "BFC 是块级格式化上下文。",
            "触发条件：float、overflow、position 等。" +
            "应用：清除浮动、防止 margin 重叠。",
            "理解 BFC 的隔离效果", 3));
        
        cssCategory.put("items", cssItems);
        categories.add(cssCategory);

        // 网络
        JSONObject networkCategory = new JSONObject();
        networkCategory.put("name", "网络");
        JSONArray networkItems = new JSONArray();
        
        networkItems.add(createKnowledgeItem("1", "网络", "HTTP 缓存",
            "HTTP 缓存分为强缓存和协商缓存。",
            "强缓存：Cache-Control、Expires。协商缓存：ETag、Last-Modified。" +
            "合理设置缓存策略可显著提升性能。",
            "max-age、no-cache、no-store 区别", 4));
        
        networkItems.add(createKnowledgeItem("2", "网络", "跨域解决方案",
            "跨域是浏览器的同源策略限制。",
            "解决方案：CORS、JSONP、代理服务器、postMessage。" +
            "CORS 需要服务端配合设置响应头。",
            "简单请求 vs 预检请求", 4));
        
        networkCategory.put("items", networkItems);
        categories.add(networkCategory);

        frontendKB.put("categories", categories);
        saveKnowledgeFile("frontend.json", frontendKB);
    }

    /**
     * 创建测试工程师知识库
     */
    private void createQAKnowledgeBase() {
        JSONObject qaKB = new JSONObject();
        qaKB.put("positionId", "qa");
        qaKB.put("positionName", "测试工程师");

        JSONArray categories = new JSONArray();

        // 测试基础
        JSONObject basicCategory = new JSONObject();
        basicCategory.put("name", "测试基础");
        JSONArray basicItems = new JSONArray();
        
        basicItems.add(createKnowledgeItem("1", "测试基础", "测试分类",
            "测试可从多个维度分类。",
            "按阶段：单元测试、集成测试、系统测试、验收测试。" +
            "按方式：黑盒测试、白盒测试、灰盒测试。" +
            "按目的：功能测试、性能测试、安全测试。",
            "理解各分类的应用场景", 5));
        
        basicItems.add(createKnowledgeItem("2", "测试基础", "测试用例设计方法",
            "常用测试用例设计方法。",
            "等价类划分、边界值分析、因果图、正交实验、场景法等。" +
            "边界值是最有效的用例设计方法。",
            "等价类、边界值的具体应用", 5));
        
        basicCategory.put("items", basicItems);
        categories.add(basicCategory);

        // 自动化测试
        JSONObject autoCategory = new JSONObject();
        autoCategory.put("name", "自动化测试");
        JSONArray autoItems = new JSONArray();
        
        autoItems.add(createKnowledgeItem("1", "自动化测试", "Selenium 原理",
            "Selenium 是 Web UI 自动化测试工具。",
            "通过 WebDriver 驱动浏览器执行操作。支持多种语言和浏览器。" +
            "元素定位：id、name、xpath、css selector 等。",
            "显式等待 vs 隐式等待", 4));
        
        autoItems.add(createKnowledgeItem("2", "自动化测试", "接口自动化",
            "接口自动化测试框架。",
            "工具：Postman、JMeter、RestAssured、pytest。" +
            "关键点：参数化、断言、数据驱动、报告生成。",
            "接口测试用例设计要点", 4));
        
        autoCategory.put("items", autoItems);
        categories.add(autoCategory);

        // 性能测试
        JSONObject perfCategory = new JSONObject();
        perfCategory.put("name", "性能测试");
        JSONArray perfItems = new JSONArray();
        
        perfItems.add(createKnowledgeItem("1", "性能测试", "性能指标",
            "常见性能测试指标。",
            "响应时间、吞吐量（TPS/QPS）、并发用户数、资源利用率、错误率。" +
            "理解各指标的含义和关系。",
            "TPS vs QPS、99 线响应时间", 4));
        
        perfItems.add(createKnowledgeItem("2", "性能测试", "JMeter 使用",
            "JMeter 是常用性能测试工具。",
            "线程组模拟用户，采样器发送请求，监听器查看结果。" +
            "支持参数化、断言、关联等功能。",
            "阶梯加压、集合点设置", 3));
        
        perfCategory.put("items", perfItems);
        categories.add(perfCategory);

        qaKB.put("categories", categories);
        saveKnowledgeFile("qa.json", qaKB);
    }

    /**
     * 创建知识条目
     */
    private JSONObject createKnowledgeItem(String id, String category, String title, 
                                          String brief, String content, String tags, int importance) {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("category", category);
        item.put("title", title);
        item.put("content", content);
        item.put("tags", tags);
        item.put("importance", importance);
        return item;
    }

    /**
     * 保存知识库文件
     */
    private void saveKnowledgeFile(String filename, JSONObject content) {
        try {
            Path filePath = Paths.get(KNOWLEDGE_BASE_PATH, filename);
            Files.writeString(filePath, content.toJSONString(com.alibaba.fastjson.serializer.SerializerFeature.PrettyFormat));
            log.info("保存知识库文件：{}", filename);
        } catch (IOException e) {
            log.error("保存知识库文件失败：{}", filename, e);
        }
    }

    /**
     * 检索相关知识
     *
     * @param positionId 岗位 ID
     * @param query 查询关键词
     * @param limit 返回数量
     * @return 相关知识列表
     */
    public List<KnowledgeItem> retrieveKnowledge(String positionId, String query, int limit) {
        Map<String, List<KnowledgeItem>> positionKnowledge = knowledgeBase.get(positionId);
        if (positionKnowledge == null) {
            log.warn("未找到岗位 [{}] 的知识库", positionId);
            return new ArrayList<>();
        }

        List<KnowledgeItem> allItems = new ArrayList<>();
        positionKnowledge.values().forEach(allItems::addAll);

        // 基于关键词匹配排序
        String lowerQuery = query.toLowerCase();
        allItems.sort((a, b) -> {
            int scoreA = calculateRelevanceScore(a, lowerQuery);
            int scoreB = calculateRelevanceScore(b, lowerQuery);
            return scoreB - scoreA;
        });

        return allItems.stream().limit(limit).toList();
    }

    /**
     * 计算相关性分数
     */
    private int calculateRelevanceScore(KnowledgeItem item, String query) {
        int score = 0;
        String lowerTitle = item.title.toLowerCase();
        String lowerContent = item.content.toLowerCase();
        String lowerTags = item.tags != null ? item.tags.toLowerCase() : "";

        if (lowerTitle.contains(query)) score += 10;
        if (lowerContent.contains(query)) score += 5;
        if (lowerTags.contains(query)) score += 8;

        // 分词匹配
        String[] words = query.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                if (lowerTitle.contains(word)) score += 3;
                if (lowerTags.contains(word)) score += 2;
            }
        }

        score += item.importance;
        return score;
    }

    /**
     * 获取岗位所有知识类别
     */
    public Set<String> getKnowledgeCategories(String positionId) {
        Map<String, List<KnowledgeItem>> positionKnowledge = knowledgeBase.get(positionId);
        if (positionKnowledge == null) {
            return new HashSet<>();
        }
        return positionKnowledge.keySet();
    }

    /**
     * 获取某类别的知识
     */
    public List<KnowledgeItem> getKnowledgeByCategory(String positionId, String category) {
        Map<String, List<KnowledgeItem>> positionKnowledge = knowledgeBase.get(positionId);
        if (positionKnowledge == null) {
            return new ArrayList<>();
        }
        return positionKnowledge.getOrDefault(category, new ArrayList<>());
    }

    /**
     * 构建 RAG 上下文
     * 将检索到的知识格式化为大模型可用的上下文
     */
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

    /**
     * 使用 RAG 增强的大模型调用
     */
    public String chatWithRag(String positionId, String query, String systemPrompt) {
        String ragContext = buildRagContext(positionId, query);
        
        String enhancedPrompt = query;
        if (!ragContext.isEmpty()) {
            enhancedPrompt = ragContext + "\n\n【问题】\n" + query;
        }

        return llmService.simpleChat(enhancedPrompt, systemPrompt);
    }

    /**
     * 使用 RAG 评估单道题目回答
     * 
     * @param positionId 岗位 ID
     * @param category 问题类别
     * @param question 问题
     * @param answer 回答
     * @return 评估结果 JSON 字符串
     */
    public String evaluateAnswerWithRag(String positionId, String category, String question, String answer) {
        // 检索相关知识
        String ragContext = buildRagContext(positionId, question);
        
        // 使用 LLM 进行评估
        return llmService.evaluateSingleAnswer(question, answer, 
            getPositionName(positionId), category, ragContext);
    }

    /**
     * 获取岗位名称
     */
    private String getPositionName(String positionId) {
        Map<String, List<KnowledgeItem>> positionKnowledge = knowledgeBase.get(positionId);
        if (positionKnowledge == null) {
            return "技术岗位";
        }
        // 从知识库中获取岗位名称（这里简化处理）
        return switch (positionId) {
            case "backend" -> "后端开发工程师";
            case "frontend" -> "前端开发工程师";
            case "qa" -> "测试工程师";
            case "algorithm" -> "算法工程师";
            default -> "技术岗位";
        };
    }

    // ==================== 向量增强搜索功能 ====================

    /**
     * 向量化整个知识库
     * 在应用启动时异步执行，或手动触发
     */
    public void vectorizeKnowledgeBase() {
        log.info("开始向量化知识库...");
        int totalVectorized = 0;

        for (Map.Entry<String, Map<String, List<KnowledgeItem>>> positionEntry : knowledgeBase.entrySet()) {
            String positionId = positionEntry.getKey();
            log.info("正在向量化岗位 [{}] 的知识...", positionId);

            // 先删除该岗位的旧向量
            vectorStoreService.deleteByPosition(positionId);

            for (Map.Entry<String, List<KnowledgeItem>> categoryEntry : positionEntry.getValue().entrySet()) {
                String category = categoryEntry.getKey();
                List<KnowledgeItem> items = categoryEntry.getValue();

                for (KnowledgeItem item : items) {
                    try {
                        // 将标题+内容+标签组合为向量输入文本
                        String vectorInput = buildVectorInput(item);
                        float[] vector = embeddingService.getEmbedding(vectorInput);

                        // 保存到向量存储
                        String vectorId = positionId + "_" + category + "_" + item.id;
                        vectorStoreService.saveVector(
                            vectorId,
                            positionId,
                            category,
                            item.title,
                            item.content,
                            item.tags,
                            vector
                        );
                        totalVectorized++;
                    } catch (Exception e) {
                        log.error("向量化知识条目失败: positionId={}, category={}, id={}", 
                            positionId, category, item.id, e);
                    }
                }
            }
        }

        log.info("知识库向量化完成，共向量化 {} 条知识", totalVectorized);
    }

    /**
     * 构建用于向量化的输入文本
     * 组合标题、内容、标签，提供更丰富的语义表示
     */
    private String buildVectorInput(KnowledgeItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.title);
        if (item.content != null && !item.content.isEmpty()) {
            sb.append(" ").append(item.content);
        }
        if (item.tags != null && !item.tags.isEmpty()) {
            sb.append(" ").append(item.tags);
        }
        return sb.toString();
    }

    /**
     * 增强检索：结合关键词匹配和向量语义搜索
     *
     * @param positionId 岗位 ID
     * @param query 查询文本
     * @param limit 返回数量
     * @return 检索结果列表
     */
    public List<KnowledgeItem> retrieveKnowledgeEnhanced(String positionId, String query, int limit) {
        // 如果向量搜索未启用，回退到纯关键词匹配
        if (!enableVectorSearch || vectorStoreService.isEmpty()) {
            log.debug("向量搜索未启用或向量库为空，使用关键词匹配检索");
            return retrieveKnowledge(positionId, query, limit);
        }

        try {
            // 获取查询向量
            float[] queryVector = embeddingService.getEmbedding(query);

            // 执行混合搜索
            List<VectorStoreService.VectorSearchResult> vectorResults = 
                vectorStoreService.hybridSearch(query, queryVector, positionId, limit, vectorWeight);

            // 将向量搜索结果转换为 KnowledgeItem
            List<KnowledgeItem> enhancedResults = new ArrayList<>();
            for (VectorStoreService.VectorSearchResult result : vectorResults) {
                KnowledgeItem item = new KnowledgeItem();
                item.id = result.getId();
                item.positionId = result.getPositionId();
                item.category = result.getCategory();
                item.title = result.getTitle();
                item.content = result.getContent();
                item.tags = result.getTags();
                enhancedResults.add(item);
            }

            log.debug("增强检索完成，返回 {} 条结果，向量权重: {}", enhancedResults.size(), vectorWeight);
            return enhancedResults;

        } catch (Exception e) {
            log.error("向量增强检索失败，回退到关键词匹配: {}", e.getMessage(), e);
            return retrieveKnowledge(positionId, query, limit);
        }
    }

    /**
     * 仅使用向量搜索
     *
     * @param positionId 岗位 ID
     * @param query 查询文本
     * @param limit 返回数量
     * @return 向量搜索结果
     */
    public List<KnowledgeItem> retrieveKnowledgeByVector(String positionId, String query, int limit) {
        if (vectorStoreService.isEmpty()) {
            log.warn("向量库为空，无法进行向量搜索");
            return new ArrayList<>();
        }

        try {
            float[] queryVector = embeddingService.getEmbedding(query);
            List<VectorStoreService.VectorSearchResult> vectorResults = 
                vectorStoreService.searchByVector(queryVector, positionId, limit);

            List<KnowledgeItem> results = new ArrayList<>();
            for (VectorStoreService.VectorSearchResult result : vectorResults) {
                KnowledgeItem item = new KnowledgeItem();
                item.id = result.getId();
                item.positionId = result.getPositionId();
                item.category = result.getCategory();
                item.title = result.getTitle();
                item.content = result.getContent();
                item.tags = result.getTags();
                results.add(item);
            }
            return results;
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建增强 RAG 上下文
     * 使用增强检索获取相关知识
     */
    public String buildRagContextEnhanced(String positionId, String query) {
        List<KnowledgeItem> relevantKnowledge = retrieveKnowledgeEnhanced(positionId, query, 5);
        
        if (relevantKnowledge.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("【相关知识库（向量增强检索）】\n");
        for (KnowledgeItem item : relevantKnowledge) {
            context.append(String.format("- %s: %s\n", item.title, item.content));
        }
        
        return context.toString();
    }

    /**
     * 使用 RAG 增强的大模型调用（增强版）
     */
    public String chatWithRagEnhanced(String positionId, String query, String systemPrompt) {
        String ragContext = buildRagContextEnhanced(positionId, query);
        
        String enhancedPrompt = query;
        if (!ragContext.isEmpty()) {
            enhancedPrompt = ragContext + "\n\n【问题】\n" + query;
        }

        return llmService.simpleChat(enhancedPrompt, systemPrompt);
    }

    /**
     * 手动触发知识库向量化
     */
    public void triggerVectorization() {
        vectorizationExecutor.submit(this::vectorizeKnowledgeBase);
        log.info("已提交知识库向量化任务");
    }

    /**
     * 获取向量化状态
     */
    public Map<String, Object> getVectorizationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enableVectorSearch", enableVectorSearch);
        status.put("vectorWeight", vectorWeight);
        status.put("vectorCount", vectorStoreService.getCount());
        status.put("embeddingCacheSize", embeddingService.getCacheSize());
        status.put("vectorStoreEmpty", vectorStoreService.isEmpty());
        return status;
    }

    /**
     * 设置向量搜索权重
     */
    public void setVectorWeight(double weight) {
        if (weight >= 0.0 && weight <= 1.0) {
            this.vectorWeight = weight;
            log.info("向量搜索权重已设置为: {}", weight);
        } else {
            log.warn("无效的权重值: {}，权重必须在 0-1 之间", weight);
        }
    }
}
