package com.app.replant.domain.verification.dto;

import com.app.replant.domain.verification.entity.VerificationPost;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class VerificationPostResponse {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userProfileImg;
    private Long userMissionId;
    private String missionType;
    private MissionInfo mission;
    private CustomMissionInfo customMission;
    private String missionTitle;  // 프론트엔드 호환성을 위해 추가
    private String content;
    private List<String> imageUrls;
    private VerificationStatus status;
    private Integer approveCount;
    private Integer rejectCount;
    private Integer commentCount;  // 댓글 수 추가
    private LocalDateTime createdAt;
    private String myVote;

    @Getter
    @Builder
    public static class MissionInfo {
        private Long id;
        private String title;
        private String type;
    }

    @Getter
    @Builder
    public static class CustomMissionInfo {
        private Long id;
        private String title;
    }

    public static VerificationPostResponse from(VerificationPost post) {
        return from(post, null, 0);
    }

    public static VerificationPostResponse from(VerificationPost post, String myVote) {
        return from(post, myVote, 0);
    }

    public static VerificationPostResponse from(VerificationPost post, String myVote, int commentCount) {
        // missionTitle 추출
        String missionTitle = "미션";
        if (post.getUserMission().getMission() != null) {
            missionTitle = post.getUserMission().getMission().getTitle();
        } else if (post.getUserMission().getCustomMission() != null) {
            missionTitle = post.getUserMission().getCustomMission().getTitle();
        }

        VerificationPostResponseBuilder builder = VerificationPostResponse.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .userNickname(post.getUser().getNickname())
                .userProfileImg(post.getUser().getProfileImg())
                .userMissionId(post.getUserMission().getId())
                .missionTitle(missionTitle)
                .content(post.getContent())
                .imageUrls(parseImageUrls(post.getImageUrls()))
                .status(post.getStatus())
                .approveCount(post.getApproveCount())
                .rejectCount(post.getRejectCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .myVote(myVote);

        if (post.getUserMission().getMission() != null) {
            builder.missionType("SYSTEM")
                    .mission(MissionInfo.builder()
                            .id(post.getUserMission().getMission().getId())
                            .title(post.getUserMission().getMission().getTitle())
                            .type(post.getUserMission().getMission().getType().name())
                            .build());
        } else if (post.getUserMission().getCustomMission() != null) {
            builder.missionType("CUSTOM")
                    .customMission(CustomMissionInfo.builder()
                            .id(post.getUserMission().getCustomMission().getId())
                            .title(post.getUserMission().getCustomMission().getTitle())
                            .build());
        }

        return builder.build();
    }

    private static List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
