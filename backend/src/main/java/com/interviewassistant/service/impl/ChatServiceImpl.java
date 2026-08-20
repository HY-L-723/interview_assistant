package com.interviewassistant.service.impl;

import com.interviewassistant.common.BusinessException;
import com.interviewassistant.common.RateLimitExceededException;
import com.interviewassistant.dto.ChatHistoryItem;
import com.interviewassistant.dto.AIChatMessage;
import com.interviewassistant.dto.ConversationDto;
import com.interviewassistant.entity.ChatMessage;
import com.interviewassistant.entity.Conversation;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ChatMessageRepository;
import com.interviewassistant.repository.ConversationRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import com.interviewassistant.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final int DAILY_LIMIT = 50;
    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final AIService aiService;

    /** 限制每次发送给模型的历史消息数，防止上下文无限增长。 */
    @Value("${chat.context-message-limit:20}")
    private int contextMessageLimit = 20;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           ConversationRepository conversationRepository,
                           UserRepository userRepository,
                           AIService aiService) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    @Override
    public void sendMessageStream(Long userId, Long conversationId, String message, String model,
                                  SseEmitter emitter) {
        Conversation conversation;
        ChatMessage userMessage;
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));

            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new BusinessException("对话不存在"));

            if (!conversation.getUser().getId().equals(userId)) {
                throw new BusinessException(403, "无权访问此对话");
            }

            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime startOfNextDay = startOfDay.plusDays(1);
            long todayCount = chatMessageRepository
                    .countByUserIdAndRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            userId, USER_ROLE, startOfDay, startOfNextDay);
            if (todayCount >= DAILY_LIMIT) {
                log.warn("用户 {} 触发限流, 今日已提问 {} 次", userId, todayCount);
                throw new RateLimitExceededException(
                        "今日提问次数已达上限（" + DAILY_LIMIT + "次），请明天再试");
            }
            log.info("用户 {} 发送消息, 今日第 {} 次提问", userId, todayCount + 1);

            userMessage = chatMessageRepository.save(
                    ChatMessage.builder()
                            .user(user)
                            .conversation(conversation)
                            .role(USER_ROLE)
                            .content(message)
                            .build());

            if ("新对话".equals(conversation.getTitle())) {
                conversation.setTitle(message.length() > 30 ? message.substring(0, 30) + "..." : message);
            }
            conversationRepository.save(conversation);

            emitter.send(SseEmitter.event()
                    .name("user_message")
                    .data(Map.of("id", userMessage.getId(),
                            "content", userMessage.getContent(),
                            "time", userMessage.getCreatedAt().toString())));

        } catch (BusinessException | RateLimitExceededException e) {
            log.warn("发送消息前校验失败: userId={}, conversationId={}, message={}",
                    userId, conversationId, e.getMessage());
            sendErrorAndComplete(emitter, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("发送消息前发生系统异常: userId={}, conversationId={}", userId, conversationId, e);
            sendErrorAndComplete(emitter, e.getMessage());
            return;
        }

        Conversation finalConversation = conversation;
        ChatMessage finalUserMessage = userMessage;
        AtomicReference<reactor.core.Disposable> subscriptionRef = new AtomicReference<>();

        emitter.onCompletion(() -> {
            reactor.core.Disposable sub = subscriptionRef.get();
            if (sub != null && !sub.isDisposed()) sub.dispose();
        });
        emitter.onTimeout(() -> {
            reactor.core.Disposable sub = subscriptionRef.get();
            if (sub != null && !sub.isDisposed()) sub.dispose();
        });

        List<AIChatMessage> context = buildConversationContext(conversationId, message);
        Flux<String> tokenFlux = aiService.chatStream(context, model)
                .subscribeOn(Schedulers.boundedElastic());

        StringBuilder fullReply = new StringBuilder();

        reactor.core.Disposable subscription = tokenFlux
                .doOnNext(token -> {
                    fullReply.append(token);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(Map.of("content", token)));
                    } catch (IOException e) {
                        throw new RuntimeException("客户端断开连接", e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        if (fullReply.isEmpty()) {
                            log.warn("AI 流式响应为空: userId={}, userMsgId={}", userId, finalUserMessage.getId());
                            sendErrorAndComplete(emitter, "AI 暂无回复内容，请稍后重试");
                            return;
                        }

                        ChatMessage assistantMessage = chatMessageRepository.save(
                                ChatMessage.builder()
                                        .user(finalUserMessage.getUser())
                                        .conversation(finalConversation)
                                        .role(ASSISTANT_ROLE)
                                        .content(fullReply.toString())
                                        .build());

                        conversationRepository.save(finalConversation);

                        log.info("用户 {} 消息处理完成, userMsgId={}, assistantMsgId={}",
                                userId, finalUserMessage.getId(), assistantMessage.getId());

                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(Map.of("id", assistantMessage.getId(),
                                        "content", fullReply.toString(),
                                        "time", assistantMessage.getCreatedAt().toString())));
                        emitter.complete();
                    } catch (IOException e) {
                        log.debug("客户端已断开，无法发送完成事件");
                    }
                })
                .doOnError(e -> {
                    log.error("流式调用出错: userId={}", userId, e);
                    sendErrorAndComplete(emitter, e.getMessage());
                })
                .subscribe();

        subscriptionRef.set(subscription);
    }

    private List<AIChatMessage> buildConversationContext(Long conversationId, String currentMessage) {
        int limit = Math.max(1, contextMessageLimit);
        List<ChatMessage> recentMessages = new ArrayList<>(chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, limit)));
        Collections.reverse(recentMessages);

        List<AIChatMessage> context = recentMessages.stream()
                .filter(item -> USER_ROLE.equals(item.getRole()) || ASSISTANT_ROLE.equals(item.getRole()))
                .map(item -> new AIChatMessage(item.getRole(), item.getContent()))
                .collect(Collectors.toCollection(ArrayList::new));

        // 当前用户消息在读取历史前已入库；仅在仓储返回未包含它时补入。
        if (context.isEmpty()
                || !USER_ROLE.equals(context.get(context.size() - 1).role())
                || !currentMessage.equals(context.get(context.size() - 1).content())) {
            context.add(new AIChatMessage(USER_ROLE, currentMessage));
        }
        log.debug("构建会话上下文: conversationId={}, messageCount={}", conversationId, context.size());
        return context;
    }

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

    @Override
    public ConversationDto createConversation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        Conversation conversation = conversationRepository.save(
                Conversation.builder()
                        .user(user)
                        .title("新对话")
                        .build());
        log.info("创建对话: userId={}, conversationId={}", userId, conversation.getId());
        return new ConversationDto(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    @Override
    public List<ConversationDto> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(c -> new ConversationDto(
                        c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatHistoryItem> getConversationMessages(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("对话不存在"));
        if (!conversation.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "无权访问此对话");
        }
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> new ChatHistoryItem(
                        m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("对话不存在"));
        if (!conversation.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "无权操作此对话");
        }
        List<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
        chatMessageRepository.deleteAll(messages);
        conversationRepository.delete(conversation);
        log.info("删除对话: userId={}, conversationId={}, 消息数={}", userId, conversationId, messages.size());
    }
}
