package com.app.replant.domain.reant.service;

import com.app.replant.domain.reant.dto.ReantResponse;
import com.app.replant.domain.reant.dto.ReantStatusResponse;
import com.app.replant.domain.reant.dto.ReantUpdateRequest;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReantService {

    private final ReantRepository reantRepository;

    @Cacheable(value = "reant", key = "#userId", unless = "#result == null")
    public ReantResponse getMyReant(Long userId) {
        // N+1 문제 방지를 위해 user를 함께 fetch join
        Reant reant = reantRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ReantResponse.from(reant);
    }

    @Transactional
    @CacheEvict(value = {"reant", "reantStatus"}, key = "#userId")
    public ReantResponse updateReant(Long userId, ReantUpdateRequest request) {
        // N+1 문제 방지를 위해 user를 함께 fetch join
        Reant reant = reantRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        reant.updateProfile(request.getName(), request.getAppearance());
        return ReantResponse.from(reant);
    }

    public Reant findByUserId(Long userId) {
        // N+1 문제 방지를 위해 user를 함께 fetch join
        return reantRepository.findByUserIdWithUser(userId)
                .orElse(null);
    }

    /**
     * 리앤트 상태 조회
     */
    @Cacheable(value = "reantStatus", key = "#userId", unless = "#result == null")
    public ReantStatusResponse getReantStatus(Long userId) {
        // N+1 문제 방지를 위해 user를 함께 fetch join
        Reant reant = reantRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ReantStatusResponse.from(reant);
    }

    /**
     * Reant 캐시 무효화 헬퍼 메서드
     * 다른 서비스에서 Reant를 변경한 후 호출하여 캐시를 무효화할 수 있음
     */
    @CacheEvict(value = {"reant", "reantStatus"}, key = "#userId")
    public void evictReantCache(Long userId) {
        // 캐시 무효화만 수행 (로깅은 선택사항)
    }
}
