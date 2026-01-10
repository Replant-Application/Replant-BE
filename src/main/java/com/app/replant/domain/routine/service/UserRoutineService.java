package com.app.replant.domain.routine.service;

import com.app.replant.domain.routine.dto.UserRoutineRequest;
import com.app.replant.domain.routine.dto.UserRoutineResponse;
import com.app.replant.domain.routine.entity.UserRoutine;
import com.app.replant.domain.routine.enums.PeriodType;
import com.app.replant.domain.routine.enums.RoutineType;
import com.app.replant.domain.routine.repository.UserRoutineRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRoutineService {

    private final UserRoutineRepository routineRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 모든 활성 루틴 조회
     */
    public List<UserRoutineResponse> getActiveRoutines(Long userId) {
        return routineRepository.findActiveRoutinesByUserId(userId)
                .stream()
                .map(UserRoutineResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 주기 타입별 활성 루틴 조회
     */
    public List<UserRoutineResponse> getActiveRoutinesByPeriod(Long userId, PeriodType periodType) {
        return routineRepository.findActiveByUserIdAndPeriodType(userId, periodType)
                .stream()
                .map(UserRoutineResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 타입의 활성 루틴 조회
     */
    public UserRoutineResponse getActiveRoutine(Long userId, RoutineType routineType) {
        return routineRepository.findActiveByUserIdAndType(userId, routineType)
                .map(UserRoutineResponse::from)
                .orElse(null);
    }

    /**
     * 루틴 히스토리 조회
     */
    public Page<UserRoutineResponse> getRoutineHistory(Long userId, RoutineType routineType, Pageable pageable) {
        return routineRepository.findHistoryByUserIdAndType(userId, routineType, pageable)
                .map(UserRoutineResponse::from);
    }

    /**
     * 루틴 생성 또는 업데이트
     */
    @Transactional
    public UserRoutineResponse saveRoutine(Long userId, UserRoutineRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoutineType routineType = request.getRoutineType();
        PeriodType periodType = request.getPeriodType() != null ?
                request.getPeriodType() : routineType.getDefaultPeriodType();

        // 기간 계산
        LocalDate periodStart = calculatePeriodStart(periodType, request.getPeriodStart());
        LocalDate periodEnd = calculatePeriodEnd(periodType, periodStart);

        // 기존 활성 루틴이 있는지 확인
        var existingRoutine = routineRepository.findActiveByUserIdAndType(userId, routineType);

        if (existingRoutine.isPresent()) {
            // 같은 기간이면 업데이트, 다른 기간이면 기존 것 비활성화 후 새로 생성
            UserRoutine existing = existingRoutine.get();

            if (isSamePeriod(existing.getPeriodStart(), periodStart, periodType)) {
                // 같은 기간 - 업데이트
                existing.update(
                        request.getValueText(),
                        request.getValueTime(),
                        request.getValueNumber(),
                        request.getValueLatitude(),
                        request.getValueLongitude(),
                        request.getNotificationEnabled(),
                        request.getNotificationTime()
                );
                return UserRoutineResponse.from(existing);
            } else {
                // 다른 기간 - 기존 비활성화
                existing.deactivate();
            }
        }

        // 새 루틴 생성
        UserRoutine newRoutine = UserRoutine.builder()
                .user(user)
                .routineType(routineType)
                .periodType(periodType)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .valueText(request.getValueText())
                .valueTime(request.getValueTime())
                .valueNumber(request.getValueNumber())
                .valueLatitude(request.getValueLatitude())
                .valueLongitude(request.getValueLongitude())
                .notificationEnabled(request.getNotificationEnabled())
                .notificationTime(request.getNotificationTime())
                .build();

        UserRoutine saved = routineRepository.save(newRoutine);
        return UserRoutineResponse.from(saved);
    }

    /**
     * 루틴 삭제 (비활성화)
     */
    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        UserRoutine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTINE_NOT_FOUND));

        if (!routine.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_ROUTINE_OWNER);
        }

        routine.deactivate();
    }

    /**
     * 루틴 알림 설정 토글
     */
    @Transactional
    public UserRoutineResponse toggleNotification(Long userId, Long routineId, Boolean enabled) {
        UserRoutine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTINE_NOT_FOUND));

        if (!routine.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_ROUTINE_OWNER);
        }

        routine.update(null, null, null, null, null, enabled, routine.getNotificationTime());
        return UserRoutineResponse.from(routine);
    }

    // ============ Private Helper Methods ============

    private LocalDate calculatePeriodStart(PeriodType periodType, LocalDate requestedStart) {
        if (requestedStart != null) {
            return requestedStart;
        }

        LocalDate today = LocalDate.now();

        return switch (periodType) {
            case DAILY -> today;
            case WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> today.withDayOfMonth(1);
            case NONE -> null;
        };
    }

    private LocalDate calculatePeriodEnd(PeriodType periodType, LocalDate periodStart) {
        if (periodStart == null) {
            return null;
        }

        return switch (periodType) {
            case DAILY -> periodStart;
            case WEEKLY -> periodStart.plusDays(6);
            case MONTHLY -> periodStart.with(TemporalAdjusters.lastDayOfMonth());
            case NONE -> null;
        };
    }

    private boolean isSamePeriod(LocalDate existing, LocalDate requested, PeriodType periodType) {
        if (existing == null && requested == null) {
            return true;
        }
        if (existing == null || requested == null) {
            return false;
        }
        return existing.equals(requested);
    }
}
