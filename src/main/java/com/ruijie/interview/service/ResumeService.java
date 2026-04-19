package com.ruijie.interview.service;

import com.ruijie.interview.entity.Resume;
import com.ruijie.interview.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 
 * 缓存机制说明：
 * - 用户上传简历时，立即解析 PDF 内容并存储到数据库的 parsedContent 字段
 * - 后续获取简历内容时，优先从缓存读取，避免重复解析 PDF
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private PdfResumeParser pdfResumeParser;

    // 简历存储目录
    private static final String RESUME_UPLOAD_DIR = "data/resumes/";

    /**
     * 上传简历
     * 上传时立即解析 PDF 内容并缓存到数据库
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

        // 先保存简历记录获取 ID
        Resume savedResume = resumeRepository.save(resume);

        // 立即解析 PDF 内容并缓存
        try {
            log.info("[简历缓存] 开始解析并缓存简历内容 - 用户 ID: {}, 文件：{}", userId, filePath);
            String parsedContent = pdfResumeParser.parsePdfContent(filePath.toString());
            resumeRepository.updateParsedContent(savedResume.getId(), parsedContent);
            savedResume.setParsedContent(parsedContent);
            log.info("[简历缓存] 简历内容缓存成功 - 用户 ID: {}, 字符数：{}", userId, parsedContent.length());
        } catch (Exception e) {
            log.warn("[简历缓存] 简历解析失败，但保留文件记录 - 用户 ID: {}, 错误：{}", userId, e.getMessage());
            // 解析失败不影响简历上传，只是没有缓存内容
        }

        return savedResume;
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

    /**
     * 解析用户上传的简历内容
     * 
     * @param resumeId 简历ID
     * @return 解析后的简历文本内容
     */
    public String parseResumeContent(Long resumeId) {
        Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            throw new IllegalArgumentException("简历不存在: " + resumeId);
        }

        Resume resume = resumeOpt.get();
        String filePath = resume.getFilePath();

        return pdfResumeParser.parsePdfContent(filePath);
    }

    /**
     * 获取并解析用户的简历内容
     * 
     * @param userId 用户 ID
     * @return 解析后的简历文本内容
     */
    public Optional<String> getUserResumeContent(Long userId) {
        // 首先尝试从缓存获取
        String cachedContent = resumeRepository.findParsedContentByUserId(userId);
        if (cachedContent != null && !cachedContent.trim().isEmpty()) {
            log.info("[简历缓存] 从缓存获取简历内容 - 用户 ID: {}, 字符数：{}", userId, cachedContent.length());
            return Optional.of(cachedContent);
        }
        
        // 缓存未命中或为空，从文件解析并更新缓存
        log.info("[简历缓存] 缓存未命中，从文件解析 - 用户 ID: {}", userId);
        Optional<Resume> resumeOpt = getUserResume(userId);
        if (resumeOpt.isPresent()) {
            Resume resume = resumeOpt.get();
            try {
                String parsedContent = pdfResumeParser.parsePdfContent(resume.getFilePath());
                // 更新缓存
                resumeRepository.updateParsedContent(resume.getId(), parsedContent);
                log.info("[简历缓存] 文件解析成功并更新缓存 - 用户 ID: {}, 字符数：{}", userId, parsedContent.length());
                return Optional.of(parsedContent);
            } catch (Exception e) {
                log.error("[简历缓存] 文件解析失败 - 用户 ID: {}, 错误：{}", userId, e.getMessage());
            }
        }
        return Optional.empty();
    }
}
