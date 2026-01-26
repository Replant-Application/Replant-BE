package com.app.replant.domain.meallog.dto;

import com.app.replant.domain.meallog.entity.MealLog;
import com.app.replant.domain.meallog.enums.MealLogStatus;
import com.app.replant.domain.meallog.enums.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MealLogResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 기록 응답")
    public static class Detail {
        @Schema(description = "식사 기록 ID")
        private Long id;

        @Schema(description = "식사 타입", example = "BREAKFAST")
        private MealType mealType;

        @Schema(description = "식사 타입 표시명", example = "아침")
        private String mealTypeDisplay;

        @Schema(description = "식사 날짜", example = "2026-01-24")
        private LocalDate mealDate;

        @Schema(description = "제목")
        private String title;

        @Schema(description = "설명")
        private String description;

        @Schema(description = "맛 평점 (1-5)")
        private Integer rating;

        @Schema(description = "상태", example = "ASSIGNED")
        private MealLogStatus status;

        @Schema(description = "상태 표시명", example = "할당됨")
        private String statusDisplay;

        @Schema(description = "할당 시간")
        private LocalDateTime assignedAt;

        @Schema(description = "인증 시간")
        private LocalDateTime verifiedAt;

        @Schema(description = "마감 시간")
        private LocalDateTime deadlineAt;

        @Schema(description = "남은 시간 (초)")
        private Long remainingSeconds;

        @Schema(description = "경험치 보상")
        private Integer expReward;

        @Schema(description = "연결된 게시글 ID")
        private Long postId;

        public static Detail from(MealLog mealLog) {
            return Detail.builder()
                    .id(mealLog.getId())
                    .mealType(mealLog.getMealType())
                    .mealTypeDisplay(mealLog.getMealType().getDisplayName())
                    .mealDate(mealLog.getMealDate())
                    .title(mealLog.getTitle())
                    .description(mealLog.getDescription())
                    .rating(mealLog.getRating())
                    .status(mealLog.getStatus())
                    .statusDisplay(mealLog.getStatus().getDescription())
                    .assignedAt(mealLog.getAssignedAt())
                    .verifiedAt(mealLog.getVerifiedAt())
                    .deadlineAt(mealLog.getDeadlineAt())
                    .remainingSeconds(mealLog.getRemainingSeconds())
                    .expReward(mealLog.getExpReward())
                    .postId(mealLog.getPost() != null ? mealLog.getPost().getId() : null)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 미션 상태 응답 (프론트 화면용)")
    public static class Status {
        @Schema(description = "식사 기록 ID")
        private Long id;

        @Schema(description = "식사 타입", example = "BREAKFAST")
        private MealType mealType;

        @Schema(description = "식사 타입 표시명", example = "아침")
        private String mealTypeDisplay;

        @Schema(description = "미션 제목", example = "아침 식사 인증")
        private String missionTitle;

        @Schema(description = "미션 설명", example = "오늘의 아침 식사를 사진과 함께 공유해주세요!")
        private String missionDescription;

        @Schema(description = "상태", example = "ASSIGNED")
        private MealLogStatus status;

        @Schema(description = "남은 시간 (초)")
        private Long remainingSeconds;

        @Schema(description = "시간 초과 여부")
        private boolean expired;

        @Schema(description = "인증 가능 여부")
        private boolean canVerify;

        @Schema(description = "경험치 보상")
        private Integer expReward;

        public static Status from(MealLog mealLog) {
            String title = mealLog.getMealType().getDisplayName() + " 식사 인증";
            String desc = "오늘의 " + mealLog.getMealType().getDisplayName() + " 식사를 사진과 함께 공유해주세요!";
            
            return Status.builder()
                    .id(mealLog.getId())
                    .mealType(mealLog.getMealType())
                    .mealTypeDisplay(mealLog.getMealType().getDisplayName())
                    .missionTitle(title)
                    .missionDescription(desc)
                    .status(mealLog.getStatus())
                    .remainingSeconds(mealLog.getRemainingSeconds())
                    .expired(mealLog.isExpired())
                    .canVerify(mealLog.canVerify())
                    .expReward(mealLog.getExpReward())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일별 식사 기록 응답")
    public static class Daily {
        @Schema(description = "날짜", example = "2026-01-24")
        private LocalDate date;

        @Schema(description = "아침 식사 기록")
        private Detail breakfast;

        @Schema(description = "점심 식사 기록")
        private Detail lunch;

        @Schema(description = "저녁 식사 기록")
        private Detail dinner;

        @Schema(description = "완료된 식사 수")
        private int completedCount;

        public static Daily from(LocalDate date, List<MealLog> mealLogs) {
            Detail breakfast = null;
            Detail lunch = null;
            Detail dinner = null;
            int completed = 0;

            for (MealLog log : mealLogs) {
                Detail detail = Detail.from(log);
                switch (log.getMealType()) {
                    case BREAKFAST -> breakfast = detail;
                    case LUNCH -> lunch = detail;
                    case DINNER -> dinner = detail;
                }
                if (log.getStatus() == MealLogStatus.COMPLETED) {
                    completed++;
                }
            }

            return Daily.builder()
                    .date(date)
                    .breakfast(breakfast)
                    .lunch(lunch)
                    .dinner(dinner)
                    .completedCount(completed)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "식사 통계 응답")
    public static class Stats {
        @Schema(description = "총 완료 식사 수")
        private long totalCompleted;

        @Schema(description = "평균 평점")
        private Double averageRating;

        @Schema(description = "이번 주 완료 식사 수")
        private long weeklyCompleted;

        @Schema(description = "이번 달 완료 식사 수")
        private long monthlyCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증 결과 응답")
    public static class VerifyResult {
        @Schema(description = "성공 여부")
        private boolean success;

        @Schema(description = "메시지")
        private String message;

        @Schema(description = "획득 경험치")
        private Integer expGained;

        @Schema(description = "식사 기록 상세")
        private Detail mealLog;
    }
}
