package com.interviewassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.BusinessException;
import com.interviewassistant.dto.InterviewDetailResponse;
import com.interviewassistant.dto.InterviewSessionResponse;
import com.interviewassistant.dto.QuestionResponse;
import com.interviewassistant.entity.InterviewQuestion;
import com.interviewassistant.entity.InterviewSession;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.InterviewQuestionRepository;
import com.interviewassistant.repository.InterviewSessionRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import com.interviewassistant.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模拟面试服务实现。
 *
 * <p>核心流程：
 * <ol>
 *   <li>开始面试 → AI 生成开场白欢迎词 → AI 生成全部题目 → 推送第一题</li>
 *   <li>用户作答 → 保存回答 → 推送下一题 / 触发总评</li>
 *   <li>全部答完 / 终止 → AI 评分 + 总评 + 建议</li>
 * </ol>
 *
 * <p>SSE 事件：greeting, session_created, question, answer_saved,
 * evaluating, final_evaluation, terminated, error。</p>
 */
@Service
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_TERMINATED = "TERMINATED";
    private static final String STATUS_EVALUATING = "EVALUATING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    @Value("${interview.default-question-count:5}")
    private int defaultQuestionCount;

    public InterviewServiceImpl(InterviewSessionRepository sessionRepository,
                                 InterviewQuestionRepository questionRepository,
                                 UserRepository userRepository,
                                 AIService aiService,
                                 ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    // ==================== 开始面试 ====================

    @Override
    public void startInterview(Long userId, String position, SseEmitter emitter) {
        InterviewSession session;
        List<InterviewQuestion> questions;
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));

            // 1. 创建面试会话
            session = InterviewSession.builder()
                    .user(user)
                    .position(position)
                    .status(STATUS_IN_PROGRESS)
                    .totalQuestions(defaultQuestionCount)
                    .answeredCount(0)
                    .startedAt(LocalDateTime.now())
                    .build();
            session = sessionRepository.save(session);

            log.info("面试会话创建: userId={}, sessionId={}, position={}", userId, session.getId(), position);

            // 2. AI 生成开场欢迎词
            String greetingPrompt = buildGreetingPrompt(position, session.getTotalQuestions());
            log.info("生成开场白: sessionId={}", session.getId());
            String greeting = aiService.chat(greetingPrompt);
            emitter.send(SseEmitter.event()
                    .name("greeting")
                    .data(Map.of("message", greeting)));

            // 3. 只生成首题；后续题目在每次评分后按薄弱点动态生成。
            InterviewQuestion firstQuestion = generateNextQuestion(session, List.of());
            questions = List.of(questionRepository.save(firstQuestion));

            log.info("面试首题生成完成: sessionId={}", session.getId());

            // 5. 推送会话创建事件
            emitter.send(SseEmitter.event()
                    .name("session_created")
                    .data(Map.of(
                            "sessionId", session.getId(),
                            "position", position,
                            "totalQuestions", session.getTotalQuestions())));

            // 6. 推送第一题
            firstQuestion = questions.get(0);
            emitter.send(SseEmitter.event()
                    .name("question")
                    .data(Map.of(
                            "questionNumber", firstQuestion.getQuestionNumber(),
                            "questionText", firstQuestion.getQuestionText(),
                            "category", firstQuestion.getCategory() != null ? firstQuestion.getCategory() : "",
                            "difficulty", firstQuestion.getDifficulty() != null ? firstQuestion.getDifficulty() : "",
                            "totalQuestions", session.getTotalQuestions())));

            emitter.complete();

        } catch (BusinessException e) {
            log.warn("开始面试失败: userId={}, position={}, msg={}", userId, position, e.getMessage());
            sendErrorAndComplete(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("开始面试异常: userId={}, position={}", userId, position, e);
            sendErrorAndComplete(emitter, "面试启动失败：" + e.getMessage());
        }
    }

    // ==================== 提交回答 ====================

    @Override
    public void submitAnswer(Long userId, Long sessionId, String answer, SseEmitter emitter) {
        try {
            // 1. 校验会话
            InterviewSession session = findAndValidateSession(userId, sessionId);
            if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
                throw new BusinessException("面试已结束，无法提交回答");
            }

            // 2. 查找当前待回答的题目
            List<InterviewQuestion> questions = questionRepository
                    .findBySessionIdOrderByQuestionNumberAsc(sessionId);
            InterviewQuestion currentQuestion = questions.stream()
                    .filter(q -> q.getUserAnswer() == null)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("所有题目已作答完毕"));

            // 3. 保存回答并立即评分
            currentQuestion.setUserAnswer(answer);
            currentQuestion.setAnsweredAt(LocalDateTime.now());
            scoreAnswer(session, currentQuestion);
            questionRepository.save(currentQuestion);

            int newAnsweredCount = session.getAnsweredCount() + 1;
            session.setAnsweredCount(newAnsweredCount);
            sessionRepository.save(session);

            log.info("回答已保存: sessionId={}, questionNumber={}, answeredCount={}/{}",
                    sessionId, currentQuestion.getQuestionNumber(), newAnsweredCount, session.getTotalQuestions());

            // 4. 推送持久化后的逐题评分
            emitter.send(SseEmitter.event()
                    .name("answer_saved")
                    .data(answerResult(currentQuestion, newAnsweredCount, session.getTotalQuestions())));

            // 5. 根据累计得分和薄弱点决定继续或结束
            questions = questionRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId);
            if (shouldFinish(questions, newAnsweredCount, session.getTotalQuestions())) {
                emitter.send(SseEmitter.event().name("interview_decision")
                        .data(Map.of("action", "FINISH", "reason", buildFinishReason(questions))));
                generateFinalEvaluation(session, questions, emitter, false);
            } else {
                InterviewQuestion nextQuestion = questionRepository.save(generateNextQuestion(session, questions));
                String focus = weakestCategory(questions);
                emitter.send(SseEmitter.event().name("interview_decision")
                        .data(Map.of("action", "CONTINUE", "reason", "继续考察当前薄弱领域", "focus", focus)));
                emitter.send(SseEmitter.event()
                        .name("question")
                        .data(Map.of(
                                "questionNumber", nextQuestion.getQuestionNumber(),
                                "questionText", nextQuestion.getQuestionText(),
                                "category", nextQuestion.getCategory() != null ? nextQuestion.getCategory() : "",
                                "difficulty", nextQuestion.getDifficulty() != null ? nextQuestion.getDifficulty() : "",
                                "totalQuestions", session.getTotalQuestions(),
                                "adaptiveFocus", focus)));
                emitter.complete();
            }

        } catch (BusinessException e) {
            log.warn("提交回答失败: userId={}, sessionId={}, msg={}", userId, sessionId, e.getMessage());
            sendErrorAndComplete(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("提交回答异常: userId={}, sessionId={}", userId, sessionId, e);
            sendErrorAndComplete(emitter, "提交回答失败：" + e.getMessage());
        }
    }

    // ==================== 终止面试 ====================

    @Override
    public void terminateInterview(Long userId, Long sessionId, SseEmitter emitter) {
        try {
            InterviewSession session = findAndValidateSession(userId, sessionId);
            if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
                emitter.send(SseEmitter.event()
                        .name("terminated")
                        .data(Map.of("message", "面试已结束",
                                "answeredCount", session.getAnsweredCount())));
                emitter.complete();
                return;
            }

            int answeredCount = session.getAnsweredCount();
            session.setStatus(STATUS_TERMINATED);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);

            log.info("面试已终止: sessionId={}, answeredCount={}", sessionId, answeredCount);

            emitter.send(SseEmitter.event()
                    .name("terminated")
                    .data(Map.of(
                            "message", "面试已终止",
                            "answeredCount", answeredCount,
                            "totalQuestions", session.getTotalQuestions())));

            // 如有已回答的题目，生成阶段性评价
            if (answeredCount > 0) {
                List<InterviewQuestion> questions = questionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(sessionId);
                generateFinalEvaluation(session, questions, emitter, true);
            } else {
                emitter.complete();
            }

        } catch (BusinessException e) {
            log.warn("终止面试失败: userId={}, sessionId={}, msg={}", userId, sessionId, e.getMessage());
            sendErrorAndComplete(emitter, e.getMessage());
        } catch (Exception e) {
            log.error("终止面试异常: userId={}, sessionId={}", userId, sessionId, e);
            sendErrorAndComplete(emitter, "终止面试失败：" + e.getMessage());
        }
    }

    // ==================== 查询接口 ====================

    @Override
    public List<InterviewSessionResponse> getSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InterviewDetailResponse getSessionDetail(Long userId, Long sessionId) {
        InterviewSession session = findAndValidateSession(userId, sessionId);
        List<InterviewQuestion> questions = questionRepository
                .findBySessionIdOrderByQuestionNumberAsc(sessionId);

        InterviewDetailResponse detail = InterviewDetailResponse.builder()
                .id(session.getId())
                .position(session.getPosition())
                .status(session.getStatus())
                .totalQuestions(session.getTotalQuestions())
                .answeredCount(session.getAnsweredCount())
                .overallScore(session.getOverallScore())
                .overallComment(session.getOverallComment())
                .studyAdvice(session.getStudyAdvice())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .questions(questions.stream().map(this::toQuestionResponse).collect(Collectors.toList()))
                .build();
        return detail;
    }

    // ==================== 私有方法：AI 交互 ====================

    /**
     * 构建开场欢迎词 Prompt。
     *
     * <p>让 AI 以面试官身份欢迎候选人、确认岗位、介绍流程。</p>
     */
    private String buildGreetingPrompt(String position, int questionCount) {
        return String.format("""
                你是一位经验丰富、态度友善的技术面试官。

                今天你正在面试一位 %s 岗位的候选人。

                请你用专业但友好的语气说一段开场白，包含以下要素：
                1. 欢迎语（1句话）
                2. 确认面试岗位："今天面试的岗位是 %s"（1句话）
                3. 面试流程说明：共 %d 道题，难度逐渐增加，覆盖基础知识→项目经验→系统设计，逐题作答（1-2句话）
                4. 鼓励："准备好了吗？我们开始第一题。"（1句话）

                要求：
                - 总共 3-5 句话，简洁不啰嗦
                - 语气专业、友善、鼓励
                - 纯文本输出，不要 markdown
                - 不要输出"面试官："等前缀
                - 不要使用任何 JSON 格式
                """, position, position, questionCount);
    }

    /**
     * 构建题目生成 Prompt。
     *
     * <p>按难度递进生成题目：基础(2题) → 进阶(2题) → 综合设计(1题)，
     * 覆盖基础知识、项目经验、系统设计、问题解决四个维度。</p>
     */
    private String buildQuestionGenerationPrompt(String position, int questionCount) {
        return String.format("""
                你是一位专业的 %s 岗位技术面试官。

                请为该岗位生成恰好 %d 道面试题。

                ## 难度分配（必须严格遵守）
                - 第 1-2 题：基础题 — 考察岗位核心基础知识（如核心概念、基本原理）
                - 第 3-4 题：进阶题 — 考察实际应用和深入理解（如技术选型、性能优化、常见陷阱）
                - 第 5 题：场景/系统设计题 — 考察综合能力（如架构设计、问题排查、方案对比）

                ## 维度覆盖（尽量覆盖以下 4 类）
                - 基础知识：核心概念、原理
                - 项目经验：实际开发中遇到的问题
                - 系统设计：架构设计、技术选型
                - 问题解决：故障排查、性能优化

                ## 题目要求
                - 具体、可回答，有明确考察点
                - 避免"请介绍一下XXX"这种过于宽泛的问法
                - 追问式题目更好（如"在XXX场景下，你会怎么做？为什么？"）
                - 场景题应贴近真实工作场景

                ## 输出格式（极其重要）
                以严格的 JSON 数组输出，不要任何其他文字，不要 markdown 代码块标记：

                [
                  {
                    "questionNumber": 1,
                    "question": "题目具体内容",
                    "category": "基础知识",
                    "difficulty": "基础",
                    "examPoints": ["考察点1", "考察点2"]
                  }
                ]

                必须恰好 %d 道题。
                """, position, questionCount, questionCount);
    }

    /** 根据已有表现生成唯一的下一题。 */
    private InterviewQuestion generateNextQuestion(InterviewSession session,
                                                    List<InterviewQuestion> previousQuestions)
            throws JsonProcessingException {
        int number = previousQuestions.size() + 1;
        String history = previousQuestions.stream()
                .map(q -> String.format("第%d题[%s/%s]：%s；得分：%s；点评：%s",
                        q.getQuestionNumber(), safe(q.getCategory()), safe(q.getDifficulty()),
                        q.getQuestionText(), q.getScore() == null ? "未评分" : q.getScore(),
                        safe(q.getComment())))
                .collect(Collectors.joining("\n"));
        String focus = previousQuestions.isEmpty() ? "岗位核心基础" : weakestCategory(previousQuestions);
        String prompt = String.format("""
                你是%s岗位的技术面试官。请生成第%d题，只生成一题。
                本题优先考察：%s。根据候选人已有表现调整难度：低分时换一个角度验证薄弱点，高分时提升为应用或系统设计题。
                不得重复已问题目。

                已有记录：
                %s

                仅输出严格 JSON：
                {"questionNumber":%d,"question":"题目","category":"基础知识|项目经验|系统设计|问题解决","difficulty":"基础|进阶|综合"}
                """, session.getPosition(), number, focus,
                history.isBlank() ? "暂无，这是首题。" : history, number);
        JsonNode node = extractJsonObject(aiService.chat(prompt));
        String text = node.path("question").asText("").trim();
        if (text.isEmpty()) throw new BusinessException("AI 生成题目失败，请重试");
        return InterviewQuestion.builder()
                .session(session).questionNumber(number).questionText(text)
                .category(node.path("category").asText("基础知识"))
                .difficulty(node.path("difficulty").asText(number <= 2 ? "基础" : "进阶"))
                .build();
    }

    /** 逐题评分，评分结果与用户回答在推送下一题前一起持久化。 */
    private void scoreAnswer(InterviewSession session, InterviewQuestion question)
            throws JsonProcessingException {
        String prompt = String.format("""
                你是%s岗位面试官。请对以下回答立即评分。
                题目：%s
                类别/%s：%s/%s
                回答：%s
                评分标准：90-100全面深入，75-89正确但欠深入，60-74基本正确但不完整，40-59偏差较大，0-39基本不会。
                仅输出严格 JSON：
                {"score":0,"comment":"具体亮点和不足，30-80字","referenceAnswer":"参考答案要点，50-150字"}
                """, session.getPosition(), question.getQuestionText(),
                question.getCategory(), question.getCategory(), question.getDifficulty(), question.getUserAnswer());
        JsonNode result = extractJsonObject(aiService.chat(prompt));
        double score = Math.max(0, Math.min(100, result.path("score").asDouble(0)));
        question.setScore(BigDecimal.valueOf(score));
        question.setComment(result.path("comment").asText(""));
        question.setReferenceAnswer(result.path("referenceAnswer").asText(""));
    }

    private boolean shouldFinish(List<InterviewQuestion> questions, int answeredCount, int maxQuestions) {
        if (answeredCount >= maxQuestions) return true;
        if (answeredCount < Math.min(3, maxQuestions)) return false;
        List<InterviewQuestion> answered = questions.stream().filter(q -> q.getScore() != null).toList();
        double average = answered.stream().mapToDouble(q -> q.getScore().doubleValue()).average().orElse(0);
        boolean hasWeakPoint = answered.stream().anyMatch(q -> q.getScore().doubleValue() < 60);
        return average >= 85 && !hasWeakPoint;
    }

    private String weakestCategory(List<InterviewQuestion> questions) {
        return questions.stream().filter(q -> q.getScore() != null)
                .collect(Collectors.groupingBy(q -> safe(q.getCategory()),
                        Collectors.averagingDouble(q -> q.getScore().doubleValue())))
                .entrySet().stream().min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("岗位核心基础");
    }

    private String buildFinishReason(List<InterviewQuestion> questions) {
        double average = questions.stream().filter(q -> q.getScore() != null)
                .mapToDouble(q -> q.getScore().doubleValue()).average().orElse(0);
        return String.format("已完成能力评估（当前平均分 %.1f），开始生成反馈报告", average);
    }

    private Map<String, Object> answerResult(InterviewQuestion q, int answeredCount, int totalQuestions) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("questionNumber", q.getQuestionNumber());
        result.put("answeredCount", answeredCount);
        result.put("totalQuestions", totalQuestions);
        result.put("score", q.getScore());
        result.put("comment", safe(q.getComment()));
        result.put("referenceAnswer", safe(q.getReferenceAnswer()));
        return result;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未分类" : value;
    }

    /**
     * 构建最终评价 Prompt（面试全部结束后或主动终止时）。
     *
     * <p>对每题独立打分、给出点评和参考答案，同时输出综合总评和学习建议。</p>
     */
    private String buildEvaluationPrompt(String position, List<InterviewQuestion> answeredQuestions,
                                          boolean isTerminated) {
        StringBuilder qaRecords = new StringBuilder();
        int qIndex = 1;
        for (InterviewQuestion q : answeredQuestions) {
            if (q.getUserAnswer() != null) {
                qaRecords.append(String.format("""
                        --- 第%d题 ---
                        题目：%s
                        分类：%s / %s
                        候选人回答：%s

                        """, qIndex++,
                        q.getQuestionText(),
                        q.getCategory() != null ? q.getCategory() : "未分类",
                        q.getDifficulty() != null ? q.getDifficulty() : "未标注",
                        q.getUserAnswer()));
            }
        }

        String scenario = isTerminated
                ? "面试被候选人主动终止，以下为已完成部分的记录。请就已完成的题目给出阶段性评价。"
                : "面试已全部结束。请综合所有题目的回答情况给出最终评价。";

        return String.format("""
                你是一位经验丰富的 %s 岗位技术面试官。

                ## 面试场景
                %s
                - 面试岗位：%s
                - 面试题总数：%d
                - 实际作答数：%d

                ## 面试记录
                %s

                ## 评分标准
                - 90-100分：回答全面深入，有自己的思考，能举一反三
                - 75-89分：回答正确但不够深入，或缺少部分要点
                - 60-74分：回答基本正确但明显不完整，或部分错误
                - 40-59分：回答有较大偏差，核心概念混淆
                - 0-39分：完全答非所问，或基本不会

                ## 评价要求
                你需要完成三件事：

                ### 1. 逐题评分
                对每道已作答的题目给出：
                - score：0-100 的整数分数
                - comment：具体点评（30-80字），指出回答的亮点和不足，不能说空话
                - referenceAnswer：参考答案要点（50-150字），列出关键知识点和最佳实践

                ### 2. 总体评价
                - overallScore：综合评分（0-100 整数），不是简单平均，而是加权综合
                - overallComment：总体评价（150-300字），包含：
                  - 整体表现概括（1-2句）
                  - 最突出的 1-2 个优点（具体，不要泛泛而谈）
                  - 最明显的 1-2 个不足（具体，指明哪些知识点薄弱）

                ### 3. 学习建议
                - studyAdvice：学习建议（100-200字），必须包含：
                  - 优先加强的 2-3 个具体知识领域
                  - 推荐的学习方法或资源类型（如"阅读XX官方文档""刷XX类型题目"）
                  - 下一步行动建议

                ## 输出格式（极其重要）
                以严格的 JSON 格式输出，不要任何其他文字，不要 markdown 代码块标记：

                {
                  "overallScore": 85,
                  "overallComment": "完整的总体评价文字...",
                  "studyAdvice": "完整的学习建议文字...",
                  "questionEvaluations": [
                    {
                      "questionNumber": 1,
                      "score": 90,
                      "comment": "对该题回答的具体点评...",
                      "referenceAnswer": "参考答案要点..."
                    }
                  ]
                }
                """, position, scenario, position, answeredQuestions.size(), answeredQuestions.size(),
                qaRecords.toString());
    }

    /**
     * 生成最终评价并推送。
     */
    private void generateFinalEvaluation(InterviewSession session, List<InterviewQuestion> questions,
                                          SseEmitter emitter, boolean isTerminated) throws IOException {
        // 更新状态
        session.setStatus(STATUS_EVALUATING);
        sessionRepository.save(session);

        // 推送评估中事件
        emitter.send(SseEmitter.event()
                .name("evaluating")
                .data(Map.of("message", isTerminated ? "正在生成阶段性评价..." : "正在生成面试评价...")));

        // 筛选已回答的题目
        List<InterviewQuestion> answeredQuestions = questions.stream()
                .filter(q -> q.getUserAnswer() != null)
                .collect(Collectors.toList());

        if (answeredQuestions.isEmpty()) {
            emitter.complete();
            return;
        }

        // 自适应面试可提前结束，此时总题数应表示实际面试题数。
        session.setTotalQuestions(answeredQuestions.size());
        sessionRepository.save(session);

        // 调用 AI 生成评价
        String evalPrompt = buildEvaluationPrompt(session.getPosition(), answeredQuestions, isTerminated);
        String aiResponse = aiService.chat(evalPrompt);

        // 解析评价结果
        try {
            JsonNode evalJson = extractJsonObject(aiResponse);

            BigDecimal overallScore = evalJson.has("overallScore")
                    ? BigDecimal.valueOf(evalJson.get("overallScore").asDouble()) : null;
            String overallComment = evalJson.has("overallComment")
                    ? evalJson.get("overallComment").asText() : "";
            String studyAdvice = evalJson.has("studyAdvice")
                    ? evalJson.get("studyAdvice").asText() : "";

            // 更新每题的评分
            JsonNode questionEvals = evalJson.get("questionEvaluations");
            if (questionEvals != null && questionEvals.isArray()) {
                for (JsonNode eval : questionEvals) {
                    int qNumber = eval.get("questionNumber").asInt();
                    questions.stream()
                            .filter(q -> q.getQuestionNumber() == qNumber)
                            .findFirst()
                            .ifPresent(q -> {
                                // 逐题分数已在提交回答时持久化，报告阶段不改写。
                                if (q.getScore() == null && eval.has("score")) {
                                    q.setScore(BigDecimal.valueOf(eval.get("score").asDouble()));
                                }
                                if ((q.getComment() == null || q.getComment().isBlank()) && eval.has("comment")) {
                                    q.setComment(eval.get("comment").asText());
                                }
                                if ((q.getReferenceAnswer() == null || q.getReferenceAnswer().isBlank())
                                        && eval.has("referenceAnswer")) {
                                    q.setReferenceAnswer(eval.get("referenceAnswer").asText());
                                }
                            });
                }
                questionRepository.saveAll(questions);
            }

            // 更新会话
            session.setOverallScore(overallScore);
            session.setOverallComment(overallComment);
            session.setStudyAdvice(studyAdvice);
            session.setStatus(STATUS_COMPLETED);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);

            log.info("面试评价完成: sessionId={}, overallScore={}", session.getId(), overallScore);

            // 推送最终评价
            List<Map<String, Object>> questionResults = questions.stream()
                    .filter(q -> q.getUserAnswer() != null)
                    .map(q -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("questionNumber", q.getQuestionNumber());
                        m.put("questionText", q.getQuestionText());
                        m.put("userAnswer", q.getUserAnswer());
                        m.put("score", q.getScore());
                        m.put("comment", q.getComment() != null ? q.getComment() : "");
                        m.put("referenceAnswer", q.getReferenceAnswer() != null ? q.getReferenceAnswer() : "");
                        return m;
                    })
                    .collect(Collectors.toList());

            emitter.send(SseEmitter.event()
                    .name("final_evaluation")
                    .data(Map.of(
                            "overallScore", overallScore != null ? overallScore : BigDecimal.ZERO,
                            "overallComment", overallComment,
                            "studyAdvice", studyAdvice,
                            "questions", questionResults)));

        } catch (Exception e) {
            log.error("解析评价结果失败: sessionId={}", session.getId(), e);
            // 即使解析失败也不影响面试完成状态
            session.setStatus(STATUS_COMPLETED);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);
            emitter.send(SseEmitter.event()
                    .name("final_evaluation")
                    .data(Map.of(
                            "overallScore", BigDecimal.ZERO,
                            "overallComment", "评价生成失败，请稍后重试",
                            "studyAdvice", "",
                            "questions", List.of())));
        }

        emitter.complete();
    }

    // ==================== 私有方法：JSON 解析 ====================

    /**
     * 从 AI 返回的题目 JSON 解析为实体列表。
     */
    private List<InterviewQuestion> parseQuestions(InterviewSession session, String aiResponse)
            throws JsonProcessingException {
        JsonNode array = extractJsonArray(aiResponse);
        List<InterviewQuestion> questions = new ArrayList<>();
        for (JsonNode node : array) {
            int number = node.has("questionNumber")
                    ? node.get("questionNumber").asInt()
                    : questions.size() + 1;
            String text = node.has("question")
                    ? node.get("question").asText()
                    : "";
            String category = node.has("category") ? node.get("category").asText() : null;
            String difficulty = node.has("difficulty") ? node.get("difficulty").asText() : null;

            if (text.isBlank()) continue;

            questions.add(InterviewQuestion.builder()
                    .session(session)
                    .questionNumber(number)
                    .questionText(text)
                    .category(category)
                    .difficulty(difficulty)
                    .build());
        }
        return questions;
    }

    /**
     * 从 AI 响应中提取 JSON 数组。
     * 处理以下情况：纯JSON、```json ... ``` 包裹、前后有额外文字。
     */
    private JsonNode extractJsonArray(String text) throws JsonProcessingException {
        String trimmed = text.trim();
        // 去掉 markdown 代码块标记
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n");
            if (start == -1) start = 3;
            int end = trimmed.lastIndexOf("```");
            if (end == -1) end = trimmed.length();
            trimmed = trimmed.substring(start, end).trim();
        }
        // 找到 [ 和 ] 的边界
        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            trimmed = trimmed.substring(arrayStart, arrayEnd + 1);
        }
        return objectMapper.readTree(trimmed);
    }

    /**
     * 从 AI 响应中提取 JSON 对象。
     */
    private JsonNode extractJsonObject(String text) throws JsonProcessingException {
        String trimmed = text.trim();
        // 去掉 markdown 代码块标记
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n");
            if (start == -1) start = 3;
            int end = trimmed.lastIndexOf("```");
            if (end == -1) end = trimmed.length();
            trimmed = trimmed.substring(start, end).trim();
        }
        // 找到 { 和 } 的边界
        int objStart = trimmed.indexOf('{');
        int objEnd = trimmed.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            trimmed = trimmed.substring(objStart, objEnd + 1);
        }
        return objectMapper.readTree(trimmed);
    }

    // ==================== 私有方法：校验与转换 ====================

    /**
     * 查找会话并校验归属权。
     */
    private InterviewSession findAndValidateSession(Long userId, Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("面试会话不存在"));
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "无权访问此面试会话");
        }
        return session;
    }

    /**
     * InterviewSession → InterviewSessionResponse
     */
    private InterviewSessionResponse toSessionResponse(InterviewSession s) {
        return InterviewSessionResponse.builder()
                .id(s.getId())
                .position(s.getPosition())
                .status(s.getStatus())
                .totalQuestions(s.getTotalQuestions())
                .answeredCount(s.getAnsweredCount())
                .overallScore(s.getOverallScore())
                .overallComment(s.getOverallComment())
                .studyAdvice(s.getStudyAdvice())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    /**
     * InterviewQuestion → QuestionResponse
     */
    private QuestionResponse toQuestionResponse(InterviewQuestion q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .questionNumber(q.getQuestionNumber())
                .questionText(q.getQuestionText())
                .category(q.getCategory())
                .difficulty(q.getDifficulty())
                .userAnswer(q.getUserAnswer())
                .score(q.getScore())
                .comment(q.getComment())
                .referenceAnswer(q.getReferenceAnswer())
                .answeredAt(q.getAnsweredAt())
                .createdAt(q.getCreatedAt())
                .build();
    }

    // ==================== 私有方法：SSE 工具 ====================

    /**
     * 发送错误事件并关闭 SSE 连接。
     */
    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", message != null ? message : "未知错误")));
        } catch (IOException ignored) {
            // 客户端已断开
        }
        emitter.complete();
    }
}
