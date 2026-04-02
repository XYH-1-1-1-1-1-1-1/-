package com.ruijie.interview.repository;

import com.ruijie.interview.entity.EvaluationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationReportRepository extends JpaRepository<EvaluationReport, Long> {
    Optional<EvaluationReport> findBySessionId(Long sessionId);
    List<EvaluationReport> findByUserIdOrderByCreatedAtDesc(Long userId);
}