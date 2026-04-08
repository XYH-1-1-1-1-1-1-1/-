package com.ruijie.interview.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位实体类 - 定义不同的技术岗位类型
 */
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(columnDefinition = "TEXT")
    private String techStack;

    @Column(length = 2000)
    private String interviewFocus;

    @Column
    private Integer difficultyLevel;

    @Column
    private Integer durationMinutes;

    @Column
    private Integer questionCount;

    @OneToMany(mappedBy = "positionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 构造函数
    public Position() {
    }

    public Position(String code, String name, String description, String requiredSkills, 
                    String techStack, String interviewFocus, Integer difficultyLevel,
                    Integer durationMinutes, Integer questionCount) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.techStack = techStack;
        this.interviewFocus = interviewFocus;
        this.difficultyLevel = difficultyLevel;
        this.durationMinutes = durationMinutes;
        this.questionCount = questionCount;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getInterviewFocus() {
        return interviewFocus;
    }

    public void setInterviewFocus(String interviewFocus) {
        this.interviewFocus = interviewFocus;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
