package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.User;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.app.replant.domain.user.entity.QUser.user;
import static com.app.replant.domain.reant.entity.QReant.reant;

/**
 * UserRepository Custom Implementation
 * QueryDSL + fetchJoin으로 User + Reant 한 번에 조회 (N+1 방지)
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static BooleanExpression isNotDeleted() {
        return user.delFlag.isFalse().or(user.delFlag.isNull());
    }

    @Override
    public Optional<User> findByIdWithReant(Long userId) {
        User result = queryFactory
                .selectFrom(user)
                .leftJoin(user.reant, reant).fetchJoin()
                .where(user.id.eq(userId).and(isNotDeleted()))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<User> findByEmailWithReant(String email) {
        User result = queryFactory
                .selectFrom(user)
                .leftJoin(user.reant, reant).fetchJoin()
                .where(user.email.eq(email).and(isNotDeleted()))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<User> findByEmailIncludingDeleted(String email) {
        User result = queryFactory
                .selectFrom(user)
                .leftJoin(user.reant, reant).fetchJoin()
                .where(user.email.eq(email))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<User> findByIdIncludingDeleted(Long userId) {
        User result = queryFactory
                .selectFrom(user)
                .leftJoin(user.reant, reant).fetchJoin()
                .where(user.id.eq(userId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
}
