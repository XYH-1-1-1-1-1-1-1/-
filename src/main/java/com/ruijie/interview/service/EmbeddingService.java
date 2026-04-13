package com.ruijie.interview.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruijie.interview.config.LlmConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedding 服务 - 将文本转换为向量表示
 * 
 * 功能说明：
 * 1. 调用 DashScope/通义千问 的 text-embedding 模型
 * 2. 将文本转换为固定维度的向量
 * 3. 支持向量缓存，避免重复调用 API
 * 4. 支持批量embedding生成
 * 
 * 向量维度：text-embedding-v3 模型输出 1024 维
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    // Embedding 模型名称
    private static final String EMBEDDING_MODEL = "text-embedding-v3";

    // 向量维度
    public static final int EMBEDDING_DIMENSION = 1024;

    @Autowired
    private LlmConfig llmConfig;

    private CloseableHttpClient httpClient;

    // 向量缓存：文本内容 -> 向量
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.httpClient = HttpClients.createDefault();
        log.info("EmbeddingService 初始化完成，使用模型：{}", EMBEDDING_MODEL);
    }

    /**
     * 获取单个文本的向量表示
     *
     * @param text 输入文本
     * @return 向量数组（1024维）
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空，返回零向量");
            return new float[EMBEDDING_DIMENSION];
        }

        String cacheKey = normalizeText(text);
        
        // 检查缓存
        if (embeddingCache.containsKey(cacheKey)) {
            log.debug("命中 embedding 缓存: {}", cacheKey.substring(0, Math.min(30, cacheKey.length())));
            return embeddingCache.get(cacheKey).clone();
        }

        try {
            float[] embedding = callEmbeddingAPI(text.trim());
            embeddingCache.put(cacheKey, embedding.clone());
            return embedding;
        } catch (Exception e) {
            log.error("获取 embedding 失败: {}", e.getMessage(), e);
            // 返回零向量作为降级方案
            return new float[EMBEDDING_DIMENSION];
        }
    }

    /**
     * 批量获取文本的向量表示
     *
     * @param texts 输入文本列表
     * @return 向量列表
     */
    public List<float[]> getBatchEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(getEmbedding(text));
        }
        return embeddings;
    }

    /**
     * 调用 DashScope Embedding API
     */
    private float[] callEmbeddingAPI(String text) throws IOException {
        String url = llmConfig.getBaseUrl() + "/embeddings";

        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", EMBEDDING_MODEL);
        requestBody.put("input", new String[]{text});

        // text-embedding-v3 支持 dimensions 参数
        JSONObject parameters = new JSONObject();
        parameters.put("dimensions", EMBEDDING_DIMENSION);
        requestBody.put("parameters", parameters);

        StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        return httpClient.execute(post, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            if (statusCode != 200) {
                log.error("Embedding API Error: {} - {}", statusCode, responseBody);
                throw new IOException("Embedding API Error: " + statusCode);
            }

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            JSONArray data = jsonResponse.getJSONArray("data");
            
            if (data != null && data.size() > 0) {
                JSONObject item = data.getJSONObject(0);
                JSONArray embeddingArray = item.getJSONArray("embedding");
                
                float[] embedding = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    embedding[i] = embeddingArray.getFloatValue(i);
                }
                return embedding;
            }
            
            throw new IOException("Embedding API returned empty result");
        });
    }

    /**
     * 规范化文本用于缓存键
     */
    private String normalizeText(String text) {
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        embeddingCache.clear();
        log.info("Embedding 缓存已清除");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return embeddingCache.size();
    }
}