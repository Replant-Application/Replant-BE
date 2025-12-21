package com.app.replant.domain.usermission.dto;

import com.app.replant.domain.usermission.enums.UserMissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class VerifyMissionResponse {

    private Long userMissionId;
    private UserMissionStatus status;
    private VerificationInfo verification;
    private RewardInfo rewards;
    private RecommendationInfo recommendation;

    @Getter
    @Builder
    public static class VerificationInfo {
        // GPS 인증
        private BigDecimal gpsLatitude;
        private BigDecimal gpsLongitude;
        private Integer gpsDistanceMeters;

        // TIME 인증
        private LocalDateTime timeStartedAt;
        private LocalDateTime timeEndedAt;
        private Integer timeActualMinutes;

        private LocalDateTime verifiedAt;
    }

    @Getter
    @Builder
    public static class RewardInfo {
        private Integer expEarned;
        private BadgeInfo badge;
    }

    @Getter
    @Builder
    public static class BadgeInfo {
        private Long id;
        private LocalDateTime expiresAt;
    }

    @Getter
    @Builder
    public static class RecommendationInfo {
        private Long id;
        private Long recommendedUserId;
        private String recommendedUserNickname;
    }
}
