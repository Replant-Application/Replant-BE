package com.app.replant.domain.mission.service;

import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.mission.dto.*;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.review.entity.MissionReview;
import com.app.replant.domain.review.repository.MissionReviewRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final MissionReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;

    public Page<MissionResponse> getMissions(MissionCategory category, VerificationType verificationType, Pageable pageable) {
        return missionRepository.findMissions(category, verificationType, pageable)
                .map(MissionResponse::from);
    }

    /**
     * 사용자 맞춤 필터링된 미션 목록 조회
     */
    public Page<MissionResponse> getFilteredMissions(
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        return missionRepository.findFilteredMissions(
                category, verificationType, worryType, ageRange, genderType, regionType, difficultyLevel, pageable
        ).map(MissionResponse::from);
    }

    public MissionResponse getMission(Long missionId) {
        Mission mission = findMissionById(missionId);
        long reviewCount = reviewRepository.countByMissionId(missionId);
        
        return MissionResponse.from(mission, reviewCount, 0);
    }

    public Page<MissionReviewResponse> getReviews(Long missionId, Pageable pageable) {
        findMissionById(missionId);
        return reviewRepository.findByMissionId(missionId, pageable)
                .map(MissionReviewResponse::from);
    }

    @Transactional
    public MissionReviewResponse createReview(Long missionId, Long userId, MissionReviewRequest request) {
        Mission mission = findMissionById(missionId);
        User user = findUserById(userId);

        if (reviewRepository.existsByMissionIdAndUserId(missionId, userId)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        var badge = userBadgeRepository.findValidBadgeForMission(userId, missionId, LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.BADGE_REQUIRED));

        MissionReview review = MissionReview.builder()
                .mission(mission)
                .user(user)
                .badge(badge)
                .content(request.getContent())
                .rating(request.getRating())
                .build();

        MissionReview saved = reviewRepository.save(review);
        return MissionReviewResponse.from(saved);
    }



    // ============ 관리자 미션 관리 ============

    @Transactional
    public MissionResponse createMission(MissionRequest request) {
        Mission mission = Mission.officialBuilder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .verificationType(request.getVerificationType())
                .requiredMinutes(request.getRequiredMinutes())
                .expReward(request.getExpReward())
                .badgeDurationDays(request.getBadgeDurationDays())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                // 사용자 맞춤 필드
                .worryType(request.getWorryType())
                .ageRanges(request.getAgeRanges())
                .genderType(request.getGenderType())
                .regionType(request.getRegionType())
                .placeType(request.getPlaceType())
                .difficultyLevel(request.getDifficultyLevel())
                .build();

        Mission saved = missionRepository.save(mission);
        return MissionResponse.from(saved);
    }

    @Transactional
    public MissionResponse updateMission(Long missionId, MissionRequest request) {
        Mission mission = findMissionById(missionId);

        mission.updateOfficial(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getVerificationType(),
                request.getRequiredMinutes(),
                request.getExpReward(),
                request.getBadgeDurationDays(),
                // 사용자 맞춤 필드
                request.getWorryType(),
                request.getAgeRanges(),
                request.getGenderType(),
                request.getRegionType(),
                request.getPlaceType(),
                request.getDifficultyLevel()
        );

        if (request.getIsActive() != null) {
            mission.setActive(request.getIsActive());
        }

        return MissionResponse.from(mission);
    }

    /**
     * 미션 대량 등록
     */
    @Transactional
    public List<MissionResponse> bulkCreateMissions(List<MissionRequest> requests) {
        List<Mission> missions = requests.stream()
                .map(request -> Mission.officialBuilder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .category(request.getCategory())
                        .verificationType(request.getVerificationType())
                        .requiredMinutes(request.getRequiredMinutes())
                        .expReward(request.getExpReward())
                        .badgeDurationDays(request.getBadgeDurationDays())
                        .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                        // 사용자 맞춤 필드
                        .worryType(request.getWorryType())
                        .ageRanges(request.getAgeRanges())
                        .genderType(request.getGenderType())
                        .regionType(request.getRegionType())
                        .placeType(request.getPlaceType())
                        .difficultyLevel(request.getDifficultyLevel())
                        .build())
                .collect(Collectors.toList());

        List<Mission> savedMissions = missionRepository.saveAll(missions);
        return savedMissions.stream()
                .map(MissionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMission(Long missionId) {
        Mission mission = findMissionById(missionId);
        missionRepository.delete(mission);
    }

    @Transactional
    public MissionResponse toggleMissionActive(Long missionId, Boolean isActive) {
        Mission mission = findMissionById(missionId);
        mission.setActive(isActive);
        return MissionResponse.from(mission);
    }

    // ============ 커스텀 미션 관리 ============

    /**
     * 커스텀 미션 목록 조회 (공개된 것만)
     */
    public Page<MissionResponse> getCustomMissions(VerificationType verificationType, Pageable pageable) {
        return missionRepository.findCustomMissions(verificationType, pageable)
                .map(MissionResponse::from);
    }

    /**
     * 커스텀 미션 상세 조회
     */
    public MissionResponse getCustomMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        if (!mission.isCustomMission()) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        return MissionResponse.from(mission);
    }

    /**
     * 커스텀 미션 생성
     * 
     * 커스텀 미션은 경험치를 지급하지 않습니다.
     * request의 expReward 값은 무시되며, 생성된 미션의 expReward는 항상 0입니다.
     */
    @Transactional
    public MissionResponse createCustomMission(Long userId, MissionRequest request) {
        User user = findUserById(userId);

        // Static Factory Method를 직접 호출하여 missionType이 확실히 CUSTOM으로 설정되도록 함
        // 커스텀 미션은 경험치 지급 없음 (expReward는 항상 0으로 설정됨)
        Mission mission = Mission.createCustomMission(
                user,                               // creator
                request.getTitle(),                 // title
                request.getDescription(),           // description
                request.getWorryType(),             // worryType
                request.getCategory(),              // category
                request.getDifficultyLevel(),       // difficultyLevel
                request.getIsChallenge(),           // isChallenge
                request.getChallengeDays(),         // challengeDays
                request.getDeadlineDays(),          // deadlineDays
                request.getDurationDays(),          // durationDays
                request.getIsPublic(),              // isPublic
                request.getVerificationType(),      // verificationType
                request.getRequiredMinutes(),       // requiredMinutes
                null,                               // startTime
                null,                               // endTime
                null,                               // expReward (무시됨, Entity에서 0으로 설정)
                request.getBadgeDurationDays(),     // badgeDurationDays
                true                                // isActive
        );

        log.info("커스텀 미션 생성: title={}, missionType={}, userId={}",
                mission.getTitle(), mission.getMissionType(), userId);

        Mission saved = missionRepository.save(mission);
        return MissionResponse.from(saved);
    }

    /**
     * 커스텀 미션 수정
     * 
     * 커스텀 미션은 경험치를 지급하지 않습니다.
     * request의 expReward 값은 무시되며, 수정 후에도 expReward는 항상 0으로 유지됩니다.
     */
    @Transactional
    public MissionResponse updateCustomMission(Long missionId, Long userId, MissionRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        if (!mission.isCustomMission()) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        if (!mission.isCreator(userId)) {
            throw new CustomException(ErrorCode.NOT_MISSION_CREATOR);
        }

        // 커스텀 미션은 경험치 지급 없음 (expReward는 무시됨, Entity에서 0으로 설정)
        mission.updateCustom(
                request.getTitle(),
                request.getDescription(),
                request.getWorryType(),
                request.getCategory(),
                request.getDifficultyLevel(),
                request.getIsChallenge(),
                request.getChallengeDays(),
                request.getDeadlineDays(),
                null,  // expReward (무시됨, Entity에서 0으로 설정)
                request.getIsPublic()
        );

        return MissionResponse.from(mission);
    }

    /**
     * 커스텀 미션 삭제
     */
    @Transactional
    public void deleteCustomMission(Long missionId, Long userId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        if (!mission.isCustomMission()) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        if (!mission.isCreator(userId)) {
            throw new CustomException(ErrorCode.NOT_MISSION_CREATOR);
        }

        missionRepository.delete(mission);
    }

    private Mission findMissionById(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
