package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.User;

import java.util.Optional;

/**
 * UserRepository Custom Interface
 * QueryDSL + fetchJoin으로 N+1 방지 조회 메서드 정의
 */
public interface UserRepositoryCustom {

    /**
     * ID로 사용자 조회 (Reant 포함) - N+1 방지
     * Soft Delete된 사용자 제외
     */
    Optional<User> findByIdWithReant(Long userId);

    /**
     * 이메일로 사용자 조회 (Reant 포함) - N+1 방지
     * Soft Delete된 사용자 제외
     */
    Optional<User> findByEmailWithReant(String email);

    /**
     * Soft Delete 포함 이메일로 사용자 조회 (Reant 포함) - 계정 복구용
     */
    Optional<User> findByEmailIncludingDeleted(String email);

    /**
     * Soft Delete 포함 ID로 사용자 조회 (Reant 포함) - 계정 복구용
     */
    Optional<User> findByIdIncludingDeleted(Long userId);
}
