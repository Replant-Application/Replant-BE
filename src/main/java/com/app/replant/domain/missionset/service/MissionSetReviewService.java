package com.app.replant.domain.missionset.service;

import com.app.replant.domain.missionset.dto.MissionSetReviewDto;
import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetReview;
import com.app.replant.domain.missionset.repository.MissionSetRepository;
import com.app.replant.domain.missionset.repository.MissionSetReviewRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MissionSetReviewService {

    private final MissionSetReviewRepository reviewRepository;
    private final MissionSetRepository missionSetRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰 작성
     */
    @Transactional
    public MissionSetReviewDto.Response createReview(Long missionSetId, Long userId, MissionSetReviewDto.CreateRequest request) {
        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 자신의 미션세트에는 리뷰 불가
        if (missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.CANNOT_REVIEW_OWN_SET);
        }

        // 이미 리뷰를 작성한 경우
        if (reviewRepository.existsByMissionSetAndUser(missionSet, user)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        MissionSetReview review = MissionSetReview.builder()
                .missionSet(missionSet)
                .user(user)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        reviewRepository.save(review);

        // 미션세트 평균 별점 업데이트
        updateMissionSetRating(missionSet);

        log.info("미션세트 리뷰 작성: missionSetId={}, userId={}, rating={}", missionSetId, userId, request.getRating());
        return MissionSetReviewDto.Response.from(review);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public MissionSetReviewDto.Response updateReview(Long reviewId, Long userId, MissionSetReviewDto.UpdateRequest request) {
        MissionSetReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 리뷰만 수정 가능
        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        review.update(request.getRating(), request.getContent());

        // 미션세트 평균 별점 업데이트
        updateMissionSetRating(review.getMissionSet());

        log.info("미션세트 리뷰 수정: reviewId={}, userId={}", reviewId, userId);
        return MissionSetReviewDto.Response.from(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        MissionSetReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 리뷰만 삭제 가능
        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        MissionSet missionSet = review.getMissionSet();
        reviewRepository.delete(review);

        // 미션세트 평균 별점 업데이트
        updateMissionSetRating(missionSet);

        log.info("미션세트 리뷰 삭제: reviewId={}, userId={}", reviewId, userId);
    }

    /**
     * 미션세트의 리뷰 목록 조회
     */
    public Page<MissionSetReviewDto.Response> getReviews(Long missionSetId, Pageable pageable) {
        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        return reviewRepository.findByMissionSetOrderByCreatedAtDesc(missionSet, pageable)
                .map(MissionSetReviewDto.Response::from);
    }

    /**
     * 내가 작성한 리뷰 조회
     */
    public MissionSetReviewDto.Response getMyReview(Long missionSetId, Long userId) {
        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return reviewRepository.findByMissionSetAndUser(missionSet, user)
                .map(MissionSetReviewDto.Response::from)
                .orElse(null);
    }

    /**
     * 미션세트 평균 별점 업데이트
     */
    private void updateMissionSetRating(MissionSet missionSet) {
        Double avgRating = reviewRepository.calculateAverageRating(missionSet);
        long reviewCount = reviewRepository.countByMissionSet(missionSet);

        missionSet.updateRating(
                avgRating != null ? avgRating : 0.0,
                (int) reviewCount
        );
    }
}
