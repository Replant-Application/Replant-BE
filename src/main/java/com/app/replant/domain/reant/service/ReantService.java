package com.app.replant.domain.reant.service;

import com.app.replant.domain.reant.dto.ReantResponse;
import com.app.replant.domain.reant.dto.ReantStatusResponse;
import com.app.replant.domain.reant.dto.ReantUpdateRequest;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReantService {

    private final ReantRepository reantRepository;

    public ReantResponse getMyReant(Long userId) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ReantResponse.from(reant);
    }

    @Transactional
    public ReantResponse updateReant(Long userId, ReantUpdateRequest request) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        reant.updateProfile(request.getName(), request.getAppearance());
        return ReantResponse.from(reant);
    }

    public Reant findByUserId(Long userId) {
        return reantRepository.findByUserId(userId)
                .orElse(null);
    }

    /**
     * 리앤트 상태 조회
     */
    public ReantStatusResponse getReantStatus(Long userId) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ReantStatusResponse.from(reant);
    }
}
