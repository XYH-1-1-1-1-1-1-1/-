package com.ruijie.interview.controller;

import com.ruijie.interview.entity.EvaluationReport;
import com.ruijie.interview.entity.InterviewSession;
import com.ruijie.interview.entity.Question;
import com.ruijie.interview.service.InterviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 */
@RestController
@RequestMapping("/interview")
@CrossOrigin(origins = "*")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    @Autowired
    private InterviewService interviewService;

    /**
     * 开始新的面试
     */
    @PostMapping("/start")
    public UserController.ApiResponse<InterviewSession> startInterview(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String positionCode = (String) request.get("positionCode");
            
            if (userId == null || userId <= 0) {
                return UserController.ApiResponse.error("用户 ID 无效");
            }
            if (positionCode == null || positionCode.isEmpty()) {
                return UserController.ApiResponse.error("岗位编码不能为空");
            }
            
            InterviewSession session = interviewService.startInterview(userId, positionCode);
            
            return UserController.ApiResponse.success(session);
        } catch (Exception e) {
            log.error("开始面试失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取面试会话
     */
    @GetMapping("/{id}")
    public UserController.ApiResponse<InterviewSession> getSession(@PathVariable Long id) {
        try {
            return interviewService.getSession(id)
                .map(UserController.ApiResponse::success)
                .orElse(UserController.ApiResponse.error("会话不存在"));
        } catch (Exception e) {
            log.error("获取会话失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户的面试历史
     */
    @GetMapping("/history/{userId}")
    public UserController.ApiResponse<List<InterviewSession>> getHistory(@PathVariable Long userId) {
        try {
            List<InterviewSession> sessions = interviewService.getUserSessions(userId);
            return UserController.ApiResponse.success(sessions);
        } catch (Exception e) {
            log.error("获取面试历史失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 提交回答
     */
    @PostMapping("/{sessionId}/answer")
    public UserController.ApiResponse<?> submitAnswer(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String answer = request.get("answer");
            String answerType = request.getOrDefault("answerType", "text");
            
            if (answer == null || answer.isEmpty()) {
                return UserController.ApiResponse.error("回答不能为空");
            }
            
            InterviewService.InterviewResponse response = 
                interviewService.submitAnswer(sessionId, userId, answer, answerType);
            
            return UserController.ApiResponse.success(response);
        } catch (Exception e) {
            log.error("提交回答失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取评估报告
     */
    @GetMapping("/{sessionId}/report")
    public UserController.ApiResponse<EvaluationReport> getReport(@PathVariable Long sessionId) {
        try {
            return interviewService.getReportBySessionId(sessionId)
                .map(UserController.ApiResponse::success)
                .orElse(UserController.ApiResponse.error("报告不存在"));
        } catch (Exception e) {
            log.error("获取报告失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户的评估报告列表
     */
    @GetMapping("/reports/{userId}")
    public UserController.ApiResponse<List<EvaluationReport>> getUserReports(@PathVariable Long userId) {
        try {
            List<EvaluationReport> reports = interviewService.getUserReports(userId);
            return UserController.ApiResponse.success(reports);
        } catch (Exception e) {
            log.error("获取报告列表失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }
}