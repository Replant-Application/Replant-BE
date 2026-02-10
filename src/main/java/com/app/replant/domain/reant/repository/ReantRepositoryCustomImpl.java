package com.app.replant.domain.reant.repository;

import com.app.replant.domain.reant.entity.Reant;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ReantRepository Custom Implementation
 * JPQL + fetchJoin으로 Reant + User 한 번에 조회 (N+1 방지)
 * QueryDSL의 fetchJoin이 제대로 동작하지 않는 경우를 대비하여 JPQL 사용
 */
@Repository
@RequiredArgsConstructor
public class ReantRepositoryCustomImpl implements ReantRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Optional<Reant> findByUserIdWithUser(Long userId) {
        // JPQL을 사용하여 fetch join 보장
        // 명시적으로 JOIN FETCH를 사용하여 User를 함께 로드
        // User는 @JsonIgnore로 직렬화되지 않으므로 순환 참조 문제 없음
        String jpql = "SELECT DISTINCT r FROM Reant r " +
                      "LEFT JOIN FETCH r.user u " +
                      "WHERE r.user.id = :userId";
        try {
            Reant result = entityManager.createQuery(jpql, Reant.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.of(result);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }
}
