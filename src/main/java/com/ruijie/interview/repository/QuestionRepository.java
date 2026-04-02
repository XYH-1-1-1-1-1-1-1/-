package com.ruijie.interview.repository;

import com.ruijie.interview.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPositionIdOrderByOrderNum(String positionId);
    List<Question> findByPositionIdAndCategoryOrderByOrderNum(String positionId, String category);
    
    @Query("SELECT q FROM Question q WHERE q.positionId = ?1 ORDER BY RANDOM() LIMIT ?2")
    List<Question> findRandomByPositionId(String positionId, int limit);
}