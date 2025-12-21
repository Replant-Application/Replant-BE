package com.app.replant.service.goal;

import com.app.replant.controller.dto.SetGoalDto;
import com.app.replant.entity.Goal;

import java.util.List;

/**
 * 목표 서비스 인터페이스
 */
public interface GoalService {

    /**
     * 목표 설정
     */
    Goal setGoal(Long memberId, SetGoalDto setGoalDto);

    /**
     * 목표 조회
     */
    Goal getGoalByMemberId(Long memberId);

    /**
     * 목표 업데이트
     */
    Goal updateGoal(Long memberId, SetGoalDto setGoalDto);

    /**
     * 목표 삭제
     */
    void deleteGoal(Long goalId);

    /**
     * 목표 이력 조회
     */
    List<Goal> getGoalHistory(Long memberId);

    /**
     * 목표 점수 계산
     */
    void calculateGoalScores();
}
