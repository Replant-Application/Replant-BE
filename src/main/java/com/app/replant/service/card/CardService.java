package com.app.replant.service.card;

import com.app.replant.controller.dto.CardResponseDto;
import com.app.replant.entity.Card;

import java.util.List;

/**
 * 카드 서비스 인터페이스
 */
public interface CardService {

    /**
     * 모든 카드 조회
     */
    List<CardResponseDto> getAllCards();

    /**
     * 카드 ID로 조회
     */
    Card getCardById(Long cardId);

    /**
     * 사용자의 카드 조회
     */
    List<CardResponseDto> getUserCards(Long memberId);

    /**
     * 카드 추천
     */
    List<CardResponseDto> getRecommendedCards(Long memberId);
}
