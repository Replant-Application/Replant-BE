package com.app.replant.repository.card;

import com.app.replant.entity.Card;
import com.app.replant.entity.type.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * 카드 타입별 조회
     */
    List<Card> findByCardType(CardType cardType);

    /**
     * 포인트 범위로 카드 조회 (추천 시스템용)
     */
    @Query("SELECT c FROM Card c WHERE c.pointsRequired <= :maxPoints ORDER BY c.pointsRequired DESC")
    List<Card> findAvailableCardsByPoints(@Param("maxPoints") Integer maxPoints);

    /**
     * 활성 카드 전체 조회 (포인트 오름차순)
     */
    @Query("SELECT c FROM Card c ORDER BY c.pointsRequired ASC")
    List<Card> findAllOrderByPointsRequired();
}
