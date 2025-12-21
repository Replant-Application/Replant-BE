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
    private String content;
    private LocalDateTime createdAt;

    public static MissionReviewResponse from(MissionReview review) {
        return MissionReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userNickname(review.getUser().getNickname())
                .userProfileImg(review.getUser().getProfileImg())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
