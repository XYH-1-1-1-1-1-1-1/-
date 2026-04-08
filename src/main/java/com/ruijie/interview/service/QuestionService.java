package com.ruijie.interview.service;

import com.ruijie.interview.entity.Question;
import com.ruijie.interview.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * 问题服务类
 */
@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private LlmService llmService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 根据 ID 查询问题
     */
    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    /**
     * 根据岗位查询问题
     */
    public List<Question> findByPositionId(String positionId) {
        return questionRepository.findByPositionIdOrderByOrderNum(positionId);
    }

    /**
     * 根据岗位和类别查询问题
     */
    public List<Question> findByPositionIdAndCategory(String positionId, String category) {
        return questionRepository.findByPositionIdAndCategoryOrderByOrderNum(positionId, category);
    }

    /**
     * 随机获取指定数量的问题
     */
    public List<Question> findRandomByPositionId(String positionId, int limit) {
        return questionRepository.findRandomByPositionId(positionId, limit);
    }

    /**
     * 保存问题
     */
    @Transactional
    public Question save(Question question) {
        return questionRepository.save(question);
    }

    /**
     * 批量保存问题
     */
    @Transactional
    public List<Question> saveAll(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }

    /**
     * 初始化默认题库
     */
    @PostConstruct
    @Transactional
    public void initDefaultQuestions() {
        if (questionRepository.count() == 0) {
            log.info("开始初始化默认题库数据...");

            try {
                String now = java.time.LocalDateTime.now().toString().replace('T', ' ');
                
                // 后端开发题库
                initBackendQuestions(now);

                // 前端开发题库
                initFrontendQuestions(now);

                // 测试工程师题库
                initQaQuestions(now);

                // 算法工程师题库
                initAlgorithmQuestions(now);

                log.info("默认题库数据初始化完成，共 {} 道题", questionRepository.count());
            } catch (Exception e) {
                log.error("默认题库数据初始化失败", e);
                // 不抛出异常，避免应用启动失败
            }
        } else {
            log.info("题库数据已存在，跳过初始化");
        }
    }

    /**
     * 初始化后端开发题库
     */
    private void initBackendQuestions(String now) {
        // Java 基础
        insertQuestion("backend", "Java 基础", 
            "请解释一下 HashMap 的实现原理，以及 JDK1.8 之后做了什么优化？",
            "HashMap 基于哈希表实现，内部使用数组 + 链表 + 红黑树。JDK1.8 引入红黑树优化链表查找，阈值 8 转树，6 退链表。",
            "哈希冲突、负载因子、扩容机制、红黑树转换", 1, now);
        insertQuestion("backend", "Java 基础",
            "谈谈你对 Java  volatile 关键字的理解，它能保证原子性吗？",
            "volatile 保证可见性和有序性，但不保证原子性。通过内存屏障禁止指令重排，通过 Lock 前缀指令刷新到主存。",
            "JMM、内存屏障、可见性、有序性、CAS", 1, now);
        insertQuestion("backend", "Java 基础",
            "Java 线程池的核心参数有哪些？请说明它们的作用。",
            "corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory、handler。分别控制核心线程数、最大线程数、空闲超时、队列、线程工厂、拒绝策略。",
            "线程池参数、拒绝策略、工作流程", 2, now);
        
        // 数据库
        insertQuestion("backend", "数据库",
            "MySQL 索引为什么使用 B+ 树而不是 B 树？",
            "B+ 树非叶子节点只存索引，叶子节点存所有数据且形成链表。优势：减少 IO、支持范围查询、查询效率稳定。",
            "B+ 树结构、聚簇索引、覆盖索引", 2, now);
        insertQuestion("backend", "数据库",
            "请解释一下 MySQL 的事务隔离级别，以及 InnoDB 是如何实现可重复读的？",
            "四种隔离级别：读未提交、读已提交、可重复读、串行化。InnoDB 通过 MVCC 和 Next-Key Lock 实现可重复读，解决幻读问题。",
            "MVCC、ReadView、Undo Log、Next-Key Lock", 3, now);
        insertQuestion("backend", "数据库",
            "SQL 优化有哪些常见手段？",
            "Explain 分析执行计划、添加合适索引、避免 SELECT*、优化 JOIN、分页优化、读写分离、分库分表等。",
            "Explain、索引优化、慢查询", 2, now);
        
        // 框架
        insertQuestion("backend", "框架",
            "Spring Bean 的生命周期是怎样的？",
            "实例化->属性赋值->初始化（各种 Aware、BeanPostProcessor、InitializingBean）->使用->销毁。",
            "BeanPostProcessor、生命周期回调", 2, now);
        insertQuestion("backend", "框架",
            "Spring AOP 的实现原理是什么？",
            "基于动态代理。接口用 JDK 代理，类用 CGLIB。通过代理对象拦截方法调用，实现横切逻辑。",
            "动态代理、切面、切点、通知", 2, now);
        
        // 系统设计
        insertQuestion("backend", "系统设计",
            "如何设计一个分布式锁？需要考虑哪些问题？",
            "可用 Redis SETNX、ZooKeeper 临时顺序节点实现。需考虑：锁超时、锁续期、可重入、主从切换问题。",
            "Redis 分布式锁、Redlock、WatchDog", 3, now);
        insertQuestion("backend", "系统设计",
            "请解释缓存穿透、击穿、雪崩的区别和解决方案。",
            "穿透：查不存在数据->布隆过滤器、缓存空值。击穿：热点 key 过期->互斥锁。雪崩：大量 key 同时过期->随机过期时间。",
            "缓存问题、解决方案", 3, now);
        
        // 项目经验
        insertQuestion("backend", "项目经验",
            "请介绍一个你印象最深刻的项目，你在其中承担了什么角色？遇到了什么技术挑战？",
            "考察项目理解深度、技术选型能力、问题解决能力。",
            "项目经历、技术深度", 1, now);
        
        // 行为题
        insertQuestion("backend", "行为题",
            "当你和团队成员在技术方案上有分歧时，你会怎么处理？",
            "考察沟通能力、团队协作能力。",
            "沟通协作、问题解决", 1, now);
    }

    /**
     * 初始化前端开发题库
     */
    private void initFrontendQuestions(String now) {
        // JavaScript
        insertQuestion("frontend", "JavaScript",
            "请解释一下 JavaScript 中的闭包是什么，有什么应用场景？",
            "闭包是函数和其词法环境的组合。应用：数据私有化、函数工厂、回调函数等。",
            "作用域链、词法环境、内存管理", 1, now);
        insertQuestion("frontend", "JavaScript",
            "JavaScript 的事件循环机制是怎样的？宏任务和微任务有什么区别？",
            "执行栈->微任务队列->宏任务队列。Promise.then 是微任务，setTimeout 是宏任务。微任务优先执行。",
            "事件循环、宏任务、微任务", 2, now);
        insertQuestion("frontend", "JavaScript",
            "请解释原型链和继承，ES6 class 的本质是什么？",
            "__proto__指向 prototype 形成原型链。class 是语法糖，本质仍是原型继承。",
            "原型链、instanceof、继承", 2, now);
        
        // 框架
        insertQuestion("frontend", "框架",
            "Vue 的响应式原理是什么？Vue2 和 Vue3 有什么区别？",
            "Vue2 用 Object.defineProperty，Vue3 用 Proxy。Vue3 性能更好，支持数组和对象新增属性。",
            "数据劫持、依赖收集、发布订阅", 2, now);
        insertQuestion("frontend", "框架",
            "React Hooks 有什么优势？使用 Hooks 需要注意什么？",
            "优势：逻辑复用、代码简洁。注意：调用顺序一致、不能条件调用、自定义 Hook 命名。",
            "useState、useEffect、useMemo", 2, now);
        insertQuestion("frontend", "框架",
            "Vue 组件之间有哪些通信方式？",
            "父子：props/emit。兄弟：事件总线、Vuex。跨级：provide/inject、$attrs/$listeners。",
            "组件通信、Vuex、Pinia", 2, now);
        
        // CSS
        insertQuestion("frontend", "CSS",
            "请解释 Flex 布局，常用的 Flex 属性有哪些？",
            "Flex 是弹性盒子。常用：flex-direction、justify-content、align-items、flex-wrap。",
            "Flex 布局、主轴、交叉轴", 1, now);
        insertQuestion("frontend", "CSS",
            "什么是 BFC？如何触发 BFC？有什么应用？",
            "块级格式化上下文。触发：float、overflow、position 等。应用：清除浮动、防止 margin 重叠。",
            "BFC、布局", 2, now);
        
        // 网络
        insertQuestion("frontend", "网络",
            "HTTP 缓存机制是怎样的？强缓存和协商缓存有什么区别？",
            "强缓存：Cache-Control、Expires 直接返回。协商缓存：ETag、Last-Modified 询问服务器。",
            "HTTP 缓存、性能优化", 2, now);
        insertQuestion("frontend", "网络",
            "跨域问题如何解决？CORS 的原理是什么？",
            "方案：CORS、JSONP、代理、postMessage。CORS 通过设置响应头允许跨域。",
            "同源策略、CORS", 2, now);
        
        // 项目经验
        insertQuestion("frontend", "项目经验",
            "请介绍一个你做过的最有挑战性的前端项目，遇到了什么技术难点？",
            "考察项目理解、技术深度、问题解决能力。",
            "项目经历、技术深度", 1, now);
        
        // 行为题
        insertQuestion("frontend", "行为题",
            "你如何保证前端代码的质量？",
            "考察工程化思维：代码规范、单元测试、Code Review、自动化测试等。",
            "代码质量、工程化", 1, now);
    }

    /**
     * 初始化测试工程师题库
     */
    private void initQaQuestions(String now) {
        // 测试基础
        insertQuestion("qa", "测试基础",
            "测试用例设计有哪些常用方法？",
            "等价类划分、边界值分析、因果图、正交实验、场景法、错误推测法等。",
            "测试用例、边界值、等价类", 1, now);
        insertQuestion("qa", "测试基础",
            "黑盒测试和白盒测试有什么区别？",
            "黑盒：不关心内部实现，关注输入输出。白盒：关注内部逻辑，需要代码知识。",
            "测试分类、测试方法", 1, now);
        insertQuestion("qa", "测试基础",
            "请解释一下测试的金字塔模型。",
            "底层大量单元测试，中层集成测试，顶层少量 UI 测试。越往上执行越慢、维护成本越高。",
            "测试策略、测试层次", 2, now);
        
        // 自动化测试
        insertQuestion("qa", "自动化测试",
            "Selenium 的原理是什么？元素定位有哪些方式？",
            "通过 WebDriver 驱动浏览器。定位：id、name、xpath、css selector、class、tag 等。",
            "Selenium、元素定位", 2, now);
        insertQuestion("qa", "自动化测试",
            "接口自动化测试框架如何设计？",
            "分层设计：数据层、业务层、用例层、报告层。关键点：参数化、断言、数据驱动。",
            "接口测试、自动化框架", 2, now);
        
        // 性能测试
        insertQuestion("qa", "性能测试",
            "性能测试有哪些关键指标？",
            "响应时间、吞吐量 (TPS/QPS)、并发用户数、资源利用率、错误率。",
            "性能指标、TPS、QPS", 2, now);
        insertQuestion("qa", "性能测试",
            "JMeter 如何进行压力测试？",
            "配置线程组模拟用户，添加采样器发送请求，设置监听器查看结果，使用阶梯加压。",
            "JMeter、压力测试", 2, now);
        
        // 项目经验
        insertQuestion("qa", "项目经验",
            "请介绍一个你参与测试的项目，你是如何保证测试覆盖率的？",
            "考察测试思维、用例设计能力、质量保障意识。",
            "项目经历、测试覆盖率", 1, now);
        
        // 行为题
        insertQuestion("qa", "行为题",
            "开发认为你提的 bug 不是问题，你会怎么处理？",
            "考察沟通能力、原则性。应基于需求文档、用户体验进行有效沟通。",
            "沟通协作、问题处理", 1, now);
    }

    /**
     * 初始化算法工程师题库
     */
    private void initAlgorithmQuestions(String now) {
        // 算法基础
        insertQuestion("algorithm", "算法基础",
            "请解释一下动态规划的核心思想，并举一个应用例子。",
            "将大问题分解为重叠子问题，保存子问题解避免重复计算。例子：背包问题、最长公共子序列。",
            "动态规划、状态转移、最优子结构", 2, now);
        insertQuestion("algorithm", "算法基础",
            "快速排序的时间复杂度是多少？最坏情况是什么？如何优化？",
            "平均 O(nlogn)，最坏 O(n²)（已排序数组）。优化：随机选 pivot、三数取中。",
            "排序算法、时间复杂度", 2, now);
        
        // 机器学习
        insertQuestion("algorithm", "机器学习",
            "请解释过拟合和欠拟合，以及如何解决？",
            "过拟合：训练集好测试集差->正则化、Dropout、数据增强。欠拟合：都差->增加特征、降低正则化。",
            "过拟合、欠拟合、正则化", 2, now);
        insertQuestion("algorithm", "机器学习",
            "常见的分类算法有哪些？请比较它们的优缺点。",
            "逻辑回归、SVM、决策树、随机森林、XGBoost、神经网络。各有适用场景。",
            "分类算法、模型比较", 3, now);
        
        // 深度学习
        insertQuestion("algorithm", "深度学习",
            "请解释 CNN 中的卷积层、池化层的作用。",
            "卷积层提取特征，池化层降维减少参数。",
            "CNN、卷积、池化", 2, now);
        insertQuestion("algorithm", "深度学习",
            "Transformer 的核心机制是什么？",
            "自注意力机制 (Self-Attention)，并行计算，位置编码。",
            "Transformer、Attention", 3, now);
        
        // 编程题
        insertQuestion("algorithm", "编程题",
            "手写一个快速排序算法。",
            "考察编码能力、算法理解。",
            "排序、编码能力", 2, now);
        
        // 项目经验
        insertQuestion("algorithm", "项目经验",
            "请介绍一个你做的算法项目，模型选型是如何考虑的？",
            "考察项目理解、技术选型能力、实验设计能力。",
            "项目经历、模型选型", 1, now);
    }

    /**
     * 插入问题辅助方法 - 使用 JdbcTemplate 避免 SQLite JDBC 驱动不支持 getGeneratedKeys 的问题
     */
    private void insertQuestion(String positionId, String category, String content,
                                String referenceAnswer, String answerPoints, int orderNum, String now) {
        jdbcTemplate.update(
            "INSERT INTO questions (position_id, category, content, reference_answer, answer_points, " +
            "difficulty_level, score_weight, order_num, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            positionId, category, content, referenceAnswer, answerPoints,
            3, 10, orderNum, now, now);
    }
}
