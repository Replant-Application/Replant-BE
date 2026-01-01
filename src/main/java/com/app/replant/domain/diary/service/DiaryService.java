package com.app.replant.domain.diary.service;

import com.app.replant.domain.diary.dto.DiaryRequest;
import com.app.replant.domain.diary.dto.DiaryResponse;
import com.app.replant.domain.diary.entity.Diary;
import com.app.replant.domain.diary.repository.DiaryRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 다이어리 목록 조회
     */
    public Page<DiaryResponse> getDiaries(Long userId, Pageable pageable) {
        return diaryRepository.findByUserId(userId, pageable)
                .map(DiaryResponse::from);
    }

    /**
     * 다이어리 상세 조회
     */
    public DiaryResponse getDiary(Long diaryId, Long userId) {
        Diary diary = findDiaryByIdAndUserId(diaryId, userId);
        return DiaryResponse.from(diary);
    }

    /**
     * 날짜별 다이어리 조회
     */
    public DiaryResponse getDiaryByDate(Long userId, LocalDate date) {
        Diary diary = diaryRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));
        return DiaryResponse.from(diary);
    }

    /**
     * 기간별 다이어리 조회
     */
    public List<DiaryResponse> getDiariesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
                .stream()
                .map(DiaryResponse::from)
                .toList();
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

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                log.error("이미지 URL JSON 변환 실패", e);
            }
        }

        Diary diary = Diary.builder()
                .user(user)
                .date(request.getDate())
                .emotion(request.getEmotion())
                .content(request.getContent())
                .weather(request.getWeather())
                .location(request.getLocation())
                .imageUrls(imageUrlsJson)
                .isPrivate(request.getIsPrivate())
                .build();

        Diary saved = diaryRepository.save(diary);
        log.info("다이어리 생성 완료 - userId={}, diaryId={}", userId, saved.getId());

        return DiaryResponse.from(saved);
    }

    /**
     * 다이어리 수정
     */
    @Transactional
    public DiaryResponse updateDiary(Long diaryId, Long userId, DiaryRequest request) {
        Diary diary = findDiaryByIdAndUserId(diaryId, userId);

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                log.error("이미지 URL JSON 변환 실패", e);
            }
        }

        diary.update(
                request.getEmotion(),
                request.getContent(),
                request.getWeather(),
                request.getLocation(),
                imageUrlsJson,
                request.getIsPrivate()
        );

        return DiaryResponse.from(diary);
    }

    /**
     * 다이어리 삭제
     */
    @Transactional
    public void deleteDiary(Long diaryId, Long userId) {
        Diary diary = findDiaryByIdAndUserId(diaryId, userId);
        diaryRepository.delete(diary);
        log.info("다이어리 삭제 완료 - userId={}, diaryId={}", userId, diaryId);
    }

    /**
     * 다이어리 통계 조회
     */
    public Map<String, Object> getDiaryStats(Long userId) {
        long totalCount = diaryRepository.countByUserId(userId);
        List<Object[]> emotionStats = diaryRepository.getEmotionStatsByUserId(userId);

        Map<String, Long> emotionCounts = new HashMap<>();
        for (Object[] stat : emotionStats) {
            emotionCounts.put((String) stat[0], (Long) stat[1]);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("emotionStats", emotionCounts);

        return stats;
    }

    private Diary findDiaryByIdAndUserId(Long diaryId, Long userId) {
        return diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
