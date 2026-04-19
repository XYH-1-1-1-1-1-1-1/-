package com.ruijie.interview.repository;

import com.ruijie.interview.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 简历数据访问接口
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    
    /**
     * 根据用户 ID 查找简历
     */
    Optional<Resume> findByUserId(Long userId);
    
    /**
     * 检查用户是否有简历
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 删除用户的简历
     */
    void deleteByUserId(Long userId);
    
    /**
     * 更新简历的解析内容（缓存）
     */
    @Modifying
    @Transactional
    @Query("UPDATE Resume r SET r.parsedContent = :content, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    int updateParsedContent(@Param("id") Long id, @Param("content") String content);
    
    /**
     * 仅获取用户的简历解析内容（缓存）
     */
    @Query("SELECT r.parsedContent FROM Resume r WHERE r.userId = :userId")
    String findParsedContentByUserId(@Param("userId") Long userId);
}
