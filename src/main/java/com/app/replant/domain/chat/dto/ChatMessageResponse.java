package com.app.replant.domain.chat.dto;

import com.app.replant.domain.chat.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long senderId;
    private String content;
    private Boolean isRead;
    private Boolean isMine;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message, Long currentUserId) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .isMine(message.getSender().getId().equals(currentUserId))
                .createdAt(message.getCreatedAt())
                .build();
    }
}
