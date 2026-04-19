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
     * 根据 ID 查询问题（别名方法）
     */
    public Optional<Question> getQuestionById(Long id) {
        return findById(id);
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
     * 初始化后端开发题库 - 50+ 题
     */
    private void initBackendQuestions(String now) {
        // ==================== Java 基础 (10题) ====================
        insertQuestion("backend", "Java 基础", 
            "请解释一下 HashMap 的实现原理，以及 JDK1.8 之后做了什么优化？",
            "HashMap 基于哈希表实现，内部使用数组 + 链表 + 红黑树。JDK1.8 引入红黑树优化链表查找，阈值 8 转树，6 退链表。",
            "哈希冲突、负载因子、扩容机制、红黑树转换", 1, now);
        insertQuestion("backend", "Java 基础",
            "谈谈你对 Java volatile 关键字的理解，它能保证原子性吗？",
            "volatile 保证可见性和有序性，但不保证原子性。通过内存屏障禁止指令重排，通过 Lock 前缀指令刷新到主存。",
            "JMM、内存屏障、可见性、有序性、CAS", 2, now);
        insertQuestion("backend", "Java 基础",
            "Java 线程池的核心参数有哪些？请说明它们的作用。",
            "corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory、handler。分别控制核心线程数、最大线程数、空闲超时、队列、线程工厂、拒绝策略。",
            "线程池参数、拒绝策略、工作流程", 2, now);
        insertQuestion("backend", "Java 基础",
            "请解释 Java 中的四种引用类型及其应用场景。",
            "强引用：普通对象引用。软引用：内存不足时回收，适合缓存。弱引用：下次 GC 时回收，用于 WeakHashMap。虚引用：跟踪对象被回收的状态。",
            "引用类型、GC、内存管理", 2, now);
        insertQuestion("backend", "Java 基础",
            "Java 中的泛型是什么原理？什么是类型擦除？",
            "Java 泛型在编译期进行类型检查，运行时擦除为原始类型。类型擦除导致运行时无法获取泛型信息，可通过反射或 TypeReference 绕过。",
            "泛型、类型擦除、桥接方法", 2, now);
        insertQuestion("backend", "Java 基础",
            "请解释 Java 中的序列化机制，如何实现安全的序列化？",
            "实现 Serializable 接口，通过 ObjectOutputStream 写入。安全问题：实现 readObject 方法校验、使用 transient 关键字、serialVersionUID 版本控制。",
            "序列化、Serializable、transient", 2, now);
        insertQuestion("backend", "Java 基础",
            "Java 8 的新特性有哪些？请详细介绍 Stream API 的使用。",
            "Lambda 表达式、Stream API、Optional、新的日期 API、接口默认方法。Stream 支持 map、filter、reduce 等操作，分并行流。",
            "Lambda、Stream API、函数式编程", 1, now);
        insertQuestion("backend", "Java 基础",
            "请解释 Java 中的反射机制及其优缺点。",
            "反射允许运行时获取类信息并操作对象。优点：灵活、动态加载类。缺点：性能开销、安全隐患、破坏封装性。",
            "反射、Class 对象、动态代理", 2, now);
        insertQuestion("backend", "Java 基础",
            "Java 中 equals 和 == 有什么区别？重写 equals 为什么要重写 hashCode？",
            "== 比较地址，equals 比较内容。hashCode 契约规定 equals 的对象 hashCode 必须相同，用于 HashMap 等哈希容器正确工作。",
            "equals、hashCode、对象比较", 1, now);
        insertQuestion("backend", "Java 基础",
            "请解释 Java 中的异常体系，Error 和 Exception 有什么区别？",
            "Throwable 是根类。Error 是 JVM 级别错误，不可恢复。Exception 分受检异常（必须处理）和非受检异常（RuntimeException）。",
            "异常体系、try-catch-finally", 1, now);

        // ==================== 并发编程 (8题) ====================
        insertQuestion("backend", "并发编程",
            "请解释 synchronized 和 ReentrantLock 的区别。",
            "synchronized 是 JVM 内置关键字，自动释放锁。ReentrantLock 是 API 级别，支持公平锁、可中断、超时获取、多条件队列。",
            "锁机制、可重入锁、CAS", 2, now);
        insertQuestion("backend", "并发编程",
            "什么是 CAS？它有什么缺点？如何解决 ABA 问题？",
            "CAS（Compare And Swap）是无锁并发机制。缺点：自旋开销、ABA 问题。解决 ABA：使用 AtomicStampedReference 添加版本号。",
            "CAS、无锁编程、ABA 问题", 3, now);
        insertQuestion("backend", "并发编程",
            "请解释 ThreadLocal 的原理和内存泄漏问题。",
            "ThreadLocal 为每个线程维护独立变量副本。key 是弱引用，value 是强引用。线程池复用线程时若不 remove 会导致内存泄漏。",
            "ThreadLocal、内存泄漏、弱引用", 3, now);
        insertQuestion("backend", "并发编程",
            "Java 中如何实现线程安全？有哪些并发容器？",
            "方式：synchronized、Lock、原子类、并发容器。容器：ConcurrentHashMap、CopyOnWriteArrayList、BlockingQueue、ConcurrentLinkedQueue。",
            "线程安全、并发容器", 2, now);
        insertQuestion("backend", "并发编程",
            "请解释 ConcurrentHashMap 的实现原理，JDK1.7 和 1.8 有什么区别？",
            "JDK1.7：Segment 分段锁。JDK1.8：CAS + synchronized 锁节点，链表转红黑树。1.8 并发度更高，内存更节省。",
            "ConcurrentHashMap、分段锁、CAS", 3, now);
        insertQuestion("backend", "并发编程",
            "什么是死锁？如何检测和避免死锁？",
            "死锁是多线程互相等待对方释放资源。检测：jstack、jconsole。避免：固定锁顺序、超时机制、Lock.tryLock。",
            "死锁、锁顺序、jstack", 2, now);
        insertQuestion("backend", "并发编程",
            "请解释 CountDownLatch、CyclicBarrier、Semaphore 的使用场景。",
            "CountDownLatch：一次性等待多个任务完成。CyclicBarrier：可重复使用的屏障，用于分阶段任务。Semaphore：控制并发访问数量。",
            "并发工具类、同步器", 2, now);
        insertQuestion("backend", "并发编程",
            "什么是 AQS？请简述其实现原理。",
            "AQS（AbstractQueuedSynchronizer）是并发框架基础。通过 state 变量表示状态，CLH 队列管理等待线程，模板方法模式实现锁和同步器。",
            "AQS、CLH 队列、同步器", 3, now);

        // ==================== 数据库 (10题) ====================
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
        insertQuestion("backend", "数据库",
            "什么是数据库的范式？请解释三大范式。",
            "1NF：字段不可再分。2NF：消除部分依赖（每个非主属性完全依赖主键）。3NF：消除传递依赖（非主属性不依赖于其他非主属性）。",
            "数据库范式、设计原则", 1, now);
        insertQuestion("backend", "数据库",
            "MySQL 中的联合索引是什么？什么是最左前缀原则？",
            "联合索引是多列组合索引。最左前缀：查询必须从索引最左列开始匹配，不能跳过。如索引 (a,b,c) 可用於 a、ab、abc 查询。",
            "联合索引、最左前缀", 2, now);
        insertQuestion("backend", "数据库",
            "请解释 MySQL 的慢查询日志，如何分析和优化慢查询？",
            "慢查询日志记录超过 long_query_time 的 SQL。分析：mysqldumpslow、pt-query-digest。优化：加索引、重写 SQL、优化表结构。",
            "慢查询、性能分析", 2, now);
        insertQuestion("backend", "数据库",
            "什么是数据库连接池？HikariCP 为什么快？",
            "连接池复用数据库连接减少开销。HikariCP 快：字节码精简、优化锁等待、使用 FastList、无额外开销。",
            "连接池、HikariCP、性能优化", 2, now);
        insertQuestion("backend", "数据库",
            "MySQL 中的锁有哪些类型？请解释行锁和表锁的区别。",
            "类型：表锁、行锁、页锁、意向锁。行锁：InnoDB 支持，锁定精确但开销大。表锁：锁定整表，开销小但并发低。",
            "锁机制、行锁、表锁", 2, now);
        insertQuestion("backend", "数据库",
            "请解释数据库的主从复制原理。",
            "主库写入 binlog，从库 IO 线程拉取到 relay log，SQL 线程回放执行。支持异步、半同步、GTID 复制。",
            "主从复制、binlog、读写分离", 2, now);
        insertQuestion("backend", "数据库",
            "Redis 的数据结构有哪些？各自的使用场景是什么？",
            "String：缓存、计数器。List：消息队列。Set：标签、去重。Hash：对象存储。ZSet：排行榜。Bitmap：位运算。HyperLogLog：基数统计。",
            "Redis 数据结构、应用场景", 1, now);

        // ==================== Spring 框架 (8题) ====================
        insertQuestion("backend", "框架",
            "Spring Bean 的生命周期是怎样的？",
            "实例化->属性赋值->初始化（各种 Aware、BeanPostProcessor、InitializingBean）->使用->销毁。",
            "BeanPostProcessor、生命周期回调", 2, now);
        insertQuestion("backend", "框架",
            "Spring AOP 的实现原理是什么？",
            "基于动态代理。接口用 JDK 代理，类用 CGLIB。通过代理对象拦截方法调用，实现横切逻辑。",
            "动态代理、切面、切点、通知", 2, now);
        insertQuestion("backend", "框架",
            "Spring 中的 IOC 是什么？请解释依赖注入的几种方式。",
            "IOC（控制反转）将对象创建交给 Spring 管理。注入方式：构造器注入（推荐）、Setter 注入、字段注入（@Autowired）。",
            "IOC、DI、容器", 1, now);
        insertQuestion("backend", "框架",
            "Spring 事务的传播行为有哪些？REQUIRED 和 REQUIRES_NEW 有什么区别？",
            "REQUIRED：加入当前事务，没有则新建。REQUIRES_NEW：挂起当前事务，创建新事务。还有 SUPPORTS、MANDATORY、NOT_SUPPORTED 等。",
            "事务传播、@Transactional", 3, now);
        insertQuestion("backend", "框架",
            "Spring Boot 的自动装配原理是什么？",
            "@SpringBootApplication 包含 @EnableAutoConfiguration，通过 spring.factories 或 AutoConfiguration.imports 加载配置类，@Conditional 按需生效。",
            "自动装配、starter、条件注解", 2, now);
        insertQuestion("backend", "框架",
            "Spring MVC 的请求处理流程是怎样的？",
            "请求->DispatcherServlet->HandlerMapping->HandlerAdapter->Controller->ModelAndView->ViewResolver->渲染视图->响应。",
            "DispatcherServlet、HandlerMapping", 2, now);
        insertQuestion("backend", "框架",
            "MyBatis 中 #{} 和 ${} 有什么区别？",
            "#{} 是预编译参数化，防止 SQL 注入。${} 是字符串替换，有注入风险。优先使用 #{}。",
            "MyBatis、SQL 注入", 1, now);
        insertQuestion("backend", "框架",
            "Spring 中 @Component、@Repository、@Service、@Controller 有什么区别？",
            "都是 @Component 的特化。@Repository 额外提供异常翻译，@Service 标记业务层，@Controller 用于 MVC 控制器。功能相同但语义不同。",
            "注解、组件扫描", 1, now);

        // ==================== 系统设计 (8题) ====================
        insertQuestion("backend", "系统设计",
            "如何设计一个分布式锁？需要考虑哪些问题？",
            "可用 Redis SETNX、ZooKeeper 临时顺序节点实现。需考虑：锁超时、锁续期、可重入、主从切换问题。",
            "Redis 分布式锁、Redlock、WatchDog", 3, now);
        insertQuestion("backend", "系统设计",
            "请解释缓存穿透、击穿、雪崩的区别和解决方案。",
            "穿透：查不存在数据->布隆过滤器、缓存空值。击穿：热点 key 过期->互斥锁。雪崩：大量 key 同时过期->随机过期时间。",
            "缓存问题、解决方案", 3, now);
        insertQuestion("backend", "系统设计",
            "如何设计一个高并发的秒杀系统？",
            "方案：Redis 预扣库存、消息队列异步下单、限流防刷、CDN 静态化、数据库乐观锁、接口幂等性设计。",
            "秒杀系统、高并发架构", 3, now);
        insertQuestion("backend", "系统设计",
            "什么是微服务架构？服务之间如何通信？",
            "微服务将应用拆分为独立部署的小服务。通信方式：REST API（同步）、gRPC（高性能）、消息队列（异步）、Feign（声明式）。",
            "微服务、服务治理", 2, now);
        insertQuestion("backend", "系统设计",
            "如何保证消息队列的消息不丢失？",
            "方案：生产者确认机制、消息持久化、消费者手动确认、死信队列处理、消息幂等消费。",
            "消息队列、可靠性保证", 3, now);
        insertQuestion("backend", "系统设计",
            "什么是 API 网关？它有哪些功能？",
            "API 网关是微服务入口。功能：路由转发、负载均衡、认证鉴权、限流熔断、日志监控、协议转换。",
            "API 网关、微服务", 2, now);
        insertQuestion("backend", "系统设计",
            "如何设计一个短链接生成系统？",
            "方案：Hash 算法生成短码、Base62 编码、Redis 缓存映射、布隆过滤器判重、分布式 ID 生成器。",
            "短链接、系统设计", 2, now);
        insertQuestion("backend", "系统设计",
            "什么是服务注册与发现？常见的注册中心有哪些？",
            "服务注册：服务启动时向注册中心报告地址。发现：消费者查询提供者地址。注册中心：Nacos、Eureka、Consul、ZooKeeper。",
            "服务注册、Nacos、Eureka", 2, now);

        // ==================== 中间件 (6题) ====================
        insertQuestion("backend", "中间件",
            "Kafka 为什么能做到高性能？",
            "顺序写磁盘、零拷贝技术、分区并行、批量发送、消息压缩、页缓存利用。",
            "Kafka、消息队列、高性能", 3, now);
        insertQuestion("backend", "中间件",
            "Redis 的持久化方式有哪些？各有什么优缺点？",
            "RDB：快照方式，恢复快但可能丢数据。AOF：追加日志，数据安全但文件大。推荐混合持久化。",
            "RDB、AOF、持久化策略", 2, now);
        insertQuestion("backend", "中间件",
            "请解释 RabbitMQ 的 Exchange 类型及其路由规则。",
            "Direct：精确匹配 routing key。Fanout：广播到所有队列。Topic：模式匹配 routing key。Headers：根据消息头匹配。",
            "RabbitMQ、Exchange、路由", 2, now);
        insertQuestion("backend", "中间件",
            "Elasticsearch 的倒排索引是什么？",
            "倒排索引将文档中的词映射到文档 ID 列表。包含词项字典、倒排列表。支持全文检索、分词、相关性评分。",
            "倒排索引、全文检索", 2, now);
        insertQuestion("backend", "中间件",
            "ZooKeeper 的应用场景有哪些？",
            "配置管理、服务注册发现、分布式锁、Leader 选举、集群管理、发布订阅。",
            "ZooKeeper、分布式协调", 2, now);
        insertQuestion("backend", "中间件",
            "Nginx 的负载均衡策略有哪些？",
            "轮询、加权轮询、IP Hash、Least Connections、URL Hash。可根据场景选择合适策略。",
            "Nginx、负载均衡", 1, now);

        // ==================== 项目经验 (4题) ====================
        insertQuestion("backend", "项目经验",
            "请介绍一个你印象最深刻的项目，你在其中承担了什么角色？遇到了什么技术挑战？",
            "考察项目理解深度、技术选型能力、问题解决能力。",
            "项目经历、技术深度", 1, now);
        insertQuestion("backend", "项目经验",
            "在你的项目中，是如何进行性能优化的？请举例说明。",
            "考察优化经验：SQL 优化、缓存策略、代码优化、架构调整、JVM 调优等。",
            "性能优化、实战经验", 2, now);
        insertQuestion("backend", "项目经验",
            "请描述一次你排查线上问题的完整过程。",
            "考察问题排查能力：日志分析、监控告警、定位问题、修复方案、复盘总结。",
            "线上问题、排查能力", 2, now);
        insertQuestion("backend", "项目经验",
            "你在项目中是如何进行技术选型的？考虑了哪些因素？",
            "考察架构思维：业务需求、团队能力、社区活跃度、维护成本、性能要求、扩展性。",
            "技术选型、架构思维", 2, now);

        // ==================== DevOps (4题) ====================
        insertQuestion("backend", "DevOps",
            "请解释 Docker 的核心概念及其优势。",
            "核心：镜像、容器、仓库。优势：环境一致、快速部署、资源隔离、轻量级、可移植。",
            "Docker、容器化", 1, now);
        insertQuestion("backend", "DevOps",
            "Kubernetes 的核心组件有哪些？",
            "Master：API Server、Scheduler、Controller Manager、etcd。Node：Kubelet、Kube-proxy、Container Runtime。",
            "K8s、容器编排", 2, now);
        insertQuestion("backend", "DevOps",
            "CI/CD 是什么？你用过哪些 CI/CD 工具？",
            "持续集成/持续交付。工具：Jenkins、GitLab CI、GitHub Actions、ArgoCD。自动化构建、测试、部署。",
            "CI/CD、自动化部署", 1, now);
        insertQuestion("backend", "DevOps",
            "如何进行 JVM 调优？常用的调优参数有哪些？",
            "参数：-Xms、-Xmx、-Xmn、GC 算法选择。工具：jstat、jmap、jstack、MAT。目标：减少 Full GC、降低延迟。",
            "JVM 调优、GC 参数", 3, now);

        // ==================== 行为题 (4题) ====================
        insertQuestion("backend", "行为题",
            "当你和团队成员在技术方案上有分歧时，你会怎么处理？",
            "考察沟通能力、团队协作能力。",
            "沟通协作、问题解决", 1, now);
        insertQuestion("backend", "行为题",
            "你平时是如何学习新技术的？最近学了什么？",
            "考察学习能力和技术热情：官方文档、源码阅读、技术博客、实践项目、社区交流。",
            "学习能力、技术热情", 1, now);
        insertQuestion("backend", "行为题",
            "请描述一个你主动推动项目改进的例子。",
            "考察主动性、领导力和影响力：发现问题、提出方案、推动执行、取得成果。",
            "主动性、推动力", 1, now);
        insertQuestion("backend", "行为题",
            "你如何平衡代码质量和开发进度？",
            "考察工程思维：核心模块严格把控、技术债务管理、Code Review 机制、自动化测试保障。",
            "代码质量、工程化思维", 2, now);
    }

    /**
     * 初始化前端开发题库 - 52 题
     */
    private void initFrontendQuestions(String now) {
        // ==================== JavaScript 基础 (10题) ====================
        insertQuestion("frontend", "JavaScript 基础",
            "请解释一下 JavaScript 中的闭包是什么，有什么应用场景？",
            "闭包是函数和其词法环境的组合。应用：数据私有化、函数工厂、回调函数等。",
            "作用域链、词法环境、内存管理", 1, now);
        insertQuestion("frontend", "JavaScript 基础",
            "JavaScript 的事件循环机制是怎样的？宏任务和微任务有什么区别？",
            "执行栈->微任务队列->宏任务队列。Promise.then 是微任务，setTimeout 是宏任务。微任务优先执行。",
            "事件循环、宏任务、微任务", 2, now);
        insertQuestion("frontend", "JavaScript 基础",
            "请解释原型链和继承，ES6 class 的本质是什么？",
            "__proto__指向 prototype 形成原型链。class 是语法糖，本质仍是原型继承。",
            "原型链、instanceof、继承", 2, now);
        insertQuestion("frontend", "JavaScript 基础",
            "JavaScript 中的 this 指向如何确定？箭头函数的 this 有什么不同？",
            "this 在运行时确定，取决于调用方式。箭头函数的 this 是词法作用域，定义时确定，不会改变。",
            "this 指向、箭头函数、call/apply/bind", 2, now);
        insertQuestion("frontend", "JavaScript 基础",
            "Promise 是什么？async/await 和 Promise 有什么关系？",
            "Promise 是异步编程解决方案，有三种状态。async/await 是基于 Promise 的语法糖，使异步代码看起来同步。",
            "Promise、async/await、异步编程", 2, now);
        insertQuestion("frontend", "JavaScript 基础",
            "请解释 var、let、const 的区别。",
            "var 是函数作用域，存在变量提升。let 和 const 是块级作用域，不存在变量提升。const 声明常量不能重新赋值。",
            "变量声明、作用域、变量提升", 1, now);
        insertQuestion("frontend", "JavaScript 基础",
            "JavaScript 中的深拷贝和浅拷贝有什么区别？如何实现？",
            "浅拷贝只复制第一层，深拷贝完整复制所有层级。实现：浅拷贝用 Object.assign、展开运算符。深拷贝用 JSON.parse/stringify、structuredClone。",
            "深拷贝、浅拷贝、引用类型", 2, now);
        insertQuestion("frontend", "JavaScript 基础",
            "什么是防抖和节流？请描述它们的应用场景。",
            "防抖：事件触发后延迟执行，若期间再次触发则重新计时。节流：固定时间内只执行一次。应用：搜索输入防抖、滚动加载节流。",
            "防抖、节流、性能优化", 2, now);
        insertQuestion("frontend", "JavaScript 基础",
            "请解释 JavaScript 中的模块化发展历史。",
            "CommonJS（Node.js）、AMD（RequireJS）、CMD（SeaJS）、ES Module（原生支持）。ES Module 是标准方案，支持静态分析。",
            "模块化、ES Module、CommonJS", 1, now);
        insertQuestion("frontend", "JavaScript 基础",
            "JavaScript 中如何判断一个变量的类型？",
            "typeof：判断基本类型（null 返回 object）。instanceof：判断引用类型。Object.prototype.toString.call()：精准判断。Array.isArray()：判断数组。",
            "类型判断、typeof、instanceof", 1, now);

        // ==================== Vue 框架 (8题) ====================
        insertQuestion("frontend", "Vue",
            "Vue 的响应式原理是什么？Vue2 和 Vue3 有什么区别？",
            "Vue2 用 Object.defineProperty，Vue3 用 Proxy。Vue3 性能更好，支持数组和对象新增属性。",
            "数据劫持、依赖收集、发布订阅", 2, now);
        insertQuestion("frontend", "Vue",
            "Vue 组件之间有哪些通信方式？",
            "父子：props/emit。兄弟：事件总线、Vuex。跨级：provide/inject、$attrs/$listeners。",
            "组件通信、Vuex、Pinia", 2, now);
        insertQuestion("frontend", "Vue",
            "Vue 的 computed 和 watch 有什么区别？",
            "computed 是计算属性，有缓存，依赖变化才重新计算。watch 是侦听器，监听数据变化执行回调，支持异步操作。",
            "computed、watch、响应式", 1, now);
        insertQuestion("frontend", "Vue",
            "Vue 的 v-if 和 v-show 有什么区别？各自的使用场景是什么？",
            "v-if 是真正的条件渲染，会销毁重建组件。v-show 只是切换 display 属性。频繁切换用 v-show，条件很少改变用 v-if。",
            "v-if、v-show、条件渲染", 1, now);
        insertQuestion("frontend", "Vue",
            "Vue 的 nextTick 原理是什么？什么时候需要使用它？",
            "Vue 异步更新 DOM，nextTick 在 DOM 更新完成后执行回调。需要在数据变化后操作 DOM 时使用。",
            "nextTick、异步更新、DOM 操作", 2, now);
        insertQuestion("frontend", "Vue",
            "Vue Router 有哪几种导航守卫？请说明它们的使用场景。",
            "全局守卫：beforeEach、afterEach。路由独享：beforeEnter。组件内：beforeRouteEnter、beforeRouteUpdate、beforeRouteLeave。",
            "导航守卫、路由、权限控制", 2, now);
        insertQuestion("frontend", "Vue",
            "Vuex 的核心概念有哪些？请解释 state、getter、mutation、action。",
            "state：状态数据。getter：计算属性。mutation：同步修改状态。action：异步操作提交 mutation。",
            "Vuex、状态管理", 2, now);
        insertQuestion("frontend", "Vue",
            "Vue3 的 Composition API 有什么优势？",
            "更好的逻辑复用、代码组织更清晰、更好的 TypeScript 支持、更小的打包体积。",
            "Composition API、setup、响应式", 2, now);

        // ==================== React 框架 (7题) ====================
        insertQuestion("frontend", "React",
            "React Hooks 有什么优势？使用 Hooks 需要注意什么？",
            "优势：逻辑复用、代码简洁。注意：调用顺序一致、不能条件调用、自定义 Hook 命名。",
            "useState、useEffect、useMemo", 2, now);
        insertQuestion("frontend", "React",
            "React 的虚拟 DOM 是什么？它如何提高性能？",
            "虚拟 DOM 是真实 DOM 的 JS 对象表示。通过 diff 算法减少真实 DOM 操作，批量更新提高性能。",
            "虚拟 DOM、diff 算法、性能优化", 2, now);
        insertQuestion("frontend", "React",
            "React 中 useState 和 useReducer 有什么区别？",
            "useState 适合简单状态。useReducer 适合复杂状态逻辑，可集中管理，支持 dispatch 模式。",
            "useState、useReducer、状态管理", 2, now);
        insertQuestion("frontend", "React",
            "请解释 React 的 useEffect 依赖数组的作用。",
            "依赖数组控制 effect 何时执行。空数组：只执行一次。有依赖：依赖变化时执行。不传：每次渲染都执行。",
            "useEffect、副作用、依赖", 2, now);
        insertQuestion("frontend", "React",
            "React 中的 key 有什么作用？为什么不能用 index 作为 key？",
            "key 帮助 React 识别列表元素变化。用 index 会导致性能问题和状态混乱，应用唯一标识。",
            "key、列表渲染、diff", 1, now);
        insertQuestion("frontend", "React",
            "Redux 的工作流程是怎样的？",
            "组件 dispatch action -> reducer 处理 -> 返回新 state -> store 更新 -> 组件重新渲染。",
            "Redux、状态管理、单向数据流", 2, now);
        insertQuestion("frontend", "React",
            "React.memo 和 useMemo 有什么区别？",
            "React.memo 是 HOC，用于组件级记忆化。useMemo 是 Hook，用于记忆计算结果。都用于性能优化。",
            "React.memo、useMemo、性能优化", 2, now);

        // ==================== CSS (7题) ====================
        insertQuestion("frontend", "CSS",
            "请解释 Flex 布局，常用的 Flex 属性有哪些？",
            "Flex 是弹性盒子。常用：flex-direction、justify-content、align-items、flex-wrap。",
            "Flex 布局、主轴、交叉轴", 1, now);
        insertQuestion("frontend", "CSS",
            "什么是 BFC？如何触发 BFC？有什么应用？",
            "块级格式化上下文。触发：float、overflow、position 等。应用：清除浮动、防止 margin 重叠。",
            "BFC、布局", 2, now);
        insertQuestion("frontend", "CSS",
            "请解释 CSS 盒模型，标准盒模型和 IE 盒模型有什么区别？",
            "盒模型：content + padding + border + margin。标准：width 是 content 宽度。IE：width 包含 content + padding + border。",
            "盒模型、box-sizing", 1, now);
        insertQuestion("frontend", "CSS",
            "CSS 选择器有哪些？优先级是如何计算的？",
            "选择器：标签、类、ID、属性、伪类、伪元素。优先级：!important > 内联 > ID > 类 > 标签。",
            "选择器、优先级、权重", 1, now);
        insertQuestion("frontend", "CSS",
            "如何实现一个元素的水平垂直居中？",
            "Flex：justify-content + align-items。Grid：place-items: center。绝对定位 + transform。绝对定位 + margin:auto。",
            "居中布局、Flex、Grid", 1, now);
        insertQuestion("frontend", "CSS",
            "什么是 CSS 预处理器？Sass/Less 有什么优势？",
            "预处理器扩展 CSS 功能。优势：变量、嵌套、混合 (mixin)、函数、继承、模块化。",
            "预处理器、Sass、Less", 1, now);
        insertQuestion("frontend", "CSS",
            "响应式设计如何实现？请解释媒体查询和 rem/em/vw 单位。",
            "媒体查询：@media 适配不同屏幕。rem：相对于根元素。em：相对于父元素。vw/vh：视口百分比。",
            "响应式、媒体查询、rem", 1, now);

        // ==================== HTML & DOM (5题) ====================
        insertQuestion("frontend", "HTML",
            "HTML5 有哪些新特性？",
            "语义化标签（header、nav、section）、多媒体（video、audio）、Canvas、本地存储（localStorage）、Web Worker、WebSocket。",
            "HTML5、新特性", 1, now);
        insertQuestion("frontend", "HTML",
            "请解释浏览器从输入 URL 到页面渲染的完整过程。",
            "DNS 解析->TCP 连接->发送请求->服务器响应->HTML 解析->构建 DOM 树->CSSOM 树->渲染树->布局->绘制。",
            "渲染流程、关键路径", 2, now);
        insertQuestion("frontend", "HTML",
            "script 标签放在 head 和 body 底部有什么区别？defer 和 async 呢？",
            "head 中阻塞渲染。body 底部不阻塞。defer：HTML 解析完后执行，按顺序。async：下载完立即执行，不保证顺序。",
            "script、defer、async", 1, now);
        insertQuestion("frontend", "HTML",
            "LocalStorage、SessionStorage 和 Cookie 有什么区别？",
            "Cookie：4KB，每次请求携带。LocalStorage：5MB+，持久存储。SessionStorage：页面会话有效。",
            "本地存储、Cookie", 1, now);
        insertQuestion("frontend", "HTML",
            "什么是 SEO？前端如何优化 SEO？",
            "搜索引擎优化。方法：语义化标签、meta 标签、合理的 title/description、SSR/SSG、sitemap、结构化数据。",
            "SEO、语义化", 1, now);

        // ==================== 前端工程化 (7题) ====================
        insertQuestion("frontend", "工程化",
            "Webpack 的核心概念有哪些？",
            "Entry、Output、Loader（处理文件类型）、Plugin（扩展功能）、Mode、Chunk、Bundle。",
            "Webpack、打包构建", 2, now);
        insertQuestion("frontend", "工程化",
            "Vite 为什么比 Webpack 快？",
            "Vite 利用浏览器原生 ES Module，开发时按需编译。Webpack 需要先打包整个应用。生产环境两者都打包。",
            "Vite、ES Module、性能", 2, now);
        insertQuestion("frontend", "工程化",
            "前端如何实现单元测试？",
            "工具：Jest（测试框架）、Vitest、Testing Library（组件测试）、Cypress（E2E 测试）。",
            "单元测试、Jest", 2, now);
        insertQuestion("frontend", "工程化",
            "什么是 Tree Shaking？如何实现？",
            "Tree Shaking 移除未使用的代码。条件：ES Module 模块语法、production 模式、正确 sideEffects 配置。",
            "Tree Shaking、代码优化", 2, now);
        insertQuestion("frontend", "工程化",
            "前端性能优化有哪些手段？",
            "代码分割、懒加载、图片优化、CDN 加速、缓存策略、减少 HTTP 请求、压缩资源、预加载。",
            "性能优化、Lighthouse", 2, now);
        insertQuestion("frontend", "工程化",
            "什么是微前端？有哪些实现方案？",
            "微前端将大型应用拆分为多个小型应用。方案：qiankun、Module Federation、iframe、Web Components。",
            "微前端、架构设计", 3, now);
        insertQuestion("frontend", "工程化",
            "TypeScript 相比 JavaScript 有什么优势？",
            "静态类型检查、更好的 IDE 支持、代码可读性、重构安全、接口和泛型支持、大型项目维护性更好。",
            "TypeScript、类型系统", 1, now);

        // ==================== 网络与安全 (5题) ====================
        insertQuestion("frontend", "网络",
            "HTTP 缓存机制是怎样的？强缓存和协商缓存有什么区别？",
            "强缓存：Cache-Control、Expires 直接返回。协商缓存：ETag、Last-Modified 询问服务器。",
            "HTTP 缓存、性能优化", 2, now);
        insertQuestion("frontend", "网络",
            "跨域问题如何解决？CORS 的原理是什么？",
            "方案：CORS、JSONP、代理、postMessage。CORS 通过设置响应头允许跨域。",
            "同源策略、CORS", 2, now);
        insertQuestion("frontend", "网络",
            "HTTP 和 HTTPS 有什么区别？HTTPS 的握手过程是怎样的？",
            "HTTPS 在 HTTP 基础上加 SSL/TLS 层。握手：客户端发送支持的加密算法->服务器返回证书->协商密钥->加密通信。",
            "HTTPS、SSL/TLS、安全", 2, now);
        insertQuestion("frontend", "网络",
            "前端有哪些常见的安全问题？如何防范？",
            "XSS：转义输入、CSP。CSRF：Token 验证、SameSite。点击劫持：X-Frame-Options。",
            "XSS、CSRF、安全", 2, now);
        insertQuestion("frontend", "网络",
            "WebSocket 和 HTTP 有什么区别？",
            "HTTP 是请求响应模式，无状态。WebSocket 是全双工通信，连接建立后可持续发送消息，适合实时场景。",
            "WebSocket、长连接", 2, now);

        // ==================== 项目经验 (3题) ====================
        insertQuestion("frontend", "项目经验",
            "请介绍一个你做过的最有挑战性的前端项目，遇到了什么技术难点？",
            "考察项目理解、技术深度、问题解决能力。",
            "项目经历、技术深度", 1, now);
        insertQuestion("frontend", "项目经验",
            "你在前端项目中做过哪些性能优化？效果如何？",
            "考察实战经验：首屏加载、Lighthouse 分数、包体积优化、懒加载、CDN 使用等。",
            "性能优化、实战经验", 2, now);
        insertQuestion("frontend", "项目经验",
            "请描述一个你从 0 到 1 搭建的前端项目，技术选型是如何考虑的？",
            "考察架构能力：框架选择、工程化、组件库、状态管理、部署方案。",
            "技术选型、架构设计", 2, now);

        // ==================== 行为题 (2题) ====================
        insertQuestion("frontend", "行为题",
            "你如何保证前端代码的质量？",
            "考察工程化思维：代码规范、单元测试、Code Review、自动化测试等。",
            "代码质量、工程化", 1, now);
        insertQuestion("frontend", "行为题",
            "你是如何跟后端协作的？遇到接口联调问题怎么处理？",
            "考察协作能力：接口文档约定、Mock 数据、提前沟通、问题及时反馈。",
            "沟通协作、联调", 1, now);
    }

    /**
     * 初始化测试工程师题库 - 52 题
     */
    private void initQaQuestions(String now) {
        // ==================== 测试基础 (10题) ====================
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
        insertQuestion("qa", "测试基础",
            "请解释软件生命周期中的测试流程。",
            "需求分析->测试计划->测试设计->执行测试->缺陷跟踪->测试报告->验收测试。",
            "测试流程、STLC", 1, now);
        insertQuestion("qa", "测试基础",
            "什么是回归测试？什么时候需要做回归测试？",
            "回归测试是修改代码后重新测试以确保没有引入新缺陷。时机：代码修复、功能新增、配置变更、版本升级后。",
            "回归测试、测试时机", 1, now);
        insertQuestion("qa", "测试基础",
            "冒烟测试和验收测试有什么区别？",
            "冒烟测试：开发完成后验证基本功能是否可用，决定能否进入正式测试。验收测试：用户验证是否满足需求，决定能否上线。",
            "冒烟测试、验收测试", 1, now);
        insertQuestion("qa", "测试基础",
            "请解释测试用例的要素有哪些？",
            "用例编号、用例名称、前置条件、测试步骤、测试数据、预期结果、实际结果、优先级。",
            "测试用例、用例设计", 1, now);
        insertQuestion("qa", "测试基础",
            "什么是缺陷（Bug）？缺陷的生命周期是怎样的？",
            "缺陷是软件中存在的问题。生命周期：新建->分配->修复->验证->关闭（或重新打开）。",
            "缺陷管理、Bug 生命周期", 1, now);
        insertQuestion("qa", "测试基础",
            "灰盒测试是什么？它和黑盒、白盒测试有什么区别？",
            "灰盒测试结合黑盒和白盒，关注输入输出同时了解部分内部结构。适合集成测试和接口测试。",
            "灰盒测试、测试分类", 2, now);
        insertQuestion("qa", "测试基础",
            "如何评估测试的充分性？",
            "指标：需求覆盖率、用例执行率、缺陷密度、代码覆盖率、遗留缺陷风险评估。",
            "测试评估、质量度量", 2, now);

        // ==================== 接口测试 (6题) ====================
        insertQuestion("qa", "接口测试",
            "接口测试的主要内容有哪些？",
            "功能验证、参数校验、边界值、异常处理、安全性、性能、兼容性、幂等性。",
            "接口测试、API 测试", 1, now);
        insertQuestion("qa", "接口测试",
            "常见的 HTTP 状态码有哪些？分别代表什么含义？",
            "200 成功、301 永久重定向、302 临时重定向、400 请求错误、401 未授权、403 禁止访问、404 未找到、500 服务器错误。",
            "HTTP 状态码", 1, now);
        insertQuestion("qa", "接口测试",
            "RESTful API 设计原则是什么？",
            "使用名词表示资源、HTTP 方法表示操作、无状态、统一接口、HATEOAS、版本控制。",
            "RESTful、API 设计", 2, now);
        insertQuestion("qa", "接口测试",
            "接口测试中如何处理鉴权？",
            "Token 认证、OAuth、API Key、Session/Cookie、JWT。测试时需要管理 token 的获取、刷新、过期处理。",
            "鉴权、Token、认证", 2, now);
        insertQuestion("qa", "接口测试",
            "如何测试接口的幂等性？",
            "幂等性指同一请求多次执行结果相同。测试方法：重复发送相同请求，验证结果一致性和副作用。",
            "幂等性、接口测试", 2, now);
        insertQuestion("qa", "接口测试",
            "Postman 和 JMeter 做接口测试有什么区别？",
            "Postman：适合单接口调试、集合测试、Mock。JMeter：适合压力测试、批量请求、性能测试。",
            "Postman、JMeter、工具", 1, now);

        // ==================== 自动化测试 (8题) ====================
        insertQuestion("qa", "自动化测试",
            "Selenium 的原理是什么？元素定位有哪些方式？",
            "通过 WebDriver 驱动浏览器。定位：id、name、xpath、css selector、class、tag、link text 等。",
            "Selenium、元素定位", 2, now);
        insertQuestion("qa", "自动化测试",
            "接口自动化测试框架如何设计？",
            "分层设计：数据层、业务层、用例层、报告层。关键点：参数化、断言、数据驱动、CI 集成。",
            "接口测试、自动化框架", 2, now);
        insertQuestion("qa", "自动化测试",
            "UI 自动化测试的难点有哪些？如何解决？",
            "难点：元素不稳定、页面变化快、执行慢、维护成本高。解决：PO 模式、等待策略、数据分离、定期维护。",
            "UI 自动化、Page Object", 2, now);
        insertQuestion("qa", "自动化测试",
            "什么是 Page Object 设计模式？",
            "将页面元素和操作封装成类，测试代码调用类方法。优势：提高可维护性、减少代码重复、元素变更只改一处。",
            "PO 模式、设计模式", 2, now);
        insertQuestion("qa", "自动化测试",
            "自动化测试用例如何选择？不是所有用例都适合自动化吗？",
            "适合：回归测试、数据驱动、重复执行、核心功能。不适合：探索性、一次性、UI 频繁变化、用户体验测试。",
            "用例选择、自动化策略", 2, now);
        insertQuestion("qa", "自动化测试",
            "Cypress 和 Selenium 有什么区别？",
            "Cypress：直接运行在浏览器中，速度快，自动等待，只能测 Web。Selenium：支持多语言多浏览器，生态成熟。",
            "Cypress、Selenium", 2, now);
        insertQuestion("qa", "自动化测试",
            "如何在 CI/CD 中集成自动化测试？",
            "方案：Jenkins/GitLab CI 触发测试、Docker 环境、测试报告生成、失败通知、质量门禁。",
            "CI/CD、持续测试", 2, now);
        insertQuestion("qa", "自动化测试",
            "Playwright 是什么？它有什么优势？",
            "Playwright 是微软开源的跨浏览器自动化框架。优势：支持 Chromium/Firefox/WebKit、自动等待、网络拦截、移动端模拟。",
            "Playwright、自动化测试", 2, now);

        // ==================== 性能测试 (7题) ====================
        insertQuestion("qa", "性能测试",
            "性能测试有哪些关键指标？",
            "响应时间、吞吐量 (TPS/QPS)、并发用户数、资源利用率、错误率。",
            "性能指标、TPS、QPS", 2, now);
        insertQuestion("qa", "性能测试",
            "JMeter 如何进行压力测试？",
            "配置线程组模拟用户，添加采样器发送请求，设置监听器查看结果，使用阶梯加压。",
            "JMeter、压力测试", 2, now);
        insertQuestion("qa", "性能测试",
            "负载测试、压力测试、稳定性测试有什么区别？",
            "负载测试：正常到峰值的负载变化。压力测试：超出极限。稳定性测试：长时间运行的可靠性。",
            "性能测试分类", 2, now);
        insertQuestion("qa", "性能测试",
            "性能测试中发现响应时间变长，如何排查？",
            "排查方向：数据库慢查询、代码性能瓶颈、资源瓶颈 (CPU/内存/IO)、网络延迟、第三方依赖。",
            "性能分析、排查", 3, now);
        insertQuestion("qa", "性能测试",
            "什么是并发测试？如何模拟真实用户行为？",
            "并发测试模拟多用户同时操作。方法：线程组模拟用户、思考时间模拟行为、场景脚本模拟业务流程。",
            "并发测试、场景设计", 2, now);
        insertQuestion("qa", "性能测试",
            "性能测试报告中应包含哪些内容？",
            "测试目标、环境配置、测试场景、关键指标、瓶颈分析、优化建议、风险评估。",
            "测试报告、性能分析", 2, now);
        insertQuestion("qa", "性能测试",
            "LoadRunner 和 JMeter 有什么区别？",
            "LoadRunner：商业软件、功能强大、协议支持广、成本高。JMeter：开源免费、Java 生态、插件丰富、社区活跃。",
            "LoadRunner、JMeter", 1, now);

        // ==================== 安全测试 (5题) ====================
        insertQuestion("qa", "安全测试",
            "常见的 Web 安全漏洞有哪些？",
            "OWASP Top 10：注入、失效认证、敏感数据泄露、XXE、失效访问控制、安全配置错误、XSS、不安全反序列化。",
            "OWASP、安全漏洞", 2, now);
        insertQuestion("qa", "安全测试",
            "如何进行 SQL 注入测试？",
            "方法：输入单引号、OR 1=1、UNION 查询、时间盲注、报错注入。工具：SQLMap、Burp Suite。",
            "SQL 注入、安全测试", 2, now);
        insertQuestion("qa", "安全测试",
            "XSS 攻击有哪几种类型？如何防御？",
            "类型：反射型、存储型、DOM 型。防御：输入验证、输出编码转义、CSP、HttpOnly。",
            "XSS、安全防御", 2, now);
        insertQuestion("qa", "安全测试",
            "什么是 CSRF 攻击？如何防范？",
            "CSRF 是伪造用户请求。防御：Anti-CSRF Token、SameSite Cookie、验证 Referer、二次确认。",
            "CSRF、安全测试", 2, now);
        insertQuestion("qa", "安全测试",
            "如何进行接口安全测试？",
            "内容：鉴权绕过、参数篡改、重放攻击、越权访问、敏感信息泄露、暴力破解。",
            "接口安全、渗透测试", 2, now);

        // ==================== 移动端测试 (4题) ====================
        insertQuestion("qa", "移动端测试",
            "Appium 的原理是什么？",
            "基于 WebDriver 协议，通过 JSON Wire Protocol 与移动端通信，支持 iOS 和 Android。",
            "Appium、移动端自动化", 2, now);
        insertQuestion("qa", "移动端测试",
            "移动端测试和 Web 测试有什么区别？",
            "差异：网络环境、屏幕适配、手势操作、中断测试（来电/短信）、安装卸载、版本兼容、性能耗电。",
            "移动端测试、测试差异", 1, now);
        insertQuestion("qa", "移动端测试",
            "如何进行弱网测试？",
            "工具：Charles/Fiddler 限速、ATC、Network Link Conditioner。测试：超时处理、重试机制、数据缓存、用户体验。",
            "弱网测试、网络模拟", 2, now);
        insertQuestion("qa", "移动端测试",
            "App 的兼容性测试如何做？",
            "维度：操作系统版本、屏幕分辨率、设备品牌、网络环境、第三方 SDK 兼容。工具：云测平台。",
            "兼容性测试、云测平台", 2, now);

        // ==================== 测试管理与工具 (4题) ====================
        insertQuestion("qa", "测试管理",
            "常用的测试管理工具有哪些？",
            "Jira（缺陷跟踪）、TestLink（用例管理）、Zephyr（测试管理）、禅道、Confluence（文档）。",
            "测试工具、缺陷管理", 1, now);
        insertQuestion("qa", "测试管理",
            "如何编写一份好的测试报告？",
            "内容：测试概述、执行统计、缺陷分析、质量评估、风险提示、改进建议。要求：数据准确、结论清晰。",
            "测试报告、质量评估", 1, now);
        insertQuestion("qa", "测试管理",
            "敏捷测试和传统测试有什么区别？",
            "敏捷：测试左移、持续测试、跨职能协作、快速反馈、自动化优先。传统：阶段化、文档驱动、瀑布流程。",
            "敏捷测试、测试左移", 2, now);
        insertQuestion("qa", "测试管理",
            "什么是 TDD 和 BDD？",
            "TDD：测试驱动开发，先写测试再写代码。BDD：行为驱动开发，用自然语言描述行为（Given-When-Then）。",
            "TDD、BDD、开发模式", 2, now);

        // ==================== 项目经验 (4题) ====================
        insertQuestion("qa", "项目经验",
            "请介绍一个你参与测试的项目，你是如何保证测试覆盖率的？",
            "考察测试思维、用例设计能力、质量保障意识。",
            "项目经历、测试覆盖率", 1, now);
        insertQuestion("qa", "项目经验",
            "你在测试过程中发现过一个最严重的 Bug 是什么？如何定位的？",
            "考察问题敏感度、排查能力、影响力。",
            "Bug 定位、问题排查", 2, now);
        insertQuestion("qa", "项目经验",
            "请描述一次你推动质量改进的经历。",
            "考察主动性、影响力：发现流程问题、提出改进方案、推动落地、取得成效。",
            "质量改进、推动力", 2, now);
        insertQuestion("qa", "项目经验",
            "你在项目中是如何平衡手动测试和自动化测试的？",
            "考察策略思维：根据项目阶段、功能稳定性、ROI 合理分配手动和自动化比例。",
            "测试策略、自动化比例", 2, now);

        // ==================== 行为题 (4题) ====================
        insertQuestion("qa", "行为题",
            "开发认为你提的 bug 不是问题，你会怎么处理？",
            "考察沟通能力、原则性。应基于需求文档、用户体验进行有效沟通。",
            "沟通协作、问题处理", 1, now);
        insertQuestion("qa", "行为题",
            "上线前发现严重 Bug 你会怎么处理？",
            "考察应急能力：评估影响、及时上报、协调修复、验证修复、决策是否延期。",
            "应急响应、风险管理", 2, now);
        insertQuestion("qa", "行为题",
            "测试时间不够你该怎么办？",
            "考察问题解决能力：风险评估、优先级排序、加班协调、范围调整、自动化补充。",
            "时间管理、风险评估", 1, now);
        insertQuestion("qa", "行为题",
            "你是如何持续提升自己的测试技能的？",
            "考察学习能力：学习新技术、参加培训、技术分享、实践总结。",
            "学习能力、自我提升", 1, now);
    }

    /**
     * 初始化算法工程师题库 - 52 题
     */
    private void initAlgorithmQuestions(String now) {
        // ==================== 数据结构与算法基础 (10题) ====================
        insertQuestion("algorithm", "算法基础",
            "请解释一下动态规划的核心思想，并举一个应用例子。",
            "将大问题分解为重叠子问题，保存子问题解避免重复计算。例子：背包问题、最长公共子序列。",
            "动态规划、状态转移、最优子结构", 2, now);
        insertQuestion("algorithm", "算法基础",
            "快速排序的时间复杂度是多少？最坏情况是什么？如何优化？",
            "平均 O(nlogn)，最坏 O(n²)（已排序数组）。优化：随机选 pivot、三数取中。",
            "排序算法、时间复杂度", 2, now);
        insertQuestion("algorithm", "算法基础",
            "请解释贪心算法和动态规划的区别。",
            "贪心算法每步选局部最优，不能保证全局最优。动态规划考虑所有子问题，保证全局最优。贪心适用于最优子结构且无后效性的问题。",
            "贪心算法、动态规划、区别", 2, now);
        insertQuestion("algorithm", "算法基础",
            "请解释二叉搜索树的特性，以及如何判断一棵树是 BST？",
            "BST 左子树所有节点小于根，右子树所有节点大于根。判断方法：中序遍历是否有序、递归检查节点范围。",
            "二叉搜索树、中序遍历", 2, now);
        insertQuestion("algorithm", "算法基础",
            "什么是哈希表？哈希冲突如何解决？",
            "哈希表通过哈希函数映射键值对。冲突解决：链地址法（链表）、开放地址法（线性探测、二次探测、双重哈希）。",
            "哈希表、哈希冲突、负载因子", 1, now);
        insertQuestion("algorithm", "算法基础",
            "请解释图的 BFS 和 DFS 的区别及应用场景。",
            "BFS 按层遍历，用队列，适合最短路径。DFS 深入到底，用栈/递归，适合连通性检测、拓扑排序、回溯。",
            "BFS、DFS、图遍历", 2, now);
        insertQuestion("algorithm", "算法基础",
            "什么是回溯算法？请举例说明。",
            "回溯是带剪枝的 DFS，尝试所有可能解，不满足条件时回退。例子：N 皇后、数独、全排列、组合总和。",
            "回溯算法、剪枝、DFS", 2, now);
        insertQuestion("algorithm", "算法基础",
            "请解释堆排序的原理及其时间复杂度。",
            "堆是完全二叉树。建堆 O(n)，每次取堆顶元素后调整 O(logn)，共 n 次。总复杂度 O(nlogn)，原地排序。",
            "堆排序、优先队列", 2, now);
        insertQuestion("algorithm", "算法基础",
            "什么是 LRU 缓存？如何实现？",
            "LRU（最近最少使用）缓存淘汰最久未访问数据。实现：哈希表 + 双向链表，O(1) 时间获取和插入。",
            "LRU、缓存设计、链表", 3, now);
        insertQuestion("algorithm", "算法基础",
            "请解释分治算法的思想及应用。",
            "分治：分解->解决->合并。应用：归并排序、快速排序、大数乘法、最近点对问题、Strassen 矩阵乘法。",
            "分治算法、归并排序", 2, now);

        // ==================== 机器学习 (10题) ====================
        insertQuestion("algorithm", "机器学习",
            "请解释过拟合和欠拟合，以及如何解决？",
            "过拟合：训练集好测试集差->正则化、Dropout、数据增强。欠拟合：都差->增加特征、降低正则化、增加模型复杂度。",
            "过拟合、欠拟合、正则化", 2, now);
        insertQuestion("algorithm", "机器学习",
            "常见的分类算法有哪些？请比较它们的优缺点。",
            "逻辑回归、SVM、决策树、随机森林、XGBoost、神经网络。各有适用场景和数据偏好。",
            "分类算法、模型比较", 3, now);
        insertQuestion("algorithm", "机器学习",
            "请解释逻辑回归的原理，为什么叫回归却是分类算法？",
            "逻辑回归用 Sigmoid 函数将线性回归输出映射到 (0,1) 概率。本质是广义线性模型，用于二分类任务。",
            "逻辑回归、Sigmoid、分类", 2, now);
        insertQuestion("algorithm", "机器学习",
            "SVM（支持向量机）的核心思想是什么？核函数有什么用？",
            "SVM 寻找最大间隔超平面分隔数据。核函数将数据映射到高维空间，使非线性可分变为线性可分。常用 RBF、多项式核。",
            "SVM、核函数、最大间隔", 3, now);
        insertQuestion("algorithm", "机器学习",
            "请解释决策树的分裂准则（信息增益、基尼系数）。",
            "信息增益：基于熵的减少，ID3 算法使用。基尼系数：度量不纯度，CART 算法使用。两者效果类似。",
            "决策树、信息增益、基尼系数", 2, now);
        insertQuestion("algorithm", "机器学习",
            "什么是集成学习？Bagging 和 Boosting 有什么区别？",
            "集成学习组合多个弱学习器。Bagging：并行训练、投票平均（随机森林）。Boosting：串行训练、加权组合（AdaBoost、GBDT、XGBoost）。",
            "集成学习、Bagging、Boosting", 2, now);
        insertQuestion("algorithm", "机器学习",
            "K-Means 聚类算法的原理和缺点是什么？",
            "K-Means 迭代优化簇中心，最小化簇内方差。缺点：需指定 K、对初始值敏感、对异常值敏感、只能发现球形簇。",
            "K-Means、聚类、无监督", 2, now);
        insertQuestion("algorithm", "机器学习",
            "请解释特征工程的重要性及常见方法。",
            "好特征比好算法更重要。方法：缺失值处理、标准化/归一化、编码类别特征、特征选择、特征构造、降维。",
            "特征工程、特征选择、预处理", 1, now);
        insertQuestion("algorithm", "机器学习",
            "交叉验证是什么？K 折交叉验证的流程是怎样的？",
            "交叉验证评估模型泛化能力。K 折：数据分 K 份，每次用 K-1 份训练、1 份验证，重复 K 次取平均。",
            "交叉验证、模型评估", 1, now);
        insertQuestion("algorithm", "机器学习",
            "如何评估分类模型的性能？精确率和召回率如何权衡？",
            "指标：准确率、精确率、召回率、F1、AUC-ROC。精确率和召回率通过 PR 曲线权衡，看业务需求。",
            "模型评估、PR 曲线、F1", 2, now);

        // ==================== 深度学习 (10题) ====================
        insertQuestion("algorithm", "深度学习",
            "请解释 CNN 中的卷积层、池化层的作用。",
            "卷积层提取特征（局部感知、权值共享），池化层降维减少参数（最大池化、平均池化）。",
            "CNN、卷积、池化", 2, now);
        insertQuestion("algorithm", "深度学习",
            "Transformer 的核心机制是什么？",
            "自注意力机制 (Self-Attention)，并行计算，多头注意力，位置编码，Encoder-Decoder 结构。",
            "Transformer、Attention", 3, now);
        insertQuestion("algorithm", "深度学习",
            "请解释反向传播算法的原理。",
            "反向传播用链式法则从输出层向输入层传播误差，计算每个参数的梯度，用于梯度下降更新权重。",
            "反向传播、梯度下降、链式法则", 2, now);
        insertQuestion("algorithm", "深度学习",
            "常见的激活函数有哪些？ReLU 有什么优缺点？",
            "Sigmoid、Tanh、ReLU、Leaky ReLU、GELU。ReLU 优点：计算简单、缓解梯度消失。缺点：Dead ReLU 问题。",
            "激活函数、ReLU、梯度消失", 2, now);
        insertQuestion("algorithm", "深度学习",
            "请解释 Batch Normalization 的作用。",
            "BN 对每层输出做归一化，加速训练、允许更大学习率、有轻微正则化效果。减少 Internal Covariate Shift。",
            "BN、归一化、训练加速", 2, now);
        insertQuestion("algorithm", "深度学习",
            "什么是 Dropout？训练和推理时有什么不同？",
            "Dropout 随机丢弃神经元防止过拟合。训练时按概率丢弃，推理时所有神经元保留但权重乘以 (1-p)。",
            "Dropout、正则化、过拟合", 2, now);
        insertQuestion("algorithm", "深度学习",
            "请解释 RNN 的梯度消失问题及 LSTM 如何解决。",
            "RNN 长序列反向传播时梯度指数衰减。LSTM 通过门控机制（遗忘门、输入门、输出门）和细胞状态保持长期记忆。",
            "RNN、LSTM、梯度消失", 3, now);
        insertQuestion("algorithm", "深度学习",
            "常见的优化器有哪些？Adam 的特点是什么？",
            "SGD、SGD+Momentum、RMSprop、Adam。Adam 结合 Momentum 和 RMSprop，自适应学习率，收敛快。",
            "优化器、Adam、学习率", 2, now);
        insertQuestion("algorithm", "深度学习",
            "什么是迁移学习？有什么优势？",
            "迁移学习将在一个任务上学到的知识应用到相关任务。优势：减少数据需求、加速训练、提升小数据集性能。",
            "迁移学习、预训练模型、Fine-tuning", 2, now);
        insertQuestion("algorithm", "深度学习",
            "请解释目标检测中 YOLO 算法的核心思想。",
            "YOLO 将目标检测视为回归问题，将图像划分为网格，每个网格预测边界框和类别，实现端到端实时检测。",
            "YOLO、目标检测、实时", 3, now);

        // ==================== NLP / CV / 大模型 (7题) ====================
        insertQuestion("algorithm", "NLP",
            "请解释 Word2Vec 的原理，Skip-gram 和 CBOW 有什么区别？",
            "Word2Vec 将词映射到稠密向量。Skip-gram：用中心词预测上下文。CBOW：用上下文预测中心词。",
            "Word2Vec、词向量、Embedding", 2, now);
        insertQuestion("algorithm", "NLP",
            "BERT 和 GPT 的架构有什么区别？",
            "BERT：Transformer Encoder，双向自注意力，擅长理解。GPT：Transformer Decoder，单向自回归，擅长生成。",
            "BERT、GPT、预训练模型", 3, now);
        insertQuestion("algorithm", "NLP",
            "请解释 Attention 机制在 NLP 中的作用。",
            "Attention 让模型在处理一个词时关注序列中其他相关词，解决长距离依赖问题，是 Transformer 的核心。",
            "Attention、自注意力、NLP", 2, now);
        insertQuestion("algorithm", "CV",
            "请解释图像分类中 ResNet 的核心贡献。",
            "ResNet 引入残差连接 (Skip Connection)，解决深层网络退化问题，使训练非常深的网络成为可能。",
            "ResNet、残差连接、图像分类", 2, now);
        insertQuestion("algorithm", "大模型",
            "什么是 Prompt Engineering？有哪些常用技巧？",
            "Prompt Engineering 是设计输入提示词引导大模型输出。技巧：Few-shot、Chain of Thought、角色设定、输出格式约束。",
            "Prompt、大模型、提示工程", 2, now);
        insertQuestion("algorithm", "大模型",
            "大模型的微调 (Fine-tuning) 和全量训练有什么区别？",
            "微调：在预训练基础上调整部分参数，资源需求小。全量训练：从头训练所有参数，需要大量数据和算力。",
            "Fine-tuning、PEFT、LoRA", 3, now);
        insertQuestion("algorithm", "大模型",
            "什么是 RAG（检索增强生成）？",
            "RAG 将外部知识检索与 LLM 生成结合。先检索相关文档，再结合文档生成回答，减少幻觉，提高准确性。",
            "RAG、检索增强、大模型应用", 2, now);

        // ==================== 工程实践与 MLOps (5题) ====================
        insertQuestion("algorithm", "工程实践",
            "模型训练时如何处理类别不平衡问题？",
            "方法：重采样（过采样少数类/欠采样多数类）、类别权重、SMOTE、Focal Loss、集成方法。",
            "类别不平衡、数据增强", 2, now);
        insertQuestion("algorithm", "工程实践",
            "如何进行模型部署和性能优化？",
            "部署：Flask/FastAPI 封装、Docker 容器化、K8s 编排。优化：模型压缩、量化、ONNX、TensorRT、蒸馏。",
            "模型部署、模型优化", 2, now);
        insertQuestion("algorithm", "工程实践",
            "什么是模型蒸馏 (Knowledge Distillation)？",
            "用大模型 (Teacher) 指导小模型 (Student) 训练，让小模型获得接近大模型的性能，用于模型压缩和加速推理。",
            "模型蒸馏、模型压缩", 3, now);
        insertQuestion("algorithm", "工程实践",
            "A/B 测试在算法中如何应用？",
            "A/B 测试用于比较新旧算法效果。随机分组、控制变量、统计显著性检验，确保结果可靠。",
            "A/B 测试、在线评估", 2, now);
        insertQuestion("algorithm", "工程实践",
            "如何监控线上模型的性能？",
            "监控：预测准确率、数据漂移、概念漂移、延迟、吞吐量。告警：阈值触发、自动回滚、定期重训练。",
            "模型监控、MLOps", 2, now);

        // ==================== 编程题 (5题) ====================
        insertQuestion("algorithm", "编程题",
            "手写一个快速排序算法。",
            "考察编码能力、算法理解。",
            "排序、编码能力", 2, now);
        insertQuestion("algorithm", "编程题",
            "请实现一个两数之和 (Two Sum) 的算法。",
            "用哈希表存储已遍历的数字及其索引，O(n) 时间复杂度完成。",
            "哈希表、Two Sum", 1, now);
        insertQuestion("algorithm", "编程题",
            "请实现反转链表的算法（迭代和递归两种方式）。",
            "迭代：prev、curr、next 三指针。递归：递归反转后续节点，调整指针。",
            "链表、递归、迭代", 2, now);
        insertQuestion("algorithm", "编程题",
            "请实现求二叉树的最大深度。",
            "递归：max(左子树深度, 右子树深度) + 1。BFS：层序遍历计数。DFS：后序遍历。",
            "二叉树、递归、DFS", 1, now);
        insertQuestion("algorithm", "编程题",
            "请实现一个有效的括号匹配算法。",
            "用栈：左括号入栈，右括号匹配栈顶。或用计数器模拟栈。时间复杂度 O(n)。",
            "栈、括号匹配", 1, now);

        // ==================== 项目经验 (3题) ====================
        insertQuestion("algorithm", "项目经验",
            "请介绍一个你做的算法项目，模型选型是如何考虑的？",
            "考察项目理解、技术选型能力、实验设计能力。",
            "项目经历、模型选型", 1, now);
        insertQuestion("algorithm", "项目经验",
            "你在算法项目中遇到的最大挑战是什么？如何解决的？",
            "考察问题解决能力：数据质量、模型选择、性能瓶颈、线上问题等。",
            "挑战、问题解决", 2, now);
        insertQuestion("algorithm", "项目经验",
            "请描述一个你从零到一搭建的算法系统。",
            "考察系统设计能力：数据收集、模型训练、部署上线、监控运维全流程。",
            "系统设计、算法工程化", 2, now);

        // ==================== 行为题 (2题) ====================
        insertQuestion("algorithm", "行为题",
            "你平时如何跟踪 AI 领域的最新进展？",
            "考察学习能力：阅读论文、GitHub、技术社区、开源项目、学术会议等。",
            "学习能力、技术敏感度", 1, now);
        insertQuestion("algorithm", "行为题",
            "当你的模型效果达不到预期时，你会怎么做？",
            "考察系统性思维：数据分析、特征工程、模型调参、换模型、集成方法、错误分析。",
            "问题解决、系统性思维", 2, now);
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
