package com.ruijie.interview.service;

import com.ruijie.interview.entity.User;
import com.ruijie.interview.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务类
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 初始化默认用户（演示账号）
     */
    @PostConstruct
    @Transactional
    public void initDefaultUser() {
        if (userRepository.count() == 0) {
            log.info("开始初始化默认用户数据...");
            try {
                String now = java.time.LocalDateTime.now().toString().replace('T', ' ');
                
                // 使用 JdbcTemplate 直接插入，避免 SQLite JDBC 驱动不支持 getGeneratedKeys 的问题
                jdbcTemplate.update(
                    "INSERT INTO users (username, password, real_name, phone, email, major, university, grade, target_position, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    "demo", "demo123", "演示用户", "13800138000", "demo@example.com",
                    "计算机科学与技术", "演示大学", "2024", "JAVA_BACKEND", now, now);
                
                log.info("默认用户数据初始化完成，用户名：demo，密码：demo123");
            } catch (Exception e) {
                log.error("默认用户数据初始化失败", e);
                // 不抛出异常，避免应用启动失败
            }
        } else {
            log.info("用户数据已存在，跳过初始化");
        }
    }

    /**
     * 根据 ID 查询用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 根据用户名查询用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户名不存在");
        }
        User user = userOpt.get();
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    /**
     * 用户注册
     */
    @Transactional
    public User register(String username, String password, String realName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        return userRepository.save(user);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User update(User user) {
        return userRepository.save(user);
    }

    /**
     * 获取所有用户
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }
}