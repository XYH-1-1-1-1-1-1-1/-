package com.ruijie.interview.repository;

import com.ruijie.interview.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<InterviewSession> findByUserIdAndPositionIdOrderByCreatedAtDesc(Long userId, String positionId);
}
