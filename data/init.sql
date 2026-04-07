-- ============================================
-- AI 模拟面试与能力提升软件 - 数据库初始化脚本
-- 数据库类型：SQLite
-- ============================================

-- ============================================
-- 1. 用户表 (users)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    major VARCHAR(50),
    university VARCHAR(50),
    grade VARCHAR(20),
    target_position VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 2. 岗位表 (positions)
-- ============================================
CREATE TABLE IF NOT EXISTS positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    required_skills TEXT,
    tech_stack TEXT,
    interview_focus VARCHAR(2000),
    difficulty_level INTEGER,
    duration_minutes INTEGER,
    question_count INTEGER,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 3. 面试题目表 (questions)
-- ============================================
CREATE TABLE IF NOT EXISTS questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    position_id VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    reference_answer VARCHAR(2000),
    answer_points VARCHAR(1000),
    difficulty_level INTEGER,
    score_weight INTEGER,
    tags VARCHAR(200),
    order_num INTEGER,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 4. 回答记录表 (answers)
-- ============================================
CREATE TABLE IF NOT EXISTS answers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    user_answer TEXT NOT NULL,
    answer_type VARCHAR(50),
    duration_seconds INTEGER,
    word_count INTEGER,
    speech_features VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 5. 面试会话表 (interview_sessions)
-- ============================================
CREATE TABLE IF NOT EXISTS interview_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    position_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_questions INTEGER,
    answered_questions INTEGER,
    conversation_history TEXT,
    duration_seconds INTEGER,
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 6. 评估报告表 (evaluation_reports)
-- ============================================
CREATE TABLE IF NOT EXISTS evaluation_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    position_id VARCHAR(50) NOT NULL,
    technical_score INTEGER,
    communication_score INTEGER,
    logic_score INTEGER,
    adaptability_score INTEGER,
    matching_score INTEGER,
    overall_score INTEGER,
    speech_rate VARCHAR(20),
    clarity VARCHAR(20),
    confidence VARCHAR(20),
    strengths TEXT,
    weaknesses TEXT,
    suggestions TEXT,
    recommended_resources TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 7. 学习资源表 (learning_resources)
-- ============================================
CREATE TABLE IF NOT EXISTS learning_resources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL,
    position_id VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    difficulty_level VARCHAR(20),
    estimated_minutes INTEGER,
    url VARCHAR(500),
    tags VARCHAR(200),
    view_count INTEGER,
    like_count INTEGER,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- ============================================
