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
     * 提交回答并获取下一个问题
     */
    @Transactional
    public InterviewResponse submitAnswer(Long sessionId, Long userId, String answer, String answerType) {
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在：" + sessionId));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("会话已结束");
        }

        // 保存回答
        Answer userAnswer = new Answer();
        userAnswer.setSessionId(sessionId);
        userAnswer.setQuestionId((long) (session.getAnsweredQuestions() + 1));
        userAnswer.setUserAnswer(answer);
        userAnswer.setAnswerType(answerType);
        userAnswer.setWordCount(answer.length());
        answerRepository.save(userAnswer);

        // 更新会话
        session.setAnsweredQuestions(session.getAnsweredQuestions() + 1);
        session.setConversationHistory(appendConversation(session.getConversationHistory(), 
            session.getAnsweredQuestions(), answer));

        // 判断是否结束
        if (session.getAnsweredQuestions() >= session.getTotalQuestions()) {
            session.setStatus("COMPLETED");
            session.setEndTime(LocalDateTime.now());
            session.setDurationSeconds(
                (int) (LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC) - 
                 session.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC)));
            
            sessionRepository.save(session);
            
            // 生成评估报告
            EvaluationReport report = generateEvaluationReport(session);
            
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
     * 生成评估报告
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