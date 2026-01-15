package com.app.replant.domain.user.service;

import com.app.replant.domain.user.dto.SpontaneousMissionRequest;
import com.app.replant.domain.user.dto.SpontaneousMissionResponse;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SpontaneousMissionService {

    private final UserRepository userRepository;

    /**
     * 돌발 미션 설정 완료 여부 조회
     */
    public SpontaneousMissionResponse getSpontaneousMissionSetup(Long userId) {
        User user = findUserById(userId);
        
        return SpontaneousMissionResponse.builder()
                .isSpontaneousMissionSetupCompleted(user.isSpontaneousMissionSetupCompleted())
                .sleepTime(user.getSleepTime())
                .wakeTime(user.getWakeTime())
                .breakfastTime(user.getBreakfastTime())
                .lunchTime(user.getLunchTime())
                .dinnerTime(user.getDinnerTime())
                .build();
    }

    /**
     * 돌발 미션 설정 제출
     */
    @Transactional
    public SpontaneousMissionResponse setupSpontaneousMission(Long userId, SpontaneousMissionRequest request) {
        User user = findUserById(userId);

        if (user.isSpontaneousMissionSetupCompleted()) {
            throw new CustomException(ErrorCode.SPONTANEOUS_MISSION_SETUP_ALREADY_COMPLETED);
        }

        // 시간 형식 검증 및 유효성 검사
        validateTimes(request.getSleepTime(), request.getWakeTime(), 
                      request.getBreakfastTime(), request.getLunchTime(), request.getDinnerTime());

        // 돌발 미션 설정 완료 처리
        user.setupSpontaneousMission(request.getSleepTime(), request.getWakeTime(),
                                     request.getBreakfastTime(), request.getLunchTime(), request.getDinnerTime());
        userRepository.save(user);

        log.info("돌발 미션 설정 완료: userId={}, sleepTime={}, wakeTime={}, breakfastTime={}, lunchTime={}, dinnerTime={}", 
                userId, request.getSleepTime(), request.getWakeTime(), 
                request.getBreakfastTime(), request.getLunchTime(), request.getDinnerTime());

        return SpontaneousMissionResponse.builder()
                .isSpontaneousMissionSetupCompleted(true)
                .sleepTime(user.getSleepTime())
                .wakeTime(user.getWakeTime())
                .breakfastTime(user.getBreakfastTime())
                .lunchTime(user.getLunchTime())
                .dinnerTime(user.getDinnerTime())
                .build();
    }

    /**
     * 돌발 미션 설정 수정
     */
    @Transactional
    public SpontaneousMissionResponse updateSpontaneousMissionSetup(Long userId, SpontaneousMissionRequest request) {
        User user = findUserById(userId);

        // 시간 형식 검증 및 유효성 검사
        validateTimes(request.getSleepTime(), request.getWakeTime(),
                      request.getBreakfastTime(), request.getLunchTime(), request.getDinnerTime());

        // 돌발 미션 설정 업데이트
        user.setupSpontaneousMission(request.getSleepTime(), request.getWakeTime(),
                                     request.getBreakfastTime(), request.getLunchTime(), request.getDinnerTime());
        userRepository.save(user);

        log.info("돌발 미션 설정 수정: userId={}, sleepTime={}, wakeTime={}, breakfastTime={}, lunchTime={}, dinnerTime={}", 
                userId, request.getSleepTime(), request.getWakeTime(),
                request.getBreakfastTime(), request.getLunchTime(), request.getDinnerTime());

        return SpontaneousMissionResponse.builder()
                .isSpontaneousMissionSetupCompleted(user.isSpontaneousMissionSetupCompleted())
                .sleepTime(user.getSleepTime())
                .wakeTime(user.getWakeTime())
                .breakfastTime(user.getBreakfastTime())
                .lunchTime(user.getLunchTime())
                .dinnerTime(user.getDinnerTime())
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 시간 유효성 검사
     */
    private void validateTimes(String sleepTime, String wakeTime, 
                              String breakfastTime, String lunchTime, String dinnerTime) {
        // 취침/기상 시간 검증
        validateTimeFormat(sleepTime, "취침");
        validateTimeFormat(wakeTime, "기상");
        
        // 취침 시간과 기상 시간이 같으면 안됨
        if (sleepTime.equals(wakeTime)) {
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }

        // 식사 시간 검증 (null이 아닌 경우만)
        if (breakfastTime != null && !breakfastTime.trim().isEmpty()) {
            validateTimeFormat(breakfastTime, "아침 식사");
        }
        if (lunchTime != null && !lunchTime.trim().isEmpty()) {
            validateTimeFormat(lunchTime, "점심 식사");
        }
        if (dinnerTime != null && !dinnerTime.trim().isEmpty()) {
            validateTimeFormat(dinnerTime, "저녁 식사");
        }
    }

    /**
     * 시간 형식 검증
     */
    private void validateTimeFormat(String time, String fieldName) {
        if (time == null || time.trim().isEmpty()) {
            return; // null은 허용
        }

        String[] parts = time.split(":");
        if (parts.length != 2) {
            throw new CustomException(ErrorCode.INVALID_TIME_FORMAT);
        }

        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            // 시간 범위 검증
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new CustomException(ErrorCode.INVALID_TIME_FORMAT);
            }
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_TIME_FORMAT);
        }
    }
}
