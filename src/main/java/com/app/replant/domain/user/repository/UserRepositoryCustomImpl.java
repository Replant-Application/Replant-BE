package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository Custom Implementation
 * JPQL + fetchJoin으로 User + Reant 한 번에 조회 (N+1 방지)
 * QueryDSL의 fetchJoin이 제대로 동작하지 않는 경우를 대비하여 JPQL 사용
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Optional<User> findByIdWithReant(Long userId) {
        // JPQL을 사용하여 fetch join 보장
        // 명시적으로 JOIN FETCH를 사용하여 Reant를 함께 로드
        String jpql = "SELECT DISTINCT u FROM User u " +
                      "LEFT JOIN FETCH u.reant r " +
                      "WHERE u.id = :userId AND (u.delFlag = false OR u.delFlag IS NULL)";
        try {
            User result = entityManager.createQuery(jpql, User.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.of(result);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmailWithReant(String email) {
        // JPQL을 사용하여 fetch join 보장
        // 명시적으로 JOIN FETCH를 사용하여 Reant를 함께 로드
        // mappedBy를 사용하는 역방향 관계에서도 제대로 동작하도록 명시적으로 지정
        String jpql = "SELECT DISTINCT u FROM User u " +
                      "LEFT JOIN FETCH u.reant r " +
                      "WHERE u.email = :email AND (u.delFlag = false OR u.delFlag IS NULL)";
        log.info("=== [UserRepositoryCustomImpl] findByEmailWithReant 호출됨 ===");
        log.info("=== [UserRepositoryCustomImpl] JPQL: {} ===", jpql);
        log.info("=== [UserRepositoryCustomImpl] Parameter: email={} ===", email);
        try {
            jakarta.persistence.TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
            query.setParameter("email", email);
            User result = query.getSingleResult();
            log.info("=== [UserRepositoryCustomImpl] User found: userId={}, reantLoaded={}, reantId={} ===", 
                    result.getId(), 
                    result.getReant() != null,
                    result.getReant() != null ? result.getReant().getId() : null);
            // Hibernate가 실제로 Reant를 로드했는지 확인
            if (result.getReant() != null) {
                log.info("=== [UserRepositoryCustomImpl] Reant 정보: id={}, name={} ===", 
                        result.getReant().getId(), result.getReant().getName());
            } else {
                log.warn("=== [UserRepositoryCustomImpl] Reant가 null입니다! ===");
            }
            return Optional.of(result);
        } catch (jakarta.persistence.NoResultException e) {
            log.info("=== [UserRepositoryCustomImpl] User not found with email: {} ===", email);
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmailIncludingDeleted(String email) {
        // JPQL을 사용하여 fetch join 보장
        // 명시적으로 JOIN FETCH를 사용하여 Reant를 함께 로드
        String jpql = "SELECT DISTINCT u FROM User u " +
                      "LEFT JOIN FETCH u.reant r " +
                      "WHERE u.email = :email";
        try {
            User result = entityManager.createQuery(jpql, User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(result);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByIdIncludingDeleted(Long userId) {
        // JPQL을 사용하여 fetch join 보장
        // 명시적으로 JOIN FETCH를 사용하여 Reant를 함께 로드
        String jpql = "SELECT DISTINCT u FROM User u " +
                      "LEFT JOIN FETCH u.reant r " +
                      "WHERE u.id = :userId";
        try {
            User result = entityManager.createQuery(jpql, User.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.of(result);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }
}
