package com.app.replant.domain.mission.service;

import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.mission.dto.*;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.enums.*;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.qna.entity.MissionQnA;
import com.app.replant.domain.qna.entity.MissionQnAAnswer;
import com.app.replant.domain.qna.repository.MissionQnAAnswerRepository;
import com.app.replant.domain.qna.repository.MissionQnARepository;
import com.app.replant.domain.review.entity.MissionReview;
import com.app.replant.domain.review.repository.MissionReviewRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final MissionReviewRepository reviewRepository;
    private final MissionQnARepository qnaRepository;
    private final MissionQnAAnswerRepository qnaAnswerRepository;
    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;

    public Page<MissionResponse> getMissions(MissionType type, VerificationType verificationType, Pageable pageable) {
        return missionRepository.findMissions(type, verificationType, pageable)
                .map(MissionResponse::from);
    }

    /**
     * 사용자 맞춤 필터링된 미션 목록 조회
     */
    public Page<MissionResponse> getFilteredMissions(
            MissionType type,
            VerificationType verificationType,
            WorryType worryType,
            AgeRange ageRange,
            GenderType genderType,
            RegionType regionType,
            DifficultyLevel difficultyLevel,
            Pageable pageable) {
        return missionRepository.findFilteredMissions(
                type, verificationType, worryType, ageRange, genderType, regionType, difficultyLevel, pageable
        ).map(MissionResponse::from);
    }

    public MissionResponse getMission(Long missionId) {
        Mission mission = findMissionById(missionId);
        long reviewCount = reviewRepository.countByMissionId(missionId);
        long qnaCount = qnaRepository.countByMissionId(missionId);
        return MissionResponse.from(mission, reviewCount, qnaCount);
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

        if (!userBadgeRepository.hasValidBadgeForMission(userId, missionId, LocalDateTime.now())) {
            throw new CustomException(ErrorCode.BADGE_REQUIRED);
        }

        MissionReview review = MissionReview.builder()
                .mission(mission)
                .user(user)
                .content(request.getContent())
                .build();

        MissionReview saved = reviewRepository.save(review);
        return MissionReviewResponse.from(saved);
    }

    public Page<MissionQnAResponse> getQnAList(Long missionId, Pageable pageable) {
        findMissionById(missionId);
        return qnaRepository.findByMissionId(missionId, pageable)
                .map(MissionQnAResponse::from);
    }

    public MissionQnAResponse getQnADetail(Long missionId, Long qnaId) {
        MissionQnA qna = qnaRepository.findByIdAndMissionIdWithAnswers(qnaId, missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
        return MissionQnAResponse.fromWithAnswers(qna);
    }

    @Transactional
    public MissionQnAResponse createQuestion(Long missionId, Long userId, MissionQnARequest request) {
        Mission mission = findMissionById(missionId);
        User user = findUserById(userId);

        MissionQnA qna = MissionQnA.builder()
                .mission(mission)
                .questioner(user)
                .question(request.getQuestion())
                .isResolved(false)
                .build();

        MissionQnA saved = qnaRepository.save(qna);
        return MissionQnAResponse.from(saved);
    }

    @Transactional
    public MissionQnAResponse.AnswerInfo createAnswer(Long missionId, Long qnaId, Long userId, MissionQnAAnswerRequest request) {
        MissionQnA qna = qnaRepository.findByIdAndMissionIdWithAnswers(qnaId, missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
        User user = findUserById(userId);

        if (!userBadgeRepository.hasValidBadgeForMission(userId, missionId, LocalDateTime.now())) {
            throw new CustomException(ErrorCode.BADGE_REQUIRED);
        }

        MissionQnAAnswer answer = MissionQnAAnswer.builder()
                .qna(qna)
                .answerer(user)
                .content(request.getContent())
                .isAccepted(false)
                .build();

        MissionQnAAnswer saved = qnaAnswerRepository.save(answer);
        return MissionQnAResponse.AnswerInfo.from(saved);
    }

    @Transactional
    public void acceptAnswer(Long missionId, Long qnaId, Long answerId, Long userId) {
        MissionQnA qna = qnaRepository.findByIdAndMissionIdWithAnswers(qnaId, missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));

        if (!qna.getQuestioner().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_QUESTIONER);
        }

        MissionQnAAnswer answer = qnaAnswerRepository.findByIdAndQnaId(answerId, qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));

        if (qna.getIsResolved()) {
            throw new CustomException(ErrorCode.ANSWER_ALREADY_ACCEPTED);
        }

        answer.accept();
        qna.resolve();
    }

    // ============ 관리자 미션 관리 ============

    @Transactional
    public MissionResponse createMission(MissionRequest request) {
        Mission mission = Mission.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .verificationType(request.getVerificationType())
                .gpsLatitude(request.getGpsLatitude())
                .gpsLongitude(request.getGpsLongitude())
                .gpsRadiusMeters(request.getGpsRadiusMeters())
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

        mission.update(
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getVerificationType(),
                request.getGpsLatitude(),
                request.getGpsLongitude(),
                request.getGpsRadiusMeters(),
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
                .map(request -> Mission.builder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .type(request.getType())
                        .verificationType(request.getVerificationType())
                        .gpsLatitude(request.getGpsLatitude())
                        .gpsLongitude(request.getGpsLongitude())
                        .gpsRadiusMeters(request.getGpsRadiusMeters())
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

    private Mission findMissionById(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
