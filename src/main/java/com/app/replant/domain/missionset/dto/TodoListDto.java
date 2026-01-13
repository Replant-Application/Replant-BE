package com.app.replant.domain.missionset.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListMission;
import com.app.replant.domain.missionset.enums.MissionSource;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TodoListDto {

    // ============ Request DTOs ============

    @Getter
    public static class CreateRequest {
        private String title;
        private String description;
        private List<Long> randomMissionIds; // 필수 공식 미션 3개
        private List<Long> customMissionIds; // 선택 커스텀 미션 (0개 이상)
        
        // 미션별 시간대 정보 (선택적) - 미션 ID를 키로 하는 맵
        // 예: { "1": { "startTime": "09:00", "endTime": "10:00" }, ... }
        private java.util.Map<Long, MissionScheduleInfo> missionSchedules;
        
        @Getter
        public static class MissionScheduleInfo {
            private LocalTime startTime; // 시작 시간 (예: 09:00)
            private LocalTime endTime;   // 종료 시간 (예: 10:00)
        }
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String description;
    }

    @Getter
    public static class AddMissionRequest {
        private Long missionId;
        private Integer displayOrder;
    }

    @Getter
    public static class ReorderMissionsRequest {
        private List<MissionOrderItem> missions;

        @Getter
        public static class MissionOrderItem {
            private Long missionId;
            private Integer displayOrder;
        }
    }

    @Getter
    public static class UpdateMissionScheduleRequest {
        private LocalTime startTime; // 시작 시간 (예: 09:00)
        private LocalTime endTime;   // 종료 시간 (예: 10:00)
    }

    // ============ Response DTOs ============

    @Getter
    @Builder
    public static class InitResponse {
        private List<MissionSimpleResponse> randomMissions; // 랜덤 배정된 공식 미션 3개
    }

    @Getter
    @Builder
    public static class SimpleResponse {
        private Long id;
        private String title;
        private String description;
        private Integer completedCount;
        private Integer totalCount;
        private Integer progressRate;
        private Boolean canCreateNew;
        private TodoListStatus status;
        private LocalDateTime createdAt;

        public static SimpleResponse from(TodoList todoList) {
            return SimpleResponse.builder()
                    .id(todoList.getId())
                    .title(todoList.getTitle())
                    .description(todoList.getDescription())
                    .completedCount(todoList.getCompletedCount() != null ? todoList.getCompletedCount() : 0)
                    .totalCount(todoList.getTotalCount() != null ? todoList.getTotalCount() : 5)
                    .progressRate(todoList.getProgressRate())
                    .canCreateNew(todoList.canCreateNewTodoList())
                    .status(todoList.getTodolistStatus())
                    .createdAt(todoList.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long id;
        private String title;
        private String description;
        private Integer completedCount;
        private Integer totalCount;
        private Integer progressRate;
        private Boolean canCreateNew;
        private TodoListStatus status;
        private List<TodoMissionInfo> missions;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static DetailResponse from(TodoList todoList) {
            List<TodoMissionInfo> missionInfos = todoList.getMissions() != null
                    ? todoList.getMissions().stream().map(TodoMissionInfo::from).collect(Collectors.toList())
                    : new ArrayList<>();

            return DetailResponse.builder()
                    .id(todoList.getId())
                    .title(todoList.getTitle())
                    .description(todoList.getDescription())
                    .completedCount(todoList.getCompletedCount() != null ? todoList.getCompletedCount() : 0)
                    .totalCount(todoList.getTotalCount() != null ? todoList.getTotalCount() : 5)
                    .progressRate(todoList.getProgressRate())
                    .canCreateNew(todoList.canCreateNewTodoList())
                    .status(todoList.getTodolistStatus())
                    .missions(missionInfos)
                    .createdAt(todoList.getCreatedAt())
                    .updatedAt(todoList.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class TodoMissionInfo {
        private Long id;
        private Long missionId;
        private String title;
        private String description;
        private String missionType;
        private String verificationType;
        private Integer displayOrder;
        private Boolean isCompleted;
        private LocalDateTime completedAt;
        private MissionSource missionSource;
        private LocalTime scheduledStartTime; // 시간대 배치: 시작 시간
        private LocalTime scheduledEndTime;   // 시간대 배치: 종료 시간

        public static TodoMissionInfo from(TodoListMission msm) {
            Mission mission = msm.getMission();
            return TodoMissionInfo.builder()
                    .id(msm.getId())
                    .missionId(mission.getId())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .missionType(mission.getMissionType().name())
                    .verificationType(mission.getVerificationType().name())
                    .displayOrder(msm.getDisplayOrder())
                    .isCompleted(msm.getIsCompleted() != null ? msm.getIsCompleted() : false)
                    .completedAt(msm.getCompletedAt())
                    .missionSource(msm.getMissionSource())
                    .scheduledStartTime(msm.getScheduledStartTime())
                    .scheduledEndTime(msm.getScheduledEndTime())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MissionSimpleResponse {
        private Long id;
        private String title;
        private String description;
        private String missionType;
        private String verificationType;
        private String category;
        private Integer expReward;

        public static MissionSimpleResponse from(Mission mission) {
            return MissionSimpleResponse.builder()
                    .id(mission.getId())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .missionType(mission.getMissionType().name())
                    .verificationType(mission.getVerificationType().name())
                    .category(mission.getCategory().name())
                    .expReward(mission.getExpReward())
                    .build();
        }
    }

    // ============ 공개 투두리스트용 DTOs 제거됨 (공유 기능 제거) ============
    // PublicResponse, PublicDetailResponse, PublicMissionInfo 모두 제거됨

    // ============ 리뷰용 DTOs ============
    // TodoListReviewDto에서 처리하거나 여기 내부 클래스 유지. 서비스에서 내부 클래스 사용 중이면 유지.
    // 기존 서비스 로직은 내부 클래스를 사용했음. 그대로 유지.

    @Getter
    public static class ReviewRequest {
        private Integer rating; // 1-5
        private String content;
    }

    @Getter
    public static class UpdateReviewRequest {
        private Integer rating;
        private String content;
    }

    @Getter
    @Builder
    public static class ReviewResponse {
        private Long id;
        private Long todoListId;
        private Long userId;
        private String userNickname;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
