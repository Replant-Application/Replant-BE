package com.app.replant.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AcceptRecommendationResponse {

    private Long recommendationId;
    private String status;
    private ChatRoomInfo chatRoom;
    private String message;

    @Getter
    @Builder
    public static class ChatRoomInfo {
        private Long id;
        private OtherUserInfo otherUser;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class OtherUserInfo {
        private Long id;
        private String nickname;
        private String profileImg;
    }
}
