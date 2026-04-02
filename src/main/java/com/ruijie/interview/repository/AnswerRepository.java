package com.ruijie.interview.repository;

import com.ruijie.interview.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}