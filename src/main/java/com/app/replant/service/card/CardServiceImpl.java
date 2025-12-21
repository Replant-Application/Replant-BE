package com.app.replant.service.card;

import com.app.replant.controller.dto.CardRequestDto;
import com.app.replant.controller.dto.CardResponseDto;
import com.app.replant.entity.Card;
import com.app.replant.entity.Member;
import com.app.replant.entity.MemberCard;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.repository.card.CardRepository;
import com.app.replant.repository.member.MemberRepository;
import com.app.replant.repository.membercard.MemberCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카드 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final MemberCardRepository memberCardRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<CardResponseDto> getAllCards() {
        log.info("getAllCards() - 모든 카드 조회");
        return cardRepository.findAllOrderByPointsRequired()
                .stream()
                .map(CardResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Card getCardById(Long cardId) {
        log.info("getCardById() - cardId: {}", cardId);
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));
    }

    @Override
    public List<CardResponseDto> getUserCards(Long memberId) {
        log.info("getUserCards() - memberId: {}", memberId);

        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 회원이 보유한 카드 조회
        List<MemberCard> memberCards = memberCardRepository.findByMemberIdWithCard(memberId);

        return memberCards.stream()
                .map(mc -> CardResponseDto.fromEntity(mc.getCard()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponseDto> getRecommendedCards(Long memberId) {
        log.info("getRecommendedCards() - memberId: {}", memberId);

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 회원의 현재 포인트로 획득 가능한 카드 조회
        // TODO: Member 엔티티에 points 필드가 있다고 가정. 없으면 다른 로직으로 대체 필요
        Integer memberPoints = getMemberPoints(member);

        List<Card> availableCards = cardRepository.findAvailableCardsByPoints(memberPoints);

        // 이미 보유한 카드는 제외
        List<Long> ownedCardIds = memberCardRepository.findByMemberIdWithCard(memberId)
                .stream()
                .map(mc -> mc.getCard().getId())
                .collect(Collectors.toList());

        return availableCards.stream()
                .filter(card -> !ownedCardIds.contains(card.getId()))
                .map(CardResponseDto::fromEntity)
                .limit(10) // 상위 10개만 추천
                .collect(Collectors.toList());
    }

    @Transactional
    public CardResponseDto createCard(CardRequestDto request) {
        log.info("createCard() - name: {}", request.getName());

        Card card = Card.builder()
                .name(request.getName())
                .description(request.getDescription())
                .cardType(request.getCardType())
                .imageUrl(request.getImageUrl())
                .pointsRequired(request.getPointsRequired())
                .build();

        Card savedCard = cardRepository.save(card);
        return CardResponseDto.fromEntity(savedCard);
    }

    @Transactional
    public CardResponseDto updateCard(Long cardId, CardRequestDto request) {
        log.info("updateCard() - cardId: {}, name: {}", cardId, request.getName());

        Card card = getCardById(cardId);

        // 카드 정보 업데이트 (Builder 패턴 사용 시 새 객체 생성 필요)
        Card updatedCard = Card.builder()
                .id(card.getId())
                .name(request.getName() != null ? request.getName() : card.getName())
                .description(request.getDescription() != null ? request.getDescription() : card.getDescription())
                .cardType(request.getCardType() != null ? request.getCardType() : card.getCardType())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : card.getImageUrl())
                .pointsRequired(request.getPointsRequired() != null ? request.getPointsRequired() : card.getPointsRequired())
                .build();

        Card saved = cardRepository.save(updatedCard);
        return CardResponseDto.fromEntity(saved);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        log.info("deleteCard() - cardId: {}", cardId);

        Card card = getCardById(cardId);

        // 이미 사용자가 보유한 카드는 삭제 불가
        long usageCount = memberCardRepository.countByCardId(cardId);
        if (usageCount > 0) {
            throw new CustomException(ErrorCode.CARD_IN_USE);
        }

        cardRepository.delete(card);
    }

    @Transactional
    public void assignCardToMember(Long memberId, Long cardId) {
        log.info("assignCardToMember() - memberId: {}, cardId: {}", memberId, cardId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Card card = getCardById(cardId);

        // 이미 보유한 카드인지 확인
        if (memberCardRepository.existsByMemberIdAndCardId(memberId, cardId)) {
            throw new CustomException(ErrorCode.CARD_ALREADY_OWNED);
        }

        MemberCard memberCard = MemberCard.builder()
                .member(member)
                .card(card)
                .isNew(true)
                .build();

        memberCardRepository.save(memberCard);
    }

    /**
     * 회원의 포인트를 가져오는 헬퍼 메서드
     * Member 엔티티 구조에 따라 수정 필요
     */
    private Integer getMemberPoints(Member member) {
        // TODO: Member 엔티티에 points 필드가 있으면 그것을 사용
        // 없으면 다른 로직으로 계산 (예: 미션 완료 수 * 10 등)
        return 100; // 임시값
    }
}
