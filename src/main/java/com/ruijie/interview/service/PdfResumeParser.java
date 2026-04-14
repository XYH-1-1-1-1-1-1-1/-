package com.ruijie.interview.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PDF简历解析服务
 * 统一处理文本型和图片型PDF的内容提取
 * 
 * 工作流程:
 * 1. 首先尝试从PDF提取文本内容
 * 2. 如果提取到的文本内容很少(说明可能是图片型PDF)，则使用OCR识别
 * 3. 返回提取的完整文本内容
 */
@Service
public class PdfResumeParser {

    private static final Logger log = LoggerFactory.getLogger(PdfResumeParser.class);

    /**
     * 判断PDF是否为图片型的阈值（字符数）
     * 如果提取的文本字符数小于此值，则认为是图片型PDF，需要使用OCR
     */
    private static final int TEXT_THRESHOLD = 100;

    /**
     * OCR临时图片存放目录
     */
    private static final String OCR_TEMP_DIR = "data/resumes/ocr_temp";

    /**
     * Tesseract数据文件路径
     * 默认使用系统环境变量TESSDATA_PREFIX，如果未设置则使用默认路径
     */
    @Value("${pdf.parser.tessdata-path:}")
    private String tessdataPath;

    /**
     * OCR识别语言，默认中文+英文
     */
    @Value("${pdf.parser.ocr-lang:chi_sim+eng}")
    private String ocrLang;

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

            // 否则使用OCR识别
            log.info("PDF可能是图片型，开始使用OCR识别...");
            return extractTextWithOcr(pdfFile);

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
     * 使用OCR识别PDF中的文本（将每页渲染为图片后识别）
     */
    private String extractTextWithOcr(File pdfFile) throws IOException, TesseractException {
        // 创建临时目录
        Path tempDir = Paths.get(OCR_TEMP_DIR);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        StringBuilder fullText = new StringBuilder();
        String prefix = "ocr_" + System.currentTimeMillis();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            log.info("PDF共 {} 页，开始OCR识别", pageCount);

            for (int i = 0; i < pageCount; i++) {
                // 将页面渲染为图片（300 DPI提高识别率）
                BufferedImage image = renderer.renderImageWithDPI(i, 300);

                // 保存临时图片
                Path imagePath = tempDir.resolve(prefix + "_page_" + (i + 1) + ".png");
                ImageIO.write(image, "PNG", imagePath.toFile());

                // 使用Tesseract OCR识别图片文字
                String pageText = ocrImage(imagePath.toFile());
                fullText.append(pageText);

                // 添加页分隔符
                if (i < pageCount - 1) {
                    fullText.append("\n\n--- 第 ").append(i + 1).append(" 页 ---\n\n");
                }

                // 删除临时图片
                Files.delete(imagePath);

                log.info("已处理第 {} 页，当前提取字符数: {}", i + 1, fullText.length());
            }
        }

        // 清理临时目录（如果为空）
        cleanTempDir(tempDir);

        return fullText.toString();
    }

    /**
     * 使用Tesseract OCR识别图片文字
     */
    private String ocrImage(File imageFile) throws TesseractException {
        Tesseract tesseract = new Tesseract();

        // 设置tessdata路径
        if (tessdataPath != null && !tessdataPath.isEmpty()) {
            tesseract.setDatapath(tessdataPath);
        }

        // 设置识别语言
        tesseract.setLanguage(ocrLang);

        // 设置OCR模式为自动识别
        tesseract.setPageSegMode(3);

        // 执行识别
        return tesseract.doOCR(imageFile);
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
            Path tempDir = Paths.get(OCR_TEMP_DIR);
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