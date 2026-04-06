package com.ruijie.interview.controller;

import com.ruijie.interview.entity.User;
import com.ruijie.interview.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<User> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || username.isEmpty()) {
                return ApiResponse.error("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return ApiResponse.error("密码不能为空");
            }
            
            User user = userService.login(username, password);
            user.setPassword(null); // 不返回密码
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("登录失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String realName = request.get("realName");
            
            if (username == null || username.isEmpty()) {
                return ApiResponse.error("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return ApiResponse.error("密码不能为空");
            }
            
            User user = userService.register(username, password, realName);
            user.setPassword(null);
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("注册失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        try {
            return userService.findById(id)
                .map(user -> {
                    user.setPassword(null);
                    return ApiResponse.success(user);
                })
                .orElse(ApiResponse.error("用户不存在"));
        } catch (Exception e) {
            log.error("获取用户失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User existingUser = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            if (user.getRealName() != null) existingUser.setRealName(user.getRealName());
            if (user.getPhone() != null) existingUser.setPhone(user.getPhone());
            if (user.getEmail() != null) existingUser.setEmail(user.getEmail());
            if (user.getMajor() != null) existingUser.setMajor(user.getMajor());
            if (user.getUniversity() != null) existingUser.setUniversity(user.getUniversity());
            if (user.getGrade() != null) existingUser.setGrade(user.getGrade());
            if (user.getTargetPosition() != null) existingUser.setTargetPosition(user.getTargetPosition());
            
            User updated = userService.update(existingUser);
            updated.setPassword(null);
            return ApiResponse.success(updated);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 统一响应格式
     */
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> resp = new ApiResponse<>();
            resp.code = 200;
            resp.message = "success";
            resp.data = data;
            return resp;
        }

        public static <T> ApiResponse<T> error(String message) {
            return error(500, message);
        }

        public static <T> ApiResponse<T> error(int code, String message) {
            ApiResponse<T> resp = new ApiResponse<>();
            resp.code = code;
            resp.message = message;
            return resp;
        }

        // Getters and Setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
}