package com.ruijie.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AI 模拟面试与能力提升软件 - 主应用程序
 * 
 * @author Ruijie Networks
 */
@SpringBootApplication
public class Application {

    // 简历上传目录
    private static final String RESUME_UPLOAD_DIR = "data/resumes/";

    public static void main(String[] args) {
        // 创建简历上传目录
        createUploadDirectory();
        
        SpringApplication.run(Application.class, args);
        System.out.println("========================================");
        System.out.println("   AI 模拟面试与能力提升软件 已启动");
        System.out.println("   锐捷网络 - 教育行业解决方案");
        System.out.println("========================================");
    }
    
    /**
     * 创建上传目录
     */
    private static void createUploadDirectory() {
        try {
            Path uploadDir = Paths.get(RESUME_UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                System.out.println("已创建简历上传目录：" + RESUME_UPLOAD_DIR);
            }
        } catch (Exception e) {
            System.err.println("创建上传目录失败：" + e.getMessage());
        }
    }
}
