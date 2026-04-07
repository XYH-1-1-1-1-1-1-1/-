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
                            Integer matchingScore, Integer overallScore,
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
    @GeneratedValue(strategy = GenerationType.AUTO)
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

    // 岗位匹配度评分 (0-100)
    @Column
    private Integer matchingScore;

    // 综合评分 (0-100)
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
}
