package com.app.replant.domain.chat.service;

import com.app.replant.domain.chat.dto.ChatHistoryResponse;
import com.app.replant.domain.chat.dto.ChatRequest;
import com.app.replant.domain.chat.dto.ChatResponse;
import com.app.replant.domain.chat.entity.ChatLog;
import com.app.replant.domain.chat.enums.ChatStatus;
import com.app.replant.domain.chat.enums.LLMProvider;
import com.app.replant.domain.chat.repository.ChatLogRepository;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 채팅 서비스
 * 사용자 메시지를 받아 LLM 응답을 생성하고 저장
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatLogRepository chatLogRepository;
    private final ReantRepository reantRepository;
    private final UserRepository userRepository;
    private final PromptService promptService;
    private final LLMService llmService;

    // 일일 채팅 제한 (Rate Limiting)
    private static final int DAILY_CHAT_LIMIT = 100;

    /**
     * 채팅 메시지 처리
     */
    @Transactional
    public ChatResponse chat(Long userId, ChatRequest request) {
        // 1. 사용자 및 리앤트 조회 - N+1 문제 방지를 위해 reant를 함께 로드
        User user = userRepository.findByIdWithReant(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 이미 로드된 reant 사용 (중복 조회 방지)
        Reant reant = user.getReant();
        if (reant == null) {
            throw new CustomException(ErrorCode.REANT_NOT_FOUND);
        }

        // 2. 일일 채팅 제한 확인
        checkDailyLimit(userId);

        // 3. 프롬프트 구성
        String prompt = promptService.buildPrompt(request.getMessage(), reant, user);
        String defaultResponse = promptService.getDefaultResponse(reant);

        // 4. LLM 호출
        LLMService.LLMResult result = llmService.generate(prompt, defaultResponse);

        // 5. 응답이 null인 경우 기본 응답 사용
        String finalResponse = result.response();
        if (finalResponse == null || finalResponse.isBlank()) {
            finalResponse = defaultResponse;
        }

        // 6. 채팅 로그 저장
        ChatLog chatLog = saveChatLog(user, reant, request.getMessage(), result, defaultResponse);

        // 7. 응답 반환
        return ChatResponse.of(
                finalResponse,
                reant.getName(),
                result.provider() != null ? result.provider() : LLMProvider.GEMINI
        );
    }

    /**
     * 채팅 이력 조회 (관리자/운영용)
     */
    public Page<ChatHistoryResponse> getChatHistory(Long userId, Pageable pageable) {
        return chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(ChatHistoryResponse::from);
    }

    /**
     * 일일 채팅 제한 확인
     */
    private void checkDailyLimit(Long userId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long todayCount = chatLogRepository.countTodayChatsByUserId(userId, todayStart);

        if (todayCount >= DAILY_CHAT_LIMIT) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 채팅 로그 저장
     */
    private ChatLog saveChatLog(User user, Reant reant, String userMessage, LLMService.LLMResult result, String defaultResponse) {
        // AI 응답이 null인 경우 기본 응답 사용
        String aiResponse = result.response();
        if (aiResponse == null || aiResponse.isBlank()) {
            aiResponse = defaultResponse;
        }
        
        ChatLog chatLog = ChatLog.builder()
                .user(user)
                .reant(reant)
                .userMessage(userMessage)
                .aiResponse(aiResponse)
                .llmProvider(result.provider() != null ? result.provider() : LLMProvider.GEMINI)
                .modelName(result.modelName())
                .promptTokens(result.promptTokens())
                .responseTokens(result.responseTokens())
                .responseTimeMs(result.responseTimeMs() != null ? result.responseTimeMs().intValue() : null)
                .status(result.status())
                .errorMessage(result.errorMessage())
                .build();

        return chatLogRepository.save(chatLog);
    }

    /**
     * 오늘 채팅 수 조회
     */
    public Long getTodayChatCount(Long userId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return chatLogRepository.countTodayChatsByUserId(userId, todayStart);
    }

    /**
     * 채팅 통계 조회 (관리자용)
     */
    public ChatStats getStats(LocalDateTime start, LocalDateTime end) {
        Long totalChats = chatLogRepository.countByCreatedAtBetween(start, end);
        Long errorCount = chatLogRepository.countByStatusAndCreatedAtAfter(ChatStatus.ERROR, start);
        Long fallbackCount = chatLogRepository.countByStatusAndCreatedAtAfter(ChatStatus.FALLBACK, start);

        return new ChatStats(totalChats, errorCount, fallbackCount);
    }

    public record ChatStats(Long totalChats, Long errorCount, Long fallbackCount) {}
}
