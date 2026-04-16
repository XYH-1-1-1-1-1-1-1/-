package com.ruijie.interview.service;

import com.ruijie.interview.entity.Position;
import com.ruijie.interview.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

/**
 * 岗位服务类
 */
@Service
public class PositionService {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 根据 ID 查询岗位
     */
    public Optional<Position> findById(Long id) {
        return positionRepository.findById(id);
    }

    /**
     * 根据编码查询岗位
     */
    public Optional<Position> findByCode(String code) {
        return positionRepository.findByCode(code);
    }

    /**
     * 获取所有岗位
     */
    @Transactional(readOnly = true)
    public List<Position> findAll() {
        return positionRepository.findAll();
    }

    /**
     * 保存岗位
     */
    @Transactional
    public Position save(Position position) {
        return positionRepository.save(position);
    }

    /**
     * 初始化默认岗位
     */
    @PostConstruct
    public void initDefaultPositions() {
        long count = positionRepository.count();
        log.info("当前岗位数量：{}", count);
        
        if (count == 0) {
            log.info("开始初始化默认岗位数据...");

            try {
                // 使用 JdbcTemplate 执行原生 SQL，避免 SQLite JDBC 驱动不支持 getGeneratedKeys 的问题
                String now = java.time.LocalDateTime.now().toString().replace('T', ' ');
                
                // 后端开发工程师
                jdbcTemplate.update(
                    "INSERT INTO positions (code, name, description, required_skills, tech_stack, interview_focus, difficulty_level, duration_minutes, question_count, created_at, updated_at) " +
                    "VALUES ('backend', '后端开发工程师', '负责服务器端应用程序的开发、维护和优化，确保系统的高性能和稳定性。', " +
                    "'Java/Python/Go, Spring Boot, MySQL, Redis, Linux', " +
                    "'Java, Spring Boot, MyBatis, MySQL, Redis, RabbitMQ, Docker, Kubernetes', " +
                    "'Java 基础、并发编程、JVM、数据库、框架原理、系统设计、分布式技术', " +
                    "4, 30, 10, ?, ?)",
                    now, now);
                log.info("保存岗位：后端开发工程师");

                // 前端开发工程师
                jdbcTemplate.update(
                    "INSERT INTO positions (code, name, description, required_skills, tech_stack, interview_focus, difficulty_level, duration_minutes, question_count, created_at, updated_at) " +
                    "VALUES ('frontend', '前端开发工程师', '负责 Web 前端界面的开发、优化和用户体验提升，实现响应式和交互式界面。', " +
                    "'HTML/CSS/JavaScript, Vue/React, Webpack, HTTP', " +
                    "'JavaScript, TypeScript, Vue.js, React, CSS3, HTML5, Webpack, Vite', " +
                    "'JavaScript 基础、框架原理、CSS 布局、网络知识、性能优化、工程化', " +
                    "3, 30, 10, ?, ?)",
                    now, now);
                log.info("保存岗位：前端开发工程师");

                // 测试工程师
                jdbcTemplate.update(
                    "INSERT INTO positions (code, name, description, required_skills, tech_stack, interview_focus, difficulty_level, duration_minutes, question_count, created_at, updated_at) " +
                    "VALUES ('qa', '测试工程师', '负责软件产品质量保证，设计和执行测试用例，发现和跟踪缺陷，确保产品交付质量。', " +
                    "'测试理论、测试用例设计、自动化测试、性能测试、SQL', " +
                    "'Selenium, JMeter, Postman, pytest, Jenkins, Linux, SQL', " +
                    "'测试理论、用例设计方法、自动化测试、性能测试、持续集成', " +
                    "3, 30, 10, ?, ?)",
                    now, now);
                log.info("保存岗位：测试工程师");

                // 算法工程师
                jdbcTemplate.update(
                    "INSERT INTO positions (code, name, description, required_skills, tech_stack, interview_focus, difficulty_level, duration_minutes, question_count, created_at, updated_at) " +
                    "VALUES ('algorithm', '算法工程师', '负责机器学习、深度学习算法的研究和应用，解决业务中的智能化问题。', " +
                    "'Python, TensorFlow/PyTorch, 机器学习，深度学习，数据结构', " +
                    "'Python, PyTorch, TensorFlow, Scikit-learn, Pandas, NumPy, SQL', " +
                    "'算法基础、机器学习、深度学习、编程能力、数学基础、项目经验', " +
                    "5, 40, 12, ?, ?)",
                    now, now);
                log.info("保存岗位：算法工程师");
                
                log.info("默认岗位数据初始化完成，当前岗位数量：{}", positionRepository.count());
            } catch (Exception e) {
                log.error("默认岗位数据初始化失败", e);
                // 不抛出异常，避免应用启动失败
            }
        } else {
            log.info("岗位数据已存在，跳过初始化");
        }
    }
}