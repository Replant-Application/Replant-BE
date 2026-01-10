package com.app.replant.domain.post.dto;

import com.app.replant.domain.post.entity.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 응답 DTO (단순화)
 */
@Getter
@Builder
public class PostResponse {

    private Long id;
    private String postType;  // GENERAL, VERIFICATION
    private Long userId;
    private String userNickname;
    private String userProfileImg;

    // 미션 정보 (인증글일 경우)
    private MissionTag missionTag;

    private String title;
    private String content;
    private List<String> imageUrls;

    // 좋아요/댓글 수
    private Long likeCount;
    private Long commentCount;
    private Boolean isLiked;

    // 인증 상태 (VERIFICATION일 경우)
    private String status;  // PENDING, APPROVED
    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class MissionTag {
        private Long id;
        private String title;
        private String type; // OFFICIAL, CUSTOM
    }

    public static PostResponse from(Post post) {
        return from(post, 0L, 0L, false);
    }

    public static PostResponse from(Post post, Long commentCount) {
        return from(post, commentCount, 0L, false);
    }

    public static PostResponse from(Post post, Long commentCount, Long likeCount, Boolean isLiked) {
        // NPE 방어: User null 체크
        Long userId = null;
        String userNickname = null;
        String userProfileImg = null;
        if (post.getUser() != null) {
            userId = post.getUser().getId();
            userNickname = post.getUser().getNickname();
            userProfileImg = post.getUser().getProfileImg();
        }

        PostResponseBuilder builder = PostResponse.builder()
                .id(post.getId())
                .postType(post.getPostType() != null ? post.getPostType().name() : "GENERAL")
                .userId(userId)
                .userNickname(userNickname)
                .userProfileImg(userProfileImg)
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrls(parseImageUrls(post.getImageUrls()))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLiked(isLiked)
                .status(post.getStatus() != null ? post.getStatus().name() : null)
                .verifiedAt(post.getVerifiedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());

        // 미션 정보 설정 (인증글일 경우 userMission에서 가져옴)
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
