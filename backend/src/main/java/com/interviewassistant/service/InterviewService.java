package com.interviewassistant.service;

import com.interviewassistant.dto.InterviewDetailResponse;
import com.interviewassistant.dto.InterviewSessionResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 模拟面试服务接口。
 *
 * <p>面试流程：开始面试(AI生成题目) → 逐题作答 → 结束面试(AI评分+评价+建议)。
 * 核心方法使用 SSE 流式推送面试过程。</p>
 */
public interface InterviewService {

    /**
     * 开始一次新的模拟面试。
     *
     * <p>根据岗位生成开场白和面试题目，通过 SSE 流式推送面试过程：
     * <ul>
     *   <li>{@code greeting} — AI 面试官开场欢迎词</li>
     *   <li>{@code session_created} — 会话创建成功</li>
     *   <li>{@code question} — 推送第一道面试题</li>
     *   <li>{@code error} — 发生错误</li>
     * </ul>
     *
     * @param userId   当前登录用户 ID
     * @param position 面试岗位
     * @param emitter  SSE 发射器
     */
    void startInterview(Long userId, String position, SseEmitter emitter);

    /**
     * 提交当前题目的回答。
     *
     * <p>保存回答后，如有剩余题目则推送下一题，否则触发 AI 总评。
     * SSE 事件：
     * <ul>
     *   <li>{@code answer_saved} — 回答已保存</li>
     *   <li>{@code question} — 推送下一道题（如有）</li>
     *   <li>{@code evaluating} — 开始 AI 总评（全部答完时）</li>
     *   <li>{@code final_evaluation} — 总评完成</li>
     *   <li>{@code done} — 流程结束</li>
     *   <li>{@code error} — 发生错误</li>
     * </ul>
     *
     * @param userId    当前登录用户 ID
     * @param sessionId 面试会话 ID
     * @param answer    用户回答内容
     * @param emitter   SSE 发射器
     */
    void submitAnswer(Long userId, Long sessionId, String answer, SseEmitter emitter);

    /**
     * 用户主动终止面试。
     *
     * <p>终止后如有已回答的题目，会生成阶段性评价。
     *
     * @param userId    当前登录用户 ID
     * @param sessionId 面试会话 ID
     * @param emitter   SSE 发射器
     */
    void terminateInterview(Long userId, Long sessionId, SseEmitter emitter);

    /**
     * 获取用户的面试历史列表。
     */
    List<InterviewSessionResponse> getSessions(Long userId);

    /**
     * 获取某次面试的完整详情（含全部 Q&A）。
     */
    InterviewDetailResponse getSessionDetail(Long userId, Long sessionId);
}
