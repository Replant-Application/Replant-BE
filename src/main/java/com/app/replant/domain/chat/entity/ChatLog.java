package com.app.replant.domain.chat.entity;

import com.app.replant.domain.chat.enums.ChatStatus;
import com.app.replant.domain.chat.enums.LLMProvider;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.user.entity.User;
import com.app.replant.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 로그 엔티티
 * 운영/관리 목적으로 모든 채팅 기록을 저장
 */
@Entity
@Table(name = "chat_log", indexes = {
        @Index(name = "idx_chat_log_user_id", columnList = "user_id"),
        @Index(name = "idx_chat_log_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reant_id", nullable = false)
    private Reant reant;

    @Column(name = "user_message", nullable = true, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "is_proactive", nullable = false)
    private Boolean isProactive = false;

    @Column(name = "ai_response", nullable = false, columnDefinition = "TEXT")
    private String aiResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_provider", nullable = false, length = 20)
    private LLMProvider llmProvider;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "response_tokens")
    private Integer responseTokens;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatStatus status = ChatStatus.SUCCESS;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Builder
    private ChatLog(User user, Reant reant, String userMessage, String aiResponse,
                    LLMProvider llmProvider, String modelName,
                    Integer promptTokens, Integer responseTokens, Integer responseTimeMs,
                    ChatStatus status, String errorMessage, Boolean isProactive) {
        this.user = user;
        this.reant = reant;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.llmProvider = llmProvider;
        this.modelName = modelName;
        this.promptTokens = promptTokens;
        this.responseTokens = responseTokens;
        this.responseTimeMs = responseTimeMs;
        this.status = status != null ? status : ChatStatus.SUCCESS;
        this.errorMessage = errorMessage;
        this.isProactive = isProactive != null ? isProactive : false;
    }

    /**
     * 에러 정보 업데이트
     */
    public void markAsError(ChatStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }

    /**
     * 폴백 처리로 변경
     */
    public void markAsFallback(LLMProvider fallbackProvider, String modelName) {
        this.status = ChatStatus.FALLBACK;
        this.llmProvider = fallbackProvider;
        this.modelName = modelName;
    }
}
