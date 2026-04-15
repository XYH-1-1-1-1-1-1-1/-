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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 语音表达评估服务
 * 综合声学特征和文本分析，评估语速、清晰度、自信度等表达表现
 */
@Service
public class SpeechEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SpeechEvaluationService.class);

    @Autowired
    private LlmConfig llmConfig;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * 语音表达评估请求
     */
    public static class SpeechEvaluationRequest {
        private String transcript;      // 语音转录文本
        private String question;        // 面试问题
        private AudioFeatures audioFeatures;  // 声学特征
        
        // Getters and Setters
        public String getTranscript() { return transcript; }
        public void setTranscript(String transcript) { this.transcript = transcript; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public AudioFeatures getAudioFeatures() { return audioFeatures; }
        public void setAudioFeatures(AudioFeatures audioFeatures) { this.audioFeatures = audioFeatures; }
    }

    /**
     * 声学特征数据
     */
    public static class AudioFeatures {
        private Double speechRate;      // 语速（字/分钟）
        private Integer pauseCount;     // 停顿次数
        private Double averageVolume;   // 平均音量 (0-100)
        private Double volumeVariance;  // 音量方差（反映语调变化）
        private Double duration;        // 录音时长（秒）
        private Integer fillerCount;    // 填充词次数（嗯、啊等）
        
        // Getters and Setters
        public Double getSpeechRate() { return speechRate; }
        public void setSpeechRate(Double speechRate) { this.speechRate = speechRate; }
        public Integer getPauseCount() { return pauseCount; }
        public void setPauseCount(Integer pauseCount) { this.pauseCount = pauseCount; }
        public Double getAverageVolume() { return averageVolume; }
        public void setAverageVolume(Double averageVolume) { this.averageVolume = averageVolume; }
        public Double getVolumeVariance() { return volumeVariance; }
        public void setVolumeVariance(Double volumeVariance) { this.volumeVariance = volumeVariance; }
        public Double getDuration() { return duration; }
        public void setDuration(Double duration) { this.duration = duration; }
        public Integer getFillerCount() { return fillerCount; }
        public void setFillerCount(Integer fillerCount) { this.fillerCount = fillerCount; }
    }

    /**
     * 语音表达评估结果
     */
    public static class SpeechEvaluationResult {
        // 表达表现评分
        private Integer speechRateScore;      // 语速评分 (0-100)
        private Integer clarityScore;         // 清晰度评分 (0-100)
        private Integer confidenceScore;      // 自信度评分 (0-100)
        private Integer fluencyScore;         // 流畅度评分 (0-100)
        private Integer expressionScore;      // 表达力评分 (0-100)
        private Integer overallScore;         // 综合评分 (0-100)
        
        // 情绪状态
        private String emotion;               // 情绪状态
        private String emotionAnalysis;       // 情绪分析详情
        
        // 详细分析
        private String speechRateAnalysis;    // 语速分析
        private String clarityAnalysis;       // 清晰度分析
        private String confidenceAnalysis;    // 自信度分析
        private String fluencyAnalysis;       // 流畅度分析
        private String expressionAnalysis;    // 表达力分析
        private String overallAnalysis;       // 综合分析
        private String suggestions;           // 改进建议
        
        // 原始数据
        private String transcript;
        private AudioFeatures audioFeatures;

        // Getters and Setters
        public Integer getSpeechRateScore() { return speechRateScore; }
        public void setSpeechRateScore(Integer speechRateScore) { this.speechRateScore = speechRateScore; }
        public Integer getClarityScore() { return clarityScore; }
        public void setClarityScore(Integer clarityScore) { this.clarityScore = clarityScore; }
        public Integer getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Integer confidenceScore) { this.confidenceScore = confidenceScore; }
        public Integer getFluencyScore() { return fluencyScore; }
        public void setFluencyScore(Integer fluencyScore) { this.fluencyScore = fluencyScore; }
        public Integer getExpressionScore() { return expressionScore; }
        public void setExpressionScore(Integer expressionScore) { this.expressionScore = expressionScore; }
        public Integer getOverallScore() { return overallScore; }
        public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
        public String getEmotion() { return emotion; }
        public void setEmotion(String emotion) { this.emotion = emotion; }
        public String getEmotionAnalysis() { return emotionAnalysis; }
        public void setEmotionAnalysis(String emotionAnalysis) { this.emotionAnalysis = emotionAnalysis; }
        public String getSpeechRateAnalysis() { return speechRateAnalysis; }
        public void setSpeechRateAnalysis(String speechRateAnalysis) { this.speechRateAnalysis = speechRateAnalysis; }
        public String getClarityAnalysis() { return clarityAnalysis; }
        public void setClarityAnalysis(String clarityAnalysis) { this.clarityAnalysis = clarityAnalysis; }
        public String getConfidenceAnalysis() { return confidenceAnalysis; }
        public void setConfidenceAnalysis(String confidenceAnalysis) { this.confidenceAnalysis = confidenceAnalysis; }
        public String getFluencyAnalysis() { return fluencyAnalysis; }
        public void setFluencyAnalysis(String fluencyAnalysis) { this.fluencyAnalysis = fluencyAnalysis; }
        public String getExpressionAnalysis() { return expressionAnalysis; }
        public void setExpressionAnalysis(String expressionAnalysis) { this.expressionAnalysis = expressionAnalysis; }
        public String getOverallAnalysis() { return overallAnalysis; }
        public void setOverallAnalysis(String overallAnalysis) { this.overallAnalysis = overallAnalysis; }
        public String getSuggestions() { return suggestions; }
        public void setSuggestions(String suggestions) { this.suggestions = suggestions; }
        public String getTranscript() { return transcript; }
        public void setTranscript(String transcript) { this.transcript = transcript; }
        public AudioFeatures getAudioFeatures() { return audioFeatures; }
        public void setAudioFeatures(AudioFeatures audioFeatures) { this.audioFeatures = audioFeatures; }
    }

    /**
     * 综合语音表达评估
     * 结合声学特征和文本分析，评估语速、清晰度、自信度等表达表现
     */
    public SpeechEvaluationResult evaluateSpeechExpression(SpeechEvaluationRequest request) {
        log.info("[语音表达评估] 开始评估 - 问题：{}, 文本长度：{}", 
            request.getQuestion(), request.getTranscript() != null ? request.getTranscript().length() : 0);
        
        SpeechEvaluationResult result = new SpeechEvaluationResult();
        result.setTranscript(request.getTranscript());
        result.setAudioFeatures(request.getAudioFeatures());
        
        try {
            // 1. 基于声学特征计算基础评分
            calculateAcousticScores(request, result);
            
            // 2. 使用千问大模型进行文本分析
            analyzeWithLLM(request, result);
            
            // 3. 综合声学特征和文本分析结果
            combineScores(result);
            
            log.info("[语音表达评估] 评估完成 - 综合评分：{}, 情绪：{}", 
                result.getOverallScore(), result.getEmotion());
            
        } catch (Exception e) {
            log.error("[语音表达评估] 评估失败，使用默认值", e);
            setDefaultResult(result);
        }
        
        return result;
    }

    /**
     * 基于声学特征计算基础评分
     */
    private void calculateAcousticScores(SpeechEvaluationRequest request, SpeechEvaluationResult result) {
        AudioFeatures features = request.getAudioFeatures();
        
        if (features == null) {
            // 没有声学特征，使用文本分析
            return;
        }
        
        // 语速评分（正常语速 120-180 字/分钟）
        Double speechRate = features.getSpeechRate();
        if (speechRate != null) {
            int rateScore;
            if (speechRate >= 120 && speechRate <= 180) {
                rateScore = 90 + (int)((180 - speechRate) / 60 * 10);  // 120-180 之间得 90-100 分
            } else if (speechRate < 80) {
                rateScore = 50 + (int)((speechRate / 80) * 20);  // 太慢，50-70 分
            } else if (speechRate > 220) {
                rateScore = 50 + (int)((260 - speechRate) / 40 * 20);  // 太快，50-70 分
            } else {
                rateScore = 70 + (int)((speechRate < 120 ? speechRate - 80 : 220 - speechRate) / 40 * 20);
            }
            result.setSpeechRateScore(Math.max(0, Math.min(100, rateScore)));
            
            // 语速分析
            if (speechRate < 100) {
                result.setSpeechRateAnalysis(String.format("语速较慢（%.1f 字/分钟），可能表示紧张或思考时间较长", speechRate));
            } else if (speechRate > 200) {
                result.setSpeechRateAnalysis(String.format("语速较快（%.1f 字/分钟），可能表示紧张或急于表达", speechRate));
            } else {
                result.setSpeechRateAnalysis(String.format("语速适中（%.1f 字/分钟），表达节奏良好", speechRate));
            }
        }
        
        // 流畅度评分（基于停顿次数和填充词）
        Integer pauseCount = features.getPauseCount();
        Integer fillerCount = features.getFillerCount();
        if (pauseCount != null || fillerCount != null) {
            int fluencyScore = 100;
            if (pauseCount != null) {
                fluencyScore -= Math.min(30, pauseCount * 3);  // 每次停顿扣 3 分，最多扣 30 分
            }
            if (fillerCount != null) {
                fluencyScore -= Math.min(30, fillerCount * 5);  // 每个填充词扣 5 分，最多扣 30 分
            }
            result.setFluencyScore(Math.max(0, Math.min(100, fluencyScore)));
            
            // 流畅度分析
            StringBuilder fluencyAnalysis = new StringBuilder();
            if (pauseCount != null && pauseCount > 0) {
                fluencyAnalysis.append(String.format("停顿%d次，", pauseCount));
            }
            if (fillerCount != null && fillerCount > 0) {
                fluencyAnalysis.append(String.format("填充词（嗯、啊等）%d次，", fillerCount));
            }
            if (fluencyAnalysis.length() > 0) {
                fluencyAnalysis.deleteCharAt(fluencyAnalysis.length() - 1);
                fluencyAnalysis.append("建议减少停顿和填充词，提高表达流畅度");
            } else {
                fluencyAnalysis.append("表达流畅，无明显停顿或填充词");
            }
            result.setFluencyAnalysis(fluencyAnalysis.toString());
        }
        
        // 自信度评分（基于音量变化和语速）
        Double volumeVariance = features.getVolumeVariance();
        if (volumeVariance != null && speechRate != null) {
            int confidenceScore = 70;  // 基础分
            // 音量变化适中表示有语调起伏，过于平稳可能表示紧张
            if (volumeVariance >= 10 && volumeVariance <= 30) {
                confidenceScore += 20;  // 语调变化良好
            } else if (volumeVariance < 5) {
                confidenceScore -= 15;  // 语调过于平稳，可能紧张
            }
            // 语速适中增加自信分
            if (speechRate >= 120 && speechRate <= 180) {
                confidenceScore += 10;
            }
            result.setConfidenceScore(Math.max(0, Math.min(100, confidenceScore)));
        }
        
        // 表达力评分（基于音量变化）
        if (volumeVariance != null) {
            int expressionScore;
            if (volumeVariance >= 15 && volumeVariance <= 35) {
                expressionScore = 90;  // 语调变化丰富
            } else if (volumeVariance < 5) {
                expressionScore = 50;  // 语调平淡
            } else {
                expressionScore = 70;  // 一般
            }
            result.setExpressionScore(expressionScore);
        }
    }

    /**
     * 使用千问大模型进行文本分析
     */
    private void analyzeWithLLM(SpeechEvaluationRequest request, SpeechEvaluationResult result) {
        String url = llmConfig.getBaseUrl() + "/chat/completions";
        String model = "qwen-max";

        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            JSONArray messagesArray = new JSONArray();
            
            // 系统提示
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", 
                "你是一位专业的语音表达评估专家，擅长通过分析演讲文本来评估表达质量。" +
                "请从以下维度进行评估（0-100 分）：\n" +
                "1. 清晰度 (clarity) - 表达是否清晰易懂\n" +
                "2. 自信度 (confidence) - 表达是否自信\n" +
                "3. 流畅度 (fluency) - 表达是否流畅\n" +
                "4. 表达力 (expression) - 表达是否有感染力\n" +
                "同时分析情绪状态：紧张、自信、从容、焦虑、兴奋等\n" +
                "请以 JSON 格式返回评估结果。");
            messagesArray.add(systemMsg);

            // 用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            
            StringBuilder content = new StringBuilder();
            content.append("面试问题：").append(request.getQuestion()).append("\n\n");
            content.append("候选人回答：").append(request.getTranscript()).append("\n\n");
            
            // 如果有声学特征，也提供给 LLM
            if (request.getAudioFeatures() != null) {
                AudioFeatures features = request.getAudioFeatures();
                content.append("声学特征：\n");
                if (features.getSpeechRate() != null) {
                    content.append("- 语速：").append(String.format("%.1f", features.getSpeechRate())).append(" 字/分钟\n");
                }
                if (features.getPauseCount() != null) {
                    content.append("- 停顿次数：").append(features.getPauseCount()).append(" 次\n");
                }
                if (features.getFillerCount() != null) {
                    content.append("- 填充词次数：").append(features.getFillerCount()).append(" 次\n");
                }
            }
            
            content.append("\n请分析候选人的表达表现，返回 JSON 格式：\n");
            content.append("{\n");
            content.append("  \"clarityScore\": 清晰度评分，\n");
            content.append("  \"confidenceScore\": 自信度评分，\n");
            content.append("  \"fluencyScore\": 流畅度评分，\n");
            content.append("  \"expressionScore\": 表达力评分，\n");
            content.append("  \"overallScore\": 综合评分，\n");
            content.append("  \"emotion\": \"情绪状态\",\n");
            content.append("  \"emotionAnalysis\": \"情绪分析\",\n");
            content.append("  \"clarityAnalysis\": \"清晰度分析\",\n");
            content.append("  \"confidenceAnalysis\": \"自信度分析\",\n");
            content.append("  \"suggestions\": \"改进建议\"\n");
            content.append("}");
            
            userMsg.put("content", content.toString());
            messagesArray.add(userMsg);

            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.7);

            StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            String response = httpClient.execute(post, response1 -> {
                int statusCode = response1.getCode();
                String responseBody = EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode != 200) {
                    log.error("[LLM 分析] API 错误：{} - {}", statusCode, responseBody);
                    throw new RuntimeException("LLM API 错误：" + statusCode);
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

            // 解析 LLM 分析结果
            parseLLMResult(response, result);

        } catch (IOException e) {
            log.error("[LLM 分析] 调用失败", e);
            // LLM 失败时使用默认值
            if (result.getEmotion() == null) {
                result.setEmotion("NORMAL");
                result.setEmotionAnalysis("情绪分析暂时不可用");
            }
            if (result.getSuggestions() == null) {
                result.setSuggestions("建议继续练习，提高表达能力");
            }
        }
    }

    /**
     * 解析 LLM 分析结果
     */
    private void parseLLMResult(String response, SpeechEvaluationResult result) {
        try {
            // 提取 JSON 部分
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start >= 0 && end > start) {
                response = response.substring(start, end + 1);
            }
            
            JSONObject json = JSON.parseObject(response);
            
            // 从 LLM 结果获取评分（如果声学特征没有计算的话）
            if (result.getClarityScore() == null) {
                result.setClarityScore(json.getInteger("clarityScore"));
            }
            if (result.getConfidenceScore() == null) {
                result.setConfidenceScore(json.getInteger("confidenceScore"));
            }
            if (result.getFluencyScore() == null) {
                result.setFluencyScore(json.getInteger("fluencyScore"));
            }
            if (result.getExpressionScore() == null) {
                result.setExpressionScore(json.getInteger("expressionScore"));
            }
            
            // 获取情绪分析
            String emotion = json.getString("emotion");
            if (emotion != null && !emotion.isEmpty()) {
                result.setEmotion(emotion);
            }
            String emotionAnalysis = json.getString("emotionAnalysis");
            if (emotionAnalysis != null && !emotionAnalysis.isEmpty()) {
                result.setEmotionAnalysis(emotionAnalysis);
            }
            
            // 获取详细分析
            String clarityAnalysis = json.getString("clarityAnalysis");
            if (clarityAnalysis != null && !clarityAnalysis.isEmpty()) {
                result.setClarityAnalysis(clarityAnalysis);
            }
            String confidenceAnalysis = json.getString("confidenceAnalysis");
            if (confidenceAnalysis != null && !confidenceAnalysis.isEmpty()) {
                result.setConfidenceAnalysis(confidenceAnalysis);
            }
            
            // 获取改进建议
            String suggestions = json.getString("suggestions");
            if (suggestions != null && !suggestions.isEmpty()) {
                result.setSuggestions(suggestions);
            }
            
        } catch (Exception e) {
            log.error("[LLM 结果解析] 解析失败", e);
        }
    }

    /**
     * 综合声学特征和文本分析结果
     */
    private void combineScores(SpeechEvaluationResult result) {
        // 计算综合评分
        int totalScore = 0;
        int count = 0;
        
        if (result.getSpeechRateScore() != null) {
            totalScore += result.getSpeechRateScore();
            count++;
        }
        if (result.getClarityScore() != null) {
            totalScore += result.getClarityScore();
            count++;
        }
        if (result.getConfidenceScore() != null) {
            totalScore += result.getConfidenceScore();
            count++;
        }
        if (result.getFluencyScore() != null) {
            totalScore += result.getFluencyScore();
            count++;
        }
        if (result.getExpressionScore() != null) {
            totalScore += result.getExpressionScore();
            count++;
        }
        
        if (count > 0) {
            result.setOverallScore(totalScore / count);
        } else {
            result.setOverallScore(70);  // 默认分
        }
        
        // 生成综合分析
        StringBuilder overallAnalysis = new StringBuilder();
        overallAnalysis.append("综合评估：");
        
        if (result.getOverallScore() >= 80) {
            overallAnalysis.append("表达表现优秀，");
        } else if (result.getOverallScore() >= 60) {
            overallAnalysis.append("表达表现良好，");
        } else {
            overallAnalysis.append("表达表现有待提高，");
        }
        
        if (result.getSpeechRateScore() != null && result.getSpeechRateScore() >= 80) {
            overallAnalysis.append("语速控制得当，");
        }
        if (result.getFluencyScore() != null && result.getFluencyScore() >= 80) {
            overallAnalysis.append("表达流畅，");
        }
        if (result.getConfidenceScore() != null && result.getConfidenceScore() >= 80) {
            overallAnalysis.append("充满自信，");
        }
        
        if (overallAnalysis.length() > 5) {
            overallAnalysis.deleteCharAt(overallAnalysis.length() - 1);
        }
        
        result.setOverallAnalysis(overallAnalysis.toString());
    }

    /**
     * 设置默认结果
     */
    private void setDefaultResult(SpeechEvaluationResult result) {
        result.setSpeechRateScore(70);
        result.setClarityScore(70);
        result.setConfidenceScore(70);
        result.setFluencyScore(70);
        result.setExpressionScore(70);
        result.setOverallScore(70);
        result.setEmotion("NORMAL");
        result.setEmotionAnalysis("情绪状态分析暂时不可用");
        result.setSpeechRateAnalysis("语速分析不可用");
        result.setClarityAnalysis("清晰度分析不可用");
        result.setConfidenceAnalysis("自信度分析不可用");
        result.setFluencyAnalysis("流畅度分析不可用");
        result.setExpressionAnalysis("表达力分析不可用");
        result.setOverallAnalysis("综合分析不可用");
        result.setSuggestions("建议继续练习，提高表达能力");
    }
}