package com.app.replant.domain.usermission.service;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.dto.WakeupMissionSettingRequest;
import com.app.replant.domain.usermission.dto.WakeupMissionSettingResponse;
import com.app.replant.domain.usermission.entity.WakeupMissionSetting;
import com.app.replant.domain.usermission.enums.WakeupTimeSlot;
import com.app.replant.domain.usermission.repository.WakeupMissionSettingRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WakeupMissionService {

    private final WakeupMissionSettingRepository wakeupSettingRepository;
    private final UserRepository userRepository;

    /**
     * 기상 미션 시간대 설정
     * 1주차에 2주차 기상미션 시간대를 설정
     */
    @Transactional
    public WakeupMissionSettingResponse setWakeupTime(Long userId, WakeupMissionSettingRequest request) {
        User user = findUserById(userId);

        // 이미 해당 주차에 설정이 있는지 확인
        if (wakeupSettingRepository.existsByUserIdAndWeekNumberAndYearAndIsActiveTrue(
                userId, request.getTargetWeekNumber(), request.getTargetYear())) {
            throw new CustomException(ErrorCode.WAKEUP_SETTING_ALREADY_EXISTS);
        }

        WakeupMissionSetting setting = WakeupMissionSetting.builder()
                .user(user)
                .timeSlot(request.getTimeSlot())
                .weekNumber(request.getTargetWeekNumber())
                .year(request.getTargetYear())
                .build();

        WakeupMissionSetting saved = wakeupSettingRepository.save(setting);
        log.info("기상 미션 시간대 설정 완료 - userId={}, weekNumber={}, timeSlot={}",
                userId, request.getTargetWeekNumber(), request.getTimeSlot());

        return WakeupMissionSettingResponse.from(saved);
    }

    /**
     * 기상 미션 시간대 수정
     */
    @Transactional
    public WakeupMissionSettingResponse updateWakeupTime(Long userId, Long settingId, WakeupTimeSlot newTimeSlot) {
        WakeupMissionSetting setting = wakeupSettingRepository.findById(settingId)
                .orElseThrow(() -> new CustomException(ErrorCode.WAKEUP_SETTING_NOT_FOUND));

        if (!setting.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.WAKEUP_SETTING_NOT_FOUND);
        }

        setting.updateTimeSlot(newTimeSlot);
        return WakeupMissionSettingResponse.from(setting);
    }

    /**
     * 현재 주차의 기상 미션 설정 조회
     */
    public WakeupMissionSettingResponse getCurrentWeekSetting(Long userId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int weekNumber = today.get(weekFields.weekOfWeekBasedYear());
        int year = today.getYear();

        WakeupMissionSetting setting = wakeupSettingRepository
                .findActiveByUserIdAndWeek(userId, weekNumber, year)
                .orElseThrow(() -> new CustomException(ErrorCode.WAKEUP_SETTING_NOT_FOUND));

        return WakeupMissionSettingResponse.from(setting);
    }

    /**
     * 특정 주차의 기상 미션 설정 조회
     */
    public WakeupMissionSettingResponse getWeekSetting(Long userId, Integer weekNumber, Integer year) {
        WakeupMissionSetting setting = wakeupSettingRepository
                .findActiveByUserIdAndWeek(userId, weekNumber, year)
                .orElseThrow(() -> new CustomException(ErrorCode.WAKEUP_SETTING_NOT_FOUND));

        return WakeupMissionSettingResponse.from(setting);
    }

    /**
     * 다음 주차 기상 미션 시간대 설정을 위한 정보 반환
     */
    public NextWeekSetupInfo getNextWeekSetupInfo(Long userId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
        int currentYear = today.getYear();

        // 다음 주차 계산
        LocalDate nextWeekDate = today.plusWeeks(1);
        int nextWeek = nextWeekDate.get(weekFields.weekOfWeekBasedYear());
        int nextYear = nextWeekDate.getYear();

        // 이미 설정이 있는지 확인
        Optional<WakeupMissionSetting> existingSetting = wakeupSettingRepository
                .findActiveByUserIdAndWeek(userId, nextWeek, nextYear);

        return NextWeekSetupInfo.builder()
                .currentWeekNumber(currentWeek)
                .currentYear(currentYear)
                .targetWeekNumber(nextWeek)
                .targetYear(nextYear)
                .alreadySet(existingSetting.isPresent())
                .existingSetting(existingSetting.map(WakeupMissionSettingResponse::from).orElse(null))
                .build();
    }

    /**
     * 기상 미션 인증 시간 검증
     * 설정된 시간대에 인증하는지 확인
     */
    public WakeupVerificationResult verifyWakeupTime(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int weekNumber = today.get(weekFields.weekOfWeekBasedYear());
        int year = today.getYear();
        int currentHour = now.getHour();

        Optional<WakeupMissionSetting> settingOpt = wakeupSettingRepository
                .findActiveByUserIdAndWeek(userId, weekNumber, year);

        if (settingOpt.isEmpty()) {
            // 설정이 없으면 기본 시간대(6-8시) 사용
            WakeupTimeSlot defaultSlot = WakeupTimeSlot.SLOT_6_8;
            boolean isWithinTime = defaultSlot.isWithinTimeSlot(currentHour);

            return WakeupVerificationResult.builder()
                    .success(isWithinTime)
                    .currentHour(currentHour)
                    .timeSlot(defaultSlot)
                    .message(isWithinTime ? "기상 인증 성공!" :
                            String.format("기상 인증 시간이 아닙니다. (설정 시간: %s)", defaultSlot.getDisplayName()))
                    .build();
        }

        WakeupMissionSetting setting = settingOpt.get();
        WakeupTimeSlot timeSlot = setting.getTimeSlot();
        boolean isWithinTime = timeSlot.isWithinTimeSlot(currentHour);

        String message;
        if (isWithinTime) {
            message = "기상 인증 성공!";
        } else if (currentHour < timeSlot.getStartHour()) {
            int minutesUntil = (timeSlot.getStartHour() - currentHour) * 60 - now.getMinute();
            message = String.format("아직 인증 시간이 아닙니다. %d분 후 인증 가능합니다.", minutesUntil);
        } else {
            message = String.format("인증 시간이 지났습니다. (설정 시간: %s)", timeSlot.getDisplayName());
        }

        return WakeupVerificationResult.builder()
                .success(isWithinTime)
                .currentHour(currentHour)
                .timeSlot(timeSlot)
                .startHour(timeSlot.getStartHour())
                .endHour(timeSlot.getEndHour())
                .message(message)
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @lombok.Builder
    @lombok.Getter
    public static class NextWeekSetupInfo {
        private Integer currentWeekNumber;
        private Integer currentYear;
        private Integer targetWeekNumber;
        private Integer targetYear;
        private Boolean alreadySet;
        private WakeupMissionSettingResponse existingSetting;
    }

    @lombok.Builder
    @lombok.Getter
    public static class WakeupVerificationResult {
        private Boolean success;
        private Integer currentHour;
        private WakeupTimeSlot timeSlot;
        private Integer startHour;
        private Integer endHour;
        private String message;
    }
}
