package com.ruijie.interview.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评估报告实体类 - 记录面试评估结果
 */
@Entity
@Table(name = "evaluation_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {

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
}