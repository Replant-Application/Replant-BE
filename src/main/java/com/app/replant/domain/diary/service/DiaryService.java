package com.app.replant.domain.diary.service;

import com.app.replant.domain.diary.dto.DiaryRequest;
import com.app.replant.domain.diary.dto.DiaryResponse;
import com.app.replant.domain.diary.dto.DiaryStatsResponse;
import com.app.replant.domain.diary.entity.Diary;
import com.app.replant.domain.diary.repository.DiaryRepository;
import com.app.replant.domain.rag.service.UserMemoryVectorService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final UserMemoryVectorService userMemoryVectorService;

    // 정렬 가능한 필드명 목록
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "date", "mood", "content"
    );

    /**
     * 다이어리 목록 조회
     */
    public Page<DiaryResponse> getDiaries(Long userId, Pageable pageable) {
        Pageable validatedPageable = validateAndSanitizePageable(pageable);
        return diaryRepository.findByUserId(userId, validatedPageable)
                .map(DiaryResponse::from);
    }

    /**
     * Pageable의 Sort를 검증하고 허용된 필드만 사용하도록 필터링
     */
    private Pageable validateAndSanitizePageable(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return pageable;
        }

        List<Sort.Order> validOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (ALLOWED_SORT_FIELDS.contains(property)) {
                validOrders.add(order);
            } else {
                log.warn("허용되지 않은 정렬 필드: {}. 기본 정렬(date DESC)을 사용합니다.", property);
            }
        }

        // 유효한 정렬이 없으면 기본 정렬 사용
        if (validOrders.isEmpty()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "date")
            );
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(validOrders)
        );
    }

    /**
     * 날짜별 다이어리 조회
     */
    public DiaryResponse getDiaryByDate(Long userId, LocalDate date) {
        Diary diary = diaryRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));
        userMemoryVectorService.upsertDiary(diary);
        return DiaryResponse.from(diary);
    }

    /**
     * 다이어리 생성
     */
    @Transactional
    public DiaryResponse createDiary(Long userId, DiaryRequest request) {
        User user = findUserById(userId);

        // 같은 날짜에 이미 다이어리가 있는지 확인
        if (diaryRepository.existsByUserIdAndDate(userId, request.getDate())) {
            throw new CustomException(ErrorCode.DIARY_DUPLICATE_DATE);
        }

        // List<String>을 JSON 문자열로 변환
        String emotionsJson = convertListToJson(request.getEmotions());
        String emotionFactorsJson = convertListToJson(request.getEmotionFactors());
        String imageUrlsJson = convertListToJson(request.getImageUrls());

        Diary diary = Diary.builder()
                .user(user)
                .date(request.getDate())
                .emotion(request.getEmotion()) // 프론트엔드 호환
                .mood(request.getMood())
                .emotions(emotionsJson)
                .emotionFactors(emotionFactorsJson)
                .content(request.getContent())
                .weather(request.getWeather())
                .location(request.getLocation())
                .imageUrls(imageUrlsJson)
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
                .build();

        Diary saved = diaryRepository.save(diary);
        userMemoryVectorService.upsertDiary(saved);
        log.info("다이어리 생성 완료 - userId={}, diaryId={}", userId, saved.getId());

        return DiaryResponse.from(saved);
    }

    /**
     * 다이어리 단건 조회
     */
    public DiaryResponse getDiary(Long diaryId, Long userId) {
        Diary diary = findDiaryByIdAndUserId(diaryId, userId);
        return DiaryResponse.from(diary);
    }

    /**
     * 다이어리 수정
     */
    @Transactional
    public DiaryResponse updateDiary(Long diaryId, Long userId, DiaryRequest request) {
        Diary diary = findDiaryByIdAndUserId(diaryId, userId);

        // List<String>을 JSON 문자열로 변환
        String emotionsJson = convertListToJson(request.getEmotions());
        String emotionFactorsJson = convertListToJson(request.getEmotionFactors());
        String imageUrlsJson = convertListToJson(request.getImageUrls());

        diary.update(
                request.getEmotion(),
                request.getMood(),
                emotionsJson,
                emotionFactorsJson,
                request.getContent(),
                request.getWeather(),
                request.getLocation(),
                imageUrlsJson,
                request.getIsPrivate()
        );

        log.info("다이어리 수정 완료 - userId={}, diaryId={}", userId, diaryId);
        return DiaryResponse.from(diary);
    }

    /**
     * 기간별 다이어리 조회
     */
    public List<DiaryResponse> getDiariesByRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
                .stream()
                .map(DiaryResponse::from)
                .toList();
    }

    /**
     * 다이어리 통계 조회
     */
    public DiaryStatsResponse getDiaryStats(Long userId) {
        long totalCount = diaryRepository.countByUserId(userId);

        // 감정별 통계 집계
        List<Object[]> emotionCounts = diaryRepository.countByEmotion(userId);
        java.util.Map<String, Long> emotionStats = new java.util.HashMap<>();
        for (Object[] row : emotionCounts) {
            String emotion = (String) row[0];
            Long count = (Long) row[1];
            if (emotion != null) {
                emotionStats.put(emotion, count);
            }
        }

        return DiaryStatsResponse.builder()
                .totalCount(totalCount)
                .emotionStats(emotionStats)
                .build();
    }

    /**
     * 다이어리 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteDiary(Long diaryId, Long userId) {
        Diary diary = findDiaryByIdAndUserId(diaryId, userId);
        diary.softDelete();
        userMemoryVectorService.deleteDiary(diaryId);
        log.info("다이어리 삭제 완료 (Soft Delete) - userId={}, diaryId={}", userId, diaryId);
    }

    private Diary findDiaryByIdAndUserId(Long diaryId, Long userId) {
        return diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));
    }

    private User findUserById(Long userId) {
        // N+1 문제 방지를 위해 reant를 함께 로드
        return userRepository.findByIdWithReant(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * List<String>을 JSON 문자열로 변환
     */
    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패", e);
            return null;
        }
    }
}
