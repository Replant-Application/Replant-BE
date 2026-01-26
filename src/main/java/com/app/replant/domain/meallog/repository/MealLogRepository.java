package com.app.replant.domain.meallog.repository;

import com.app.replant.domain.meallog.entity.MealLog;
import com.app.replant.domain.meallog.enums.MealLogStatus;
import com.app.replant.domain.meallog.enums.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    /**
     * 특정 사용자의 특정 날짜, 특정 식사 타입 조회
     */
    Optional<MealLog> findByUserIdAndMealTypeAndMealDate(Long userId, MealType mealType, LocalDate mealDate);

    /**
     * 특정 사용자의 특정 날짜 모든 식사 조회
     */
    List<MealLog> findByUserIdAndMealDateOrderByMealType(Long userId, LocalDate mealDate);

    /**
     * 특정 사용자의 날짜 범위 식사 조회 (캘린더용)
     */
    @Query("SELECT m FROM MealLog m WHERE m.user.id = :userId " +
           "AND m.mealDate BETWEEN :startDate AND :endDate " +
           "ORDER BY m.mealDate, m.mealType")
    List<MealLog> findByUserIdAndMealDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 오늘 이미 할당된 식사 미션이 있는지 확인
     */
    boolean existsByUserIdAndMealTypeAndMealDate(Long userId, MealType mealType, LocalDate mealDate);

    /**
     * ASSIGNED 상태이면서 마감 시간이 지난 미션 조회 (실패 처리용)
     */
    @Query("SELECT m FROM MealLog m WHERE m.status = :status AND m.deadlineAt < :now")
    List<MealLog> findExpiredMissions(
            @Param("status") MealLogStatus status,
            @Param("now") LocalDateTime now
    );

    /**
     * 만료된 미션 일괄 실패 처리
     */
    @Modifying
    @Query("UPDATE MealLog m SET m.status = 'FAILED' " +
           "WHERE m.status = 'ASSIGNED' AND m.deadlineAt < :now")
    int updateExpiredMissionsToFailed(@Param("now") LocalDateTime now);

    /**
     * 특정 사용자의 완료된 식사 수 조회
     */
    long countByUserIdAndStatus(Long userId, MealLogStatus status);

    /**
     * 특정 사용자의 특정 기간 완료된 식사 수 조회
     */
    @Query("SELECT COUNT(m) FROM MealLog m WHERE m.user.id = :userId " +
           "AND m.status = :status AND m.mealDate BETWEEN :startDate AND :endDate")
    long countByUserIdAndStatusAndMealDateBetween(
            @Param("userId") Long userId,
            @Param("status") MealLogStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 사용자의 평균 평점 조회
     */
    @Query("SELECT AVG(m.rating) FROM MealLog m WHERE m.user.id = :userId " +
           "AND m.status = 'COMPLETED' AND m.rating IS NOT NULL")
    Double getAverageRatingByUserId(@Param("userId") Long userId);

    /**
     * 현재 ASSIGNED 상태인 특정 사용자의 식사 미션 조회
     */
    @Query("SELECT m FROM MealLog m WHERE m.user.id = :userId " +
           "AND m.status = 'ASSIGNED' AND m.mealDate = :today " +
           "ORDER BY m.assignedAt DESC")
    List<MealLog> findCurrentAssignedMissions(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    /**
     * 가장 최근 ASSIGNED 상태인 식사 미션 조회
     */
    @Query("SELECT m FROM MealLog m WHERE m.user.id = :userId " +
           "AND m.status = 'ASSIGNED' " +
           "ORDER BY m.assignedAt DESC LIMIT 1")
    Optional<MealLog> findLatestAssignedMission(@Param("userId") Long userId);
}