-- 创建索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_positions_code ON positions(code);
CREATE INDEX IF NOT EXISTS idx_questions_position_id ON questions(position_id);
CREATE INDEX IF NOT EXISTS idx_questions_category ON questions(category);
CREATE INDEX IF NOT EXISTS idx_answers_session_id ON answers(session_id);
CREATE INDEX IF NOT EXISTS idx_answers_question_id ON answers(question_id);
CREATE INDEX IF NOT EXISTS idx_interview_sessions_user_id ON interview_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_interview_sessions_status ON interview_sessions(status);
CREATE INDEX IF NOT EXISTS idx_evaluation_reports_session_id ON evaluation_reports(session_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_reports_user_id ON evaluation_reports(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_resources_position_id ON learning_resources(position_id);

-- ============================================
-- 初始化数据 - 岗位数据
-- ============================================
INSERT INTO positions (code, name, description, required_skills, tech_stack, interview_focus, difficulty_level, duration_minutes, question_count, created_at, updated_at)
VALUES 
('JAVA_BACKEND', 'Java 后端开发工程师', '负责后端服务开发、数据库设计和系统架构', 
'Java 基础，Spring Boot，MySQL，Redis，消息队列', 
'Java, Spring Boot, Spring Cloud, MyBatis, MySQL, Redis, RabbitMQ/Kafka',
'Java 基础、Spring 框架、数据库设计、分布式系统、微服务架构', 3, 30, 10,
datetime('now'), datetime('now')),

('FRONTEND', '前端开发工程师', '负责 Web 前端开发和用户体验优化', 
'HTML/CSS/JavaScript, React/Vue, TypeScript', 
'HTML5, CSS3, JavaScript, TypeScript, React, Vue, Webpack, Vite',
'JavaScript 基础、框架使用、组件设计、性能优化、工程化', 2, 30, 10,
datetime('now'), datetime('now')),

('PYTHON', 'Python 开发工程师', '负责 Python 后端开发、数据处理和自动化脚本', 
'Python 基础，Django/Flask，SQLAlchemy，数据处理', 
'Python, Django, Flask, FastAPI, Pandas, NumPy, PostgreSQL',
'Python 基础、Web 框架、数据处理、异步编程', 2, 30, 10,
datetime('now'), datetime('now')),

('GO', 'Go 后端开发工程师', '负责高性能后端服务和分布式系统开发', 
'Go 语言基础，Gin/Echo，gRPC，微服务', 
'Go, Gin, Echo, gRPC, Docker, Kubernetes, etcd',
'Go 语言特性、并发编程、微服务、云原生', 3, 30, 10,
datetime('now'), datetime('now')),

('ALGORITHM', '算法工程师', '负责机器学习、深度学习算法研发', 
'Python，机器学习，深度学习，数据结构与算法', 
'Python, TensorFlow, PyTorch, Scikit-learn, Pandas',
'算法基础、机器学习、深度学习、编程能力', 4, 45, 12,
datetime('now'), datetime('now'));

-- ============================================
-- 初始化数据 - 面试题目示例
-- ============================================
INSERT INTO questions (position_id, category, content, reference_answer, answer_points, difficulty_level, score_weight, tags, order_num, created_at, updated_at)
VALUES 
('JAVA_BACKEND', '基础', '请解释一下 Java 中的 HashMap 原理？', 
'HashMap 基于哈希表实现，通过 key 的 hashCode 计算存储位置，使用链表或红黑树解决哈希冲突。Java8 中当链表长度超过 8 时转换为红黑树。主要特点：1.允许 null 键和 null 值 2.非线程安全 3.时间复杂度 O(1)',
'哈希函数、碰撞解决、链表转红黑树条件、扩容机制', 2, 10, '集合，数据结构', 1,
datetime('now'), datetime('now')),

('JAVA_BACKEND', '框架', 'Spring Boot 自动配置原理是什么？', 
'Spring Boot 通过@EnableAutoConfiguration 注解开启自动配置，利用 SPI 机制加载 META-INF/spring.factories 中的配置类。@Conditional 系列注解根据条件决定是否创建 Bean。核心是按需加载和约定优于配置。',
'自动配置注解、SPI 机制、条件注解、starter 原理', 3, 10, 'Spring Boot, 原理', 2,
datetime('now'), datetime('now')),

('JAVA_BACKEND', '数据库', 'MySQL 索引失效的场景有哪些？', 
'1.模糊查询以%开头 2.使用函数或表达式 3.类型隐式转换 4.OR 连接条件字段不都有索引 5.不符合最左前缀原则 6.使用!=或<> 7.IS NULL/IS NOT NULL 可能失效',
'最左前缀、函数操作、类型转换、OR 条件', 2, 10, 'MySQL, 索引', 3,
datetime('now'), datetime('now')),

('FRONTEND', '基础', '请解释 JavaScript 中的闭包及其应用场景', 
'闭包是指有权访问另一个函数作用域中变量的函数。创建方式：函数嵌套函数，内部函数引用外部函数变量。应用：1.数据私有化 2.函数工厂 3.迭代器 4.防抖节流。注意内存泄漏问题。',
'闭包定义、创建方式、应用场景、注意事项', 2, 10, 'JavaScript, 闭包', 1,
datetime('now'), datetime('now')),

('FRONTEND', '框架', 'React hooks 相比 class 组件有什么优势？', 
'1.代码更简洁，逻辑复用方便 2.可以在函数组件中使用状态 3.自定义 hooks 便于复用 4.避免 this 指向问题 5.便于代码分割和优化。常用 hooks: useState, useEffect, useContext, useReducer, useMemo, useCallback',
'hooks 优势、常用 hooks、自定义 hooks', 2, 10, 'React, Hooks', 2,
datetime('now'), datetime('now')),

('PYTHON', '基础', 'Python 中的装饰器是什么？如何使用？', 
'装饰器是修改其他函数功能的函数，本质是 higher-order function。使用@语法糖。可以带参数或不带参数。常用场景：日志、权限校验、性能测试、缓存等。',
'装饰器定义、语法、带参装饰器、应用场景', 2, 10, 'Python, 装饰器', 1,
datetime('now'), datetime('now')),

('GO', '基础', 'Go 语言中 goroutine 和线程的区别？', 
'1.goroutine 是用户态协程，线程是内核态 2.goroutine 内存更小 (2KB vs 1MB) 3.goroutine 由 Go 运行时调度，线程由 OS 调度 4.goroutine 数量可远超线程 5.goroutine 使用 channel 通信，线程用锁',
'调度方式、内存占用、通信方式、数量级', 2, 10, 'Go, 并发', 1,
datetime('now'), datetime('now')),

('ALGORITHM', '基础', '请解释过拟合和欠拟合，以及如何解决？', 
'欠拟合：模型复杂度不够，训练集和测试集表现都差。解决：增加特征、提高模型复杂度、减少正则化。过拟合：模型过于复杂，训练集好测试集差。解决：增加数据、正则化、Dropout、早停、简化模型、集成学习。',
'定义区分、原因分析、解决方案', 3, 10, '机器学习，模型评估', 1,
datetime('now'), datetime('now'));

-- ============================================
-- 初始化数据 - 示例用户
-- ============================================
INSERT INTO users (username, password, real_name, phone, email, major, university, grade, target_position, created_at, updated_at)
VALUES 
('demo', 'demo123', '演示用户', '13800138000', 'demo@example.com', '计算机科学与技术', '演示大学', '2024', 'JAVA_BACKEND',
datetime('now'), datetime('now'));

-- ============================================
-- 初始化数据 - 学习资源示例
-- ============================================
INSERT INTO learning_resources (title, description, type, position_id, category, difficulty_level, estimated_minutes, url, tags, view_count, like_count, created_at, updated_at)
VALUES 
('Java 基础入门教程', '从零基础开始学习 Java 编程语言', 'course', 'JAVA_BACKEND', '基础', '入门', 60, 'https://example.com/java-basic', 'Java, 入门，基础', 0, 0, datetime('now'), datetime('now')),
('Spring Boot 实战教程', '学习如何使用 Spring Boot 快速开发 Web 应用', 'course', 'JAVA_BACKEND', '框架', '进阶', 120, 'https://example.com/springboot', 'Spring Boot, Web 开发', 0, 0, datetime('now'), datetime('now')),
('JavaScript 高级程序设计', '深入理解 JavaScript 核心概念', 'book', 'FRONTEND', '基础', '进阶', 180, 'https://example.com/js-advanced', 'JavaScript, 高级', 0, 0, datetime('now'), datetime('now')),
('Python 数据分析入门', '使用 Python 进行数据分析和可视化', 'course', 'PYTHON', '数据分析', '入门', 90, 'https://example.com/python-data', 'Python, 数据分析', 0, 0, datetime('now'), datetime('now'));

-- ============================================
-- 完成提示
-- ============================================
-- 数据库初始化完成！
-- 包含：7 张表、5 个岗位、8 道面试题目、1 个示例用户、4 个学习资源