package com.app.replant.repository.goal;

import com.app.replant.entity.Goal;
import com.app.replant.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    /**
     * 회원의 최신 목표 조회
     */
    @Query("SELECT g FROM Goal g WHERE g.member.id = :memberId ORDER BY g.goalStartDate DESC LIMIT 1")
    Optional<Goal> findLatestByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 모든 목표 조회 (최신순)
     */
    @Query("SELECT g FROM Goal g WHERE g.member.id = :memberId ORDER BY g.goalStartDate DESC")
    List<Goal> findAllByMemberIdOrderByStartDateDesc(@Param("memberId") Long memberId);

    /**
     * 특정 기간의 목표 조회
     */
    @Query("SELECT g FROM Goal g WHERE g.member.id = :memberId " +
           "AND g.goalStartDate BETWEEN :startDate AND :endDate")
    List<Goal> findByMemberIdAndDateRange(@Param("memberId") Long memberId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * 회원의 목표 존재 여부 확인
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 회원으로 목표 조회
     */
    Optional<Goal> findByMember(Member member);
}
