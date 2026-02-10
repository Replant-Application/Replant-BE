package com.app.replant.domain.reant.repository;

import com.app.replant.domain.reant.entity.Reant;

import java.util.Optional;

public interface ReantRepositoryCustom {
    Optional<Reant> findByUserIdWithUser(Long userId);
}
