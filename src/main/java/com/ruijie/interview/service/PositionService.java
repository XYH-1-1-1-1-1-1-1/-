package com.ruijie.interview.service;

import com.ruijie.interview.entity.Position;
import com.ruijie.interview.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Transactional
    public void initDefaultPositions() {
        if (positionRepository.count() == 0) {
            log.info("初始化默认岗位数据...");

            try {
                // 后端开发工程师
                Position backend = new Position();
                backend.setCode("backend");
                backend.setName("后端开发工程师");
                backend.setDescription("负责服务器端应用程序的开发、维护和优化，确保系统的高性能和稳定性。");
                backend.setRequiredSkills("Java/Python/Go, Spring Boot, MySQL, Redis, Linux");
                backend.setTechStack("Java, Spring Boot, MyBatis, MySQL, Redis, RabbitMQ, Docker, Kubernetes");
                backend.setInterviewFocus("Java 基础、并发编程、JVM、数据库、框架原理、系统设计、分布式技术");
                backend.setDifficultyLevel(4);
                backend.setDurationMinutes(30);
                backend.setQuestionCount(10);
                positionRepository.save(backend);

                // 前端开发工程师
                Position frontend = new Position();
                frontend.setCode("frontend");
                frontend.setName("前端开发工程师");
                frontend.setDescription("负责 Web 前端界面的开发、优化和用户体验提升，实现响应式和交互式界面。");
                frontend.setRequiredSkills("HTML/CSS/JavaScript, Vue/React, Webpack, HTTP");
                frontend.setTechStack("JavaScript, TypeScript, Vue.js, React, CSS3, HTML5, Webpack, Vite");
                frontend.setInterviewFocus("JavaScript 基础、框架原理、CSS 布局、网络知识、性能优化、工程化");
                frontend.setDifficultyLevel(3);
                frontend.setDurationMinutes(30);
                frontend.setQuestionCount(10);
                positionRepository.save(frontend);

                // 测试工程师
                Position qa = new Position();
                qa.setCode("qa");
                qa.setName("测试工程师");
                qa.setDescription("负责软件产品质量保证，设计和执行测试用例，发现和跟踪缺陷，确保产品交付质量。");
                qa.setRequiredSkills("测试理论、测试用例设计、自动化测试、性能测试、SQL");
                qa.setTechStack("Selenium, JMeter, Postman, pytest, Jenkins, Linux, SQL");
                qa.setInterviewFocus("测试理论、用例设计方法、自动化测试、性能测试、持续集成");
                qa.setDifficultyLevel(3);
                qa.setDurationMinutes(30);
                qa.setQuestionCount(10);
                positionRepository.save(qa);

                // 算法工程师
                Position algorithm = new Position();
                algorithm.setCode("algorithm");
                algorithm.setName("算法工程师");
                algorithm.setDescription("负责机器学习、深度学习算法的研究和应用，解决业务中的智能化问题。");
                algorithm.setRequiredSkills("Python, TensorFlow/PyTorch, 机器学习，深度学习，数据结构");
                algorithm.setTechStack("Python, PyTorch, TensorFlow, Scikit-learn, Pandas, NumPy, SQL");
                algorithm.setInterviewFocus("算法基础、机器学习、深度学习、编程能力、数学基础、项目经验");
                algorithm.setDifficultyLevel(5);
                algorithm.setDurationMinutes(40);
                algorithm.setQuestionCount(12);
                positionRepository.save(algorithm);

                log.info("默认岗位数据初始化完成");
            } catch (Exception e) {
                log.warn("默认岗位数据初始化失败（SQLite 不支持批量插入获取主键），请手动添加岗位数据。错误：{}", e.getMessage());
            }
        }
    }
}