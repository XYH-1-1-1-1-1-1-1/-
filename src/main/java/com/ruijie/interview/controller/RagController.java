package com.ruijie.interview.controller;

import com.ruijie.interview.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG (检索增强生成) 控制器
 * 提供知识库检索、向量搜索、向量化管理等功能
 */
@RestController
@RequestMapping("/rag")
@CrossOrigin(origins = "*")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    @Autowired
    private RagService ragService;

    /**
     * 搜索知识库（自动选择最佳检索方式）
     * 如果向量搜索已启用且有数据，将使用混合搜索；否则使用关键词匹配
     */
    @PostMapping("/search")
    public UserController.ApiResponse<List<RagService.KnowledgeItem>> searchKnowledge(@RequestBody Map<String, Object> request) {
        try {
            String positionId = (String) request.get("positionId");
            String query = (String) request.get("query");
            int limit = request.containsKey("limit") ? ((Number) request.get("limit")).intValue() : 10;

            if (positionId == null || positionId.isEmpty()) {
                return UserController.ApiResponse.error("岗位 ID 不能为空");
            }
            if (query == null || query.isEmpty()) {
                return UserController.ApiResponse.error("查询内容不能为空");
            }

            // 使用增强检索（自动选择向量或关键词）
            List<RagService.KnowledgeItem> results = ragService.retrieveKnowledgeEnhanced(positionId, query, limit);

            return UserController.ApiResponse.success(results);
        } catch (Exception e) {
            log.error("搜索知识库失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 仅使用向量搜索
     */
    @PostMapping("/search/vector")
    public UserController.ApiResponse<List<RagService.KnowledgeItem>> searchByVector(@RequestBody Map<String, Object> request) {
        try {
            String positionId = (String) request.get("positionId");
            String query = (String) request.get("query");
            int limit = request.containsKey("limit") ? ((Number) request.get("limit")).intValue() : 10;

            if (positionId == null || positionId.isEmpty()) {
                return UserController.ApiResponse.error("岗位 ID 不能为空");
            }
            if (query == null || query.isEmpty()) {
                return UserController.ApiResponse.error("查询内容不能为空");
            }

            List<RagService.KnowledgeItem> results = ragService.retrieveKnowledgeByVector(positionId, query, limit);

            return UserController.ApiResponse.success(results);
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 仅使用关键词搜索
     */
    @PostMapping("/search/keyword")
    public UserController.ApiResponse<List<RagService.KnowledgeItem>> searchByKeyword(@RequestBody Map<String, Object> request) {
        try {
            String positionId = (String) request.get("positionId");
            String query = (String) request.get("query");
            int limit = request.containsKey("limit") ? ((Number) request.get("limit")).intValue() : 10;

            if (positionId == null || positionId.isEmpty()) {
                return UserController.ApiResponse.error("岗位 ID 不能为空");
            }
            if (query == null || query.isEmpty()) {
                return UserController.ApiResponse.error("查询内容不能为空");
            }

            List<RagService.KnowledgeItem> results = ragService.retrieveKnowledge(positionId, query, limit);

            return UserController.ApiResponse.success(results);
        } catch (Exception e) {
            log.error("关键词搜索失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 使用 RAG 增强对话
     */
    @PostMapping("/chat")
    public UserController.ApiResponse<String> chatWithRag(@RequestBody Map<String, Object> request) {
        try {
            String positionId = (String) request.get("positionId");
            String query = (String) request.get("query");
            String systemPrompt = (String) request.get("systemPrompt");

            if (positionId == null || positionId.isEmpty()) {
                return UserController.ApiResponse.error("岗位 ID 不能为空");
            }
            if (query == null || query.isEmpty()) {
                return UserController.ApiResponse.error("查询内容不能为空");
            }

            // 使用增强版 RAG 对话
            String response = ragService.chatWithRagEnhanced(positionId, query, systemPrompt);

            return UserController.ApiResponse.success(response);
        } catch (Exception e) {
            log.error("RAG 对话失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取向量化状态
     */
    @GetMapping("/vector/status")
    public UserController.ApiResponse<Map<String, Object>> getVectorStatus() {
        try {
            Map<String, Object> status = ragService.getVectorizationStatus();
            return UserController.ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取向量化状态失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 手动触向量化知识库
     */
    @PostMapping("/vector/vectorize")
    public UserController.ApiResponse<Map<String, Object>> triggerVectorization() {
        try {
            ragService.triggerVectorization();

            Map<String, Object> result = new HashMap<>();
            result.put("message", "已开始向量化知识库，此操作在后台异步执行");

            return UserController.ApiResponse.success(result);
        } catch (Exception e) {
            log.error("触向量化失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 设置向量搜索权重
     */
    @PostMapping("/vector/weight")
    public UserController.ApiResponse<Map<String, Object>> setVectorWeight(@RequestBody Map<String, Object> request) {
        try {
            double weight = ((Number) request.get("weight")).doubleValue();
            ragService.setVectorWeight(weight);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "向量搜索权重已更新");
            result.put("weight", weight);

            return UserController.ApiResponse.success(result);
        } catch (Exception e) {
            log.error("设置权重失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 重新加载知识库
     */
    @PostMapping("/reload")
    public UserController.ApiResponse<Map<String, Object>> reloadKnowledgeBase() {
        try {
            ragService.loadKnowledgeBase();
            // 向量化也在后台异步执行
            ragService.triggerVectorization();

            Map<String, Object> result = new HashMap<>();
            result.put("message", "知识库已重新加载，并开始向量化");

            return UserController.ApiResponse.success(result);
        } catch (Exception e) {
            log.error("重新加载知识库失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }
}