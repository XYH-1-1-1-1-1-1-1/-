package com.ruijie.interview.controller;

import com.ruijie.interview.service.SpeechEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 语音表达评估控制器
 * 综合声学特征和文本分析，评估语速、清晰度、自信度等表达表现
 */
@RestController
@RequestMapping("/speech")
@CrossOrigin(origins = "*")
public class SpeechEvaluationController {

    private static final Logger log = LoggerFactory.getLogger(SpeechEvaluationController.class);

    @Autowired
    private SpeechEvaluationService speechEvaluationService;

    /**
     * 语音表达评估接口
     * 接收前端采集的声学特征和转录文本，进行综合评估
     *
     * @param request 包含转录文本、问题、声学特征的 JSON 数据
     * @return 评估结果
     */
    @PostMapping("/evaluate")
    public UserController.ApiResponse<Map<String, Object>> evaluateSpeech(
            @RequestBody SpeechEvaluationService.SpeechEvaluationRequest request) {
        
        try {
            log.info("[语音表达评估] 收到请求 - 问题：{}, 文本长度：{}, 声学特征：{}", 
                request.getQuestion(), 
                request.getTranscript() != null ? request.getTranscript().length() : 0,
                request.getAudioFeatures() != null ? "有" : "无");
            
            // 进行语音表达评估
            SpeechEvaluationService.SpeechEvaluationResult result = 
                speechEvaluationService.evaluateSpeechExpression(request);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("speechRateScore", result.getSpeechRateScore());
            response.put("clarityScore", result.getClarityScore());
            response.put("confidenceScore", result.getConfidenceScore());
            response.put("fluencyScore", result.getFluencyScore());
            response.put("expressionScore", result.getExpressionScore());
            response.put("overallScore", result.getOverallScore());
            response.put("emotion", result.getEmotion());
            response.put("emotionAnalysis", result.getEmotionAnalysis());
            response.put("speechRateAnalysis", result.getSpeechRateAnalysis());
            response.put("clarityAnalysis", result.getClarityAnalysis());
            response.put("confidenceAnalysis", result.getConfidenceAnalysis());
            response.put("fluencyAnalysis", result.getFluencyAnalysis());
            response.put("expressionAnalysis", result.getExpressionAnalysis());
            response.put("overallAnalysis", result.getOverallAnalysis());
            response.put("suggestions", result.getSuggestions());
            response.put("transcript", result.getTranscript());
            
            // 如果有声学特征，返回原始数据
            if (result.getAudioFeatures() != null) {
                Map<String, Object> features = new HashMap<>();
                features.put("speechRate", result.getAudioFeatures().getSpeechRate());
                features.put("pauseCount", result.getAudioFeatures().getPauseCount());
                features.put("fillerCount", result.getAudioFeatures().getFillerCount());
                features.put("duration", result.getAudioFeatures().getDuration());
                response.put("audioFeatures", features);
            }
            
            return UserController.ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("[语音表达评估] 评估失败", e);
            return UserController.ApiResponse.error("语音表达评估失败：" + e.getMessage());
        }
    }

    /**
     * 简化的语音表达评估接口
     * 仅接收转录文本和问题，进行文本分析
     *
     * @param request 包含转录文本和问题的 JSON 数据
     * @return 评估结果
     */
    @PostMapping("/evaluate/simple")
    public UserController.ApiResponse<Map<String, Object>> evaluateSpeechSimple(
            @RequestBody Map<String, String> request) {
        
        try {
            String transcript = request.get("transcript");
            String question = request.get("question");
            
            log.info("[语音表达评估 - 简化版] 收到请求 - 问题：{}, 文本长度：{}", 
                question, transcript != null ? transcript.length() : 0);
            
            // 构建请求（无声学特征）
            SpeechEvaluationService.SpeechEvaluationRequest evalRequest = 
                new SpeechEvaluationService.SpeechEvaluationRequest();
            evalRequest.setTranscript(transcript);
            evalRequest.setQuestion(question);
            
            // 进行语音表达评估
            SpeechEvaluationService.SpeechEvaluationResult result = 
                speechEvaluationService.evaluateSpeechExpression(evalRequest);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("speechRateScore", result.getSpeechRateScore());
            response.put("clarityScore", result.getClarityScore());
            response.put("confidenceScore", result.getConfidenceScore());
            response.put("fluencyScore", result.getFluencyScore());
            response.put("expressionScore", result.getExpressionScore());
            response.put("overallScore", result.getOverallScore());
            response.put("emotion", result.getEmotion());
            response.put("emotionAnalysis", result.getEmotionAnalysis());
            response.put("suggestions", result.getSuggestions());
            
            return UserController.ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("[语音表达评估 - 简化版] 评估失败", e);
            return UserController.ApiResponse.error("语音表达评估失败：" + e.getMessage());
        }
    }
}