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
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    
    @PersistenceContext
    private EntityManager entityManager;

    public Page<MissionResponse> getMissions(MissionCategory category, VerificationType verificationType, Pageable pageable) {
        return getMissions(category, verificationType, pageable, null);
    }

    public Page<MissionResponse> getMissions(MissionCategory category, VerificationType verificationType, Pageable pageable, Long userId) {
        // 전체 미션을 먼저 조회 (페이지네이션 없이)
        // Pageable.unpaged()를 사용하여 전체 미션을 조회
        Page<Mission> allMissions = missionRepository.findMissions(category, verificationType, Pageable.unpaged());
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회 (미션 도감용: 과거 포함 전체 수행 이력)
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !allMissions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findAllByUserIdAndMissionIds(
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
        
        // 미션별 참여자 수 일괄 조회
        List<Long> missionIds = allMissions.getContent().stream()
                .map(Mission::getId)
                .collect(Collectors.toList());
        Map<Long, Long> participantCountMap = userMissionRepository.countDistinctUsersByMissionIds(missionIds);
        
        // MissionResponse로 변환 (전체 미션)
        // 트랜잭션 범위 내에서 ageRanges 일괄 초기화 (N+1 문제 방지)
        initializeAgeRangesBatch(allMissions.getContent());
        
        List<MissionResponse> allMissionResponses = allMissions.getContent().stream()
                .map(mission -> {
                    boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
                    boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
                    Long participantCount = participantCountMap.getOrDefault(mission.getId(), 0L);
                    return MissionResponse.from(mission, isAttempted, isCompleted, participantCount);
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
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회 (미션 도감용: 과거 포함 전체 수행 이력)
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findAllByUserIdAndMissionIds(
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
        
        // 미션별 참여자 수 일괄 조회
        List<Long> missionIds = missions.getContent().stream()
                .map(Mission::getId)
                .collect(Collectors.toList());
        Map<Long, Long> participantCountMap = userMissionRepository.countDistinctUsersByMissionIds(missionIds);
        
        // 트랜잭션 범위 내에서 ageRanges 일괄 초기화 (N+1 문제 방지)
        initializeAgeRangesBatch(missions.getContent());
        
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            Long participantCount = participantCountMap.getOrDefault(mission.getId(), 0L);
            return MissionResponse.from(mission, isAttempted, isCompleted, participantCount);
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
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회 (미션 도감용: 과거 포함 전체 수행 이력)
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findAllByUserIdAndMissionIds(
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
        
        // 미션별 참여자 수 일괄 조회
        List<Long> missionIds = missions.getContent().stream()
                .map(Mission::getId)
                .collect(Collectors.toList());
        Map<Long, Long> participantCountMap = userMissionRepository.countDistinctUsersByMissionIds(missionIds);
        
        // 트랜잭션 범위 내에서 ageRanges 일괄 초기화 (N+1 문제 방지)
        initializeAgeRangesBatch(missions.getContent());
        
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            Long participantCount = participantCountMap.getOrDefault(mission.getId(), 0L);
            return MissionResponse.from(mission, isAttempted, isCompleted, participantCount);
        });
    }

    public MissionResponse getMission(Long missionId) {
        return getMission(missionId, null);
    }

    public MissionResponse getMission(Long missionId, Long userId) {
        Mission mission = findMissionById(missionId);
        // 트랜잭션 범위 내에서 ageRanges 초기화
        initializeAgeRanges(mission);
        
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
        
        // 참여자 수 조회
        long participantCount = userMissionRepository.countDistinctUsersByMissionId(missionId);
        
        return MissionResponse.from(mission, reviewCount, isAttempted, isCompleted, participantCount);
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
                .rating(request.getRating() != null ? request.getRating() : 5)
                .build();

        MissionReview saved = reviewRepository.save(review);
        return MissionReviewResponse.from(saved);
    }

    @Transactional
    public void deleteReview(Long missionId, Long reviewId, Long userId) {
        findMissionById(missionId);
        MissionReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getMission().getId().equals(missionId)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }
        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_AUTHOR);
        }
        reviewRepository.delete(review);
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
        // 새로 생성된 미션이므로 참여자 수는 0
        return MissionResponse.from(saved, false, false, 0L);
    }

    @Transactional
    public MissionResponse updateMission(Long missionId, MissionRequest request) {
        Mission mission = findMissionById(missionId);
        // 트랜잭션 범위 내에서 ageRanges 초기화
        initializeAgeRanges(mission);

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

        // 참여자 수 조회
        long participantCount = userMissionRepository.countDistinctUsersByMissionId(missionId);
        return MissionResponse.from(mission, false, false, participantCount);
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
        // 새로 생성된 미션이므로 참여자 수는 모두 0
        return savedMissions.stream()
                .map(mission -> MissionResponse.from(mission, false, false, 0L))
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
        // 트랜잭션 범위 내에서 ageRanges 초기화
        initializeAgeRanges(mission);
        
        mission.setActive(isActive);
        // 참여자 수 조회
        long participantCount = userMissionRepository.countDistinctUsersByMissionId(missionId);
        return MissionResponse.from(mission, false, false, participantCount);
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
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회 (미션 도감용: 과거 포함 전체 수행 이력)
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findAllByUserIdAndMissionIds(
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
        
        // 미션별 참여자 수 일괄 조회
        List<Long> missionIds = missions.getContent().stream()
                .map(Mission::getId)
                .collect(Collectors.toList());
        Map<Long, Long> participantCountMap = userMissionRepository.countDistinctUsersByMissionIds(missionIds);
        
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            Long participantCount = participantCountMap.getOrDefault(mission.getId(), 0L);
            return MissionResponse.from(mission, isAttempted, isCompleted, participantCount);
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
        
        // 사용자가 수행한 미션 ID 목록 및 완료한 미션 ID 목록 조회 (미션 도감용: 과거 포함 전체 수행 이력)
        Set<Long> attemptedMissionIds = Collections.emptySet();
        Set<Long> completedMissionIds = Collections.emptySet();
        if (userId != null && !missions.getContent().isEmpty()) {
            List<UserMission> userMissions = userMissionRepository.findAllByUserIdAndMissionIds(
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
        
        // 미션별 참여자 수 일괄 조회
        List<Long> missionIds = missions.getContent().stream()
                .map(Mission::getId)
                .collect(Collectors.toList());
        Map<Long, Long> participantCountMap = userMissionRepository.countDistinctUsersByMissionIds(missionIds);
        
        // 트랜잭션 범위 내에서 ageRanges 일괄 초기화 (N+1 문제 방지)
        initializeAgeRangesBatch(missions.getContent());
        
        return missions.map(mission -> {
            boolean isAttempted = userId != null && finalAttemptedMissionIds.contains(mission.getId());
            boolean isCompleted = userId != null && finalCompletedMissionIds.contains(mission.getId());
            Long participantCount = participantCountMap.getOrDefault(mission.getId(), 0L);
            return MissionResponse.from(mission, isAttempted, isCompleted, participantCount);
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

        // 트랜잭션 범위 내에서 ageRanges 초기화
        initializeAgeRanges(mission);

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

        // 참여자 수 조회
        long participantCount = userMissionRepository.countDistinctUsersByMissionId(missionId);

        return MissionResponse.from(mission, isAttempted, isCompleted, participantCount);
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
        // 새로 생성된 미션이므로 참여자 수는 0
        return MissionResponse.from(saved, false, false, 0L);
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

        // 트랜잭션 범위 내에서 ageRanges 초기화
        initializeAgeRanges(mission);

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

        // 참여자 수 조회
        long participantCount = userMissionRepository.countDistinctUsersByMissionId(missionId);
        return MissionResponse.from(mission, false, false, participantCount);
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
        // N+1 문제 방지를 위해 reant를 함께 로드
        return userRepository.findByIdWithReant(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Mission의 ageRanges를 트랜잭션 범위 내에서 초기화
     * LazyInitializationException 방지를 위해 사용
     */
    private void initializeAgeRanges(Mission mission) {
        try {
            if (mission.getAgeRanges() != null) {
                // 명시적으로 접근하여 초기화
                mission.getAgeRanges().size();
            }
        } catch (Exception e) {
            // 초기화 실패 시 무시 (나중에 MissionResponse에서 처리)
        }
    }

    /**
     * 여러 Mission의 ageRanges를 한 번에 초기화 (N+1 문제 방지)
     * EntityManager를 사용하여 한 번의 쿼리로 모든 ageRanges를 로드
     */
    private void initializeAgeRangesBatch(List<Mission> missions) {
        if (missions == null || missions.isEmpty()) {
            return;
        }
        
        List<Long> missionIds = missions.stream()
                .map(Mission::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (missionIds.isEmpty()) {
            return;
        }
        
        // 한 번의 쿼리로 모든 Mission의 ageRanges를 로드
        // ElementCollection은 JOIN FETCH로 함께 조회 가능
        String jpql = "SELECT DISTINCT m FROM Mission m " +
                      "LEFT JOIN FETCH m.ageRanges " +
                      "WHERE m.id IN :missionIds";
        
        try {
            List<Mission> missionsWithAgeRanges = entityManager.createQuery(jpql, Mission.class)
                    .setParameter("missionIds", missionIds)
                    .getResultList();
            
            // 결과를 Map으로 변환하여 빠른 조회
            Map<Long, Mission> missionMap = missionsWithAgeRanges.stream()
                    .collect(Collectors.toMap(Mission::getId, m -> m, (m1, m2) -> m1));
            
            // 원본 Mission 리스트의 ageRanges를 초기화된 것으로 교체
            // Hibernate는 같은 세션에서 같은 ID의 엔티티를 공유하므로,
            // missionsWithAgeRanges에서 로드된 엔티티가 원본 missions의 엔티티와 동일한 인스턴스입니다.
            missions.forEach(mission -> {
                Mission initializedMission = missionMap.get(mission.getId());
                if (initializedMission != null) {
                    // 같은 세션 내에서 같은 엔티티이므로 ageRanges가 이미 초기화되어 있음
                    try {
                        // 명시적으로 접근하여 초기화 확인
                        if (initializedMission.getAgeRanges() != null) {
                            initializedMission.getAgeRanges().size();
                        }
                    } catch (Exception e) {
                        // 초기화 실패 시 무시
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Failed to initialize ageRanges in batch: {}", e.getMessage());
            // 실패 시 개별 초기화로 폴백
            missions.forEach(this::initializeAgeRanges);
        }
    }

    /**
     * [DEBUG] 특정 이메일 사용자의 공식 미션 수행/완료 상태 확인
     */
    public Map<String, Object> getUserMissionStatusForDebug(String email) {
        // N+1 문제 방지를 위해 reant를 함께 로드
        User user = userRepository.findByEmailWithReant(email)
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
