package com.app.replant.domain.routine.repository;

import com.app.replant.domain.routine.entity.UserRoutine;
import com.app.replant.domain.routine.enums.PeriodType;
import com.app.replant.domain.routine.enums.RoutineType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRoutineRepository extends JpaRepository<UserRoutine, Long> {

    /**
     * 사용자의 활성화된 모든 루틴 조회
     */
    @Query("SELECT r FROM UserRoutine r WHERE r.user.id = :userId AND r.isActive = true " +
           "ORDER BY r.routineType")
    List<UserRoutine> findActiveRoutinesByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 특정 타입 활성 루틴 조회
     */
    @Query("SELECT r FROM UserRoutine r WHERE r.user.id = :userId AND r.routineType = :routineType " +
           "AND r.isActive = true ORDER BY r.createdAt DESC")
    Optional<UserRoutine> findActiveByUserIdAndType(
        @Param("userId") Long userId,
        @Param("routineType") RoutineType routineType
    );

    /**
     * 사용자의 특정 주기 타입 활성 루틴 조회
     */
    @Query("SELECT r FROM UserRoutine r WHERE r.user.id = :userId AND r.periodType = :periodType " +
           "AND r.isActive = true ORDER BY r.routineType")
    List<UserRoutine> findActiveByUserIdAndPeriodType(
        @Param("userId") Long userId,
        @Param("periodType") PeriodType periodType
    );

    /**
     * 사용자의 특정 타입 루틴 히스토리 조회 (비활성 포함)
     */
    @Query("SELECT r FROM UserRoutine r WHERE r.user.id = :userId AND r.routineType = :routineType " +
           "ORDER BY r.periodStart DESC, r.createdAt DESC")
    Page<UserRoutine> findHistoryByUserIdAndType(
        @Param("userId") Long userId,
        @Param("routineType") RoutineType routineType,
        Pageable pageable
    );

    /**
     * 특정 날짜에 알림이 필요한 루틴 조회
     */
    @Query("SELECT r FROM UserRoutine r WHERE r.isActive = true " +
           "AND r.notificationEnabled = true " +
           "AND (r.periodStart IS NULL OR r.periodStart <= :date) " +
           "AND (r.periodEnd IS NULL OR r.periodEnd >= :date)")
    List<UserRoutine> findRoutinesForNotification(@Param("date") LocalDate date);

    /**
     * 사용자의 특정 타입, 특정 기간의 루틴 조회
     */
    @Query("SELECT r FROM UserRoutine r WHERE r.user.id = :userId AND r.routineType = :routineType " +
           "AND r.periodStart = :periodStart AND r.isActive = true")
    Optional<UserRoutine> findByUserIdAndTypeAndPeriod(
        @Param("userId") Long userId,
        @Param("routineType") RoutineType routineType,
        @Param("periodStart") LocalDate periodStart
    );

    /**
     * 사용자의 특정 루틴 존재 여부 확인
     */
    boolean existsByUserIdAndRoutineTypeAndIsActiveTrue(Long userId, RoutineType routineType);
}
