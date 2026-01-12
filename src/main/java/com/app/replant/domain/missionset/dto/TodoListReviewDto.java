package com.app.replant.domain.missionset.dto;

import com.app.replant.domain.missionset.entity.TodoListReview;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class TodoListReviewDto {

    // ============ Request DTOs ============

    @Getter
    public static class CreateRequest {
        private Integer rating;
        private String content;
    }

    @Getter
    public static class UpdateRequest {
        private Integer rating;
        private String content;
    }

    // ============ Response DTOs ============

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long todoListId;
        private String todoListTitle;
        private UserInfo user;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(TodoListReview review) {
            UserInfo userInfo = null;
            if (review.getUser() != null) {
                userInfo = UserInfo.builder()
                        .id(review.getUser().getId())
                        .nickname(review.getUser().getNickname())
                        .build();
            }

            return Response.builder()
                    .id(review.getId())
                    .todoListId(review.getTodoList().getId())
                    .todoListTitle(review.getTodoList().getTitle())
                    .user(userInfo)
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String nickname;
    }
}
