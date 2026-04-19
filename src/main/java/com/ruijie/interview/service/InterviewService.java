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
import java.util.concurrent.*;

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

    @Autowired
    private AudioAnalysisService audioAnalysisService;

    @Autowired
    private ResumeService resumeService;

    // 用于缓存简历相关问题的生成状态
    private final Map<Long, List<String>> resumeQuestionCache = new HashMap<>();
    
    // 异步评分线程池
    private final ExecutorService scoringExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors()),
        new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AsyncScorer-" + (++count));
                t.setDaemon(true);
                return t;
            }
        }
    );
    
    // 评分任务状态跟踪：sessionId -> 评分任务状态
    private final ConcurrentHashMap<Long, ScoringStatus> scoringStatusMap = new ConcurrentHashMap<>();
    
    /**
     * 评分任务状态
     */
    public static class ScoringStatus {
        public int totalQuestions;      // 总题数
        public int scoredQuestions;     // 已评分数
        public boolean completed;       // 是否完成
        public LocalDateTime startTime; // 开始时间
        
        public ScoringStatus(int totalQuestions) {
            this.totalQuestions = totalQuestions;
            this.scoredQuestions = 0;
            this.completed = false;
            this.startTime = LocalDateTime.now();
        }
        
        public synchronized void incrementScored() {
            this.scoredQuestions++;
        }
        
        public synchronized void markCompleted() {
            this.completed = true;
        }
        
        public int getProgress() {
            if (totalQuestions == 0) return 0;
            return (int) ((scoredQuestions * 100.0) / totalQuestions);
        }
    }
    
    /**
     * 获取评分状态
     */
    public ScoringStatus getScoringStatus(Long sessionId) {
        return scoringStatusMap.get(sessionId);
    }
    
    /**
     * 清除评分状态
     */
    public void removeScoringStatus(Long sessionId) {
        scoringStatusMap.remove(sessionId);
    }

    /**
     * 开始新的面试会话（默认练习模式）
     */
    @Transactional
    public InterviewSession startInterview(Long userId, String positionCode) {
        return startInterview(userId, positionCode, "PRACTICE");
    }

    /**
     * 开始新的面试会话（支持指定模式）
     * @param interviewMode 面试模式：REAL（真实面试 - 语音）/ PRACTICE（练习面试 - 文字）
     */
    @Transactional
    public InterviewSession startInterview(Long userId, String positionCode, String interviewMode) {
        Position position = positionService.findByCode(positionCode)
            .orElseThrow(() -> new RuntimeException("岗位不存在：" + positionCode));

        // 真实面试模式下检查是否有简历
        if ("REAL".equals(interviewMode)) {
            if (!resumeService.hasResume(userId)) {
                throw new RuntimeException("真实面试模式需要先提交个人简历");
            }
        }

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPositionId(positionCode);
        session.setStatus("IN_PROGRESS");
        // 真实面试模式 18 题：1 自我介绍 + 10 简历相关 + 5 题库随机 + 2 开放性
        // 练习模式保持原有题目数量
        int totalQuestions = "REAL".equals(interviewMode) ? 18 : position.getQuestionCount();
        session.setTotalQuestions(totalQuestions);
        session.setAnsweredQuestions(0);
        session.setConversationHistory("");
        session.setStartTime(LocalDateTime.now());
        session.setInterviewMode(interviewMode != null ? interviewMode : "PRACTICE");

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
     * 获取用户指定岗位的面试会话
     */
    public List<InterviewSession> getUserSessionsByPosition(Long userId, String positionCode) {
        return sessionRepository.findByUserIdAndPositionIdOrderByCreatedAtDesc(userId, positionCode);
    }

    /**
     * 获取用户所有不同的岗位类型（用于历史记录筛选）
     */
    public List<Map<String, Object>> getUserDistinctPositions(Long userId) {
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Integer> positionCount = new HashMap<>();
        
        for (InterviewSession session : sessions) {
            String posId = session.getPositionId();
            positionCount.put(posId, positionCount.getOrDefault(posId, 0) + 1);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : positionCount.entrySet()) {
            Map<String, Object> pos = new HashMap<>();
            pos.put("code", entry.getKey());
            pos.put("count", entry.getValue());
            result.add(pos);
        }
        
        return result;
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
     * 优化：使用异步评分，立即返回下一个问题
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
        
        // 保存回答（使用临时分数）
        Answer userAnswer = saveAnswerWithTempScore(sessionId, currentQuestion, answer, answerType, false);

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
            
            // 初始化评分状态跟踪
            ScoringStatus status = new ScoringStatus(session.getTotalQuestions());
            scoringStatusMap.put(sessionId, status);
            
            // 异步生成评估报告（包含异步评分）
            asyncGenerateReportWithScoring(session);
            
            return new InterviewResponse(
                true,
                "面试已结束，正在生成评估报告...",
                null,
                null  // 报告稍后生成
            );
        }

        // 异步评分当前回答
        asyncScoreAnswer(sessionId, currentQuestion, answer, answerType, false);

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
     * 异步评分回答
     */
    private void asyncScoreAnswer(Long sessionId, Question question, String answer, String answerType, boolean isSkipped) {
        scoringExecutor.submit(() -> {
            try {
                log.info("[异步评分] 开始异步评分 - 会话 ID: {}, 问题：{}", sessionId, 
                    question != null ? question.getContent() : "N/A");
                
                // 执行实际评分
                Answer scoredAnswer = scoreAnswerWithRag(sessionId, question, answer, answerType, isSkipped);
                
                // 更新已评分计数
                ScoringStatus status = scoringStatusMap.get(sessionId);
                if (status != null) {
                    status.incrementScored();
                    log.info("[异步评分] 评分进度：{}/{} ({}%)", 
                        status.scoredQuestions, status.totalQuestions, status.getProgress());
                }
                
                log.info("[异步评分] 评分完成 - 会话 ID: {}, 总体分数：{}", sessionId, scoredAnswer.getOverallScore());
            } catch (Exception e) {
                log.error("[异步评分] 评分失败 - 会话 ID: {}", sessionId, e);
            }
        });
    }
    
    /**
     * 异步生成评估报告（等待所有评分完成后）
     */
    private void asyncGenerateReportWithScoring(InterviewSession session) {
        scoringExecutor.submit(() -> {
            try {
                Long sessionId = session.getId();
                ScoringStatus status = scoringStatusMap.get(sessionId);
                
                // 等待所有评分完成（最多等待 5 分钟）
                long startTime = System.currentTimeMillis();
                long timeout = 5 * 60 * 1000; // 5 分钟
                
                while (status != null && !status.completed && 
                       status.scoredQuestions < status.totalQuestions &&
                       (System.currentTimeMillis() - startTime) < timeout) {
                    Thread.sleep(500);
                }
                
                if (status != null && !status.completed) {
                    log.warn("[异步报告] 评分超时，但仍生成报告 - 会话 ID: {}, 进度：{}/{}", 
                        sessionId, status.scoredQuestions, status.totalQuestions);
                }
                
                // 生成评估报告
                log.info("[异步报告] 开始生成评估报告 - 会话 ID: {}", sessionId);
                EvaluationReport report = generateEvaluationReportFromScores(session);
                
                // 标记评分完成
                if (status != null) {
                    status.markCompleted();
                }
                
                log.info("[异步报告] 评估报告生成完成 - 会话 ID: {}, 总体分数：{}", sessionId, report.getOverallScore());
                
            } catch (Exception e) {
                log.error("[异步报告] 生成报告失败 - 会话 ID: {}", session.getId(), e);
            }
        });
    }
    
    /**
     * 保存回答时使用临时分数（用于异步评分场景）
     */
    private Answer saveAnswerWithTempScore(Long sessionId, Question question, String answer, String answerType, boolean isSkipped) {
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
            // 给临时分数，等待异步评分更新
            userAnswer.setTechnicalScore(50);
            userAnswer.setCommunicationScore(50);
            userAnswer.setLogicScore(50);
            userAnswer.setKnowledgeDepth(50);
            userAnswer.setOverallScore(50);
            userAnswer.setEvaluationComment("正在评分中...");
        }

        return answerRepository.save(userAnswer);
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
     * 根据问题类别选择评分方式：
     * - 题库类问题：使用 RAG 增强评分（检索知识库中的相关知识）
     * - 非题库类问题（自我介绍、简历相关、开放性、行为面试）：直接使用 LLM 评分
     * 
     * 真实面试模式出题逻辑：
     * - 第 1 题：自我介绍（类别："开场"）
     * - 第 2-11 题（10 题）：简历相关（类别："简历相关"）
     * - 第 12-16 题（5 题）：题库随机（类别："题库"）
     * - 第 17-18 题（2 题）：开放性（类别："开放性"）
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
            // 获取问题类别，决定评分方式
            String category = question.getCategory() != null ? question.getCategory() : "技术知识";
            
            // 判断是否属于题库类问题（使用 RAG 增强评分）
            // 题库类别包括：技术知识、项目经验、行为题、数据库、框架、系统设计等
            // 非题库类别：开场、简历相关、开放性、行为面试
            boolean useRag = shouldUseRagScoring(category);
            
            if (useRag) {
                // 使用 RAG 增强评分
                scoreWithRag(userAnswer, sessionId, question, answer, category);
            } else {
                // 使用纯 LLM 评分（不检索知识库）
                scoreWithLlmOnly(userAnswer, sessionId, question, answer, category);
            }
        }

        return answerRepository.save(userAnswer);
    }
    
    /**
     * 判断是否应该使用 RAG 评分
     * @param category 问题类别
     * @return true 表示使用 RAG 增强评分，false 表示使用纯 LLM 评分
     */
    private boolean shouldUseRagScoring(String category) {
        // 非题库类别，不使用 RAG
        if ("开场".equals(category) || "简历相关".equals(category) || 
            "开放性".equals(category) || "行为面试".equals(category)) {
            return false;
        }
        // 其他类别（技术知识、项目经验、行为题、数据库、框架、系统设计、题库等）使用 RAG
        return true;
    }
    
    /**
     * 使用 RAG 增强评分（检索知识库后评分）
     */
    private void scoreWithRag(Answer userAnswer, Long sessionId, Question question, String answer, String category) {
        int maxRetries = 2;
        int retryCount = 0;
        boolean scored = false;
        
        while (!scored && retryCount <= maxRetries) {
            try {
                InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
                if (session == null) {
                    log.warn("[RAG 评分] 会话不存在 - 会话 ID: {}", sessionId);
                    break;
                }
                
                log.info("[RAG 评分] 开始评分 (重试 {}/{}) - 会话 ID: {}, 岗位：{}, 类别：{}", 
                    retryCount, maxRetries, sessionId, session.getPositionId(), category);
                
                String evalJson = ragService.evaluateAnswerWithRag(
                    session.getPositionId(), 
                    category, 
                    question.getContent(), 
                    answer
                );
                
                log.info("[RAG 评分] LLM 返回结果：{}", evalJson);
                
                JSONObject evalData = parseScoreJson(evalJson);
                
                Integer technicalScore = ensureMinScore(evalData.getInteger("technicalScore"));
                Integer communicationScore = ensureMinScore(evalData.getInteger("communicationScore"));
                Integer logicScore = ensureMinScore(evalData.getInteger("logicScore"));
                Integer knowledgeDepthScore = ensureMinScore(evalData.getInteger("knowledgeDepth"));
                Integer overallScore = ensureMinScore(evalData.getInteger("overallScore"));
                
                // 验证分数是否有效（不能全是默认值 50）
                if (technicalScore != 50 || communicationScore != 50 || 
                    logicScore != 50 || knowledgeDepthScore != 50 || overallScore != 50) {
                    userAnswer.setTechnicalScore(technicalScore);
                    userAnswer.setCommunicationScore(communicationScore);
                    userAnswer.setLogicScore(logicScore);
                    userAnswer.setKnowledgeDepth(knowledgeDepthScore);
                    userAnswer.setOverallScore(overallScore);
                    userAnswer.setEvaluationComment(evalData.getString("evaluationComment"));
                    scored = true;
                    log.info("[RAG 评分] 评分成功 - 会话 ID: {}, 总体分数：{}", sessionId, overallScore);
                } else if (retryCount >= maxRetries) {
                    log.warn("[RAG 评分] 达到最大重试次数，使用默认分数 - 会话 ID: {}", sessionId);
                    scored = true;
                } else {
                    retryCount++;
                    log.warn("[RAG 评分] 评分结果为默认值，{} 秒后重试 ({}/{})", 
                        2 * retryCount, retryCount, maxRetries);
                    Thread.sleep(2000 * retryCount);
                }
                
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("[RAG 评分] 评分被中断 - 会话 ID: {}", sessionId, e);
                    scored = true;
                    break;
                }
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("[RAG 评分] 评分失败，使用默认分数 - 会话 ID: {}, 错误：{}", 
                        sessionId, e.getMessage(), e);
                    scored = true;
                } else {
                    log.warn("[RAG 评分] 评分失败，{} 秒后重试 ({}/{}) - 错误：{}", 
                        2 * retryCount, retryCount, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(2000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        scored = true;
                    }
                }
            }
        }
        
        // 如果最终失败，给中等分数 50 分
        if (!scored || userAnswer.getOverallScore() == null) {
            userAnswer.setTechnicalScore(50);
            userAnswer.setCommunicationScore(50);
            userAnswer.setLogicScore(50);
            userAnswer.setKnowledgeDepth(50);
            userAnswer.setOverallScore(50);
            userAnswer.setEvaluationComment("评分系统暂时不可用，已使用默认分数。建议检查网络连接或 API 配置。");
        }
    }
    
    /**
     * 使用纯 LLM 评分（不检索知识库，适用于非题库类问题）
     * 用于自我介绍、简历相关问题、开放性问题、行为面试等
     */
    private void scoreWithLlmOnly(Answer userAnswer, Long sessionId, Question question, String answer, String category) {
        int maxRetries = 2;
        int retryCount = 0;
        boolean scored = false;
        
        while (!scored && retryCount <= maxRetries) {
            try {
                InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
                if (session == null) {
                    log.warn("[LLM 评分] 会话不存在 - 会话 ID: {}", sessionId);
                    break;
                }
                
                log.info("[LLM 评分] 开始评分 (重试 {}/{}) - 会话 ID: {}, 类别：{}", 
                    retryCount, maxRetries, sessionId, category);
                
                // 构建评分提示词
                String prompt = buildLlmScoringPrompt(question.getContent(), answer, category, session.getPositionId());
                
                String evalJson = llmService.simpleChat(prompt, 
                    "你是一位专业的面试官，擅长评估候选人的回答质量。请根据评分标准给出客观公正的分数。");
                
                log.info("[LLM 评分] LLM 返回结果：{}", evalJson);
                
                JSONObject evalData = parseScoreJson(evalJson);
                
                Integer technicalScore = ensureMinScore(evalData.getInteger("technicalScore"));
                Integer communicationScore = ensureMinScore(evalData.getInteger("communicationScore"));
                Integer logicScore = ensureMinScore(evalData.getInteger("logicScore"));
                Integer knowledgeDepthScore = ensureMinScore(evalData.getInteger("knowledgeDepth"));
                Integer overallScore = ensureMinScore(evalData.getInteger("overallScore"));
                
                // 验证分数是否有效
                if (technicalScore != 50 || communicationScore != 50 || 
                    logicScore != 50 || knowledgeDepthScore != 50 || overallScore != 50) {
                    userAnswer.setTechnicalScore(technicalScore);
                    userAnswer.setCommunicationScore(communicationScore);
                    userAnswer.setLogicScore(logicScore);
                    userAnswer.setKnowledgeDepth(knowledgeDepthScore);
                    userAnswer.setOverallScore(overallScore);
                    userAnswer.setEvaluationComment(evalData.getString("evaluationComment"));
                    scored = true;
                    log.info("[LLM 评分] 评分成功 - 会话 ID: {}, 总体分数：{}", sessionId, overallScore);
                } else if (retryCount >= maxRetries) {
                    log.warn("[LLM 评分] 达到最大重试次数，使用默认分数 - 会话 ID: {}", sessionId);
                    scored = true;
                } else {
                    retryCount++;
                    log.warn("[LLM 评分] 评分结果为默认值，{} 秒后重试 ({}/{})", 
                        2 * retryCount, retryCount, maxRetries);
                    Thread.sleep(2000 * retryCount);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[LLM 评分] 评分被中断 - 会话 ID: {}", sessionId, e);
                scored = true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("[LLM 评分] 评分失败，使用默认分数 - 会话 ID: {}, 错误：{}", 
                        sessionId, e.getMessage(), e);
                    scored = true;
                } else {
                    log.warn("[LLM 评分] 评分失败，{} 秒后重试 ({}/{}) - 错误：{}", 
                        2 * retryCount, retryCount, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(2000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        scored = true;
                    }
                }
            }
        }
        
        // 如果最终失败，给中等分数 50 分
        if (!scored || userAnswer.getOverallScore() == null) {
            userAnswer.setTechnicalScore(50);
            userAnswer.setCommunicationScore(50);
            userAnswer.setLogicScore(50);
            userAnswer.setKnowledgeDepth(50);
            userAnswer.setOverallScore(50);
            userAnswer.setEvaluationComment("评分系统暂时不可用，已使用默认分数。");
        }
    }
    
    /**
     * 构建纯 LLM 评分的提示词
     */
    private String buildLlmScoringPrompt(String question, String answer, String category, String positionId) {
        String positionName = getPositionName(positionId);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的").append(positionName).append("岗位面试官。\n\n");
        prompt.append("请对候选人的回答进行评分，评分维度包括：\n");
        prompt.append("1. technicalScore（技术分数 0-100）：考察技术深度和专业性\n");
        prompt.append("2. communicationScore（沟通分数 0-100）：考察表达清晰度和逻辑性\n");
        prompt.append("3. logicScore（逻辑分数 0-100）：考察回答的结构和条理性\n");
        prompt.append("4. knowledgeDepth（知识深度 0-100）：考察知识广度和深度\n");
        prompt.append("5. overallScore（综合分数 0-100）：综合以上维度的整体评价\n\n");
        
        // 根据类别调整评分侧重点
        if ("开场".equals(category)) {
            prompt.append("【评分侧重点】这是自我介绍环节，请重点考察：\n");
            prompt.append("- 表达是否清晰流畅\n");
            prompt.append("- 是否突出了个人优势和亮点\n");
            prompt.append("- 是否与岗位匹配度高\n");
        } else if ("简历相关".equals(category)) {
            prompt.append("【评分侧重点】这是基于简历的提问，请重点考察：\n");
            prompt.append("- 是否使用 STAR 法则清晰描述了项目经历\n");
            prompt.append("- 是否突出了个人贡献和成果\n");
            prompt.append("- 回答是否具体、有说服力\n");
        } else if ("开放性".equals(category)) {
            prompt.append("【评分侧重点】这是开放性问题，请重点考察：\n");
            prompt.append("- 思考是否有深度和广度\n");
            prompt.append("- 观点是否清晰、有逻辑\n");
            prompt.append("- 是否展现了良好的职业素养\n");
        } else if ("行为面试".equals(category)) {
            prompt.append("【评分侧重点】这是行为面试问题，请重点考察：\n");
            prompt.append("- 是否使用 STAR 法则描述具体事例\n");
            prompt.append("- 是否展现了良好的软技能\n");
            prompt.append("- 回答是否真实、有说服力\n");
        }
        
        prompt.append("\n【问题】\n").append(question);
        prompt.append("\n\n【候选人回答】\n").append(answer);
        prompt.append("\n\n请返回 JSON 格式的评分结果，包含以下字段：\n");
        prompt.append("technicalScore, communicationScore, logicScore, knowledgeDepth, overallScore, evaluationComment\n\n");
        prompt.append("evaluationComment 请包含：\n");
        prompt.append("1. 优点（strengths）\n");
        prompt.append("2. 不足（weaknesses）\n");
        prompt.append("3. 改进建议（suggestions）\n\n");
        prompt.append("直接返回 JSON，不要有任何前缀说明。");
        
        return prompt.toString();
    }
    
    /**
     * 根据岗位 ID 获取岗位名称
     */
    private String getPositionName(String positionId) {
        return switch (positionId != null ? positionId : "") {
            case "backend" -> "后端开发工程师";
            case "frontend" -> "前端开发工程师";
            case "qa" -> "测试工程师";
            case "algorithm" -> "算法工程师";
            default -> "技术";
        };
    }

    /**
     * 确保分数在有效范围内 (0-100)
     */
    private Integer ensureMinScore(Integer score) {
        if (score == null) {
            return 50;
        }
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
     * 真实面试模式出题逻辑：
     * - 第 1 题：自我介绍
     * - 第 2-11 题（10 题）：围绕简历实践经历提问（使用 STAR 法则）
     * - 第 12-16 题（5 题）：题库随机抽取
     * - 第 17-18 题（2 题）：开放性问题
     */
    private Question getNextQuestion(InterviewSession session, int questionIndex) {
        // 真实面试模式使用新的出题逻辑
        if ("REAL".equals(session.getInterviewMode())) {
            return getRealInterviewQuestion(session, questionIndex);
        }
        
        // 练习模式保持原有逻辑
        int difficulty = 1;
        if (questionIndex > 2) difficulty = 2;
        if (questionIndex > 5) difficulty = 3;
        if (questionIndex > 8) difficulty = 4;

        String[] categories = {"技术知识", "项目经验", "行为题"};
        String category = categories[questionIndex % categories.length];

        List<Question> questions = questionService.findByPositionIdAndCategory(
            session.getPositionId(), category);

        if (questions.isEmpty()) {
            questions = questionService.findByPositionId(session.getPositionId());
        }

        if (questions.isEmpty()) {
            return generateAiQuestion(session.getPositionId(), category, difficulty);
        }

        return questions.get(questionIndex % questions.size());
    }
    
    /**
     * 真实面试模式出题逻辑
     * @param session 面试会话
     * @param questionIndex 当前题号（从 0 开始）
     * @return 下一个问题
     */
    private Question getRealInterviewQuestion(InterviewSession session, int questionIndex) {
        Position position = positionService.findByCode(session.getPositionId()).orElse(null);
        String positionName = position != null ? position.getName() : "技术岗位";
        
        // 第 1 题：自我介绍（questionIndex = 0）
        if (questionIndex == 0) {
            Question q = new Question();
            q.setPositionId(session.getPositionId());
            q.setCategory("开场");
            q.setContent("您好，请先做一个简短的自我介绍吧。");
            q.setDifficultyLevel(1);
            return q;
        }
        
        // 第 2-11 题（questionIndex = 1~10）：围绕简历实践经历提问
        if (questionIndex >= 1 && questionIndex <= 10) {
            return getResumeBasedQuestion(session, positionName, questionIndex);
        }
        
        // 第 12-16 题（questionIndex = 11~15）：题库随机抽取
        if (questionIndex >= 11 && questionIndex <= 15) {
            return getRandomQuestionFromBank(session.getPositionId());
        }
        
        // 第 17-18 题（questionIndex = 16~17）：开放性问题
        if (questionIndex >= 16 && questionIndex <= 17) {
            return getOpenQuestion(session, positionName, questionIndex);
        }
        
        // 默认返回 AI 生成问题
        return generateAiQuestion(session.getPositionId(), "技术知识", 3);
    }
    
    /**
     * 基于简历内容的提问（第 2-11 题）
     * 使用 STAR 法则或行为面试法，围绕实习经历、项目经历提问
     * 增加重试机制和更好的错误处理
     */
    private Question getResumeBasedQuestion(InterviewSession session, String positionName, int questionIndex) {
        int maxRetries = 2;
        int retryCount = 0;
        
        while (retryCount <= maxRetries) {
            try {
                // 获取用户简历内容
                Optional<String> resumeContentOpt = resumeService.getUserResumeContent(session.getUserId());
                
                if (resumeContentOpt.isPresent()) {
                    String resumeContent = resumeContentOpt.get();
                    
                    // 获取之前的回答历史，用于生成追问
                    String conversationHistory = session.getConversationHistory();
                    
                    // 构建提示词，让大模型基于简历内容生成问题
                    String prompt = String.format(
                        "你是一位专业的 %s 岗位面试官，正在对候选人进行面试。\n" +
                        "以下是候选人的简历内容：\n---\n%s\n---\n\n" +
                        "之前的面试对话历史：\n---\n%s\n---\n\n" +
                        "这是面试的第 %d 题（共 10 题围绕简历的提问，题号从 1 到 10）。\n" +
                        "请根据以下规则生成问题：\n" +
                        "1. 如果简历中有实习经历、项目经历或其他实践内容，请针对这些内容使用 STAR 法则（Situation 情境、Task 任务、Action 行动、Result 结果）进行提问。\n" +
                        "2. 如果简历中没有明显的实践经历，请使用行为面试法，通过假设性提问来考察候选人的能力。\n" +
                        "3. 如果之前已经有过相关提问，请避免重复，可以从不同角度深入追问。\n" +
                        "4. 问题应该具体、有针对性，能够考察候选人的实际能力和经验。\n" +
                        "5. 第 1-3 题可以侧重基本情况了解，第 4-7 题深入具体项目，第 8-10 题可以进行行为面试或追问。\n\n" +
                        "请直接返回问题内容，不要有任何前缀说明。");
                    
                    log.info("[简历问题] 开始生成问题 (重试 {}/{}) - 会话 ID: {}, 用户 ID: {}", 
                        retryCount, maxRetries, session.getId(), session.getUserId());
                    
                    String questionContent = llmService.simpleChat(prompt, 
                        "你是一位专业的面试官，擅长使用 STAR 法则和行为面试法进行面试。");
                    
                    if (questionContent != null && !questionContent.trim().isEmpty()) {
                        Question q = new Question();
                        q.setPositionId(session.getPositionId());
                        q.setCategory("简历相关");
                        q.setContent(questionContent.trim());
                        q.setDifficultyLevel(2 + (questionIndex - 1) / 3); // 难度递增
                        log.info("[简历问题] 问题生成成功 - 会话 ID: {}", session.getId());
                        return q;
                    } else {
                        log.warn("[简历问题] LLM 返回空内容，重试 ({}/{})", retryCount + 1, maxRetries);
                    }
                } else {
                    log.warn("[简历问题] 未找到简历内容 - 用户 ID: {}", session.getUserId());
                    // 简历不存在，直接使用备用方案，不重试
                    return getBehavioralQuestion(session, positionName, questionIndex);
                }
                
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("[简历问题] 生成被中断 - 会话 ID: {}", session.getId(), e);
                    return getBehavioralQuestion(session, positionName, questionIndex);
                }
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("[简历问题] 生成失败，使用备用方案 - 会话 ID: {}, 错误：{}", 
                        session.getId(), e.getMessage(), e);
                } else {
                    log.warn("[简历问题] 生成失败，{} 秒后重试 ({}/{}) - 错误：{}", 
                        2 * retryCount, retryCount, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(2000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return getBehavioralQuestion(session, positionName, questionIndex);
                    }
                }
            }
        }
        
        // 所有重试失败，使用备用方案
        return getBehavioralQuestion(session, positionName, questionIndex);
    }
    
    /**
     * 行为面试法提问（当简历内容无法解析时使用）
     */
    private Question getBehavioralQuestion(InterviewSession session, String positionName, int questionIndex) {
        String[] behavioralQuestions = {
            "请描述一次你在团队中遇到冲突的经历，你是如何处理的？",
            "请举例说明你是如何在压力下完成一项重要任务的？",
            "描述一次你主动承担责任并解决问题的经历。",
            "请分享一次你从失败中学习的经历，你学到了什么？",
            "描述一次你需要快速学习新知识或技能来完成任务的经历。",
            "请举例说明你是如何设定目标并达成它的？",
            "描述一次你提出创新想法并付诸实践的经历。",
            "请分享一次你帮助团队成员克服困难的经历。",
            "描述一次你处理多任务并确定优先级的经历。",
            "请举例说明你是如何说服他人接受你的观点的？"
        };
        
        Question q = new Question();
        q.setPositionId(session.getPositionId());
        q.setCategory("行为面试");
        q.setContent(behavioralQuestions[(questionIndex - 1) % behavioralQuestions.length]);
        q.setDifficultyLevel(2);
        return q;
    }
    
    /**
     * 从题库随机抽取问题（第 12-16 题）
     */
    private Question getRandomQuestionFromBank(String positionId) {
        List<Question> allQuestions = questionService.findByPositionId(positionId);
        
        if (allQuestions.isEmpty()) {
            // 如果题库为空，生成技术问题
            return generateAiQuestion(positionId, "技术知识", 3);
        }
        
        // 随机选择一个问题
        Random random = new Random();
        Question selected = allQuestions.get(random.nextInt(allQuestions.size()));
        
        Question q = new Question();
        q.setPositionId(positionId);
        q.setCategory("题库");
        q.setContent(selected.getContent());
        q.setDifficultyLevel(selected.getDifficultyLevel());
        return q;
    }
    
    /**
     * 开放性问题（第 17-18 题）
     */
    private Question getOpenQuestion(InterviewSession session, String positionName, int questionIndex) {
        String[] openQuestions = {
            "你对我们公司的这个岗位有什么了解？为什么选择应聘这个岗位？",
            "你对这个岗位的未来发展有什么期望？你希望在这个岗位上实现什么目标？",
            "你认为自己最大的优势和劣势是什么？它们如何影响你的工作？",
            "你如何看待团队合作？请分享一次你成功协作的经历。",
            "你对自己的职业发展有什么规划？这个岗位如何帮助你实现目标？"
        };
        
        String[] openQuestionsAlt = {
            "你认为一个优秀的 " + positionName + " 应该具备哪些核心能力？",
            "请谈谈你对行业技术发展趋势的看法。",
            "你如何平衡工作质量和工作效率？",
            "描述一下你理想中的工作环境和团队氛围。",
            "如果工作中遇到技术难题，你通常会如何解决？"
        };
        
        Question q = new Question();
        q.setPositionId(session.getPositionId());
        q.setCategory("开放性");
        
        // 第 17 题使用第一组，第 18 题使用第二组
        if (questionIndex == 16) {
            q.setContent(openQuestions[new Random().nextInt(openQuestions.length)]);
        } else {
            q.setContent(openQuestionsAlt[new Random().nextInt(openQuestionsAlt.length)]);
        }
        
        q.setDifficultyLevel(2);
        return q;
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
     * 基于各题平均分和大模型分析生成评估报告
     */
    @Transactional
    public EvaluationReport generateEvaluationReportFromScores(InterviewSession session) {
        List<Answer> answers = answerRepository.findBySessionIdOrderByQuestionId(session.getId());
        
        if (answers.isEmpty()) {
            return createDefaultReport(session);
        }

        int totalTechnical = 0;
        int totalCommunication = 0;
        int totalLogic = 0;
        int totalKnowledge = 0;
        int totalOverall = 0;
        int answeredCount = 0;
        int skippedCount = 0;
        
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
            }
        }
        
        int avgTechnical = answeredCount > 0 ? totalTechnical / answeredCount : 10;
        int avgCommunication = answeredCount > 0 ? totalCommunication / answeredCount : 10;
        int avgLogic = answeredCount > 0 ? totalLogic / answeredCount : 10;
        int avgKnowledge = answeredCount > 0 ? totalKnowledge / answeredCount : 10;
        int avgOverall = answeredCount > 0 ? totalOverall / answeredCount : 10;
        
        int skipPenalty = skippedCount * 2;
        avgOverall = Math.max(10, avgOverall - skipPenalty);
        
        String strengths = "候选人完成了面试";
        String weaknesses = "部分问题可以回答得更深入";
        String suggestions = "建议继续学习和实践";
        String recommendedResources = "推荐学习相关技术文档和开源项目";
        
        try {
            Position position = positionService.findByCode(session.getPositionId()).orElse(null);
            String positionName = position != null ? position.getName() : "技术岗位";
            
            log.info("[大模型评估报告] 开始生成评估报告 - 会话 ID: {}, 岗位：{}", session.getId(), positionName);
            
            String evalJson = llmService.generateEvaluationReport(
                session.getConversationHistory(), 
                positionName
            );
            
            log.info("[大模型评估报告] LLM 返回结果：{}", evalJson);
            
            JSONObject evalData = parseEvaluationJson(evalJson);
            
            strengths = evalData.getString("strengths");
            weaknesses = evalData.getString("weaknesses");
            suggestions = evalData.getString("suggestions");
            recommendedResources = evalData.getString("recommendedResources");
            
            if (strengths == null || strengths.isEmpty()) {
                strengths = "候选人完成了面试";
            }
            if (weaknesses == null || weaknesses.isEmpty()) {
                weaknesses = "部分问题可以回答得更深入";
            }
            if (suggestions == null || suggestions.isEmpty()) {
                suggestions = "建议继续学习和实践";
            }
            if (recommendedResources == null || recommendedResources.isEmpty()) {
                recommendedResources = "推荐学习相关技术文档和开源项目";
            }
            
            log.info("[大模型评估报告] 解析成功 - strengths: {}, weaknesses: {}", 
                strengths.length() > 50 ? strengths.substring(0, 50) + "..." : strengths,
                weaknesses.length() > 50 ? weaknesses.substring(0, 50) + "..." : weaknesses);
                
        } catch (Exception e) {
            log.error("[大模型评估报告] 生成失败，使用默认反馈文本", e);
        }
        
        EvaluationReport report = new EvaluationReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setPositionId(session.getPositionId());
        report.setTechnicalScore(avgTechnical);
        report.setCommunicationScore(avgCommunication);
        report.setLogicScore(avgLogic);
        report.setAdaptabilityScore(avgKnowledge);
        report.setMatchingScore(avgOverall);
        report.setOverallScore(avgOverall);
        report.setStrengths(strengths);
        report.setWeaknesses(weaknesses);
        report.setSuggestions(suggestions);
        report.setRecommendedResources(recommendedResources);
        report.setSpeechRate("NORMAL");
        report.setClarity("GOOD");
        report.setConfidence("NORMAL");
        
        if ("REAL".equals(session.getInterviewMode())) {
            try {
                log.info("[情绪分析] 真实面试模式，开始分析情绪状态 - 会话 ID: {}", session.getId());
                
                AudioAnalysisService.AudioAnalysisResult emotionResult = 
                    audioAnalysisService.analyzeEmotionAndScore(
                        session.getConversationHistory(), 
                        "请根据以下面试对话历史分析候选人的情绪状态"
                    );
                
                report.setEmotion(emotionResult.getEmotion());
                report.setEmotionAnalysis(emotionResult.getEmotionAnalysis());
                report.setConfidenceLevel(emotionResult.getConfidenceLevel());
                report.setTranscript(emotionResult.getTranscript());
                
                log.info("[情绪分析] 情绪分析完成 - 情绪：{}, 自信程度：{}", 
                    emotionResult.getEmotion(), emotionResult.getConfidenceLevel());
                    
            } catch (Exception e) {
                log.error("[情绪分析] 分析失败，使用默认值", e);
                report.setEmotion("NORMAL");
                report.setEmotionAnalysis("情绪状态分析暂时不可用");
                report.setConfidenceLevel(3);
            }
        } else {
            report.setEmotion("NORMAL");
            report.setEmotionAnalysis("练习模式下不进行情绪分析");
            report.setConfidenceLevel(3);
        }

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

        String evalJson = llmService.generateEvaluationReport(
            session.getConversationHistory(), 
            positionName
        );

        JSONObject evalData = parseEvaluationJson(evalJson);

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
            int start = jsonStr.indexOf("{");
            int end = jsonStr.lastIndexOf("}");
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }
            
            JSONObject json = JSON.parseObject(jsonStr);
            
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
        List<Answer> answers = answerRepository.findBySessionIdOrderByQuestionId(sessionId);
        if (!answers.isEmpty()) {
            answerRepository.deleteAll(answers);
        }
        
        Optional<EvaluationReport> report = reportRepository.findBySessionId(sessionId);
        report.ifPresent(reportRepository::delete);
        
        sessionRepository.deleteById(sessionId);
    }

    /**
     * 主动退出面试（不保存任何记录）
     */
    @Transactional
    public void exitInterview(Long sessionId) {
        log.info("[退出面试] 开始退出面试会话，会话 ID: {}", sessionId);
        deleteInterviewSession(sessionId);
        log.info("[退出面试] 面试会话已删除，会话 ID: {}", sessionId);
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
    
    /**
     * 获取真实面试模式下的问题（供 Controller 调用）
     * @param sessionId 面试会话 ID
     * @param questionIndex 问题索引（从 0 开始）
     * @return 问题对象
     */
    public Question getQuestionForRealInterview(Long sessionId, int questionIndex) {
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在：" + sessionId));
        
        if (!"REAL".equals(session.getInterviewMode())) {
            // 非真实面试模式，返回练习模式问题
            return getNextQuestion(session, questionIndex);
        }
        
        // 真实面试模式，使用真实面试出题逻辑
        return getRealInterviewQuestion(session, questionIndex);
    }
}
