package com.app.replant.domain.missionset.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListMission;
import com.app.replant.domain.missionset.enums.MissionSource;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
        private Boolean isPublic; // 공개 여부
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
    @Setter
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
        private Long creatorId;
        private String creatorNickname;
        private Integer likeCount;   // 좋아요 수
        private Boolean isLiked;     // 현재 사용자 좋아요 여부 (선택, 목록/상세에서 설정)

        public static SimpleResponse from(TodoList todoList) {
            return from(todoList, 0, false);
        }

        /** 목록용: likeCount, isLiked 포함 (공유 게시판 목록에서 좋아요 수/여부 표시) */
        public static SimpleResponse from(TodoList todoList, int likeCount, boolean isLiked) {
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
                    .creatorId(todoList.getCreator() != null ? todoList.getCreator().getId() : null)
                    .creatorNickname(todoList.getCreator() != null ? todoList.getCreator().getNickname() : null)
                    .likeCount(likeCount)
                    .isLiked(isLiked)
                    .build();
        }
    }

    @Getter
    @Setter
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
        // 공개 투두리스트용 필드
        private Long creatorId;
        private String creatorNickname;
        private Integer missionCount;  // totalCount와 동일하지만 프론트엔드 호환성을 위해
        private Integer likeCount;    // 좋아요 수
        private Boolean isLiked;     // 현재 사용자 좋아요 여부

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
                    .creatorId(todoList.getCreator() != null ? todoList.getCreator().getId() : null)
                    .creatorNickname(todoList.getCreator() != null ? todoList.getCreator().getNickname() : null)
                    .missionCount(todoList.getTotalCount() != null ? todoList.getTotalCount() : 5)
                    .build();
        }
        
        // UserMission 정보를 포함하여 인증 완료 여부를 설정하는 팩토리 메서드
        public static DetailResponse from(TodoList todoList, Long userId, 
                com.app.replant.domain.usermission.repository.UserMissionRepository userMissionRepository) {
            List<TodoMissionInfo> missionInfos = new ArrayList<>();
            
            if (todoList.getMissions() != null && !todoList.getMissions().isEmpty()) {
                // 모든 미션 ID 수집
                List<Long> missionIds = todoList.getMissions().stream()
                        .map(msm -> msm.getMission() != null ? msm.getMission().getId() : null)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
                
                // 한 번에 모든 UserMission 조회 (N+1 문제 방지)
                java.util.Map<Long, com.app.replant.domain.usermission.entity.UserMission> userMissionMap = new java.util.HashMap<>();
                if (!missionIds.isEmpty()) {
                    List<com.app.replant.domain.usermission.entity.UserMission> userMissions = 
                            userMissionRepository.findByUserIdAndMissionIds(userId, missionIds);
                    // 각 미션 ID별로 UserMission 저장 (PENDING 상태 우선)
                    // 같은 미션 ID에 여러 UserMission이 있을 경우 PENDING 상태를 우선적으로 저장
                    for (com.app.replant.domain.usermission.entity.UserMission um : userMissions) {
                        if (um.getMission() != null && um.getMission().getId() != null) {
                            Long missionId = um.getMission().getId();
                            com.app.replant.domain.usermission.entity.UserMission existing = userMissionMap.get(missionId);
                            
                            // 기존 UserMission이 없거나, 현재가 PENDING 상태이면 저장
                            if (existing == null) {
                                userMissionMap.put(missionId, um);
                            } else if (um.getStatus() == com.app.replant.domain.usermission.enums.UserMissionStatus.PENDING 
                                    && existing.getStatus() != com.app.replant.domain.usermission.enums.UserMissionStatus.PENDING) {
                                // PENDING 상태를 우선적으로 저장
                                userMissionMap.put(missionId, um);
                            }
                            
                            // 디버깅: UserMission 상태 로그
                            System.out.println(String.format("[TodoListDto] UserMission 조회: missionId=%d, status=%s, assignedAt=%s", 
                                    um.getMission().getId(), um.getStatus(), um.getAssignedAt()));
                            System.err.println(String.format("[TodoListDto] UserMission 조회: missionId=%d, status=%s, assignedAt=%s", 
                                    um.getMission().getId(), um.getStatus(), um.getAssignedAt()));
                        }
                    }
                }
                
                // 각 미션에 대해 UserMission 매핑
                for (TodoListMission msm : todoList.getMissions()) {
                    if (msm.getMission() != null && msm.getMission().getId() != null) {
                        com.app.replant.domain.usermission.entity.UserMission userMission = 
                                userMissionMap.get(msm.getMission().getId());
                        // 디버깅: UserMission 매핑 로그
                        if (userMission == null) {
                            System.out.println(String.format("[TodoListDto] UserMission 없음: missionId=%d, title=%s", 
                                    msm.getMission().getId(), msm.getMission().getTitle()));
                            System.err.println(String.format("[TodoListDto] UserMission 없음: missionId=%d, title=%s", 
                                    msm.getMission().getId(), msm.getMission().getTitle()));
                        } else {
                            System.out.println(String.format("[TodoListDto] UserMission 매핑: missionId=%d, status=%s", 
                                    msm.getMission().getId(), userMission.getStatus()));
                            System.err.println(String.format("[TodoListDto] UserMission 매핑: missionId=%d, status=%s", 
                                    msm.getMission().getId(), userMission.getStatus()));
                        }
                        missionInfos.add(TodoMissionInfo.from(msm, userMission));
                    } else {
                        // Mission이 null인 경우 기본값으로 처리
                        missionInfos.add(TodoMissionInfo.from(msm, null));
                    }
                }
            }

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
                    .creatorId(todoList.getCreator() != null ? todoList.getCreator().getId() : null)
                    .creatorNickname(todoList.getCreator() != null ? todoList.getCreator().getNickname() : null)
                    .missionCount(todoList.getTotalCount() != null ? todoList.getTotalCount() : 5)
                    .build();
        }

        /** 공개 투두리스트 상세: 작성자 완료 여부 + 작성자 인증 게시글 ID가 채워진 미션 목록 + 좋아요 수/여부 */
        public static DetailResponse fromPublicDetail(TodoList todoList, List<TodoMissionInfo> missionInfos, int likeCount, boolean isLiked) {
            return DetailResponse.builder()
                    .id(todoList.getId())
                    .title(todoList.getTitle())
                    .description(todoList.getDescription())
                    .completedCount(todoList.getCompletedCount() != null ? todoList.getCompletedCount() : 0)
                    .totalCount(todoList.getTotalCount() != null ? todoList.getTotalCount() : 5)
                    .progressRate(todoList.getProgressRate())
                    .canCreateNew(false)
                    .status(todoList.getTodolistStatus())
                    .missions(missionInfos)
                    .createdAt(todoList.getCreatedAt())
                    .updatedAt(todoList.getUpdatedAt())
                    .creatorId(todoList.getCreator() != null ? todoList.getCreator().getId() : null)
                    .creatorNickname(todoList.getCreator() != null ? todoList.getCreator().getNickname() : null)
                    .missionCount(todoList.getTotalCount() != null ? todoList.getTotalCount() : 5)
                    .likeCount(likeCount)
                    .isLiked(isLiked)
                    .build();
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS) // 클래스 레벨에서도 설정
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
        private Boolean isVerified; // 인증 완료 여부 (필수 미션의 경우만 의미 있음)
        @JsonProperty("userMissionStatus")
        @JsonInclude(JsonInclude.Include.ALWAYS) // null이어도 JSON에 포함
        private String userMissionStatus; // UserMission의 상태 (ASSIGNED, PENDING, COMPLETED)
        /** 작성자가 해당 미션을 완료했을 때의 인증 게시글 ID (공개 상세에서만 사용) */
        private Long verificationPostId;

        public static TodoMissionInfo from(TodoListMission msm) {
            Mission mission = msm.getMission();
            String missionTypeStr = mission != null && mission.getMissionType() != null ? mission.getMissionType().name() : "UNKNOWN";
            String verificationTypeStr = mission != null && mission.getVerificationType() != null ? mission.getVerificationType().name() : "UNKNOWN";
            return TodoMissionInfo.builder()
                    .id(msm.getId())
                    .missionId(mission != null ? mission.getId() : null)
                    .title(mission != null ? mission.getTitle() : null)
                    .description(mission != null ? mission.getDescription() : null)
                    .missionType(missionTypeStr)
                    .verificationType(verificationTypeStr)
                    .displayOrder(msm.getDisplayOrder())
                    .isCompleted(msm.getIsCompleted() != null ? msm.getIsCompleted() : false)
                    .completedAt(msm.getCompletedAt())
                    .missionSource(msm.getMissionSource())
                    .scheduledStartTime(msm.getScheduledStartTime())
                    .scheduledEndTime(msm.getScheduledEndTime())
                    .isVerified(false) // 기본값은 false (나중에 Service에서 설정)
                    .userMissionStatus(null) // UserMission이 없으면 null
                    .build();
        }
        
        // UserMission 정보를 포함하여 인증 완료 여부를 설정하는 팩토리 메서드
        public static TodoMissionInfo from(TodoListMission msm, com.app.replant.domain.usermission.entity.UserMission userMission) {
            Mission mission = msm.getMission();
            
            // Mission이 null인 경우 기본값 반환
            if (mission == null) {
                return TodoMissionInfo.builder()
                        .id(msm.getId())
                        .missionId(null)
                        .title("알 수 없는 미션")
                        .description("")
                        .missionType("UNKNOWN")
                        .verificationType("UNKNOWN")
                        .displayOrder(msm.getDisplayOrder())
                        .isCompleted(msm.getIsCompleted() != null ? msm.getIsCompleted() : false)
                        .completedAt(msm.getCompletedAt())
                        .missionSource(msm.getMissionSource())
                    .scheduledStartTime(msm.getScheduledStartTime())
                    .scheduledEndTime(msm.getScheduledEndTime())
                    .isVerified(false)
                    .userMissionStatus(null) // Mission이 null이면 UserMission도 없음
                    .build();
            }
            
            // 공식 미션인 경우: UserMission의 상태가 COMPLETED면 인증 완료
            // 커스텀 미션인 경우: 인증이 필요 없으므로 항상 false
            boolean isVerified = false;
            String userMissionStatus = null;
            if (userMission != null) {
                userMissionStatus = userMission.getStatus().name();
                if (mission.isOfficialMission()) {
                    isVerified = userMission.getStatus() == com.app.replant.domain.usermission.enums.UserMissionStatus.COMPLETED;
                }
            }
            
            String missionTypeStr = mission.getMissionType() != null ? mission.getMissionType().name() : "UNKNOWN";
            String verificationTypeStr = mission.getVerificationType() != null ? mission.getVerificationType().name() : "UNKNOWN";
            return TodoMissionInfo.builder()
                    .id(msm.getId())
                    .missionId(mission.getId())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .missionType(missionTypeStr)
                    .verificationType(verificationTypeStr)
                    .displayOrder(msm.getDisplayOrder())
                    .isCompleted(msm.getIsCompleted() != null ? msm.getIsCompleted() : false)
                    .completedAt(msm.getCompletedAt())
                    .missionSource(msm.getMissionSource())
                    .scheduledStartTime(msm.getScheduledStartTime())
                    .scheduledEndTime(msm.getScheduledEndTime())
                    .isVerified(isVerified)
                    .userMissionStatus(userMissionStatus)
                    .verificationPostId(null)
                    .build();
        }

        /** 공개 상세용: UserMission + 인증 게시글 ID 포함 */
        public static TodoMissionInfo from(TodoListMission msm, com.app.replant.domain.usermission.entity.UserMission userMission, Long verificationPostId) {
            TodoMissionInfo base = from(msm, userMission);
            return TodoMissionInfo.builder()
                    .id(base.getId())
                    .missionId(base.getMissionId())
                    .title(base.getTitle())
                    .description(base.getDescription())
                    .missionType(base.getMissionType())
                    .verificationType(base.getVerificationType())
                    .displayOrder(base.getDisplayOrder())
                    .isCompleted(base.getIsCompleted())
                    .completedAt(base.getCompletedAt())
                    .missionSource(base.getMissionSource())
                    .scheduledStartTime(base.getScheduledStartTime())
                    .scheduledEndTime(base.getScheduledEndTime())
                    .isVerified(base.getIsVerified())
                    .userMissionStatus(base.getUserMissionStatus())
                    .verificationPostId(verificationPostId)
                    .build();
        }

        /** 공개 투두리스트 상세용: 작성자 완료 여부(isCompleted) + 인증 게시글 ID. 작성자가 해당 미션에 대해 쓴 인증글이 있으면 표시. */
        public static TodoMissionInfo fromPublic(TodoListMission msm, Long verificationPostId) {
            Mission mission = msm.getMission();
            if (mission == null) {
                return TodoMissionInfo.builder()
                        .id(msm.getId())
                        .missionId(null)
                        .title("알 수 없는 미션")
                        .description("")
                        .missionType("UNKNOWN")
                        .verificationType("UNKNOWN")
                        .displayOrder(msm.getDisplayOrder())
                        .isCompleted(msm.getIsCompleted() != null && msm.getIsCompleted())
                        .completedAt(msm.getCompletedAt())
                        .missionSource(msm.getMissionSource())
                        .scheduledStartTime(msm.getScheduledStartTime())
                        .scheduledEndTime(msm.getScheduledEndTime())
                        .isVerified(false)
                        .userMissionStatus(null)
                        .verificationPostId(verificationPostId)
                        .build();
            }
            String mType = mission.getMissionType() != null ? mission.getMissionType().name() : "UNKNOWN";
            String vType = mission.getVerificationType() != null ? mission.getVerificationType().name() : "UNKNOWN";
            return TodoMissionInfo.builder()
                    .id(msm.getId())
                    .missionId(mission.getId())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .missionType(mType)
                    .verificationType(vType)
                    .displayOrder(msm.getDisplayOrder())
                    .isCompleted(msm.getIsCompleted() != null && msm.getIsCompleted())
                    .completedAt(msm.getCompletedAt())
                    .missionSource(msm.getMissionSource())
                    .scheduledStartTime(msm.getScheduledStartTime())
                    .scheduledEndTime(msm.getScheduledEndTime())
                    .isVerified(false)
                    .userMissionStatus(null)
                    .verificationPostId(verificationPostId)
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
            String mType = mission.getMissionType() != null ? mission.getMissionType().name() : "UNKNOWN";
            String vType = mission.getVerificationType() != null ? mission.getVerificationType().name() : "UNKNOWN";
            String cat = mission.getCategory() != null ? mission.getCategory().name() : "UNKNOWN";
            return MissionSimpleResponse.builder()
                    .id(mission.getId())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .missionType(mType)
                    .verificationType(vType)
                    .category(cat)
                    .expReward(mission.isCustomMission() ? 0 : mission.getExpReward())
                    .build();
        }
    }

    // ============ 공개 투두리스트용 DTOs 제거됨 (공유 기능 제거) ============
    // PublicResponse, PublicDetailResponse, PublicMissionInfo 모두 제거됨
}
