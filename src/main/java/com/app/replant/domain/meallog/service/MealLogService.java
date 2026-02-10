package com.app.replant.domain.meallog.service;

import com.app.replant.domain.meallog.dto.MealLogRequest;
import com.app.replant.domain.meallog.dto.MealLogResponse;
import com.app.replant.domain.meallog.entity.MealLog;
import com.app.replant.domain.meallog.enums.MealLogStatus;
import com.app.replant.domain.meallog.enums.MealType;
import com.app.replant.domain.meallog.repository.MealLogRepository;
import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.notification.repository.NotificationRepository;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.domain.reant.service.ReantService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReantRepository reantRepository;
    private final ReantService reantService;
    private final NotificationRepository notificationRepository;

    // 식사 인증 마감 시간 (분)
    private static final int MEAL_DEADLINE_MINUTES = 120;  // 2시간

    /**
     * 식사 미션 할당 (스케줄러에서 호출)
     */
    @Transactional
    public MealLog assignMealMission(User user, MealType mealType, LocalDate date) {
        // 이미 해당 날짜/타입에 기록이 있는지 확인
        Optional<MealLog> existingMealLog = mealLogRepository.findByUserIdAndMealTypeAndMealDate(
                user.getId(), mealType, date);
        
        if (existingMealLog.isPresent()) {
            MealLog mealLog = existingMealLog.get();
            log.info("이미 {} {} 식사 미션이 존재합니다. userId={}, mealLogId={}, status={}, assignedAt={}, deadlineAt={}", 
                    date, mealType.getDisplayName(), user.getId(), mealLog.getId(), 
                    mealLog.getStatus(), mealLog.getAssignedAt(), mealLog.getDeadlineAt());
            // 기존 미션이 있으면 그것을 반환 (알림 전송을 위해)
            return mealLog;
        }

        MealLog mealLog = MealLog.assign(user, mealType, date, MEAL_DEADLINE_MINUTES);
        MealLog saved = mealLogRepository.save(mealLog);
        
        log.info("식사 미션 할당 완료: userId={}, mealType={}, mealLogId={}, assignedAt={}, deadlineAt={}", 
                user.getId(), mealType.getDisplayName(), saved.getId(), 
                saved.getAssignedAt(), saved.getDeadlineAt());
        
        return saved;
    }

    /**
     * 식사 인증
     */
    @Transactional
    public MealLogResponse.VerifyResult verifyMeal(Long userId, Long mealLogId, MealLogRequest.Verify request) {
        MealLog mealLog = mealLogRepository.findById(mealLogId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        log.info("식사 미션 인증 시도: userId={}, mealLogId={}, status={}, assignedAt={}, deadlineAt={}, now={}", 
                userId, mealLogId, mealLog.getStatus(), mealLog.getAssignedAt(), 
                mealLog.getDeadlineAt(), LocalDateTime.now());

        // 소유자 확인
        if (!mealLog.isOwner(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 완료/실패 상태인지 확인
        if (mealLog.getStatus() != MealLogStatus.ASSIGNED) {
            log.warn("식사 미션 인증 실패 - 이미 완료/실패 상태: userId={}, mealLogId={}, status={}", 
                    userId, mealLogId, mealLog.getStatus());
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED);
        }

        // 시간 초과 확인
        if (mealLog.isExpired()) {
            log.warn("식사 미션 인증 실패 - 시간 초과: userId={}, mealLogId={}, deadlineAt={}, now={}", 
                    userId, mealLogId, mealLog.getDeadlineAt(), LocalDateTime.now());
            mealLog.fail();
            mealLogRepository.save(mealLog);
            return MealLogResponse.VerifyResult.builder()
                    .success(false)
                    .message("인증 시간이 초과되었습니다.")
                    .expGained(0)
                    .mealLog(MealLogResponse.Detail.from(mealLog))
                    .build();
        }

        // 게시글 처리: postId가 있으면 기존 게시글 연결, 없으면 자동 생성
        Post post = null;
        if (request.getPostId() != null) {
            // 기존 게시글 연결
            post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // 이미지가 있으면 게시글 자동 생성
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
            String postTitle = request.getTitle() != null ? request.getTitle() 
                    : mealLog.getMealType().getDisplayName() + " 식사 인증";
            String postContent = request.getDescription() != null ? request.getDescription() 
                    : mealLog.getMealType().getDisplayName() + " 식사를 인증합니다!";
            
            // 이미지 URL을 JSON 배열로 변환
            String imageUrlsJson = convertToJson(request.getImageUrls());
            
            // 일반 게시글로 생성 (VERIFICATION 타입이지만 userMission 없이)
            post = Post.generalBuilder()
                    .user(user)
                    .title(postTitle)
                    .content(postContent)
                    .imageUrls(imageUrlsJson)
                    .build();
            // postType을 VERIFICATION으로 변경하고 상태 설정
            post.convertToMealVerification();
            post = postRepository.save(post);
            log.info("식사 인증 게시글 자동 생성: userId={}, postId={}", userId, post.getId());
        }

        // 인증 처리
        mealLog.verify(post, request.getTitle(), request.getDescription(), request.getRating());

        // 경험치 지급 (리앤트) - N+1 문제 방지를 위해 user를 함께 fetch join
        int expReward = mealLog.getExpReward();
        try {
            reantRepository.findByUserIdWithUser(userId)
                    .ifPresent(reant -> {
                        reant.addExp(expReward);
                        reantRepository.save(reant);  // 변경사항 저장
                        reantService.evictReantCache(userId);  // 캐시 무효화
                    });
            log.info("식사 인증 경험치 지급: userId={}, exp={}", userId, expReward);
        } catch (Exception e) {
            log.warn("식사 인증 경험치 지급 실패: userId={}, error={}", userId, e.getMessage());
        }

        // 사용자 통계 업데이트
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.completeMission(expReward);
            }
        } catch (Exception e) {
            log.warn("사용자 통계 업데이트 실패: userId={}, error={}", userId, e.getMessage());
        }

        // 식사 미션 완료 시 관련 알림 자동 읽음 처리
        try {
            List<Notification> relatedNotifications = notificationRepository
                    .findByUserIdAndReference(userId, "MEAL_LOG", mealLogId);
            
            for (Notification notification : relatedNotifications) {
                if (!notification.getIsRead()) {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    log.info("식사 미션 완료로 인한 알림 자동 읽음 처리: notificationId={}, mealLogId={}, userId={}", 
                            notification.getId(), mealLogId, userId);
                }
            }
        } catch (Exception e) {
            log.warn("식사 미션 알림 읽음 처리 실패: userId={}, mealLogId={}, error={}", 
                    userId, mealLogId, e.getMessage());
        }

        log.info("식사 인증 완료: userId={}, mealLogId={}, mealType={}", 
                userId, mealLogId, mealLog.getMealType().getDisplayName());

        return MealLogResponse.VerifyResult.builder()
                .success(true)
                .message(mealLog.getMealType().getDisplayName() + " 식사 인증이 완료되었습니다!")
                .expGained(expReward)
                .mealLog(MealLogResponse.Detail.from(mealLog))
                .build();
    }

    /**
     * 현재 진행 중인 식사 미션 상태 조회
     * 오늘 날짜의 ASSIGNED 상태 미션을 조회하거나, 없으면 오늘 날짜의 가장 최근 미션을 조회
     */
    public MealLogResponse.Status getCurrentMealMissionStatus(Long userId) {
        LocalDate today = LocalDate.now();
        
        // 먼저 오늘 날짜의 ASSIGNED 상태 미션 조회
        List<MealLog> todayAssignedMissions = mealLogRepository.findCurrentAssignedMissions(userId, today);
        if (!todayAssignedMissions.isEmpty()) {
            MealLog latestMission = todayAssignedMissions.get(0); // 가장 최근 미션
            log.info("오늘 할당된 식사 미션 조회: userId={}, mealLogId={}, status={}, mealType={}", 
                    userId, latestMission.getId(), latestMission.getStatus(), latestMission.getMealType());
            return MealLogResponse.Status.from(latestMission);
        }
        
        // 오늘 날짜의 모든 미션 조회 (상태 무관)
        List<MealLog> todayMissions = mealLogRepository.findByUserIdAndMealDateOrderByMealType(userId, today);
        if (!todayMissions.isEmpty()) {
            // 가장 최근에 할당된 미션 반환
            MealLog latestMission = todayMissions.stream()
                    .max(java.util.Comparator.comparing(MealLog::getAssignedAt))
                    .orElse(null);
            if (latestMission != null) {
                log.info("오늘 날짜의 식사 미션 조회 (상태 무관): userId={}, mealLogId={}, status={}, mealType={}", 
                        userId, latestMission.getId(), latestMission.getStatus(), latestMission.getMealType());
                return MealLogResponse.Status.from(latestMission);
            }
        }
        
        log.info("오늘 날짜의 식사 미션 없음: userId={}", userId);
        return null;
    }

    /**
     * 특정 날짜의 식사 기록 조회
     */
    public MealLogResponse.Daily getDailyMealLogs(Long userId, LocalDate date) {
        List<MealLog> mealLogs = mealLogRepository.findByUserIdAndMealDateOrderByMealType(userId, date);
        return MealLogResponse.Daily.from(date, mealLogs);
    }

    /**
     * 날짜 범위의 식사 기록 조회 (캘린더용)
     */
    public List<MealLogResponse.Daily> getMealLogsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<MealLog> mealLogs = mealLogRepository.findByUserIdAndMealDateBetween(userId, startDate, endDate);
        
        // 날짜별로 그룹핑
        return mealLogs.stream()
                .collect(Collectors.groupingBy(MealLog::getMealDate))
                .entrySet().stream()
                .map(entry -> MealLogResponse.Daily.from(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /**
     * 식사 통계 조회
     */
    public MealLogResponse.Stats getMealStats(Long userId) {
        long totalCompleted = mealLogRepository.countByUserIdAndStatus(userId, MealLogStatus.COMPLETED);
        Double avgRating = mealLogRepository.getAverageRatingByUserId(userId);

        // 이번 주 (월요일 ~ 일요일)
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        long weeklyCompleted = mealLogRepository.countByUserIdAndStatusAndMealDateBetween(
                userId, MealLogStatus.COMPLETED, weekStart, weekEnd);

        // 이번 달
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        long monthlyCompleted = mealLogRepository.countByUserIdAndStatusAndMealDateBetween(
                userId, MealLogStatus.COMPLETED, monthStart, monthEnd);

        return MealLogResponse.Stats.builder()
                .totalCompleted(totalCompleted)
                .averageRating(avgRating)
                .weeklyCompleted(weeklyCompleted)
                .monthlyCompleted(monthlyCompleted)
                .build();
    }

    /**
     * 특정 식사 기록 상세 조회
     */
    public MealLogResponse.Detail getMealLogDetail(Long userId, Long mealLogId) {
        MealLog mealLog = mealLogRepository.findById(mealLogId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        if (!mealLog.isOwner(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return MealLogResponse.Detail.from(mealLog);
    }

    /**
     * 오늘 해당 식사 타입의 미션이 이미 있는지 확인
     */
    public boolean hasTodayMealMission(Long userId, MealType mealType) {
        return mealLogRepository.existsByUserIdAndMealTypeAndMealDate(userId, mealType, LocalDate.now());
    }

    /**
     * 만료된 미션 일괄 실패 처리 (스케줄러에서 호출)
     */
    @Transactional
    public int processExpiredMissions() {
        int updated = mealLogRepository.updateExpiredMissionsToFailed(LocalDateTime.now());
        if (updated > 0) {
            log.info("만료된 식사 미션 {}개 실패 처리 완료", updated);
        }
        return updated;
    }

    /**
     * List<String>을 JSON 배열 문자열로 변환
     */
    private String convertToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            // 수동으로 JSON 배열 생성
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
