package com.app.replant.domain.meallog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MealLogRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 인증 요청")
    public static class Verify {
        @Schema(description = "게시글 ID (기존 게시글과 연결 시 사용, 없으면 자동 생성)")
        private Long postId;

        @Schema(description = "식사 일지 제목")
        private String title;

        @Schema(description = "식사 요약 설명 (게시글 내용)")
        private String description;

        @Min(1) @Max(5)
        @Schema(description = "맛 평점 (1-5)", example = "3")
        private Integer rating;

        @Schema(description = "이미지 URL 목록 (게시글 자동 생성 시 사용)")
        private java.util.List<String> imageUrls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 기록 날짜 조회 요청")
    public static class DateRange {
        @Schema(description = "시작 날짜 (YYYY-MM-DD)", example = "2026-01-01")
        private String startDate;

        @Schema(description = "종료 날짜 (YYYY-MM-DD)", example = "2026-01-31")
        private String endDate;
    }
}
