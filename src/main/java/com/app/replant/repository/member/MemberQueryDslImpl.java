package com.app.replant.repository.member;

import com.app.replant.entity.Member;
import com.app.replant.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Member> findMembersLoggedInWithinThreeMonths(LocalDateTime threeMonthsAgo) {
        QMember m = QMember.member;

        return queryFactory
                .selectFrom(m)
                .where(
                        m.lastLoginAt.goe(threeMonthsAgo)
                )
                .fetch();
    }
}


