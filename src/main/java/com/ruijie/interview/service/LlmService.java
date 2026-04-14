package com.ruijie.interview.service;

import com.ruijie.interview.config.LlmConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大语言模型服务 - 调用免费开源 API
 * 支持 OpenRouter、HuggingFace 等提供商
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Autowired
    private LlmConfig llmConfig;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * 发送对话请求获取 AI 响应
     *
     * @param messages 消息列表
     * @return AI 响应内容
     */
    public String chat(List<Map<String, String>> messages) {
        return chat(messages, llmConfig.getModel());
    }

    /**
     * 发送对话请求获取 AI 响应（指定模型）
     *
     * @param messages 消息列表
     * @param model    模型名称
     * @return AI 响应内容
     */
    public String chat(List<Map<String, String>> messages, String model) {
        String url = llmConfig.getBaseUrl() + "/chat/completions";

        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());
            post.setHeader("HTTP-Referer", "http://localhost:8080");
            post.setHeader("X-Title", "AI Interview Simulation");

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            JSONArray messagesArray = new JSONArray();
            for (Map<String, String> msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.get("role"));
                msgObj.put("content", msg.get("content"));
                messagesArray.add(msgObj);
            }
            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            String response = httpClient.execute(post, response1 -> {
                int statusCode = response1.getCode();
                String responseBody = EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8);
                log.debug("LLM API Response: {}", responseBody);

                if (statusCode != 200) {
                    log.error("LLM API Error: {} - {}", statusCode, responseBody);
                    throw new RuntimeException("LLM API Error: " + statusCode);
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
                return "";
            });

            return response;

        } catch (IOException e) {
            log.error("LLM API call failed", e);
            // 尝试使用备选模型
            return chatWithFallback(messages, model);
        }
    }

    /**
     * 使用备选模型重试
     */
    private String chatWithFallback(List<Map<String, String>> messages, String failedModel) {
        log.warn("Model {} failed, trying fallback models", failedModel);

        for (String fallbackModel : llmConfig.getFallbackModels()) {
            if (!fallbackModel.equals(failedModel)) {
                try {
                    log.info("Trying fallback model: {}", fallbackModel);
                    return chat(messages, fallbackModel);
                } catch (Exception e) {
                    log.warn("Fallback model {} also failed", fallbackModel, e);
                }
            }
        }

        throw new RuntimeException("All models failed");
    }

    /**
     * 简单对话接口
     *
     * @param prompt 提示词
     * @param systemPrompt 系统提示
     * @return AI 响应
     */
    public String simpleChat(String prompt, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        messages.add(Map.of("role", "user", "content", prompt));

        return chat(messages);
    }

    /**
     * 简单对话接口（可指定 max_tokens）
     *
     * @param prompt 提示词
     * @param systemPrompt 系统提示
     * @param maxTokens 最大生成 token 数
     * @return AI 响应
     */
    public String chatWithMaxTokens(String prompt, String systemPrompt, int maxTokens) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        messages.add(Map.of("role", "user", "content", prompt));

        String url = llmConfig.getBaseUrl() + "/chat/completions";

        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());
            post.setHeader("HTTP-Referer", "http://localhost:8080");
            post.setHeader("X-Title", "AI Interview Simulation");

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", llmConfig.getModel());

            JSONArray messagesArray = new JSONArray();
            for (Map<String, String> msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.get("role"));
                msgObj.put("content", msg.get("content"));
                messagesArray.add(msgObj);
            }
            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);

            StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            String response = httpClient.execute(post, response1 -> {
                int statusCode = response1.getCode();
                String responseBody = EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8);
                log.debug("LLM API Response: {}", responseBody);

                if (statusCode != 200) {
                    log.error("LLM API Error: {} - {}", statusCode, responseBody);
                    throw new RuntimeException("LLM API Error: " + statusCode);
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
                return "";
            });

            return response;

        } catch (IOException e) {
            log.error("LLM API call failed", e);
            // 尝试使用备选模型
            return chatWithFallback(messages, llmConfig.getModel());
        }
    }

    /**
     * 生成面试问题
     *
     * @param position 岗位名称
     * @param category 问题类别
     * @param difficulty 难度等级
     * @return 生成的问题
     */
    public String generateQuestion(String position, String category, int difficulty) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请生成一个%s类别的面试问题，难度等级为%d（1-5）。" +
            "问题应该简洁明了，能够考察候选人的专业能力。只返回问题本身，不要有其他说明。",
            position, category, difficulty
        );

        return simpleChat("请生成一个面试问题", systemPrompt);
    }

    /**
     * 评估回答
     *
     * @param question 问题
     * @param answer 回答
     * @param position 岗位
     * @return 评估结果 JSON 字符串
     */
    public String evaluateAnswer(String question, String answer, String position) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请评估候选人的回答，从以下维度进行打分（0-100）：" +
            "1. 技术正确性 (technicalCorrectness)" +
            "2. 知识深度 (knowledgeDepth)" +
            "3. 逻辑严谨性 (logicRigor)" +
            "4. 表达清晰度 (clarity)" +
            "5. 岗位匹配度 (positionMatching)" +
            "请以 JSON 格式返回评估结果，包含各维度分数、亮点 (strengths)、不足 (weaknesses) 和改进建议 (suggestions)。",
            position
        );

        String prompt = String.format("问题：%s\n\n候选人回答：%s\n\n请评估：", question, answer);

        return simpleChat(prompt, systemPrompt);
    }

    /**
     * 生成追问
     *
     * @param question 原问题
     * @param answer 回答
     * @return 追问问题
     */
    public String generateFollowUp(String question, String answer) {
        String systemPrompt = "你是一位面试官。根据候选人的回答，生成一个恰当的追问问题，以深入了解其能力。" +
            "追问应该自然、有针对性，能够帮助你更好地评估候选人。只返回追问问题，不要有其他说明。";

        String prompt = String.format("原问题：%s\n\n候选人回答：%s\n\n请生成追问：", question, answer);

        return simpleChat(prompt, systemPrompt);
    }

    /**
     * 生成综合评估报告
     *
     * @param conversationHistory 对话历史
     * @param position 岗位
     * @return 评估报告 JSON 字符串
     */
    public String generateEvaluationReport(String conversationHistory, String position) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请根据面试对话历史生成综合评估报告。" +
            "报告应包含以下维度的评分（0-100）：" +
            "1. 技术能力 (technicalScore)" +
            "2. 沟通能力 (communicationScore)" +
            "3. 逻辑思维 (logicScore)" +
            "4. 应变能力 (adaptabilityScore)" +
            "5. 岗位匹配度 (matchingScore)" +
            "6. 综合评分 (overallScore)" +
            "同时包含以下四个文本字段：" +
            "- 亮点分析 (strengths)：候选人表现突出的方面，列出具体优点，结合对话中的具体回答进行分析" +
            "- 待改进之处 (weaknesses)：候选人需要改进的方面，列出具体不足，结合对话中的具体回答进行分析" +
            "- 改进建议 (suggestions)：针对不足的具体改进建议和学习方法，给出可操作的步骤" +
            "- 推荐学习资源 (recommendedResources)：相关的学习资源推荐，包括书籍、课程、项目等" +
            "请以 JSON 格式返回评估报告，确保 strengths、weaknesses、suggestions、recommendedResources 都是详细的中文文本，每部分不少于3-5句话，内容要具体有针对性，不要泛泛而谈。" +
            "只返回 JSON 格式，不要有其他说明。",
            position
        );

        String prompt = "面试对话历史：\n" + conversationHistory + "\n\n请生成综合评估报告：";

        return chatWithMaxTokens(prompt, systemPrompt, 2000);
    }

    /**
     * 评估单道题目回答（使用 RAG 知识库）
     *
     * @param question 问题
     * @param answer 回答
     * @param position 岗位
     * @param category 问题类别
     * @param knowledgeContext 相关知识库上下文（由 RAG 提供）
     * @return 评估结果 JSON 字符串
     */
    public String evaluateSingleAnswer(String question, String answer, String position, 
                                       String category, String knowledgeContext) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官，正在评估候选人对%s类别问题的回答。" +
            "请根据提供的知识库信息和候选人的回答，从以下维度进行评分（0-100 分）：" +
            "- 90-100 分：回答非常出色，全面准确，有深入见解" +
            "- 70-89 分：回答良好，基本正确，有一定深度" +
            "- 50-69 分：回答一般，部分正确但不够完整" +
            "- 30-49 分：回答较差，有明显错误或遗漏" +
            "- 0-29 分：回答很差，几乎完全错误或未作答" +
            "评分维度：" +
            "1. 技术正确性 (technicalScore) - 回答是否符合技术事实" +
            "2. 知识深度 (knowledgeDepth) - 回答的深度和广度" +
            "3. 逻辑思维 (logicScore) - 回答是否逻辑清晰" +
            "4. 表达清晰 (communicationScore) - 表达是否清晰易懂" +
            "5. 综合评分 (overallScore) - 综合以上维度的平均分" +
            "同时返回简短评价 (evaluationComment)，指出亮点和不足。" +
            "请以 JSON 格式返回评估结果，只返回 JSON 不要有其他说明。",
            position, category
        );

        StringBuilder prompt = new StringBuilder();
        prompt.append("【问题】\n").append(question).append("\n\n");
        prompt.append("【候选人回答】\n").append(answer).append("\n\n");
        
        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            prompt.append("【参考知识库】\n").append(knowledgeContext).append("\n\n");
        } else {
            log.debug("RAG 知识库上下文为空，将基于通用知识进行评分");
        }
        
        prompt.append("请根据以上信息进行评估，返回 JSON 格式的评分结果：");

        log.info("开始 RAG 评分 - 岗位：{}, 类别：{}, 问题：{}", position, category, question);
        log.debug("候选人回答：{}", answer);
        
        String result = simpleChat(prompt.toString(), systemPrompt);
        
        log.info("RAG 评分完成 - 结果：{}", result);
        
        return result;
    }

    /**
     * 使用千问视觉模型分析图片（多模态）
     * 调用 qwen-vl-max 模型进行图片理解
     *
     * @param imageBase64 图片的 Base64 编码（data:image/jpeg;base64,...）
     * @param prompt 提示词
     * @return AI 响应内容
     */
    public String analyzeImage(String imageBase64, String prompt) {
        String url = llmConfig.getBaseUrl() + "/chat/completions";
        String visionModel = "qwen-vl-max";

        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + llmConfig.getApiKey());
            post.setHeader("HTTP-Referer", "http://localhost:8080");
            post.setHeader("X-Title", "AI Interview Simulation");

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", visionModel);

            // 构建多模态消息
            JSONArray messagesArray = new JSONArray();

            // 系统消息
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个专业的简历解析助手，擅长从简历图片中提取关键信息。");
            messagesArray.add(systemMsg);

            // 用户消息（包含图片和文本）
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");

            JSONArray contentArray = new JSONArray();

            // 图片内容
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", imageBase64);
            imageContent.put("image_url", imageUrl);
            contentArray.add(imageContent);

            // 文本内容
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            contentArray.add(textContent);

            userMsg.put("content", contentArray);
            messagesArray.add(userMsg);

            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.5);

            StringEntity entity = new StringEntity(requestBody.toJSONString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            String response = httpClient.execute(post, response1 -> {
                int statusCode = response1.getCode();
                String responseBody = EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8);
                log.debug("Qwen VL API Response: {}", responseBody);

                if (statusCode != 200) {
                    log.error("Qwen VL API Error: {} - {}", statusCode, responseBody);
                    throw new RuntimeException("Qwen VL API Error: " + statusCode);
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && choices.size() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
                return "";
            });

            return response;

        } catch (IOException e) {
            log.error("Qwen VL API call failed", e);
            throw new RuntimeException("千问视觉模型调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析简历图片内容
     * 将简历页面转换为图片后调用千问视觉模型提取文本
     *
     * @param imageBase64 简历图片的 Base64 编码
     * @return 提取的简历文本内容
     */
    public String parseResumeFromImage(String imageBase64) {
        String prompt = "请仔细识别这张图片中的简历内容，并提取所有文本信息。" +
            "请按以下格式返回：\n" +
            "1. 基本信息（姓名、联系方式、邮箱等）\n" +
            "2. 教育背景\n" +
            "3. 工作经历\n" +
            "4. 项目经历\n" +
            "5. 技能特长\n" +
            "6. 其他相关信息\n" +
            "请尽可能详细地提取所有可见内容。";
        
        return analyzeImage(imageBase64, prompt);
    }
}
