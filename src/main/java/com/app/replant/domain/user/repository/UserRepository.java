package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회 (Soft Delete된 사용자 제외)
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND (u.delFlag = false OR u.delFlag IS NULL)")
    Optional<User> findByEmail(String email);

    /**
     * 이메일로 사용자 조회 (Reant 포함) - JWT 인증용
     * N+1 문제 방지를 위해 JOIN FETCH 사용
     * Soft Delete된 사용자 제외
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.reant WHERE u.email = :email AND (u.delFlag = false OR u.delFlag IS NULL)")
    Optional<User> findByEmailWithReant(String email);

    boolean existsByEmail(String email);

    Optional<User> findByNicknameAndPhone(String nickname, String phone);

    boolean existsByNickname(String nickname);

    /**
     * 모든 활성 사용자 조회 (알림 발송용)
     * Soft Delete된 사용자 제외 (delFlag = false)
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND (u.delFlag = false OR u.delFlag IS NULL)")
    List<User> findAllActiveUsers();

    /**
     * 특정 시간에 기상 미션을 받을 사용자 조회
     * Soft Delete된 사용자 제외
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' " +
           "AND (u.delFlag = false OR u.delFlag IS NULL) " +
           "AND u.isSpontaneousMissionSetupCompleted = true " +
           "AND u.wakeTime = :time")
    List<User> findUsersByWakeTime(String time);

    /**
     * 특정 시간에 식사 미션을 받을 사용자 조회 (아침/점심/저녁)
     * Soft Delete된 사용자 제외
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' " +
           "AND (u.delFlag = false OR u.delFlag IS NULL) " +
           "AND u.isSpontaneousMissionSetupCompleted = true " +
           "AND (u.breakfastTime = :time OR u.lunchTime = :time OR u.dinnerTime = :time)")
    List<User> findUsersByMealTime(String time);

    /**
     * 특정 시간에 취침 미션을 받을 사용자 조회
     * Soft Delete된 사용자 제외
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' " +
           "AND (u.delFlag = false OR u.delFlag IS NULL) " +
           "AND u.isSpontaneousMissionSetupCompleted = true " +
           "AND u.sleepTime = :time")
    List<User> findUsersBySleepTime(String time);

    /**
     * 30일 이상 지난 INACTIVE 사용자 조회 (DELETED로 변경 대상)
     */
    @Query("SELECT u FROM User u WHERE u.delFlag = true " +
           "AND u.status = 'INACTIVE' " +
           "AND u.deletedAt IS NOT NULL " +
           "AND u.deletedAt < :thresholdDate")
    List<User> findInactiveUsersToMarkAsDeleted(LocalDateTime thresholdDate);

    /**
     * 30일 이상 지난 INACTIVE 사용자를 DELETED로 일괄 변경
     */
    @Modifying
    @Query("UPDATE User u SET u.status = 'DELETED' WHERE u.delFlag = true " +
           "AND u.status = 'INACTIVE' " +
           "AND u.deletedAt IS NOT NULL " +
           "AND u.deletedAt < :thresholdDate")
    int markInactiveUsersAsDeleted(LocalDateTime thresholdDate);

    /**
     * 30일 이상 전에 DELETED 상태인 사용자 조회 (완전 삭제 대상)
     */
    @Query("SELECT u FROM User u WHERE u.delFlag = true " +
           "AND u.status = 'DELETED' " +
           "AND u.deletedAt IS NOT NULL " +
           "AND u.deletedAt < :thresholdDate")
    List<User> findUsersToPermanentlyDelete(LocalDateTime thresholdDate);

    /**
     * 30일 이상 전에 DELETED 상태인 사용자 일괄 삭제 (배치 처리 - 시간복잡도 개선)
     * @Modifying과 함께 사용하여 UPDATE/DELETE 쿼리 실행
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.delFlag = true " +
           "AND u.status = 'DELETED' " +
           "AND u.deletedAt IS NOT NULL " +
           "AND u.deletedAt < :thresholdDate")
    int deleteUsersPermanentlyByThreshold(LocalDateTime thresholdDate);

    /**
     * Soft Delete된 사용자 조회 (계정 복구용)
     * 이메일로 조회하되, delFlag가 true인 사용자도 포함
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeleted(String email);

    /**
     * Soft Delete된 사용자 조회 (계정 복구용)
     * ID로 조회하되, delFlag가 true인 사용자도 포함
     */
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdIncludingDeleted(Long userId);
}
