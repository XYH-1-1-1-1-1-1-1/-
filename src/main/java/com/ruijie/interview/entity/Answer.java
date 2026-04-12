package com.ruijie.interview.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 回答实体类 - 记录用户的面试回答
 */
@Entity
@Table(name = "answers")
public class Answer {

    // 构造函数
    public Answer() {
    }

    public Answer(Long sessionId, Long questionId, String userAnswer, String answerType,
                  Integer durationSeconds, Integer wordCount, String speechFeatures) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.answerType = answerType;
        this.durationSeconds = durationSeconds;
        this.wordCount = wordCount;
        this.speechFeatures = speechFeatures;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long questionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    @Column
    private String answerType;

    @Column
    private Integer durationSeconds;

    @Column
    private Integer wordCount;

    @Column
    private String speechFeatures;

    // 评分相关字段
    @Column
    private Integer technicalScore;

    @Column
    private Integer communicationScore;

    @Column
    private Integer logicScore;

    @Column
    private Integer knowledgeDepth;

    @Column
    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String evaluationComment;

    @Column
    private Boolean isSkipped;

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

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getAnswerType() {
        return answerType;
    }

    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getSpeechFeatures() {
        return speechFeatures;
    }

    public void setSpeechFeatures(String speechFeatures) {
        this.speechFeatures = speechFeatures;
    }

    // 评分相关 Getter 和 Setter
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

    public Boolean getIsSkipped() {
        return isSkipped;
    }

    public void setIsSkipped(Boolean isSkipped) {
        this.isSkipped = isSkipped;
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
