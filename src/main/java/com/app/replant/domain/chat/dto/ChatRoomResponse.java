package com.app.replant.domain.chat.dto;

import com.app.replant.domain.chat.entity.ChatRoom;
import com.app.replant.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {

    private Long id;
    private OtherUserInfo otherUser;
    private MissionInfo matchedMission;
    private LastMessageInfo lastMessage;
    private Long unreadCount;
    private Boolean isActive;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class OtherUserInfo {
        private Long id;
        private String nickname;
        private String profileImg;
    }

    @Getter
    @Builder
    public static class MissionInfo {
        private Long id;
        private String title;
    }

    @Getter
    @Builder
    public static class LastMessageInfo {
        private String content;
        private LocalDateTime createdAt;
        private Boolean isRead;
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, Long currentUserId, Long unreadCount) {
        User otherUser = chatRoom.getOtherUser(currentUserId);

        // NPE 방어: otherUser null 체크
        OtherUserInfo otherUserInfo = null;
        if (otherUser != null) {
            otherUserInfo = OtherUserInfo.builder()
                    .id(otherUser.getId())
                    .nickname(otherUser.getNickname())
                    .profileImg(otherUser.getProfileImg())
                    .build();
        }

        ChatRoomResponseBuilder builder = ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .otherUser(otherUserInfo)
                .unreadCount(unreadCount)
                .isActive(chatRoom.getIsActive())
                .createdAt(chatRoom.getCreatedAt());

        // Mission info from recommendation - NPE 방어 추가
        if (chatRoom.getRecommendation() != null) {
            if (chatRoom.getRecommendation().getMission() != null) {
                builder.matchedMission(MissionInfo.builder()
                        .id(chatRoom.getRecommendation().getMission().getId())
                        .title(chatRoom.getRecommendation().getMission().getTitle())
                        .build());
            } else if (chatRoom.getRecommendation().getCustomMission() != null) {
                builder.matchedMission(MissionInfo.builder()
                        .id(chatRoom.getRecommendation().getCustomMission().getId())
                        .title(chatRoom.getRecommendation().getCustomMission().getTitle())
                        .build());
            }
        }

        return builder.build();
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, Long currentUserId, Long unreadCount, String lastMessageContent, LocalDateTime lastMessageCreatedAt, Boolean lastMessageIsRead) {
        ChatRoomResponse response = from(chatRoom, currentUserId, unreadCount);

        if (lastMessageContent != null) {
            return ChatRoomResponse.builder()
                    .id(response.getId())
                    .otherUser(response.getOtherUser())
                    .matchedMission(response.getMatchedMission())
                    .lastMessage(LastMessageInfo.builder()
                            .content(lastMessageContent)
                            .createdAt(lastMessageCreatedAt)
                            .isRead(lastMessageIsRead)
                            .build())
                    .unreadCount(response.getUnreadCount())
                    .isActive(response.getIsActive())
                    .createdAt(response.getCreatedAt())
                    .build();
        }

        return response;
    }
}
