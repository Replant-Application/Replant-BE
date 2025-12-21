package com.app.replant.repository.membercard;

import com.app.replant.entity.Card;
import com.app.replant.entity.Member;
import com.app.replant.entity.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberCardRepository extends JpaRepository<MemberCard, Long> {

    /**
     * 회원이 보유한 모든 카드 조회
     */
    @Query("SELECT mc FROM MemberCard mc JOIN FETCH mc.card WHERE mc.member.id = :memberId ORDER BY mc.acquiredAt DESC")
    List<MemberCard> findByMemberIdWithCard(@Param("memberId") Long memberId);

    /**
     * 회원이 보유한 새로운 카드만 조회
     */
    @Query("SELECT mc FROM MemberCard mc JOIN FETCH mc.card WHERE mc.member.id = :memberId AND mc.isNew = true")
    List<MemberCard> findNewCardsByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원이 특정 카드를 보유하고 있는지 확인
     */
    Optional<MemberCard> findByMemberAndCard(Member member, Card card);

    /**
     * 회원의 카드 보유 개수
     */
    long countByMemberId(Long memberId);

    /**
     * 회원이 특정 카드를 보유하고 있는지 확인
     */
    boolean existsByMemberIdAndCardId(Long memberId, Long cardId);

    /**
     * 특정 카드를 보유한 회원 수
     */
    long countByCardId(Long cardId);
}
