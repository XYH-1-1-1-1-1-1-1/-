package com.ruijie.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 大模型配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.llm")
public class LlmConfig {

    /**
     * API 提供商
     */
    private String provider;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API 基础 URL
     */
    private String baseUrl;

    /**
     * 默认模型
     */
    private String model;

    /**
     * 备选模型列表
     */
    private List<String> fallbackModels;
}