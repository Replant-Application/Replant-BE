package com.app.replant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_key", nullable = false, unique = true)
    private String tokenKey; // 사용자 식별 키 (memberId 등)

    @Column(name = "token_value", nullable = false)
    private String tokenValue; // refresh token 값

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void updateToken(String newTokenValue, LocalDateTime newExpiresAt) {
        this.tokenValue = newTokenValue;
        this.expiresAt = newExpiresAt;
    }
}
