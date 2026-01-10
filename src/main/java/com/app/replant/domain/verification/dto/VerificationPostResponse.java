package com.app.replant.domain.verification.dto;

import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.verification.enums.VerificationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 인증글 응답 DTO
 * - 좋아요 = 인증 시스템으로 변경
 * - 미션 정보는 userMission을 통해 접근
 */
@Getter
@Builder
public class VerificationPostResponse {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userProfileImg;
    private Long userMissionId;
    private MissionTag missionTag;  // 미션 정보 (통합)
    private String content;
    private List<String> imageUrls;
    private VerificationStatus status;
    private Long likeCount;  // 좋아요 수 (= 인증 투표)
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;

    @Getter
    @Builder
    public static class MissionTag {
        private Long id;
        private String title;
        private String type;  // OFFICIAL, CUSTOM
    }

    public static VerificationPostResponse from(Post post) {
        return from(post, 0L, 0);
    }

    public static VerificationPostResponse from(Post post, Long likeCount) {
        return from(post, likeCount, 0);
    }

    public static VerificationPostResponse from(Post post, Long likeCount, int commentCount) {
        Long userMissionId = post.getUserMission() != null ? post.getUserMission().getId() : null;

        VerificationPostResponseBuilder builder = VerificationPostResponse.builder()
                .id(post.getId())
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .userNickname(post.getUser() != null ? post.getUser().getNickname() : null)
                .userProfileImg(post.getUser() != null ? post.getUser().getProfileImg() : null)
                .userMissionId(userMissionId)
                .content(post.getContent())
                .imageUrls(parseImageUrls(post.getImageUrls()))
                .status(post.getStatus())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .verifiedAt(post.getVerifiedAt());

        // 미션 정보 설정 (userMission을 통해 접근)
        if (post.getUserMission() != null) {
            Long missionId = post.getMissionId();
            String missionTitle = post.getMissionTitle();
            String missionType = post.getMissionType();

            if (missionId != null && missionTitle != null) {
                builder.missionTag(MissionTag.builder()
                        .id(missionId)
                        .title(missionTitle)
                        .type(missionType)
                        .build());
            }
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
