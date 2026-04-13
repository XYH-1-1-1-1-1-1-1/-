package com.ruijie.interview.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruijie.interview.entity.*;
import com.ruijie.interview.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 面试会话服务类 - 核心业务逻辑
 */
@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private EvaluationReportRepository reportRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private LlmService llmService;

    @Autowired
    private RagService ragService;

    /**
     * 开始新的面试会话
     */
    @Transactional
    public InterviewSession startInterview(Long userId, String positionCode) {
        Position position = positionService.findByCode(positionCode)
            .orElseThrow(() -> new RuntimeException("岗位不存在：" + positionCode));

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPositionId(positionCode);
        session.setStatus("IN_PROGRESS");
        session.setTotalQuestions(position.getQuestionCount());
        session.setAnsweredQuestions(0);
        session.setConversationHistory("");
        session.setStartTime(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    /**
     * 获取面试会话
     */
    public Optional<InterviewSession> getSession(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * 获取用户的所有面试会话
     */
    public List<InterviewSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户进行中的面试会话
     */
    public Optional<InterviewSession> getInProgressSession(Long userId) {
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (InterviewSession session : sessions) {
            if ("IN_PROGRESS".equals(session.getStatus())) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    /**
     * 恢复进行中的面试会话
     */
    @Transactional
    public InterviewSession resumeInterview(Long userId, Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在：" + sessionId));
        
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该会话");
        }
        
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("该会话已结束，无法恢复");
        }
        
        return session;
    }

    /**
     * 获取当前问题的索引（用于恢复面试时加载正确的问题）
     */
    public int getCurrentQuestionIndex(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return 0;
        }
        return session.getAnsweredQuestions();
    }

    /**
     * 提交回答并获取下一个问题
     */
    @Transactional
    public InterviewResponse submitAnswer(Long sessionId, Long userId, String answer, String answerType) {
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在：" + sessionId));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("会话已结束");
        }

        // 获取当前问题
        int questionIndex = session.getAnsweredQuestions() + 1;
        Question currentQuestion = getCurrentQuestion(session, questionIndex);
        
        // 保存回答并使用 RAG 评分
        Answer userAnswer = scoreAnswerWithRag(sessionId, currentQuestion, answer, answerType, false);

        // 更新会话
        session.setAnsweredQuestions(session.getAnsweredQuestions() + 1);
        session.setConversationHistory(appendConversation(session.getConversationHistory(), 
            questionIndex, answer));

        // 判断是否结束
        if (session.getAnsweredQuestions() >= session.getTotalQuestions()) {
            session.setStatus("COMPLETED");
            session.setEndTime(LocalDateTime.now());
            session.setDurationSeconds(
                (int) (LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC) - 
                 session.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC)));
            
            sessionRepository.save(session);
            
            // 生成评估报告（基于各题平均分）
            EvaluationReport report = generateEvaluationReportFromScores(session);
            
            return new InterviewResponse(
                true,
                "面试已结束",
                null,
                report
            );
        }

        // 获取下一个问题
        Question nextQuestion = getNextQuestion(session, session.getAnsweredQuestions());
        
        sessionRepository.save(session);

        return new InterviewResponse(
            false,
            "请回答下一个问题",
            nextQuestion,
            null
        );
    }

    /**
     * 跳过问题
     */
    @Transactional
    public InterviewResponse skipQuestion(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在：" + sessionId));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("会话已结束");
        }

        // 记录跳过
        int questionIndex = session.getAnsweredQuestions() + 1;
        Question currentQuestion = getCurrentQuestion(session, questionIndex);
        
        // 保存跳过记录（最低分 10 分）
        Answer skippedAnswer = new Answer();
        skippedAnswer.setSessionId(sessionId);
        skippedAnswer.setQuestionId((long) questionIndex);
        skippedAnswer.setUserAnswer("(跳过)");
        skippedAnswer.setAnswerType("skipped");
        skippedAnswer.setIsSkipped(true);
        // 跳过题目给最低分 10 分
        skippedAnswer.setTechnicalScore(10);
        skippedAnswer.setCommunicationScore(10);
        skippedAnswer.setLogicScore(10);
        skippedAnswer.setKnowledgeDepth(10);
        skippedAnswer.setOverallScore(10);
        skippedAnswer.setEvaluationComment("候选人跳过此问题");
        answerRepository.save(skippedAnswer);

        // 更新跳过计数
        session.setSkippedQuestions(session.getSkippedQuestions() != null ? session.getSkippedQuestions() + 1 : 1);
        session.setAnsweredQuestions(session.getAnsweredQuestions() + 1);
        session.setConversationHistory(appendConversation(session.getConversationHistory(), 
            questionIndex, "(跳过)"));

        // 判断是否结束
        if (session.getAnsweredQuestions() >= session.getTotalQuestions()) {
            session.setStatus("COMPLETED");
            session.setEndTime(LocalDateTime.now());
            session.setDurationSeconds(
                (int) (LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC) - 
                 session.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC)));
            
            sessionRepository.save(session);
            
            // 生成评估报告
            EvaluationReport report = generateEvaluationReportFromScores(session);
            
            return new InterviewResponse(
                true,
                "面试已结束",
                null,
                report
            );
        }

        // 获取下一个问题
        Question nextQuestion = getNextQuestion(session, session.getAnsweredQuestions());
        
        sessionRepository.save(session);

        return new InterviewResponse(
            false,
            "请回答下一个问题",
            nextQuestion,
            null
        );
    }

    /**
     * 使用 RAG 评分保存回答
     */
    private Answer scoreAnswerWithRag(Long sessionId, Question question, String answer, String answerType, boolean isSkipped) {
        Answer userAnswer = new Answer();
        userAnswer.setSessionId(sessionId);
        userAnswer.setQuestionId(question != null ? question.getId() : 0L);
        userAnswer.setUserAnswer(answer);
        userAnswer.setAnswerType(answerType);
        userAnswer.setWordCount(answer != null ? answer.length() : 0);
        userAnswer.setIsSkipped(isSkipped);

        if (isSkipped || answer == null || answer.trim().isEmpty()) {
            // 跳过或空回答给最低分 10 分
            userAnswer.setTechnicalScore(10);
            userAnswer.setCommunicationScore(10);
            userAnswer.setLogicScore(10);
            userAnswer.setKnowledgeDepth(10);
            userAnswer.setOverallScore(10);
            userAnswer.setEvaluationComment("候选人跳过此问题或回答为空");
        } else {
            // 使用 RAG 评分
            try {
                InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
                if (session != null && question != null) {
                    String category = question.getCategory() != null ? question.getCategory() : "技术知识";
                    log.info("[RAG 评分] 开始评分 - 会话 ID: {}, 岗位：{}, 类别：{}, 问题：{}", 
                        sessionId, session.getPositionId(), category, question.getContent());
                    
                    String evalJson = ragService.evaluateAnswerWithRag(
                        session.getPositionId(), 
                        category, 
                        question.getContent(), 
                        answer
                    );
                    
                    log.info("[RAG 评分] LLM 返回结果：{}", evalJson);
                    
                    // 解析评分
                    JSONObject evalData = parseScoreJson(evalJson);
                    log.info("[RAG 评分] 解析后的分数 - technicalScore: {}, communicationScore: {}, logicScore: {}, knowledgeDepth: {}, overallScore: {}",
                        evalData.getInteger("technicalScore"),
                        evalData.getInteger("communicationScore"),
                        evalData.getInteger("logicScore"),
                        evalData.getInteger("knowledgeDepth"),
                        evalData.getInteger("overallScore"));
                    
                    Integer technicalScore = ensureMinScore(evalData.getInteger("technicalScore"));
                    Integer communicationScore = ensureMinScore(evalData.getInteger("communicationScore"));
                    Integer logicScore = ensureMinScore(evalData.getInteger("logicScore"));
                    Integer knowledgeDepthScore = ensureMinScore(evalData.getInteger("knowledgeDepth"));
                    Integer overallScore = ensureMinScore(evalData.getInteger("overallScore"));
                    
                    log.info("[RAG 评分] 最终分数 - technicalScore: {}, communicationScore: {}, logicScore: {}, knowledgeDepth: {}, overallScore: {}",
                        technicalScore, communicationScore, logicScore, knowledgeDepthScore, overallScore);
                    
                    userAnswer.setTechnicalScore(technicalScore);
                    userAnswer.setCommunicationScore(communicationScore);
                    userAnswer.setLogicScore(logicScore);
                    userAnswer.setKnowledgeDepth(knowledgeDepthScore);
                    userAnswer.setOverallScore(overallScore);
                    userAnswer.setEvaluationComment(evalData.getString("evaluationComment"));
                }
            } catch (Exception e) {
                log.error("RAG 评分失败，使用默认分数", e);
                // 评分失败给中等分数 50 分，而不是最低分
                // 这样可以让用户看到真实的评分差异
                userAnswer.setTechnicalScore(50);
                userAnswer.setCommunicationScore(50);
                userAnswer.setLogicScore(50);
                userAnswer.setKnowledgeDepth(50);
                userAnswer.setOverallScore(50);
                userAnswer.setEvaluationComment("评分系统暂时不可用，已使用默认分数。建议检查网络连接或 API 配置。");
            }
        }

        return answerRepository.save(userAnswer);
    }

    /**
     * 确保分数在有效范围内 (0-100)
     * 注意：不再强制设置最低分，让评分真实反映回答质量
     */
    private Integer ensureMinScore(Integer score) {
        if (score == null) {
            return 50; // 默认中等分数，而不是最低分
        }
        // 限制在 0-100 范围内
        return Math.max(0, Math.min(score, 100));
    }

    /**
     * 解析评分 JSON
     */
    private JSONObject parseScoreJson(String jsonStr) {
        try {
            int start = jsonStr.indexOf("{");
            int end = jsonStr.lastIndexOf("}");
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }
            return JSON.parseObject(jsonStr);
        } catch (Exception e) {
            log.error("解析评分 JSON 失败", e);
            JSONObject defaultJson = new JSONObject();
            defaultJson.put("technicalScore", 50);
            defaultJson.put("communicationScore", 50);
            defaultJson.put("logicScore", 50);
            defaultJson.put("knowledgeDepth", 50);
            defaultJson.put("overallScore", 50);
            defaultJson.put("evaluationComment", "评分解析失败");
            return defaultJson;
        }
    }

    /**
     * 获取当前问题
     */
    private Question getCurrentQuestion(InterviewSession session, int questionIndex) {
        int difficulty = 1;
        if (questionIndex > 2) difficulty = 2;
        if (questionIndex > 5) difficulty = 3;
        if (questionIndex > 8) difficulty = 4;

        String[] categories = {"技术知识", "项目经验", "行为题"};
        String category = categories[(questionIndex - 1) % categories.length];

        List<Question> questions = questionService.findByPositionIdAndCategory(
            session.getPositionId(), category);

        if (questions.isEmpty()) {
            questions = questionService.findByPositionId(session.getPositionId());
        }

        if (questions.isEmpty()) {
            return generateAiQuestion(session.getPositionId(), category, difficulty);
        }

        return questions.get((questionIndex - 1) % questions.size());
    }

    /**
     * 获取下一个问题（结合 RAG）
     */
    private Question getNextQuestion(InterviewSession session, int questionIndex) {
        // 根据进度选择不同难度的问题
        int difficulty = 1;
        if (questionIndex > 2) difficulty = 2;
        if (questionIndex > 5) difficulty = 3;
        if (questionIndex > 8) difficulty = 4;

        // 按类别轮询
        String[] categories = {"技术知识", "项目经验", "行为题"};
        String category = categories[questionIndex % categories.length];

        List<Question> questions = questionService.findByPositionIdAndCategory(
            session.getPositionId(), category);

        if (questions.isEmpty()) {
            questions = questionService.findByPositionId(session.getPositionId());
        }

        if (questions.isEmpty()) {
            // 如果题库为空，使用 AI 生成问题
            return generateAiQuestion(session.getPositionId(), category, difficulty);
        }

        return questions.get(questionIndex % questions.size());
    }

    /**
     * 使用 AI 生成问题
     */
    private Question generateAiQuestion(String positionId, String category, int difficulty) {
        Position position = positionService.findByCode(positionId).orElse(null);
        String positionName = position != null ? position.getName() : "技术岗位";

        String prompt = String.format(
            "请为%s岗位生成一个%s类别的面试问题，难度等级%d（1-5）。" +
            "只返回问题本身，不要有其他说明。",
            positionName, category, difficulty
        );

        String questionContent = llmService.simpleChat(prompt, 
            "你是一位专业的面试官");

        Question q = new Question();
        q.setPositionId(positionId);
        q.setCategory(category);
        q.setContent(questionContent);
        q.setDifficultyLevel(difficulty);
        return q;
    }

    /**
     * 追加对话历史
     */
    private String appendConversation(String history, int questionNum, String answer) {
        String entry = String.format("\n[问题 %d] 候选人回答：%s", questionNum, answer);
        return history + entry;
    }

    /**
     * 基于各题平均分生成评估报告
     */
    @Transactional
    public EvaluationReport generateEvaluationReportFromScores(InterviewSession session) {
        // 获取所有回答
        List<Answer> answers = answerRepository.findBySessionIdOrderByQuestionId(session.getId());
        
        if (answers.isEmpty()) {
            // 没有回答记录，使用默认分数
            return createDefaultReport(session);
        }

        // 计算各维度平均分
        int totalTechnical = 0;
        int totalCommunication = 0;
        int totalLogic = 0;
        int totalKnowledge = 0;
        int totalOverall = 0;
        int answeredCount = 0;
        int skippedCount = 0;
        
        StringBuilder strengthsBuilder = new StringBuilder();
        StringBuilder weaknessesBuilder = new StringBuilder();
        StringBuilder suggestionsBuilder = new StringBuilder();
        
        for (Answer answer : answers) {
            if (answer.getIsSkipped() != null && answer.getIsSkipped()) {
                skippedCount++;
                continue;
            }
            
            if (answer.getOverallScore() != null) {
                totalTechnical += answer.getTechnicalScore() != null ? answer.getTechnicalScore() : 10;
                totalCommunication += answer.getCommunicationScore() != null ? answer.getCommunicationScore() : 10;
                totalLogic += answer.getLogicScore() != null ? answer.getLogicScore() : 10;
                totalKnowledge += answer.getKnowledgeDepth() != null ? answer.getKnowledgeDepth() : 10;
                totalOverall += answer.getOverallScore();
                answeredCount++;
                
                // 收集评价
                if (answer.getEvaluationComment() != null && !answer.getEvaluationComment().isEmpty()) {
                    // 高分作为亮点
                    if (answer.getOverallScore() >= 70) {
                        strengthsBuilder.append("第").append(answer.getQuestionId())
                            .append("题：").append(answer.getEvaluationComment()).append(" ");
                    } else {
                        weaknessesBuilder.append("第").append(answer.getQuestionId())
                            .append("题：").append(answer.getEvaluationComment()).append(" ");
                    }
                }
            }
        }
        
        // 计算平均分（至少有一题才计算）
        int avgTechnical = answeredCount > 0 ? totalTechnical / answeredCount : 10;
        int avgCommunication = answeredCount > 0 ? totalCommunication / answeredCount : 10;
        int avgLogic = answeredCount > 0 ? totalLogic / answeredCount : 10;
        int avgKnowledge = answeredCount > 0 ? totalKnowledge / answeredCount : 10;
        int avgOverall = answeredCount > 0 ? totalOverall / answeredCount : 10;
        
        // 根据跳过题目数量调整分数（每跳过一题扣 2 分）
        int skipPenalty = skippedCount * 2;
        avgOverall = Math.max(10, avgOverall - skipPenalty);
        
        // 生成建议
        if (skippedCount > 0) {
            suggestionsBuilder.append("面试中跳过了").append(skippedCount)
                .append("道题目，建议勇敢尝试回答。");
        }
        if (avgTechnical < 50) {
            suggestionsBuilder.append("建议加强基础知识学习。");
        }
        if (avgOverall >= 70) {
            suggestionsBuilder.append("整体表现良好，继续保持。");
        }
        
        // 创建评估报告
        EvaluationReport report = new EvaluationReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setPositionId(session.getPositionId());
        report.setTechnicalScore(avgTechnical);
        report.setCommunicationScore(avgCommunication);
        report.setLogicScore(avgLogic);
        report.setAdaptabilityScore(avgKnowledge); // 用知识深度作为应变能力
        report.setMatchingScore(avgOverall); // 用平均分作为匹配度
        report.setOverallScore(avgOverall);
        report.setStrengths(strengthsBuilder.length() > 0 ? strengthsBuilder.toString() : "候选人完成了面试");
        report.setWeaknesses(weaknessesBuilder.length() > 0 ? weaknessesBuilder.toString() : "部分问题可以回答得更深入");
        report.setSuggestions(suggestionsBuilder.length() > 0 ? suggestionsBuilder.toString() : "建议继续学习和实践");
        report.setRecommendedResources("推荐学习相关技术文档和开源项目");
        report.setSpeechRate("NORMAL");
        report.setClarity("GOOD");
        report.setConfidence("NORMAL");

        return reportRepository.save(report);
    }

    /**
     * 创建默认评估报告
     */
    private EvaluationReport createDefaultReport(InterviewSession session) {
        EvaluationReport report = new EvaluationReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setPositionId(session.getPositionId());
        report.setTechnicalScore(10);
        report.setCommunicationScore(10);
        report.setLogicScore(10);
        report.setAdaptabilityScore(10);
        report.setMatchingScore(10);
        report.setOverallScore(10);
        report.setStrengths("无");
        report.setWeaknesses("未回答任何问题");
        report.setSuggestions("建议勇敢尝试回答问题");
        report.setRecommendedResources("推荐从基础开始学习");
        report.setSpeechRate("NORMAL");
        report.setClarity("GOOD");
        report.setConfidence("NORMAL");
        return reportRepository.save(report);
    }

    /**
     * 生成评估报告（使用 AI）
     */
    @Transactional
    public EvaluationReport generateEvaluationReport(InterviewSession session) {
        Position position = positionService.findByCode(session.getPositionId()).orElse(null);
        String positionName = position != null ? position.getName() : "技术岗位";

        // 使用 AI 生成评估
        String evalJson = llmService.generateEvaluationReport(
            session.getConversationHistory(), 
            positionName
        );

        // 解析评估结果
        JSONObject evalData = parseEvaluationJson(evalJson);

        // 创建评估报告
        EvaluationReport report = new EvaluationReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setPositionId(session.getPositionId());
        report.setTechnicalScore(evalData.getInteger("technicalScore"));
        report.setCommunicationScore(evalData.getInteger("communicationScore"));
        report.setLogicScore(evalData.getInteger("logicScore"));
        report.setAdaptabilityScore(evalData.getInteger("adaptabilityScore"));
        report.setMatchingScore(evalData.getInteger("matchingScore"));
        report.setOverallScore(evalData.getInteger("overallScore"));
        report.setStrengths(evalData.getString("strengths"));
        report.setWeaknesses(evalData.getString("weaknesses"));
        report.setSuggestions(evalData.getString("suggestions"));
        report.setRecommendedResources(evalData.getString("recommendedResources"));

        // 语音相关评估（暂时使用默认值）
        report.setSpeechRate("NORMAL");
        report.setClarity("GOOD");
        report.setConfidence("NORMAL");

        return reportRepository.save(report);
    }

    /**
     * 解析评估 JSON（容错处理）
     */
    private JSONObject parseEvaluationJson(String jsonStr) {
        try {
            // 尝试提取 JSON 部分
            int start = jsonStr.indexOf("{");
            int end = jsonStr.lastIndexOf("}");
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }
            
            JSONObject json = JSON.parseObject(jsonStr);
            
            // 确保所有字段都有值
            ensureField(json, "technicalScore", 70);
            ensureField(json, "communicationScore", 70);
            ensureField(json, "logicScore", 70);
            ensureField(json, "adaptabilityScore", 70);
            ensureField(json, "matchingScore", 70);
            ensureField(json, "overallScore", 70);
            ensureField(json, "strengths", "候选人展现了良好的专业知识");
            ensureField(json, "weaknesses", "部分问题回答不够深入");
            ensureField(json, "suggestions", "建议加强基础知识学习，多做项目实践");
            ensureField(json, "recommendedResources", "推荐学习相关技术文档和开源项目");
            
            return json;
        } catch (Exception e) {
            log.error("解析评估结果失败", e);
            // 返回默认评估
            JSONObject defaultJson = new JSONObject();
            defaultJson.put("technicalScore", 70);
            defaultJson.put("communicationScore", 70);
            defaultJson.put("logicScore", 70);
            defaultJson.put("adaptabilityScore", 70);
            defaultJson.put("matchingScore", 70);
            defaultJson.put("overallScore", 70);
            defaultJson.put("strengths", "候选人展现了良好的专业知识");
            defaultJson.put("weaknesses", "部分问题回答不够深入");
            defaultJson.put("suggestions", "建议加强基础知识学习，多做项目实践");
            defaultJson.put("recommendedResources", "推荐学习相关技术文档和开源项目");
            return defaultJson;
        }
    }

    private void ensureField(JSONObject json, String field, Object defaultValue) {
        if (!json.containsKey(field) || json.get(field) == null) {
            json.put(field, defaultValue);
        }
    }

    /**
     * 获取评估报告
     */
    public Optional<EvaluationReport> getReportBySessionId(Long sessionId) {
        return reportRepository.findBySessionId(sessionId);
    }

    /**
     * 获取用户的评估报告列表
     */
    public List<EvaluationReport> getUserReports(Long userId) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除面试会话记录
     */
    @Transactional
    public void deleteInterviewSession(Long sessionId) {
        // 先删除相关的回答记录
        List<Answer> answers = answerRepository.findBySessionIdOrderByQuestionId(sessionId);
        if (!answers.isEmpty()) {
            answerRepository.deleteAll(answers);
        }
        
        // 删除相关的评估报告
        Optional<EvaluationReport> report = reportRepository.findBySessionId(sessionId);
        report.ifPresent(reportRepository::delete);
        
        // 最后删除面试会话
        sessionRepository.deleteById(sessionId);
    }

    /**
     * 主动退出面试（不保存任何记录）
     * 用于用户在面试过程中主动退出或拒绝恢复面试
     */
    @Transactional
    public void exitInterview(Long sessionId) {
        log.info("[退出面试] 开始退出面试会话，会话ID: {}", sessionId);
        // 直接删除会话及相关数据，不保存任何历史记录
        deleteInterviewSession(sessionId);
        log.info("[退出面试] 面试会话已删除，会话ID: {}", sessionId);
    }

    /**
     * 面试响应 DTO
     */
    public static class InterviewResponse {
        public boolean isFinished;
        public String message;
        public Question nextQuestion;
        public EvaluationReport report;

        public InterviewResponse(boolean isFinished, String message, 
                                Question nextQuestion, EvaluationReport report) {
            this.isFinished = isFinished;
            this.message = message;
            this.nextQuestion = nextQuestion;
            this.report = report;
        }
    }
}