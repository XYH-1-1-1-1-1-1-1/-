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
import java.util.Optional;

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
            String interviewMode = (String) request.getOrDefault("interviewMode", "PRACTICE");
            
            if (userId == null || userId <= 0) {
                return UserController.ApiResponse.error("用户 ID 无效");
            }
            if (positionCode == null || positionCode.isEmpty()) {
                return UserController.ApiResponse.error("岗位编码不能为空");
            }
            
            InterviewSession session = interviewService.startInterview(userId, positionCode, interviewMode);
            
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
     * 获取用户指定岗位的面试历史
     */
    @GetMapping("/history/{userId}/{positionCode}")
    public UserController.ApiResponse<List<InterviewSession>> getHistoryByPosition(
            @PathVariable Long userId,
            @PathVariable String positionCode) {
        try {
            List<InterviewSession> sessions = interviewService.getUserSessionsByPosition(userId, positionCode);
            return UserController.ApiResponse.success(sessions);
        } catch (Exception e) {
            log.error("获取岗位面试历史失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户所有岗位类型列表（用于历史记录的岗位筛选）
     */
    @GetMapping("/history/{userId}/positions")
    public UserController.ApiResponse<List<Map<String, Object>>> getUserPositions(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> positions = interviewService.getUserDistinctPositions(userId);
            return UserController.ApiResponse.success(positions);
        } catch (Exception e) {
            log.error("获取用户岗位列表失败", e);
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
     * 跳过问题
     */
    @PostMapping("/{sessionId}/skip")
    public UserController.ApiResponse<?> skipQuestion(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            
            InterviewService.InterviewResponse response = 
                interviewService.skipQuestion(sessionId, userId);
            
            return UserController.ApiResponse.success(response);
        } catch (Exception e) {
            log.error("跳过问题失败", e);
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

    /**
     * 删除面试会话记录
     */
    @DeleteMapping("/{sessionId}")
    public UserController.ApiResponse<Map<String, Object>> deleteSession(@PathVariable Long sessionId) {
        try {
            // 先检查会话是否存在
            if (!interviewService.getSession(sessionId).isPresent()) {
                return UserController.ApiResponse.error("会话不存在：" + sessionId);
            }
            
            // 删除会话及相关数据
            interviewService.deleteInterviewSession(sessionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("message", "删除成功");
            
            return UserController.ApiResponse.success(result);
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 检查并恢复进行中的面试
     */
    @GetMapping("/resume/{userId}")
    public UserController.ApiResponse<Map<String, Object>> resumeInterview(@PathVariable Long userId) {
        try {
            // 查找进行中的面试
            java.util.Optional<InterviewSession> inProgressSession = interviewService.getInProgressSession(userId);
            
            if (inProgressSession.isPresent()) {
                InterviewSession session = inProgressSession.get();
                int questionIndex = interviewService.getCurrentQuestionIndex(session.getId());
                
                Map<String, Object> result = new HashMap<>();
                result.put("hasInProgress", true);
                result.put("sessionId", session.getId());
                result.put("positionId", session.getPositionId());
                result.put("questionIndex", questionIndex);
                result.put("totalQuestions", session.getTotalQuestions());
                result.put("status", session.getStatus());
                result.put("interviewMode", session.getInterviewMode()); // 返回面试模式
                
                return UserController.ApiResponse.success(result);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("hasInProgress", false);
                
                return UserController.ApiResponse.success(result);
            }
        } catch (Exception e) {
            log.error("检查进行中的面试失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 退出面试（不保存任何历史记录）
     * 用于用户在面试过程中主动退出，或拒绝恢复面试
     */
    @PostMapping("/{sessionId}/exit")
    public UserController.ApiResponse<Map<String, Object>> exitInterview(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        try {
            // 检查会话是否存在
            if (!interviewService.getSession(sessionId).isPresent()) {
                return UserController.ApiResponse.error("会话不存在：" + sessionId);
            }
            
            // 退出面试，删除所有相关数据
            interviewService.exitInterview(sessionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "已退出面试");
            
            return UserController.ApiResponse.success(result);
        } catch (Exception e) {
            log.error("退出面试失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 检查评估报告生成进度（用于异步评分场景）
     */
    @GetMapping("/{sessionId}/report-status")
    public UserController.ApiResponse<Map<String, Object>> getReportStatus(@PathVariable Long sessionId) {
        try {
            // 先检查报告是否已生成
            Optional<EvaluationReport> reportOpt = interviewService.getReportBySessionId(sessionId);
            
            Map<String, Object> result = new HashMap<>();
            
            if (reportOpt.isPresent()) {
                // 报告已生成
                result.put("completed", true);
                result.put("report", reportOpt.get());
                return UserController.ApiResponse.success(result);
            }
            
            // 报告未生成，检查评分进度
            InterviewService.ScoringStatus status = interviewService.getScoringStatus(sessionId);
            
            if (status != null) {
                result.put("completed", false);
                result.put("progress", status.getProgress());
                result.put("scoredQuestions", status.scoredQuestions);
                result.put("totalQuestions", status.totalQuestions);
                result.put("message", "正在生成评估报告，请稍候...");
            } else {
                // 没有评分任务，可能是会话不存在或已结束但未开始评分
                result.put("completed", false);
                result.put("progress", 0);
                result.put("message", "等待报告生成...");
            }
            
            return UserController.ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("获取报告状态失败", e);
            return UserController.ApiResponse.error(e.getMessage());
        }
    }
}
