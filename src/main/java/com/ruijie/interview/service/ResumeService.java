package com.ruijie.interview.service;

import com.ruijie.interview.entity.Resume;
import com.ruijie.interview.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历服务类
 */
@Service
public class ResumeService {

    @Autowired
    private ResumeRepository resumeRepository;

    // 简历存储目录
    private static final String RESUME_UPLOAD_DIR = "data/resumes/";

    /**
     * 上传简历
     */
    @Transactional
    public Resume uploadResume(Long userId, MultipartFile file) throws IOException {
        // 验证文件是否为 PDF
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("只支持 PDF 格式的简历文件");
        }

        // 验证文件大小 (最大 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("简历文件大小不能超过 10MB");
        }

        // 删除用户之前的简历
        resumeRepository.deleteByUserId(userId);

        // 创建存储目录
        Path uploadDir = Paths.get(RESUME_UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName != null && originalFileName.contains(".") 
                ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".pdf";
        String uniqueFileName = userId + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
                "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

        Path filePath = uploadDir.resolve(uniqueFileName);

        // 保存文件
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 创建简历记录
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(originalFileName != null ? originalFileName : uniqueFileName);
        resume.setFilePath(filePath.toString());
        resume.setFileSize(file.getSize());
        resume.setContentType(contentType);

        return resumeRepository.save(resume);
    }

    /**
     * 获取用户的简历
     */
    public Optional<Resume> getUserResume(Long userId) {
        return resumeRepository.findByUserId(userId);
    }

    /**
     * 检查用户是否有简历
     */
    public boolean hasResume(Long userId) {
        return resumeRepository.existsByUserId(userId);
    }

    /**
     * 删除简历
     */
    @Transactional
    public void deleteResume(Long userId) {
        Optional<Resume> resumeOpt = resumeRepository.findByUserId(userId);
        if (resumeOpt.isPresent()) {
            Resume resume = resumeOpt.get();
            // 删除文件
            try {
                Path filePath = Paths.get(resume.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                // 文件删除失败不影响数据库操作
            }
            resumeRepository.delete(resume);
        }
    }
}