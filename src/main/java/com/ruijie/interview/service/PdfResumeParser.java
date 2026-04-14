package com.ruijie.interview.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * PDF简历解析服务
 * 使用千问多模态视觉模型进行PDF内容提取
 * 
 * 工作流程:
 * 1. 首先尝试从PDF提取文本内容
 * 2. 如果提取到的文本内容很少(说明可能是图片型PDF)，则将页面转为图片
 * 3. 调用千问视觉模型(qwen-vl-max)识别图片内容
 * 4. 返回提取的完整文本内容
 * 
 * 注意：此功能仅供后端使用，前端不显示解析过程
 */
@Service
public class PdfResumeParser {

    private static final Logger log = LoggerFactory.getLogger(PdfResumeParser.class);

    /**
     * 判断PDF是否为图片型的阈值（字符数）
     * 如果提取的文本字符数小于此值，则认为是图片型PDF，需要使用视觉模型
     */
    private static final int TEXT_THRESHOLD = 100;

    /**
     * 临时图片存放目录
     */
    private static final String TEMP_DIR = "data/resumes/temp";

    @Autowired
    private LlmService llmService;

    /**
     * 解析PDF文件内容，统一返回文本内容
     * 
     * @param pdfFilePath PDF文件的完整路径
     * @return 提取的文本内容
     */
    public String parsePdfContent(String pdfFilePath) {
        File pdfFile = new File(pdfFilePath);
        if (!pdfFile.exists()) {
            log.error("PDF文件不存在: {}", pdfFilePath);
            throw new IllegalArgumentException("PDF文件不存在: " + pdfFilePath);
        }

        try {
            // 先尝试直接提取文本
            String text = extractTextFromPdf(pdfFile);
            log.info("直接提取文本成功，字符数: {}", text.length());

            // 如果文本内容超过阈值，直接返回
            if (text.length() > TEXT_THRESHOLD) {
                log.info("PDF包含可提取的文本，使用文本提取结果");
                return text;
            }

            // 否则使用千问视觉模型识别
            log.info("PDF可能是图片型，开始使用千问视觉模型识别...");
            return extractTextWithQwenVL(pdfFile);

        } catch (Exception e) {
            log.error("PDF解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从PDF直接提取文本
     */
    private String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * 使用千问视觉模型识别PDF中的文本（将每页渲染为图片后识别）
     */
    private String extractTextWithQwenVL(File pdfFile) throws IOException {
        // 创建临时目录
        Path tempDir = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        StringBuilder fullText = new StringBuilder();
        String prefix = "qwen_vl_" + System.currentTimeMillis();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            log.info("PDF共 {} 页，开始千问视觉模型识别", pageCount);

            for (int i = 0; i < pageCount; i++) {
                // 将页面渲染为图片（200 DPI，兼顾清晰度和文件大小）
                BufferedImage image = renderer.renderImageWithDPI(i, 200);

                // 转换为Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                byte[] imageBytes = baos.toByteArray();
                String imageBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

                // 调用千问视觉模型识别
                log.info("正在调用千问视觉模型识别第 {} 页...", i + 1);
                String pageText = llmService.parseResumeFromImage(imageBase64);
                fullText.append(pageText);

                // 添加页分隔符
                if (i < pageCount - 1) {
                    fullText.append("\n\n--- 第 ").append(i + 1).append(" 页 ---\n\n");
                }

                log.info("已处理第 {} 页，当前提取字符数: {}", i + 1, fullText.length());

                // 避免API限流，适当延迟
                if (i < pageCount - 1) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("解析过程被中断", e);
        }

        // 清理临时目录
        cleanTempDir(tempDir);

        return fullText.toString();
    }

    /**
     * 清理临时目录
     */
    private void cleanTempDir(Path tempDir) {
        try {
            File[] files = tempDir.toFile().listFiles();
            if (files != null && files.length == 0) {
                Files.delete(tempDir);
            }
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", e.getMessage());
        }
    }

    /**
     * 解析PDF内容（直接传入字节数组）
     * 
     * @param pdfContent PDF文件内容
     * @param fileName 文件名（用于创建临时文件）
     * @return 提取的文本内容
     */
    public String parsePdfContent(byte[] pdfContent, String fileName) {
        Path tempFile = null;
        try {
            // 创建临时文件
            Path tempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            tempFile = tempDir.resolve(System.currentTimeMillis() + "_" + fileName);
            Files.write(tempFile, pdfContent);

            return parsePdfContent(tempFile.toString());
        } catch (IOException e) {
            log.error("创建临时文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建临时文件失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.delete(tempFile);
                } catch (IOException e) {
                    log.warn("清理临时文件失败: {}", e.getMessage());
                }
            }
        }
    }
}
