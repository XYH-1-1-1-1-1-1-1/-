package com.ruijie.interview.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 音频分析服务 - 调用阿里云 DashScope 千问多模态大模型
 * 支持语音识别和情绪分析（紧张、自信等）
 */
@Service
public class AudioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AudioAnalysisService.class);

    @Autowired
    private LlmConfig llmConfig;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * 分析音频文件 - 语音识别和情绪分析
     * 使用阿里云 DashScope 的音频理解 API
     *
     * @param audioFile 音频文件
     * @param prompt 分析提示词
     * @return 分析结果 JSON 字符串
     */
    public AudioAnalysisResult analyzeAudio(MultipartFile audioFile, String prompt) {
        try {
            // 方案 1：前端已将音频转换为 Base64，直接调用千问 API
            // 方案 2：使用阿里云智能语音交互服务（ASR）先转文字，再调用千问分析情绪
            
            // 这里使用方案 1 的简化版本 - 假设前端已提供音频的 Base64 或文本
            // 实际生产环境需要集成阿里云 ASR 服务
            
            log.info("[音频分析] 开始分析音频文件：{}", audioFile.getOriginalFilename());
            
            // 由于千问 API 不直接支持音频输入，需要先将音频转为文本
            // 这里调用语音识别服务
            String transcript = transcribeAudio(audioFile);
            
            log.info("[音频分析] 语音识别结果：{}", transcript);
            
            // 使用千问大模型分析情绪和评分
            AudioAnalysisResult result = analyzeEmotionAndScore(transcript, prompt);
            result.setTranscript(transcript);
            
            return result;
            
        } catch (Exception e) {
            log.error("[音频分析] 分析失败", e);
            throw new RuntimeException("音频分析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 语音识别 - 将音频转换为文本
     * 使用阿里云智能语音交互服务（ASR）
     */
    public String transcribeAudio(MultipartFile audioFile) {
        try {
            // 阿里云 DashScope 的语音识别 API
            String url = "https://dashscope.aliyuncs.com/api/v1/services/asr/speech-to-text";
            
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());
            post.setHeader("Content-Type", "application/json");
            
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            
            // 将音频文件转换为 Base64
            byte[] audioBytes = audioFile.getBytes();
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            
            // 任务配置
            JSONObject taskConfig = new JSONObject();
            taskConfig.put("format", getAudioFormat(audioFile.getOriginalFilename()));
            taskConfig.put("sample_rate", 16000);
            taskConfig.put("enable_intermediate_result", false);
            taskConfig.put("enable_punctuation_prediction", true);
            taskConfig.put("enable_inverse_text_normalization", true);
            
            // 输入
            JSONObject input = new JSONObject();
            input.put("speech", base64Audio);
            
            requestBody.put("model", "paraformer-realtime-v2");
            requestBody.put("input", input);
            requestBody.put("task_config", taskConfig);
            
            StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            
            String response = httpClient.execute(post, response1 -> {
                int statusCode = response1.getCode();
                String responseBody = EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8);
                log.debug("[语音识别] API 响应：{}", responseBody);
                
                if (statusCode != 200) {
                    log.error("[语音识别] API 错误：{} - {}", statusCode, responseBody);
                    throw new RuntimeException("语音识别 API 错误：" + statusCode);
                }
                
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                
                // 解析 DashScope ASR 响应
                if (jsonResponse.containsKey("output") && 
                    jsonResponse.getJSONObject("output").containsKey("results")) {
                    JSONArray results = jsonResponse.getJSONObject("output").getJSONArray("results");
                    StringBuilder transcript = new StringBuilder();
                    for (int i = 0; i < results.size(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        if (result.containsKey("text")) {
                            transcript.append(result.getString("text"));
                        }
                    }
                    return transcript.toString();
                }
                
                return "";
            });
            
            return response;
            
        } catch (IOException e) {
            log.error("[语音识别] 调用失败", e);
            // 如果 ASR 调用失败，返回空字符串，由前端提供转录文本作为备选
            return "";
        }
    }

    /**
     * 使用千问大模型分析情绪和评分
     */
    public AudioAnalysisResult analyzeEmotionAndScore(String transcript, String interviewContext) {
        String url = llmConfig.getBaseUrl() + "/chat/completions";
        String model = "qwen-max";

        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());
            post.setHeader("HTTP-Referer", "http://localhost:8080");
            post.setHeader("X-Title", "AI Interview - Audio Analysis");

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            // 构建消息
            JSONArray messagesArray = new JSONArray();
            
            // 系统提示
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一位专业的面试评估专家，擅长通过候选人的语音回答分析其情绪状态和表现。" +
                "请从以下维度进行评估：\n" +
                "1. 情绪状态：紧张、自信、从容、焦虑、兴奋等\n" +
                "2. 技术能力评分（0-100）\n" +
                "3. 沟通能力评分（0-100）\n" +
                "4. 逻辑思维评分（0-100）\n" +
                "5. 知识深度评分（0-100）\n" +
                "6. 综合评分（0-100）\n" +
                "请以 JSON 格式返回评估结果。");
            messagesArray.add(systemMsg);

            // 用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            String userContent = "面试问题：" + interviewContext + "\n\n" +
                "候选人语音回答转录文本：" + transcript + "\n\n" +
                "请分析候选人的情绪状态并进行评分，返回 JSON 格式：\n" +
                "{\n" +
                "  \"emotion\": \"情绪状态（紧张/自信/从容/焦虑/兴奋等）\",\n" +
                "  \"emotionAnalysis\": \"情绪分析详细说明\",\n" +
                "  \"confidenceLevel\": \"自信程度（1-5 分）\",\n" +
                "  \"technicalScore\": 技术评分，\n" +
                "  \"communicationScore\": 沟通评分，\n" +
                "  \"logicScore\": 逻辑评分，\n" +
                "  \"knowledgeDepth\": 知识深度评分，\n" +
                "  \"overallScore\": 综合评分，\n" +
                "  \"evaluationComment\": \"评价意见\",\n" +
                "  \"strengths\": \"亮点\",\n" +
                "  \"weaknesses\": \"待改进之处\"\n" +
                "}";
            userMsg.put("content", userContent);
            messagesArray.add(userMsg);

            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 1500);
            requestBody.put("temperature", 0.7);

            StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            String response = httpClient.execute(post, response1 -> {
                int statusCode = response1.getCode();
                String responseBody = EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8);
                log.debug("[情绪分析] API 响应：{}", responseBody);

                if (statusCode != 200) {
                    log.error("[情绪分析] API 错误：{} - {}", statusCode, responseBody);
                    throw new RuntimeException("情绪分析 API 错误：" + statusCode);
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
                return "";
            });

            // 解析结果
            return parseAnalysisResult(response);

        } catch (IOException e) {
            log.error("[情绪分析] 调用失败", e);
            throw new RuntimeException("情绪分析调用失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析分析结果
     */
    private AudioAnalysisResult parseAnalysisResult(String response) {
        AudioAnalysisResult result = new AudioAnalysisResult();
        
        try {
            // 提取 JSON 部分
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start >= 0 && end > start) {
                response = response.substring(start, end + 1);
            }
            
            JSONObject json = JSON.parseObject(response);
            
            result.setEmotion(json.getString("emotion"));
            result.setEmotionAnalysis(json.getString("emotionAnalysis"));
            result.setConfidenceLevel(json.getInteger("confidenceLevel"));
            result.setTechnicalScore(json.getInteger("technicalScore"));
            result.setCommunicationScore(json.getInteger("communicationScore"));
            result.setLogicScore(json.getInteger("logicScore"));
            result.setKnowledgeDepth(json.getInteger("knowledgeDepth"));
            result.setOverallScore(json.getInteger("overallScore"));
            result.setEvaluationComment(json.getString("evaluationComment"));
            result.setStrengths(json.getString("strengths"));
            result.setWeaknesses(json.getString("weaknesses"));
            
        } catch (Exception e) {
            log.error("[音频分析] 解析结果失败，使用默认值", e);
            // 设置默认值
            result.setEmotion("NORMAL");
            result.setEmotionAnalysis("情绪状态分析暂时不可用");
            result.setConfidenceLevel(3);
            result.setTechnicalScore(50);
            result.setCommunicationScore(50);
            result.setLogicScore(50);
            result.setKnowledgeDepth(50);
            result.setOverallScore(50);
            result.setEvaluationComment("评分系统暂时不可用");
        }
        
        return result;
    }

    /**
     * 获取音频格式
     */
    private String getAudioFormat(String filename) {
        if (filename == null) return "pcm";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".wav")) return "wav";
        if (lower.endsWith(".mp3")) return "mp3";
        if (lower.endsWith(".ogg")) return "ogg";
        if (lower.endsWith(".m4a")) return "m4a";
        return "pcm";
    }

    /**
     * 音频分析结果类
     */
    public static class AudioAnalysisResult {
        private String transcript;
        private String emotion;
        private String emotionAnalysis;
        private Integer confidenceLevel;
        private Integer technicalScore;
        private Integer communicationScore;
        private Integer logicScore;
        private Integer knowledgeDepth;
        private Integer overallScore;
        private String evaluationComment;
        private String strengths;
        private String weaknesses;

        // Getters and Setters
        public String getTranscript() {
            return transcript;
        }

        public void setTranscript(String transcript) {
            this.transcript = transcript;
        }

        public String getEmotion() {
            return emotion;
        }

        public void setEmotion(String emotion) {
            this.emotion = emotion;
        }

        public String getEmotionAnalysis() {
            return emotionAnalysis;
        }

        public void setEmotionAnalysis(String emotionAnalysis) {
            this.emotionAnalysis = emotionAnalysis;
        }

        public Integer getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(Integer confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }

        public Integer getTechnicalScore() {
            return technicalScore;
        }

        public void setTechnicalScore(Integer technicalScore) {
            this.technicalScore = technicalScore;
        }

        public Integer getCommunicationScore() {
            return communicationScore;
        }

        public void setCommunicationScore(Integer communicationScore) {
            this.communicationScore = communicationScore;
        }

        public Integer getLogicScore() {
            return logicScore;
        }

        public void setLogicScore(Integer logicScore) {
            this.logicScore = logicScore;
        }

        public Integer getKnowledgeDepth() {
            return knowledgeDepth;
        }

        public void setKnowledgeDepth(Integer knowledgeDepth) {
            this.knowledgeDepth = knowledgeDepth;
        }

        public Integer getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(Integer overallScore) {
            this.overallScore = overallScore;
        }

        public String getEvaluationComment() {
            return evaluationComment;
        }

        public void setEvaluationComment(String evaluationComment) {
            this.evaluationComment = evaluationComment;
        }

        public String getStrengths() {
            return strengths;
        }

        public void setStrengths(String strengths) {
            this.strengths = strengths;
        }

        public String getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(String weaknesses) {
            this.weaknesses = weaknesses;
        }
    }
}