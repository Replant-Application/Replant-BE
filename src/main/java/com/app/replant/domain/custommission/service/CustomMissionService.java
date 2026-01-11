package com.app.replant.domain.custommission.service;

import com.app.replant.domain.custommission.dto.CustomMissionRequest;
import com.app.replant.domain.custommission.dto.CustomMissionResponse;
import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.custommission.repository.CustomMissionRepository;
import com.app.replant.domain.mission.enums.MissionType;
import com.app.replant.domain.mission.enums.VerificationType;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomMissionService {

    private final CustomMissionRepository customMissionRepository;
    private final UserRepository userRepository;

    public Page<CustomMissionResponse> getCustomMissions(VerificationType verificationType, Pageable pageable) {
        return customMissionRepository.findPublicMissions(verificationType, pageable)
                .map(CustomMissionResponse::from);
    }

    public CustomMissionResponse getCustomMission(Long customMissionId) {
        CustomMission customMission = findCustomMissionById(customMissionId);
        return CustomMissionResponse.from(customMission);
    }

    @Transactional
    public CustomMissionResponse createCustomMission(Long userId, CustomMissionRequest request) {
        User user = findUserById(userId);

        CustomMission customMission = CustomMission.builder()
                .creator(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .worryType(request.getWorryType())
                .missionType(MissionType.CUSTOM)  // 커스텀 미션은 항상 CUSTOM 타입
                .difficultyLevel(request.getDifficultyLevel())
                .isChallenge(request.getIsChallenge())
                .challengeDays(request.getChallengeDays())
                .deadlineDays(request.getDeadlineDays())
                .durationDays(request.getDurationDays())
                .isPublic(request.getIsPublic())
                .verificationType(request.getVerificationType())
                .gpsLatitude(request.getGpsLatitude())
                .gpsLongitude(request.getGpsLongitude())
                .gpsRadiusMeters(request.getGpsRadiusMeters())
                .requiredMinutes(request.getRequiredMinutes())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .expReward(request.getExpReward())
                .badgeDurationDays(request.getBadgeDurationDays())
                .isActive(true)
                .isPromoted(false)
                .build();

        CustomMission saved = customMissionRepository.save(customMission);

        log.info("커스텀 미션 생성 완료: id={}, title={}, userId={}, missionType=CUSTOM",
                saved.getId(), saved.getTitle(), userId);

        return CustomMissionResponse.from(saved);
    }

    @Transactional
    public CustomMissionResponse updateCustomMission(Long customMissionId, Long userId, CustomMissionRequest request) {
        CustomMission customMission = findCustomMissionById(customMissionId);

        if (!customMission.isCreator(userId)) {
            throw new CustomException(ErrorCode.NOT_MISSION_CREATOR);
        }

        // missionType은 항상 CUSTOM으로 유지되어야 하므로 null 전달 (변경 불가)
        customMission.update(request.getTitle(), request.getDescription(), request.getWorryType(),
                null, request.getDifficultyLevel(), request.getIsChallenge(),
                request.getChallengeDays(), request.getDeadlineDays(), request.getExpReward(), request.getIsPublic());
        return CustomMissionResponse.from(customMission);
    }

    @Transactional
    public void deleteCustomMission(Long customMissionId, Long userId) {
        CustomMission customMission = findCustomMissionById(customMissionId);

        if (!customMission.isCreator(userId)) {
            throw new CustomException(ErrorCode.NOT_MISSION_CREATOR);
        }

        customMissionRepository.delete(customMission);
    }

    private CustomMission findCustomMissionById(Long customMissionId) {
        return customMissionRepository.findById(customMissionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_MISSION_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
