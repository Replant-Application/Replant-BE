package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByNicknameAndPhone(String nickname, String phone);

    boolean existsByNickname(String nickname);
}
