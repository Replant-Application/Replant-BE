package com.app.replant.domain.reant.service;

import com.app.replant.controller.dto.InteractionResponse;
import com.app.replant.controller.dto.ReantResponse;
import com.app.replant.controller.dto.ReantStatusResponse;
import com.app.replant.controller.dto.ReantUpdateRequest;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.repository.ReantRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
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

    /**
     * 먹이주기 - 배고픔 -30, 건강도 +5, 기분 +10
     */
    @Transactional
    public InteractionResponse feed(Long userId) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int prevMood = reant.getMood();
        int prevHealth = reant.getHealth();
        int prevHunger = reant.getHunger();

        reant.feed();

        return InteractionResponse.of(
                reant,
                "맛있게 먹었어요! 배가 부르네요 🍚",
                reant.getMood() - prevMood,
                reant.getHealth() - prevHealth,
                reant.getHunger() - prevHunger
        );
    }

    /**
     * 쉬게하기 - 건강도 +20, 기분 +10
     */
    @Transactional
    public InteractionResponse rest(Long userId) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int prevMood = reant.getMood();
        int prevHealth = reant.getHealth();
        int prevHunger = reant.getHunger();

        reant.rest();

        return InteractionResponse.of(
                reant,
                "푹 쉬었어요! 건강해졌어요 😴",
                reant.getMood() - prevMood,
                reant.getHealth() - prevHealth,
                reant.getHunger() - prevHunger
        );
    }

    /**
     * 놀아주기 - 기분 +20, 배고픔 +5
     */
    @Transactional
    public InteractionResponse play(Long userId) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int prevMood = reant.getMood();
        int prevHealth = reant.getHealth();
        int prevHunger = reant.getHunger();

        reant.play();

        return InteractionResponse.of(
                reant,
                "신나게 놀았어요! 기분이 좋아요 🎮",
                reant.getMood() - prevMood,
                reant.getHealth() - prevHealth,
                reant.getHunger() - prevHunger
        );
    }

    /**
     * 쓰다듬기 - 기분 +15
     */
    @Transactional
    public InteractionResponse pet(Long userId) {
        Reant reant = reantRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int prevMood = reant.getMood();
        int prevHealth = reant.getHealth();
        int prevHunger = reant.getHunger();

        reant.pet();

        return InteractionResponse.of(
                reant,
                "쓰다듬어줘서 고마워요! 💕",
                reant.getMood() - prevMood,
                reant.getHealth() - prevHealth,
                reant.getHunger() - prevHunger
        );
    }
}
