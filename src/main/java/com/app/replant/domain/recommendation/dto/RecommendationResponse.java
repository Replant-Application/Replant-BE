package com.app.replant.domain.recommendation.dto;

import com.app.replant.domain.recommendation.entity.UserRecommendation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class RecommendationResponse {

    private Long id;
    private RecommendedUserInfo recommendedUser;
    private MissionInfo mission;
    private Map<String, Object> matchReason;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class RecommendedUserInfo {
        private Long id;
        private String nickname;
        private String profileImg;
        private ReantInfo reant;
    }

    @Getter
    @Builder
    public static class ReantInfo {
        private String name;
        private Integer level;
        private String stage;
    }

    @Getter
    @Builder
    public static class MissionInfo {
        private Long id;
        private String title;
        private String type; // "SYSTEM" or "CUSTOM"
    }

    public static RecommendationResponse from(UserRecommendation recommendation) {
        RecommendationResponseBuilder builder = RecommendationResponse.builder()
                .id(recommendation.getId())
                .status(recommendation.getStatus().name())
                .expiresAt(recommendation.getExpiresAt())
                .createdAt(recommendation.getCreatedAt());

        // Recommended user info
        RecommendedUserInfo.RecommendedUserInfoBuilder userBuilder = RecommendedUserInfo.builder()
                .id(recommendation.getRecommendedUser().getId())
                .nickname(recommendation.getRecommendedUser().getNickname())
                .profileImg(recommendation.getRecommendedUser().getProfileImg());

        if (recommendation.getRecommendedUser().getReant() != null) {
            userBuilder.reant(ReantInfo.builder()
                    .name(recommendation.getRecommendedUser().getReant().getName())
                    .level(recommendation.getRecommendedUser().getReant().getLevel())
                    .stage(recommendation.getRecommendedUser().getReant().getStage().name())
                    .build());
        }

        builder.recommendedUser(userBuilder.build());

        // Mission info
        if (recommendation.getMission() != null) {
            builder.mission(MissionInfo.builder()
                    .id(recommendation.getMission().getId())
                    .title(recommendation.getMission().getTitle())
                    .type("SYSTEM")
                    .build());
        } else if (recommendation.getCustomMission() != null) {
            builder.mission(MissionInfo.builder()
                    .id(recommendation.getCustomMission().getId())
                    .title(recommendation.getCustomMission().getTitle())
                    .type("CUSTOM")
                    .build());
        }

        // Match reason
        builder.matchReason(parseMatchReason(recommendation.getMatchReason()));

        return builder.build();
    }

    private static Map<String, Object> parseMatchReason(String matchReasonJson) {
        if (matchReasonJson == null || matchReasonJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(matchReasonJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}
