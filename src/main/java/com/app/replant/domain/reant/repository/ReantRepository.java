package com.app.replant.domain.reant.repository;

import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReantRepository extends JpaRepository<Reant, Long> {

    Optional<Reant> findByUser(User user);

    Optional<Reant> findByUserId(Long userId);
}
