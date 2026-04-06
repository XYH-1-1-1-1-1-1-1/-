package com.ruijie.interview.service;

import com.ruijie.interview.entity.User;
import com.ruijie.interview.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务类
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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