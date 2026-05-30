package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.AnswerRequest;
import com.interviewassistant.dto.InterviewDetailResponse;
import com.interviewassistant.dto.InterviewSessionResponse;
import com.interviewassistant.dto.StartInterviewRequest;
import com.interviewassistant.service.InterviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 模拟面试控制器。
 *
 * <p>提供面试的全生命周期 API：开始、作答、终止、查询历史、查看详情。
 * 核心接口（start / answer / terminate）使用 SSE 流式推送。</p>
 *
 * <p>所有接口均需要 JWT 认证。</p>
 */
@RestController
@RequestMapping("/api/interview")
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /**
     * 开始一次新的模拟面试。
     *
     * <p>SSE 事件流：
     * <ul>
     *   <li>{@code greeting} — AI 面试官开场欢迎词</li>
     *   <li>{@code session_created} — 会话创建</li>
     *   <li>{@code question} — 推送第一道面试题</li>
     *   <li>{@code error} — 异常</li>
     * </ul>
     */
    @PostMapping("/start")
    public SseEmitter start(@AuthenticationPrincipal Long userId,
                            @Valid @RequestBody StartInterviewRequest request) {
        log.info("开始模拟面试: userId={}, position={}", userId, request.getPosition());
        SseEmitter emitter = new SseEmitter(120_000L);
        interviewService.startInterview(userId, request.getPosition(), emitter);
        return emitter;
    }

    /**
     * 提交当前题目的回答。
     *
     * <p>如有剩余题目则推送下一题，否则触发 AI 总评。
     *
     * <p>SSE 事件流：
     * <ul>
     *   <li>{@code answer_saved} — 回答已保存</li>
     *   <li>{@code question} — 下一道题（如有剩余）</li>
     *   <li>{@code evaluating} — 正在生成总评</li>
     *   <li>{@code final_evaluation} — 总评结果</li>
     *   <li>{@code error} — 异常</li>
     * </ul>
     */
    @PostMapping("/answer")
    public SseEmitter answer(@AuthenticationPrincipal Long userId,
                             @Valid @RequestBody AnswerRequest request) {
        log.info("提交面试回答: userId={}, sessionId={}", userId, request.getSessionId());
        SseEmitter emitter = new SseEmitter(180_000L);
        interviewService.submitAnswer(userId, request.getSessionId(), request.getAnswer(), emitter);
        return emitter;
    }

    /**
     * 用户主动终止面试。
     *
     * <p>终止后如有已回答的题目，会生成阶段性评价。
     */
    @PostMapping("/terminate")
    public SseEmitter terminate(@AuthenticationPrincipal Long userId,
                                @RequestBody Map<String, Long> body) {
        Long sessionId = body.get("sessionId");
        log.info("终止模拟面试: userId={}, sessionId={}", userId, sessionId);
        SseEmitter emitter = new SseEmitter(180_000L);
        interviewService.terminateInterview(userId, sessionId, emitter);
        return emitter;
    }

    /**
     * 获取当前用户的面试历史列表。
     */
    @GetMapping("/sessions")
    public ResponseEntity<Result<List<InterviewSessionResponse>>> getSessions(
            @AuthenticationPrincipal Long userId) {
        log.info("查询面试历史: userId={}", userId);
        List<InterviewSessionResponse> sessions = interviewService.getSessions(userId);
        return ResponseEntity.ok(Result.success(sessions));
    }

    /**
     * 获取某次面试的完整详情（含全部 Q&A 和评分）。
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<Result<InterviewDetailResponse>> getSessionDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        log.info("查询面试详情: userId={}, sessionId={}", userId, id);
        InterviewDetailResponse detail = interviewService.getSessionDetail(userId, id);
        return ResponseEntity.ok(Result.success(detail));
    }
}
