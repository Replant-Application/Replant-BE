package com.app.replant.domain.user.repository;

import com.app.replant.domain.user.entity.UserOAuth;
import com.app.replant.domain.user.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOAuthRepository extends JpaRepository<UserOAuth, Long> {

    Optional<UserOAuth> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
