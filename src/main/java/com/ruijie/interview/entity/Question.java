package com.ruijie.interview.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 面试题目实体类
 */
@Entity
@Table(name = "questions")
public class Question {

    // 构造函数
    public Question() {
    }

    public Question(String positionId, String category, String content, String referenceAnswer,
                    String answerPoints, Integer difficultyLevel, Integer scoreWeight,
                    String tags, Integer orderNum) {
        this.positionId = positionId;
        this.category = category;
        this.content = content;
        this.referenceAnswer = referenceAnswer;
        this.answerPoints = answerPoints;
        this.difficultyLevel = difficultyLevel;
        this.scoreWeight = scoreWeight;
        this.tags = tags;
        this.orderNum = orderNum;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String positionId;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 2000)
    private String referenceAnswer;

    @Column(length = 1000)
    private String answerPoints;

    @Column
    private Integer difficultyLevel;

    @Column
    private Integer scoreWeight;

    @Column(length = 200)
    private String tags;

    @Column
    private Integer orderNum;

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

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public String getAnswerPoints() {
        return answerPoints;
    }

    public void setAnswerPoints(String answerPoints) {
        this.answerPoints = answerPoints;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Integer getScoreWeight() {
        return scoreWeight;
    }

    public void setScoreWeight(Integer scoreWeight) {
        this.scoreWeight = scoreWeight;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
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
