package com.ruijie.interview.repository;

import com.ruijie.interview.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}