package com.ruijie.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 模拟面试与能力提升软件 - 主应用程序
 * 
 * @author Ruijie Networks
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("========================================");
        System.out.println("   AI 模拟面试与能力提升软件 已启动");
        System.out.println("   锐捷网络 - 教育行业解决方案");
        System.out.println("========================================");
    }
}