package com.app.replant.domain.recommendation.service;

import com.app.replant.domain.chat.entity.ChatRoom;
import com.app.replant.domain.chat.repository.ChatRoomRepository;
import com.app.replant.domain.recommendation.dto.AcceptRecommendationResponse;
import com.app.replant.domain.recommendation.dto.RecommendationResponse;
import com.app.replant.domain.recommendation.entity.UserRecommendation;
import com.app.replant.domain.recommendation.repository.UserRecommendationRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserRecommendationRepository userRecommendationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserMissionRepository userMissionRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_RECOMMENDATIONS_PER_MISSION = 3;
    private static final int RECOMMENDATION_EXPIRY_DAYS = 7;

    public List<RecommendationResponse> getRecommendations(Long userId, String status) {
        UserRecommendation.RecommendationStatus recommendationStatus = null;
        if (status != null) {
            try {
                recommendationStatus = UserRecommendation.RecommendationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_RECOMMENDATION_STATUS);
            }
        }

        return userRecommendationRepository.findByUserIdAndStatus(userId, recommendationStatus)
                .stream()
                .map(RecommendationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AcceptRecommendationResponse acceptRecommendation(Long recommendationId, Long userId) {
        UserRecommendation recommendation = findRecommendationByIdAndUserId(recommendationId, userId);

        if (recommendation.getStatus() != UserRecommendation.RecommendationStatus.PENDING) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED);
        }

        recommendation.accept();

        // Create ChatRoom
        User user = recommendation.getUser();
        User recommendedUser = recommendation.getRecommendedUser();

        ChatRoom chatRoom = ChatRoom.builder()
                .recommendation(recommendation)
                .user1(user)
                .user2(recommendedUser)
                .isActive(true)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        User otherUser = savedChatRoom.getOtherUser(userId);

        return AcceptRecommendationResponse.builder()
                .recommendationId(recommendationId)
                .status("ACCEPTED")
                .chatRoom(AcceptRecommendationResponse.ChatRoomInfo.builder()
                        .id(savedChatRoom.getId())
                        .otherUser(AcceptRecommendationResponse.OtherUserInfo.builder()
                                .id(otherUser.getId())
                                .nickname(otherUser.getNickname())
                                .profileImg(otherUser.getProfileImg())
                                .build())
                        .createdAt(savedChatRoom.getCreatedAt())
                        .build())
                .message("추천을 수락하고 채팅방이 생성되었습니다.")
                .build();
    }

    @Transactional
    public void rejectRecommendation(Long recommendationId, Long userId) {
        UserRecommendation recommendation = findRecommendationByIdAndUserId(recommendationId, userId);

        if (recommendation.getStatus() != UserRecommendation.RecommendationStatus.PENDING) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED);
        }

        recommendation.reject();
    }

    private UserRecommendation findRecommendationByIdAndUserId(Long recommendationId, Long userId) {
        return userRecommendationRepository.findByIdAndUserId(recommendationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND));
    }

    /**
     * 미션 완료 시 유사 유저 추천 생성
     * - 같은 미션을 최근에 완료한 다른 유저들을 추천
     */
    @Transactional
    public List<UserRecommendation> generateRecommendationsForCompletedMission(UserMission completedUserMission) {
        User user = completedUserMission.getUser();
        Long userId = user.getId();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(RECOMMENDATION_EXPIRY_DAYS);

        List<UserMission> similarUserMissions;

        // 시스템 미션인 경우
        if (completedUserMission.getMission() != null) {
            Long missionId = completedUserMission.getMission().getId();
            similarUserMissions = userMissionRepository.findRecentCompletedByMissionExcludingUser(
                    missionId, userId, PageRequest.of(0, MAX_RECOMMENDATIONS_PER_MISSION));
        }
        // 커스텀 미션인 경우
        else if (completedUserMission.getCustomMission() != null) {
            Long customMissionId = completedUserMission.getCustomMission().getId();
            similarUserMissions = userMissionRepository.findRecentCompletedByCustomMissionExcludingUser(
                    customMissionId, userId, PageRequest.of(0, MAX_RECOMMENDATIONS_PER_MISSION));
        } else {
            return Collections.emptyList();
        }

        if (similarUserMissions.isEmpty()) {
            log.info("미션 완료 추천 생성: 유사 유저 없음 - userId={}, userMissionId={}",
                    userId, completedUserMission.getId());
            return Collections.emptyList();
        }

        List<UserRecommendation> recommendations = new ArrayList<>();

        for (UserMission similarUserMission : similarUserMissions) {
            User recommendedUser = similarUserMission.getUser();

            // 매칭 사유 JSON 생성
            String matchReason = buildMatchReason(completedUserMission, similarUserMission);

            UserRecommendation recommendation = UserRecommendation.builder()
                    .user(user)
                    .recommendedUser(recommendedUser)
                    .mission(completedUserMission.getMission())
                    .customMission(completedUserMission.getCustomMission())
                    .userMission(completedUserMission)
                    .matchReason(matchReason)
                    .expiresAt(expiresAt)
                    .build();

            recommendations.add(recommendation);
        }

        List<UserRecommendation> saved = userRecommendationRepository.saveAll(recommendations);
        log.info("미션 완료 추천 생성 완료: userId={}, 추천 수={}", userId, saved.size());

        return saved;
    }

    /**
     * 매칭 사유 JSON 생성
     */
    private String buildMatchReason(UserMission userMission, UserMission similarUserMission) {
        try {
            Map<String, Object> reason = new HashMap<>();
            reason.put("type", "SAME_MISSION");
            reason.put("missionTitle", getMissionTitle(userMission));
            reason.put("completedAt", similarUserMission.getCreatedAt().toString());

            return objectMapper.writeValueAsString(reason);
        } catch (Exception e) {
            log.error("매칭 사유 JSON 생성 실패", e);
            return "{\"type\":\"SAME_MISSION\"}";
        }
    }

    /**
     * 미션 타이틀 조회
     */
    private String getMissionTitle(UserMission userMission) {
        if (userMission.getMission() != null) {
            return userMission.getMission().getTitle();
        } else if (userMission.getCustomMission() != null) {
            return userMission.getCustomMission().getTitle();
        }
        return "Unknown Mission";
    }
}
