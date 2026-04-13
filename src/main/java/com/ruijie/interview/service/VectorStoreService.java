package com.ruijie.interview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 向量存储服务 - 基于 SQLite 的轻量级向量检索
 * 
 * 功能说明：
 * 1. 在 SQLite 中存储知识条目的向量表示
 * 2. 支持余弦相似度计算
 * 3. 支持按向量相似度检索
 * 4. 支持向量与关键词混合检索
 * 
 * 技术实现：
 * - 使用 SQLite 表存储向量（JSON 格式）
 * - 使用 Java 计算余弦相似度
 * - 支持混合检索策略
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private static final String TABLE_NAME = "knowledge_vectors";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 初始化向量存储表
     */
    @PostConstruct
    public void init() {
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id TEXT PRIMARY KEY,
                    position_id TEXT NOT NULL,
                    category TEXT,
                    title TEXT,
                    content TEXT,
                    tags TEXT,
                    vector_data TEXT NOT NULL,
                    vector_dimension INTEGER DEFAULT 1024,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(TABLE_NAME);
            
            jdbcTemplate.execute(createTableSql);
            log.info("向量存储表初始化完成");
        } catch (Exception e) {
            log.error("向量存储表初始化失败", e);
        }
    }

    /**
     * 保存或更新知识条目的向量
     *
     * @param id 知识条目 ID
     * @param positionId 岗位 ID
     * @param category 知识类别
     * @param title 标题
     * @param content 内容
     * @param tags 标签
     * @param vector 向量数据
     */
    public void saveVector(String id, String positionId, String category, 
                          String title, String content, String tags, float[] vector) {
        try {
            String vectorJson = vectorToJson(vector);
            
            String upsertSql = """
                INSERT INTO %s (id, position_id, category, title, content, tags, vector_data)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    position_id = excluded.position_id,
                    category = excluded.category,
                    title = excluded.title,
                    content = excluded.content,
                    tags = excluded.tags,
                    vector_data = excluded.vector_data,
                    updated_at = CURRENT_TIMESTAMP
                """.formatted(TABLE_NAME);
            
            jdbcTemplate.update(upsertSql, id, positionId, category, title, content, tags, vectorJson);
            log.debug("保存向量成功: id={}, positionId={}", id, positionId);
        } catch (Exception e) {
            log.error("保存向量失败: id={}", id, e);
        }
    }

    /**
     * 删除指定岗位的所有向量
     *
     * @param positionId 岗位 ID
     */
    public void deleteByPosition(String positionId) {
        String deleteSql = "DELETE FROM %s WHERE position_id = ?".formatted(TABLE_NAME);
        int count = jdbcTemplate.update(deleteSql, positionId);
        log.info("删除岗位 {} 的 {} 条向量", positionId, count);
    }

    /**
     * 基于向量相似度检索知识条目
     *
     * @param queryVector 查询向量
     * @param positionId 岗位 ID
     * @param limit 返回数量
     * @return 按相似度排序的知识条目列表
     */
    public List<VectorSearchResult> searchByVector(float[] queryVector, String positionId, int limit) {
        String querySql = """
            SELECT id, position_id, category, title, content, tags, vector_data
            FROM %s WHERE position_id = ?
            """.formatted(TABLE_NAME);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySql, positionId);
        List<VectorSearchResult> results = new ArrayList<>();
        
        for (Map<String, Object> row : rows) {
            String vectorData = (String) row.get("vector_data");
            float[] storedVector = jsonToVector(vectorData);
            
            double similarity = cosineSimilarity(queryVector, storedVector);
            
            VectorSearchResult result = new VectorSearchResult(
                (String) row.get("id"),
                (String) row.get("position_id"),
                (String) row.get("category"),
                (String) row.get("title"),
                (String) row.get("content"),
                (String) row.get("tags"),
                similarity
            );
            results.add(result);
        }
        
        // 按相似度降序排序
        results.sort(Comparator.comparingDouble(VectorSearchResult::getSimilarity).reversed());
        
        // 返回前 N 条
        return results.subList(0, Math.min(limit, results.size()));
    }

    /**
     * 混合检索：结合向量相似度和关键词匹配
     *
     * @param query 查询文本
     * @param queryVector 查询向量
     * @param positionId 岗位 ID
     * @param limit 返回数量
     * @param vectorWeight 向量搜索权重 (0-1)，剩余权重用于关键词匹配
     * @return 混合排序后的结果
     */
    public List<VectorSearchResult> hybridSearch(String query, float[] queryVector, String positionId, 
                                                  int limit, double vectorWeight) {
        String querySql = """
            SELECT id, position_id, category, title, content, tags, vector_data
            FROM %s WHERE position_id = ?
            """.formatted(TABLE_NAME);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(querySql, positionId);
        List<VectorSearchResult> results = new ArrayList<>();
        
        String lowerQuery = query.toLowerCase();
        
        for (Map<String, Object> row : rows) {
            String vectorData = (String) row.get("vector_data");
            float[] storedVector = jsonToVector(vectorData);
            
            // 向量相似度
            double vectorSimilarity = cosineSimilarity(queryVector, storedVector);
            
            // 关键词匹配分数
            double keywordScore = calculateKeywordScore(
                (String) row.get("title"),
                (String) row.get("content"),
                (String) row.get("tags"),
                lowerQuery
            );
            
            // 归一化关键词分数到 0-1 范围
            double normalizedKeywordScore = keywordScore / 30.0; // 最大可能分数是 10+8+5+重要性
            
            // 混合分数
            double hybridScore = (vectorWeight * vectorSimilarity) + 
                                 ((1 - vectorWeight) * normalizedKeywordScore);
            
            VectorSearchResult result = new VectorSearchResult(
                (String) row.get("id"),
                (String) row.get("position_id"),
                (String) row.get("category"),
                (String) row.get("title"),
                (String) row.get("content"),
                (String) row.get("tags"),
                hybridScore
            );
            result.setVectorSimilarity(vectorSimilarity);
            result.setKeywordScore(keywordScore);
            results.add(result);
        }
        
        // 按混合分数降序排序
        results.sort(Comparator.comparingDouble(VectorSearchResult::getSimilarity).reversed());
        
        return results.subList(0, Math.min(limit, results.size()));
    }

    /**
     * 计算关键词匹配分数（与 RagService 中的逻辑保持一致）
     */
    private double calculateKeywordScore(String title, String content, String tags, String query) {
        double score = 0;
        String lowerTitle = title != null ? title.toLowerCase() : "";
        String lowerContent = content != null ? content.toLowerCase() : "";
        String lowerTags = tags != null ? tags.toLowerCase() : "";
        
        if (lowerTitle.contains(query)) score += 10;
        if (lowerContent.contains(query)) score += 5;
        if (lowerTags.contains(query)) score += 8;
        
        // 分词匹配
        String[] words = query.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                if (lowerTitle.contains(word)) score += 3;
                if (lowerTags.contains(word)) score += 2;
            }
        }
        
        return score;
    }

    /**
     * 计算余弦相似度
     *
     * @param a 向量 A
     * @param b 向量 B
     * @return 余弦相似度 (0-1)
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 向量转 JSON 数组字符串
     */
    private String vectorToJson(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * JSON 数组字符串转向量
     */
    private float[] jsonToVector(String json) {
        String[] parts = json.replaceAll("[\\[\\]]", "").split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    /**
     * 检查向量存储是否为空
     */
    public boolean isEmpty() {
        String countSql = "SELECT COUNT(*) FROM %s".formatted(TABLE_NAME);
        try {
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            return count == null || count == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取向量总数
     */
    public long getCount() {
        String countSql = "SELECT COUNT(*) FROM %s".formatted(TABLE_NAME);
        try {
            Long count = jdbcTemplate.queryForObject(countSql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 向量搜索结果
     */
    public static class VectorSearchResult {
        private String id;
        private String positionId;
        private String category;
        private String title;
        private String content;
        private String tags;
        private double similarity;
        private double vectorSimilarity;
        private double keywordScore;

        public VectorSearchResult(String id, String positionId, String category, 
                                  String title, String content, String tags, double similarity) {
            this.id = id;
            this.positionId = positionId;
            this.category = category;
            this.title = title;
            this.content = content;
            this.tags = tags;
            this.similarity = similarity;
            this.vectorSimilarity = similarity;
            this.keywordScore = 0;
        }

        public String getId() { return id; }
        public String getPositionId() { return positionId; }
        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getTags() { return tags; }
        public double getSimilarity() { return similarity; }
        public double getVectorSimilarity() { return vectorSimilarity; }
        public void setVectorSimilarity(double vectorSimilarity) { this.vectorSimilarity = vectorSimilarity; }
        public double getKeywordScore() { return keywordScore; }
        public void setKeywordScore(double keywordScore) { this.keywordScore = keywordScore; }

        @Override
        public String toString() {
            return String.format("Result{id='%s', title='%s', similarity=%.4f, vectorSim=%.4f, keywordScore=%.1f}",
                id, title, similarity, vectorSimilarity, keywordScore);
        }
    }
}