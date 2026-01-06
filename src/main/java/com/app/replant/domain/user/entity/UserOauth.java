package com.app.replant.domain.user.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.user.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_oauth", uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOauth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Builder
    private UserOauth(User user, OAuthProvider provider, String providerId, String accessToken, String refreshToken) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void updateTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
