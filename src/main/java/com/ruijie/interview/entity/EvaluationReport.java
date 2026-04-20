package com.ruijie.interview.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 评估报告实体类 - 记录面试评估结果
 */
@Entity
@Table(name = "evaluation_reports")
public class EvaluationReport {

    // 构造函数
    public EvaluationReport() {
    }

    public EvaluationReport(Long sessionId, Long userId, String positionId, 
                            Integer technicalScore, Integer communicationScore,
                            Integer logicScore, Integer adaptabilityScore,
                            Integer matchingScore, Integer comprehensiveScore,
                            Integer overallScore,
                            String speechRate, String clarity, String confidence,
                            String strengths, String weaknesses, 
                            String suggestions, String recommendedResources) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.positionId = positionId;
        this.technicalScore = technicalScore;
        this.communicationScore = communicationScore;
        this.logicScore = logicScore;
        this.adaptabilityScore = adaptabilityScore;
        this.matchingScore = matchingScore;
        this.comprehensiveScore = comprehensiveScore;
        this.overallScore = overallScore;
        this.speechRate = speechRate;
        this.clarity = clarity;
        this.confidence = confidence;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.suggestions = suggestions;
        this.recommendedResources = recommendedResources;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String positionId;

    // 技术能力评分 (0-100)
    @Column
    private Integer technicalScore;

    // 表达能力评分 (0-100)
    @Column
    private Integer communicationScore;

    // 逻辑思维评分 (0-100)
    @Column
    private Integer logicScore;

    // 应变能力评分 (0-100)
    @Column
    private Integer adaptabilityScore;

    // 岗位匹配度评分 (0-100) - 由 LLM 评估生成
    @Column
    private Integer matchingScore;

    // 综合评分 (0-100) - 由五个维度的加权平均计算得出
    @Column
    private Integer comprehensiveScore;

    // 总体评分 (0-100) - 保留字段，用于兼容
    @Column
    private Integer overallScore;

    // 语速评估
    @Column(length = 20)
    private String speechRate;

    // 清晰度评估
    @Column(length = 20)
    private String clarity;

    // 自信度评估
    @Column(length = 20)
    private String confidence;

    // 情绪状态（用于真实面试模式）
    @Column(length = 50)
    private String emotion;

    // 情绪分析详细说明
    @Column(columnDefinition = "TEXT")
    private String emotionAnalysis;

    // 自信程度评分（1-5 分）
    @Column
    private Integer confidenceLevel;

    // 语音转录文本（用于真实面试模式）
    @Column(columnDefinition = "TEXT")
    private String transcript;

    // 语音表达评分（用于真实面试模式）
    @Column
    private Integer speechRateScore;        // 语速评分 (0-100)

    @Column
    private Integer clarityScore;           // 清晰度评分 (0-100)

    @Column
    private Integer confidenceScore;        // 自信度评分 (0-100)

    @Column
    private Integer fluencyScore;           // 流畅度评分 (0-100)

    @Column
    private Integer expressionScore;        // 表达力评分 (0-100)

    // 语音表达分析详情
    @Column(columnDefinition = "TEXT")
    private String speechRateAnalysis;      // 语速分析

    @Column(columnDefinition = "TEXT")
    private String clarityAnalysis;         // 清晰度分析

    @Column(columnDefinition = "TEXT")
    private String confidenceAnalysis;      // 自信度分析

    @Column(columnDefinition = "TEXT")
    private String fluencyAnalysis;         // 流畅度分析

    @Column(columnDefinition = "TEXT")
    private String expressionAnalysis;      // 表达力分析

    @Column(columnDefinition = "TEXT")
    private String overallAnalysis;         // 综合分析

    // 亮点分析
    @Column(columnDefinition = "TEXT")
    private String strengths;

    // 不足分析
    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    // 改进建议
    @Column(columnDefinition = "TEXT")
    private String suggestions;

    // 推荐学习资源
    @Column(columnDefinition = "TEXT")
    private String recommendedResources;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
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

    public Integer getAdaptabilityScore() {
        return adaptabilityScore;
    }

    public void setAdaptabilityScore(Integer adaptabilityScore) {
        this.adaptabilityScore = adaptabilityScore;
    }

    public Integer getMatchingScore() {
        return matchingScore;
    }

    public void setMatchingScore(Integer matchingScore) {
        this.matchingScore = matchingScore;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(String speechRate) {
        this.speechRate = speechRate;
    }

    public String getClarity() {
        return clarity;
    }

    public void setClarity(String clarity) {
        this.clarity = clarity;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
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

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Integer getSpeechRateScore() {
        return speechRateScore;
    }

    public void setSpeechRateScore(Integer speechRateScore) {
        this.speechRateScore = speechRateScore;
    }

    public Integer getClarityScore() {
        return clarityScore;
    }

    public void setClarityScore(Integer clarityScore) {
        this.clarityScore = clarityScore;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getFluencyScore() {
        return fluencyScore;
    }

    public void setFluencyScore(Integer fluencyScore) {
        this.fluencyScore = fluencyScore;
    }

    public Integer getExpressionScore() {
        return expressionScore;
    }

    public void setExpressionScore(Integer expressionScore) {
        this.expressionScore = expressionScore;
    }

    public String getSpeechRateAnalysis() {
        return speechRateAnalysis;
    }

    public void setSpeechRateAnalysis(String speechRateAnalysis) {
        this.speechRateAnalysis = speechRateAnalysis;
    }

    public String getClarityAnalysis() {
        return clarityAnalysis;
    }

    public void setClarityAnalysis(String clarityAnalysis) {
        this.clarityAnalysis = clarityAnalysis;
    }

    public String getConfidenceAnalysis() {
        return confidenceAnalysis;
    }

    public void setConfidenceAnalysis(String confidenceAnalysis) {
        this.confidenceAnalysis = confidenceAnalysis;
    }

    public String getFluencyAnalysis() {
        return fluencyAnalysis;
    }

    public void setFluencyAnalysis(String fluencyAnalysis) {
        this.fluencyAnalysis = fluencyAnalysis;
    }

    public String getExpressionAnalysis() {
        return expressionAnalysis;
    }

    public void setExpressionAnalysis(String expressionAnalysis) {
        this.expressionAnalysis = expressionAnalysis;
    }

    public String getOverallAnalysis() {
        return overallAnalysis;
    }

    public void setOverallAnalysis(String overallAnalysis) {
        this.overallAnalysis = overallAnalysis;
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

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public String getRecommendedResources() {
        return recommendedResources;
    }

    public void setRecommendedResources(String recommendedResources) {
        this.recommendedResources = recommendedResources;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getComprehensiveScore() {
        return comprehensiveScore;
    }

    public void setComprehensiveScore(Integer comprehensiveScore) {
        this.comprehensiveScore = comprehensiveScore;
    }
}
