package com.app.replant.domain.mission.dto;

import com.app.replant.domain.review.entity.MissionReview;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MissionReviewResponse {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userProfileImg;
    private Integer userReantLevel; // 작성자 캐릭터 레벨 (프로필 캐릭터 이미지용)
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;

    public static MissionReviewResponse from(MissionReview review) {
        Integer userReantLevel = null;
        if (review.getUser() != null && review.getUser().getReant() != null) {
            userReantLevel = review.getUser().getReant().getLevel();
        }
        return MissionReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userNickname(review.getUser().getNickname())
                .userProfileImg(review.getUser().getProfileImg())
                .userReantLevel(userReantLevel)
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
