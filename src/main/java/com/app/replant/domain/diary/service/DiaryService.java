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

        Diary diary = Diary.builder()
                .user(user)
                .date(request.getDate())
                .mood(request.getMood())
                .emotions(emotionsJson)
                .emotionFactors(emotionFactorsJson)
                .content(request.getContent())
                .build();

        Diary saved = diaryRepository.save(diary);
        log.info("다이어리 생성 완료 - userId={}, diaryId={}", userId, saved.getId());

        return DiaryResponse.from(saved);
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

    private Diary findDiaryByIdAndUserId(Long diaryId, Long userId) {
        return diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
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
