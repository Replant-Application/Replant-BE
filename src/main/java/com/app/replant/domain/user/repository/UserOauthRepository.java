package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.UserOauth;
import com.app.replant.domain.user.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOauthRepository extends JpaRepository<UserOauth, Long> {

    Optional<UserOauth> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    Optional<UserOauth> findByUserIdAndProvider(Long userId, OAuthProvider provider);

    boolean existsByProviderAndProviderId(OAuthProvider provider, String providerId);
}
