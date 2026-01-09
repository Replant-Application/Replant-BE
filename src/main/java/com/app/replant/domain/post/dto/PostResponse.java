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

@Getter
@Builder
public class PostResponse {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userProfileImg;
    private MissionTag missionTag;
    private String title;
    private String content;
    private List<String> imageUrls;
    private Boolean hasValidBadge;
    private Long commentCount;
    private Long likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class MissionTag {
        private Long id;
        private String title;
        private String type; // "SYSTEM" or "CUSTOM"
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
                .userId(userId)
                .userNickname(userNickname)
                .userProfileImg(userProfileImg)
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrls(parseImageUrls(post.getImageUrls()))
                .hasValidBadge(post.getHasValidBadge())
                .commentCount(commentCount)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());

        if (post.getMission() != null) {
            builder.missionTag(MissionTag.builder()
                    .id(post.getMission().getId())
                    .title(post.getMission().getTitle())
                    .type("SYSTEM")
                    .build());
        } else if (post.getCustomMission() != null) {
            builder.missionTag(MissionTag.builder()
                    .id(post.getCustomMission().getId())
                    .title(post.getCustomMission().getTitle())
                    .type("CUSTOM")
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
