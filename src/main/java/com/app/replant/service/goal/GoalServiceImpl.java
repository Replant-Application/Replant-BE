package com.app.replant.service.goal;

import com.app.replant.controller.dto.SetGoalDto;
import com.app.replant.entity.Goal;
import com.app.replant.entity.Member;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.repository.goal.GoalRepository;
import com.app.replant.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 목표 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Goal setGoal(Long memberId, SetGoalDto setGoalDto) {
        log.info("setGoal() - memberId: {}, goalJob: {}", memberId, setGoalDto.getGoalJob());

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 목표 검증
        validateGoalValues(setGoalDto);

        // 새 목표 생성
        Goal goal = Goal.builder()
                .member(member)
                .goalJob(setGoalDto.getGoalJob())
                .goalStartDate(setGoalDto.getGoalStartDate())
                .goalIncome(setGoalDto.getGoalIncome())
                .previousGoalMoney(setGoalDto.getPreviousGoalMoney())
                .build();

        Goal savedGoal = goalRepository.save(goal);

        // 필수 카테고리 업데이트 (MemberService를 통해)
        // TODO: MemberService에 updateEssentialCategories 메서드 추가 후 호출

        log.info("Goal created successfully - id: {}", savedGoal.getId());
        return savedGoal;
    }

    @Override
    public Goal getGoalByMemberId(Long memberId) {
        log.info("getGoalByMemberId() - memberId: {}", memberId);

        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 최신 목표 조회
        return goalRepository.findLatestByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));
    }

    @Override
    @Transactional
    public Goal updateGoal(Long memberId, SetGoalDto setGoalDto) {
        log.info("updateGoal() - memberId: {}", memberId);

        // 기존 목표 조회
        Goal existingGoal = getGoalByMemberId(memberId);

        // 목표 검증
        validateGoalValues(setGoalDto);

        // 목표 업데이트 (새 객체 생성)
        Goal updatedGoal = Goal.builder()
                .id(existingGoal.getId())
                .member(existingGoal.getMember())
                .goalJob(setGoalDto.getGoalJob() != null ? setGoalDto.getGoalJob() : existingGoal.getGoalJob())
                .goalStartDate(setGoalDto.getGoalStartDate() != null ? setGoalDto.getGoalStartDate() : existingGoal.getGoalStartDate())
                .goalIncome(setGoalDto.getGoalIncome() != null ? setGoalDto.getGoalIncome() : existingGoal.getGoalIncome())
                .previousGoalMoney(setGoalDto.getPreviousGoalMoney() != null ? setGoalDto.getPreviousGoalMoney() : existingGoal.getPreviousGoalMoney())
                .build();

        Goal saved = goalRepository.save(updatedGoal);

        log.info("Goal updated successfully - id: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteGoal(Long goalId) {
        log.info("deleteGoal() - goalId: {}", goalId);

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));

        goalRepository.delete(goal);
        log.info("Goal deleted successfully - id: {}", goalId);
    }

    @Override
    public List<Goal> getGoalHistory(Long memberId) {
        log.info("getGoalHistory() - memberId: {}", memberId);

        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return goalRepository.findAllByMemberIdOrderByStartDateDesc(memberId);
    }

    @Override
    @Transactional
    public void calculateGoalScores() {
        log.info("calculateGoalScores() - 전체 회원의 목표 달성도 계산");

        // 모든 활성 목표를 가진 회원들의 달성도 계산
        List<Goal> allGoals = goalRepository.findAll();

        for (Goal goal : allGoals) {
            try {
                calculateGoalScore(goal);
            } catch (Exception e) {
                log.error("Failed to calculate goal score for goal id: {}", goal.getId(), e);
            }
        }

        log.info("Goal scores calculation completed");
    }

    /**
     * 개별 목표 달성도 계산
     */
    private void calculateGoalScore(Goal goal) {
        // TODO: 실제 달성도 계산 로직 구현
        // 1. 회원의 실제 소비 금액 조회 (CardHistory 등에서)
        // 2. 목표 금액과 비교하여 달성률 계산
        // 3. 달성률에 따른 점수 부여
        // 4. 뱃지 지급 여부 판단

        log.debug("Calculating score for goal id: {}", goal.getId());
    }

    /**
     * 목표값 검증
     */
    private void validateGoalValues(SetGoalDto setGoalDto) {
        // 목표 금액이 수입보다 큰지 확인
        if (setGoalDto.getGoalIncome() != null && setGoalDto.getPreviousGoalMoney() != null) {
            try {
                Integer income = Integer.parseInt(setGoalDto.getGoalIncome());
                Integer goalMoney = setGoalDto.getPreviousGoalMoney();

                if (goalMoney > income) {
                    throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
                }

                if (goalMoney < 0 || income < 0) {
                    throw new CustomException(ErrorCode.GOAL_INVALIDNUM);
                }
            } catch (NumberFormatException e) {
                throw new CustomException(ErrorCode.GOAL_INVALIDNUM);
            }
        }
    }
}
