package com.ruijie.interview.controller;

import com.ruijie.interview.entity.Position;
import com.ruijie.interview.entity.Question;
import com.ruijie.interview.service.PositionService;
import com.ruijie.interview.service.QuestionService;
import com.ruijie.interview.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 岗位控制器
 */
@Slf4j
@RestController
@RequestMapping("/position")
@CrossOrigin(origins = "*")
public class PositionController {

    @Autowired
    private PositionService positionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private RagService ragService;

    /**
     * 获取所有岗位列表
     */
    @GetMapping("/list")
    public UserController.ApiResponse<List<Position>> getPositions() {
        try {
            List<Position> positions = positionService.findAll();
            return UserController.ApiResponse.success(positions);
        } catch (Exception e) {
            log.error("获取岗位列表失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取岗位详情
     */
    @GetMapping("/{code}")
    public UserController.ApiResponse<Position> getPosition(@PathVariable String code) {
        try {
            return positionService.findByCode(code)
                .map(UserController.ApiResponse::success)
                .orElse(UserController.ApiResponse.error("岗位不存在"));
        } catch (Exception e) {
            log.error("获取岗位详情失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取岗位题库
     */
    @GetMapping("/{code}/questions")
    public UserController.ApiResponse<List<Question>> getQuestions(@PathVariable String code) {
        try {
            List<Question> questions = questionService.findByPositionId(code);
            return UserController.ApiResponse.success(questions);
        } catch (Exception e) {
            log.error("获取题库失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取岗位知识库类别
     */
    @GetMapping("/{code}/knowledge-categories")
    public UserController.ApiResponse<Set<String>> getKnowledgeCategories(@PathVariable String code) {
        try {
            Set<String> categories = ragService.getKnowledgeCategories(code);
            return UserController.ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取知识库类别失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取岗位知识库内容
     */
    @GetMapping("/{code}/knowledge")
    public UserController.ApiResponse<List<RagService.KnowledgeItem>> getKnowledge(
            @PathVariable String code,
            @RequestParam(required = false) String category) {
        try {
            List<RagService.KnowledgeItem> items;
            if (category != null && !category.isEmpty()) {
                items = ragService.getKnowledgeByCategory(code, category);
            } else {
                // 检索所有知识
                items = ragService.retrieveKnowledge(code, "", 100);
            }
            return UserController.ApiResponse.success(items);
        } catch (Exception e) {
            log.error("获取知识库内容失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 检索知识库
     */
    @GetMapping("/{code}/knowledge/search")
    public UserController.ApiResponse<List<RagService.KnowledgeItem>> searchKnowledge(
            @PathVariable String code,
            @RequestParam String query) {
        try {
            List<RagService.KnowledgeItem> items = ragService.retrieveKnowledge(code, query, 20);
            return UserController.ApiResponse.success(items);
        } catch (Exception e) {
            log.error("检索知识库失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }
}