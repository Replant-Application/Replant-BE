package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetReview;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MissionSetReviewRepository extends JpaRepository<MissionSetReview, Long> {

    /**
     * 미션세트와 사용자로 리뷰 조회
     */
    Optional<MissionSetReview> findByMissionSetAndUser(MissionSet missionSet, User user);

    /**
     * 미션세트와 사용자로 리뷰 존재 여부 확인
     */
    boolean existsByMissionSetAndUser(MissionSet missionSet, User user);

    /**
     * 미션세트의 리뷰 목록 조회 (최신순)
     */
    Page<MissionSetReview> findByMissionSetOrderByCreatedAtDesc(MissionSet missionSet, Pageable pageable);

    /**
     * 미션세트의 평균 별점 계산
     */
    @Query("SELECT AVG(r.rating) FROM MissionSetReview r WHERE r.missionSet = :missionSet")
    Double calculateAverageRating(@Param("missionSet") MissionSet missionSet);

    /**
     * 미션세트의 리뷰 수 계산
     */
    long countByMissionSet(MissionSet missionSet);

    /**
     * 사용자의 리뷰 목록 조회
     */
    Page<MissionSetReview> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
