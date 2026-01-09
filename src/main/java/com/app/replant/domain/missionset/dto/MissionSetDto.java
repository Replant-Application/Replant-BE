package com.app.replant.domain.missionset.dto;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetMission;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MissionSetDto {

    // ============ Request DTOs ============

    @Getter
    public static class CreateRequest {
        private String title;
        private String description;
        private Boolean isPublic;
        private List<Long> missionIds;
    }

    @Getter
    public static class UpdateRequest {
        private String title;
        private String description;
        private Boolean isPublic;
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

    // ============ Response DTOs ============

    @Getter
    @Builder
    public static class SimpleResponse {
        private Long id;
        private String title;
        private String description;
        private Boolean isPublic;
        private Integer addedCount;
        private Double averageRating;
        private Integer reviewCount;
        private Integer missionCount;
        private CreatorInfo creator;
        private LocalDateTime createdAt;

        public static SimpleResponse from(MissionSet missionSet) {
            // NPE 방어: missions, creator null 체크
            int missionCount = missionSet.getMissions() != null ? missionSet.getMissions().size() : 0;
            CreatorInfo creatorInfo = null;
            if (missionSet.getCreator() != null) {
                creatorInfo = CreatorInfo.builder()
                        .id(missionSet.getCreator().getId())
                        .nickname(missionSet.getCreator().getNickname())
                        .build();
            }

            return SimpleResponse.builder()
                    .id(missionSet.getId())
                    .title(missionSet.getTitle())
                    .description(missionSet.getDescription())
                    .isPublic(missionSet.getIsPublic())
                    .addedCount(missionSet.getAddedCount())
                    .averageRating(missionSet.getAverageRating())
                    .reviewCount(missionSet.getReviewCount())
                    .missionCount(missionCount)
                    .creator(creatorInfo)
                    .createdAt(missionSet.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long id;
        private String title;
        private String description;
        private Boolean isPublic;
        private Integer addedCount;
        private Double averageRating;
        private Integer reviewCount;
        private CreatorInfo creator;
        private List<MissionInfo> missions;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static DetailResponse from(MissionSet missionSet) {
            // NPE 방어: missions, creator null 체크
            CreatorInfo creatorInfo = null;
            if (missionSet.getCreator() != null) {
                creatorInfo = CreatorInfo.builder()
                        .id(missionSet.getCreator().getId())
                        .nickname(missionSet.getCreator().getNickname())
                        .build();
            }

            List<MissionInfo> missionInfos = missionSet.getMissions() != null
                    ? missionSet.getMissions().stream().map(MissionInfo::from).collect(Collectors.toList())
                    : new ArrayList<>();

            return DetailResponse.builder()
                    .id(missionSet.getId())
                    .title(missionSet.getTitle())
                    .description(missionSet.getDescription())
                    .isPublic(missionSet.getIsPublic())
                    .addedCount(missionSet.getAddedCount())
                    .averageRating(missionSet.getAverageRating())
                    .reviewCount(missionSet.getReviewCount())
                    .creator(creatorInfo)
                    .missions(missionInfos)
                    .createdAt(missionSet.getCreatedAt())
                    .updatedAt(missionSet.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CreatorInfo {
        private Long id;
        private String nickname;
    }

    @Getter
    @Builder
    public static class MissionInfo {
        private Long id;
        private Long missionId;
        private String title;
        private String description;
        private String missionSource;
        private String verificationType;
        private Integer displayOrder;

        public static MissionInfo from(MissionSetMission msm) {
            Mission mission = msm.getMission();
            return MissionInfo.builder()
                    .id(msm.getId())
                    .missionId(mission.getId())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .missionSource(mission.getMissionSource().name())
                    .verificationType(mission.getVerificationType().name())
                    .displayOrder(msm.getDisplayOrder())
                    .build();
        }
    }
}
