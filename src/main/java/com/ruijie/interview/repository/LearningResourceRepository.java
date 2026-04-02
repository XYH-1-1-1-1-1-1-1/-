package com.ruijie.interview.repository;

import com.ruijie.interview.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    List<LearningResource> findByPositionIdOrderByCreatedAtDesc(String positionId);
    List<LearningResource> findByPositionIdAndCategoryOrderByCreatedAtDesc(String positionId, String category);
    List<LearningResource> findByTagsContainingOrderByViewCountDesc(String tag);
}