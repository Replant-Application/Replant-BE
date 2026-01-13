package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * 이메일로 사용자 조회 (Reant 포함) - JWT 인증용
     * N+1 문제 방지를 위해 JOIN FETCH 사용
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.reant WHERE u.email = :email")
    Optional<User> findByEmailWithReant(String email);

    boolean existsByEmail(String email);

    Optional<User> findByNicknameAndPhone(String nickname, String phone);

    boolean existsByNickname(String nickname);

    /**
     * 모든 활성 사용자 조회 (알림 발송용)
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE'")
    List<User> findAllActiveUsers();
}
