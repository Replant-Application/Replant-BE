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
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final UserMissionRepository userMissionRepository;

    public Page<MissionResponse> getMissions(MissionCategory category, VerificationType verificationType, Pageable pageable) {
        return getMissions(category, verificationType, pageable, null);
    }

    public Page<MissionResponse> getMissions(MissionCategory category, VerificationType verificationType, Pageable pageable, Long userId) {
        // 전체 미션을 먼저 조회 (페이지네이션 없이)
        // Pageable.unpaged()를 사용하여 전체 미션을 조회
        Page<Mission> allMissions = missionRepository.findMissions(category, verificationType, Pageable.unpaged());
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회 (전체 미션 기준)
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !allMissions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionIds(
                    userId, 
                    allMissions.getContent().stream().map(Mission::getId).collect(Collectors.toList())
            );
            // 수행한 미션 ID 목록 (UserMission이 존재하면 수행한 것으로 간주, 상태 무관)
            attemptedMissionIds = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
            // 완료한 미션 ID 목록 (COMPLETED 상태만)
            completedMissionIds = userMissions.stream()
                    .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
        }
        
        final Set<Long> finalAttemptedMissionIds = attemptedMissionIds;
        final Set<Long> finalCompletedMissionIds = completedMissionIds;
        
        // MissionResponse로 변환 (전체 미션)
        List<MissionResponse> allMissionResponses = allMissions.getContent().stream()
                .map(mission -> {
                    boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
                    boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
                    return MissionResponse.from(mission, isAttempted, isCompleted);
                })
                .collect(Collectors.toList());
        
        // 전체 미션을 정렬: 수행한 미션을 먼저, 미수행 미션을 나중에 (자물쇠는 항상 마지막)
        if (userId != null) {
            // 수행한 미션과 미수행 미션을 명확히 분리
            List<MissionResponse> attemptedMissions = new ArrayList<>();
            List<MissionResponse> notAttemptedMissions = new ArrayList<>();
            
            for (MissionResponse mission : allMissionResponses) {
                Boolean isAttempted = mission.getIsAttempted();
                boolean isAttemptedValue = isAttempted != null && isAttempted;
                
                if (isAttemptedValue) {
                    attemptedMissions.add(mission);
                } else {
                    notAttemptedMissions.add(mission);
                }
            }
            
            // 수행한 미션들 + 미수행 미션들 순서로 합치기 (자물쇠는 항상 마지막)
            allMissionResponses = new ArrayList<>();
            allMissionResponses.addAll(attemptedMissions);
            allMissionResponses.addAll(notAttemptedMissions);
        }
        
        // 정렬된 전체 미션에서 페이지네이션 적용
        int totalElements = allMissionResponses.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        
        List<MissionResponse> pagedMissionResponses = start < totalElements 
                ? allMissionResponses.subList(start, end)
                : Collections.emptyList();
        
        return new PageImpl<>(pagedMissionResponses, pageable, totalElements);
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
        return getFilteredMissions(category, verificationType, worryType, ageRange, genderType, regionType, difficultyLevel, pageable, null);
    }

    public Page<MissionResponse> getFilteredMissions(
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable,
            Long userId) {
        Page<Mission> missions = missionRepository.findFilteredMissions(
                category, verificationType, worryType, ageRange, genderType, regionType, difficultyLevel, pageable
        );
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionIds(
                    userId, 
                    missions.getContent().stream().map(Mission::getId).collect(Collectors.toList())
            );
            // 수행한 미션 ID 목록 (UserMission이 존재하면 수행한 것으로 간주, 상태 무관)
            attemptedMissionIds = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
            // 완료한 미션 ID 목록 (COMPLETED 상태만)
            completedMissionIds = userMissions.stream()
                    .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
        }
        
        final Set<Long> finalAttemptedMissionIds = attemptedMissionIds;
        final Set<Long> finalCompletedMissionIds = completedMissionIds;
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            return MissionResponse.from(mission, isAttempted, isCompleted);
        });
    }

    /**
     * 공식 미션 검색 (제목/설명 검색 + 필터링)
     */
    public Page<MissionResponse> searchOfficialMissions(
            String keyword,
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        return searchOfficialMissions(keyword, category, verificationType, worryType, ageRange, genderType, regionType, difficultyLevel, pageable, null);
    }

    public Page<MissionResponse> searchOfficialMissions(
            String keyword,
            MissionCategory category,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable,
            Long userId) {
        Page<Mission> missions = missionRepository.searchOfficialMissions(
                keyword, category, verificationType, worryType, ageRange,
                genderType, regionType, difficultyLevel, pageable
        );
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionIds(
                    userId, 
                    missions.getContent().stream().map(Mission::getId).collect(Collectors.toList())
            );
            // 수행한 미션 ID 목록 (UserMission이 존재하면 수행한 것으로 간주, 상태 무관)
            attemptedMissionIds = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
            // 완료한 미션 ID 목록 (COMPLETED 상태만)
            completedMissionIds = userMissions.stream()
                    .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
        }
        
        final Set<Long> finalAttemptedMissionIds = attemptedMissionIds;
        final Set<Long> finalCompletedMissionIds = completedMissionIds;
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            return MissionResponse.from(mission, isAttempted, isCompleted);
        });
    }

    public MissionResponse getMission(Long missionId) {
        return getMission(missionId, null);
    }

    public MissionResponse getMission(Long missionId, Long userId) {
        Mission mission = findMissionById(missionId);
        long reviewCount = reviewRepository.countByMissionId(missionId);
        
        // 사용자가 해당 미션을 수행했는지 및 완료했는지 확인
        boolean isAttempted = false;
        boolean isCompleted = false;
        if (userId != null) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionId(userId, missionId);
            isAttempted = !userMissions.isEmpty();  // UserMission이 존재하면 수행한 것으로 간주
            isCompleted = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .anyMatch(um -> um.getStatus() == UserMissionStatus.COMPLETED);
        }
        
        return MissionResponse.from(mission, reviewCount, isAttempted, isCompleted);
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
     * 커스텀 미션 목록 조회 (공개된 것만, 전체 조회)
     */
    public Page<MissionResponse> getCustomMissions(Pageable pageable) {
        return getCustomMissions(pageable, null);
    }

    public Page<MissionResponse> getCustomMissions(Pageable pageable, Long userId) {
        Page<Mission> missions = missionRepository.findCustomMissions(pageable);
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionIds(
                    userId, 
                    missions.getContent().stream().map(Mission::getId).collect(Collectors.toList())
            );
            // 수행한 미션 ID 목록 (UserMission이 존재하면 수행한 것으로 간주, 상태 무관)
            attemptedMissionIds = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
            // 완료한 미션 ID 목록 (COMPLETED 상태만)
            completedMissionIds = userMissions.stream()
                    .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
        }
        
        final Set<Long> finalAttemptedMissionIds = attemptedMissionIds;
        final Set<Long> finalCompletedMissionIds = completedMissionIds;
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            return MissionResponse.from(mission, isAttempted, isCompleted);
        });
    }

    /**
     * 커스텀 미션 검색 (제목/설명 검색 + 필터링)
     */
    public Page<MissionResponse> searchCustomMissions(
            String keyword,
            WorryType worryType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        return searchCustomMissions(keyword, worryType, difficultyLevel, pageable, null);
    }

    public Page<MissionResponse> searchCustomMissions(
            String keyword,
            WorryType worryType,
            DifficultyLevel difficultyLevel,
            Pageable pageable,
            Long userId) {
        Page<Mission> missions = missionRepository.searchCustomMissions(
                keyword, worryType, difficultyLevel, pageable
        );
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionIds(
                    userId, 
                    missions.getContent().stream().map(Mission::getId).collect(Collectors.toList())
            );
            // 수행한 미션 ID 목록 (UserMission이 존재하면 수행한 것으로 간주, 상태 무관)
            attemptedMissionIds = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
            // 완료한 미션 ID 목록 (COMPLETED 상태만)
            completedMissionIds = userMissions.stream()
                    .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                    .filter(um -> um.getMission() != null)  // null 체크
                    .map(um -> um.getMission().getId())
                    .filter(id -> id != null)  // null ID 제외
                    .collect(Collectors.toSet());
        }
        
        final Set<Long> finalAttemptedMissionIds = attemptedMissionIds;
        final Set<Long> finalCompletedMissionIds = completedMissionIds;
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            return MissionResponse.from(mission, isAttempted, isCompleted);
        });
    }

    /**
     * 커스텀 미션 상세 조회
     */
    public MissionResponse getCustomMission(Long missionId) {
        return getCustomMission(missionId, null);
    }

    public MissionResponse getCustomMission(Long missionId, Long userId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        if (!mission.isCustomMission()) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND);
        }

        // 사용자가 해당 미션을 수행했는지 및 완료했는지 확인
        boolean isAttempted = false;
        boolean isCompleted = false;
        if (userId != null) {
            List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionId(userId, missionId);
            isAttempted = !userMissions.isEmpty();  // UserMission이 존재하면 수행한 것으로 간주
            isCompleted = userMissions.stream()
                    .filter(um -> um.getMission() != null)  // null 체크
                    .anyMatch(um -> um.getStatus() == UserMissionStatus.COMPLETED);
        }

        return MissionResponse.from(mission, isAttempted, isCompleted);
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

    /**
     * [DEBUG] 특정 이메일 사용자의 공식 미션 수행/완료 상태 확인
     */
    public Map<String, Object> getUserMissionStatusForDebug(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        Long userId = user.getId();
        
        // 전체 공식 미션 개수 및 ID 목록
        Page<Mission> allOfficialMissions = missionRepository.findMissions(null, null, Pageable.unpaged());
        long totalOfficialMissions = allOfficialMissions.getTotalElements();
        List<Long> allOfficialMissionIds = allOfficialMissions.getContent().stream()
                .map(Mission::getId)
                .collect(Collectors.toList());
        
        // 사용자가 수행한 공식 미션들 조회 (전체 기간)
        List<UserMission> officialUserMissions = allOfficialMissionIds.isEmpty() 
                ? Collections.emptyList()
                : userMissionRepository.findByUserIdAndMissionIds(userId, allOfficialMissionIds);
        
        // 완료한 미션들
        List<UserMission> completedMissions = officialUserMissions.stream()
                .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                .collect(Collectors.toList());
        
        // 미완료 미션들
        List<UserMission> incompleteMissions = officialUserMissions.stream()
                .filter(um -> um.getStatus() != UserMissionStatus.COMPLETED)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("email", email);
        result.put("nickname", user.getNickname());
        result.put("totalOfficialMissions", totalOfficialMissions);
        result.put("attemptedCount", officialUserMissions.size());
        result.put("completedCount", completedMissions.size());
        result.put("incompleteCount", incompleteMissions.size());
        result.put("notAttemptedCount", totalOfficialMissions - officialUserMissions.size());
        
        // 화면 표시 요약
        Map<String, Object> screenDisplay = new HashMap<>();
        screenDisplay.put("MissionScreen_공식미션탭_정상카드표시", completedMissions.size() + "개 (완료한 미션)");
        screenDisplay.put("MissionScreen_공식미션탭_자물쇠표시", (incompleteMissions.size() + (totalOfficialMissions - officialUserMissions.size())) + "개 (미완료 + 미수행)");
        screenDisplay.put("MissionGroupScreen_표시되는미션", officialUserMissions.size() + "개 (수행한 미션만)");
        screenDisplay.put("MissionGroupScreen_정상정보표시", completedMissions.size() + "개 (완료한 미션)");
        screenDisplay.put("MissionGroupScreen_물음표마스킹", incompleteMissions.size() + "개 (미완료 미션)");
        
        result.put("screenDisplay", screenDisplay);
        
        // 샘플 미션 목록 (최근 10개)
        List<Map<String, Object>> sampleMissions = officialUserMissions.stream()
                .limit(10)
                .map(um -> {
                    Map<String, Object> missionInfo = new HashMap<>();
                    missionInfo.put("missionId", um.getMission().getId());
                    missionInfo.put("title", um.getMission().getTitle());
                    missionInfo.put("status", um.getStatus().toString());
                    missionInfo.put("isCompleted", um.getStatus() == UserMissionStatus.COMPLETED);
                    return missionInfo;
                })
                .collect(Collectors.toList());
        
        result.put("sampleMissions", sampleMissions);
        
        return result;
    }
}
