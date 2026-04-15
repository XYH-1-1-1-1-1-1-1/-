package com.ruijie.interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 大模型配置类
 */
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

    /**
     * 是否启用深度思考（仅 qwen3.6-plus 等模型支持）
     */
    private boolean enableThinking = false;

    // Getter 和 Setter 方法
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getFallbackModels() {
        return fallbackModels;
    }

    public void setFallbackModels(List<String> fallbackModels) {
        this.fallbackModels = fallbackModels;
    }

    public boolean isEnableThinking() {
        return enableThinking;
    }

    public void setEnableThinking(boolean enableThinking) {
        this.enableThinking = enableThinking;
    }
}
