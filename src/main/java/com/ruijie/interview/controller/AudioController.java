package com.ruijie.interview.controller;

import com.ruijie.interview.service.AudioAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 音频处理控制器
 * 支持接收音频文件并调用千问大模型进行分析
 */
@RestController
@RequestMapping("/audio")
@CrossOrigin(origins = "*")
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private AudioAnalysisService audioAnalysisService;

    /**
     * 上传音频文件并进行分析
     * 前端可以发送音频文件（FormData 格式）或 Base64 编码的音频
     *
     * @param audioFile 音频文件
     * @param question 面试问题（用于上下文分析）
     * @param transcript 语音转录文本（备选方案：如果前端已使用 Web Speech API 转录）
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public UserController.ApiResponse<Map<String, Object>> analyzeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "question", required = false, defaultValue = "") String question,
            @RequestParam(value = "transcript", required = false, defaultValue = "") String transcript) {
        
        try {
            log.info("[音频分析] 收到音频文件：{}, 大小：{} bytes, 问题：{}", 
                audioFile.getOriginalFilename(), audioFile.getSize(), question);
            
            Map<String, Object> result = new HashMap<>();
            
            // 如果前端已提供转录文本，直接使用千问分析情绪
            if (transcript != null && !transcript.trim().isEmpty()) {
                log.info("[音频分析] 使用前端提供的转录文本：{}", transcript);
                AudioAnalysisService.AudioAnalysisResult analysisResult = 
                    audioAnalysisService.analyzeEmotionAndScore(transcript, question);
                analysisResult.setTranscript(transcript);
                
                result.put("transcript", transcript);
                result.put("emotion", analysisResult.getEmotion());
                result.put("emotionAnalysis", analysisResult.getEmotionAnalysis());
                result.put("confidenceLevel", analysisResult.getConfidenceLevel());
                result.put("technicalScore", analysisResult.getTechnicalScore());
                result.put("communicationScore", analysisResult.getCommunicationScore());
                result.put("logicScore", analysisResult.getLogicScore());
                result.put("knowledgeDepth", analysisResult.getKnowledgeDepth());
                result.put("overallScore", analysisResult.getOverallScore());
                result.put("evaluationComment", analysisResult.getEvaluationComment());
                result.put("strengths", analysisResult.getStrengths());
                result.put("weaknesses", analysisResult.getWeaknesses());
                
                return UserController.ApiResponse.success(result);
            }
            
            // 否则使用后端 ASR 服务进行语音识别
            if (audioFile.isEmpty()) {
                return UserController.ApiResponse.error("音频文件不能为空");
            }
            
            // 检查文件大小（最大 10MB）
            if (audioFile.getSize() > 10 * 1024 * 1024) {
                return UserController.ApiResponse.error("音频文件大小不能超过 10MB");
            }
            
            // 检查文件类型
            String contentType = audioFile.getContentType();
            String fileName = audioFile.getOriginalFilename();
            if (!isValidAudioFile(fileName, contentType)) {
                return UserController.ApiResponse.error("不支持的音频格式，请上传 WAV、MP3、OGG 或 M4A 格式");
            }
            
            // 调用音频分析服务
            AudioAnalysisService.AudioAnalysisResult analysisResult = 
                audioAnalysisService.analyzeAudio(audioFile, question);
            
            result.put("transcript", analysisResult.getTranscript());
            result.put("emotion", analysisResult.getEmotion());
            result.put("emotionAnalysis", analysisResult.getEmotionAnalysis());
            result.put("confidenceLevel", analysisResult.getConfidenceLevel());
            result.put("technicalScore", analysisResult.getTechnicalScore());
            result.put("communicationScore", analysisResult.getCommunicationScore());
            result.put("logicScore", analysisResult.getLogicScore());
            result.put("knowledgeDepth", analysisResult.getKnowledgeDepth());
            result.put("overallScore", analysisResult.getOverallScore());
            result.put("evaluationComment", analysisResult.getEvaluationComment());
            result.put("strengths", analysisResult.getStrengths());
            result.put("weaknesses", analysisResult.getWeaknesses());
            
            return UserController.ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("[音频分析] 分析失败", e);
            return UserController.ApiResponse.error("音频分析失败：" + e.getMessage());
        }
    }

    /**
     * 接收 Base64 编码的音频数据并进行分析
     *
     * @param requestData 包含 audioBase64、question 的 JSON 数据
     * @return 分析结果
     */
    @PostMapping("/analyze/base64")
    public UserController.ApiResponse<Map<String, Object>> analyzeAudioBase64(
            @RequestBody Map<String, String> requestData) {
        
        try {
            String audioBase64 = requestData.get("audioBase64");
            String question = requestData.get("question");
            String transcript = requestData.get("transcript");
            
            log.info("[音频分析 Base64] 收到音频数据，大小：{} bytes, 问题：{}", 
                audioBase64 != null ? audioBase64.length() : 0, question);
            
            Map<String, Object> result = new HashMap<>();
            
            // 如果前端已提供转录文本，直接使用千问分析情绪
            if (transcript != null && !transcript.trim().isEmpty()) {
                log.info("[音频分析 Base64] 使用前端提供的转录文本：{}", transcript);
                AudioAnalysisService.AudioAnalysisResult analysisResult = 
                    audioAnalysisService.analyzeEmotionAndScore(transcript, question);
                analysisResult.setTranscript(transcript);
                
                result.put("transcript", transcript);
                result.put("emotion", analysisResult.getEmotion());
                result.put("emotionAnalysis", analysisResult.getEmotionAnalysis());
                result.put("confidenceLevel", analysisResult.getConfidenceLevel());
                result.put("technicalScore", analysisResult.getTechnicalScore());
                result.put("communicationScore", analysisResult.getCommunicationScore());
                result.put("logicScore", analysisResult.getLogicScore());
                result.put("knowledgeDepth", analysisResult.getKnowledgeDepth());
                result.put("overallScore", analysisResult.getOverallScore());
                result.put("evaluationComment", analysisResult.getEvaluationComment());
                result.put("strengths", analysisResult.getStrengths());
                result.put("weaknesses", analysisResult.getWeaknesses());
                
                return UserController.ApiResponse.success(result);
            }
            
            // 如果没有转录文本，返回错误提示
            if (audioBase64 == null || audioBase64.trim().isEmpty()) {
                return UserController.ApiResponse.error("音频数据不能为空");
            }
            
            // 由于千问 API 不直接支持音频 Base64，需要前端先转录或后端调用 ASR
            // 这里返回提示，让前端提供转录文本
            result.put("message", "请提供语音转录文本（transcript 字段），或使用 FormData 方式上传音频文件");
            return UserController.ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("[音频分析 Base64] 分析失败", e);
            return UserController.ApiResponse.error("音频分析失败：" + e.getMessage());
        }
    }

    /**
     * 验证音频文件
     */
    private boolean isValidAudioFile(String fileName, String contentType) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".wav") || lower.endsWith(".mp3") || 
               lower.endsWith(".ogg") || lower.endsWith(".m4a");
    }
}