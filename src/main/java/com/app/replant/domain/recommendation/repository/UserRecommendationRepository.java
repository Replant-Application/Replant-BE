package com.app.replant.domain.recommendation.repository;

import com.app.replant.domain.recommendation.entity.UserRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {

    @Query("SELECT ur FROM UserRecommendation ur WHERE ur.user.id = :userId " +
           "AND (:status IS NULL OR ur.status = :status)")
    List<UserRecommendation> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserRecommendation.RecommendationStatus status);

    @Query("SELECT ur FROM UserRecommendation ur WHERE ur.id = :recommendationId AND ur.user.id = :userId")
    Optional<UserRecommendation> findByIdAndUserId(@Param("recommendationId") Long recommendationId, @Param("userId") Long userId);
}
