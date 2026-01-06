package com.app.replant.service.token;

import com.app.replant.config.InMemoryTokenConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token을 Redis로 관리하는 서비스
 * Redis 연결 실패 시 인메모리 저장소로 자동 폴백
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 7;  // 7일
    private static final long REFRESH_TOKEN_EXPIRE_MS = REFRESH_TOKEN_EXPIRE_DAYS * 24 * 60 * 60 * 1000;

    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Refresh Token 저장
     * @param memberId 회원 ID (key)
     * @param refreshToken Refresh Token 값
     */
    public void saveRefreshToken(String memberId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + memberId;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            redisTemplate.opsForValue().set(
                    key,
                    refreshToken,
                    REFRESH_TOKEN_EXPIRE_DAYS,
                    TimeUnit.DAYS
            );
            log.info("Refresh Token Redis 저장: {}", memberId);
        } catch (Exception e) {
            // Redis 연결 실패 시 인메모리로 저장
            long expiresAt = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS;
            InMemoryTokenConfig.getTokenStore().put(key, new InMemoryTokenConfig.TokenEntry(refreshToken, expiresAt));
            log.warn("Redis 연결 실패 ({}), 인메모리에 Refresh Token 저장: {}", e.getClass().getSimpleName(), memberId);
        }
    }

    /**
     * Refresh Token 조회
     * @param memberId 회원 ID
     * @return Refresh Token (없으면 empty)
     */
    public Optional<String> getRefreshToken(String memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            String token = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(token);
        } catch (Exception e) {
            // Redis 연결 실패 시 인메모리에서 조회
            log.debug("Redis 연결 실패, 인메모리에서 조회: {}", memberId);
            InMemoryTokenConfig.TokenEntry entry = InMemoryTokenConfig.getTokenStore().get(key);
            if (entry != null && entry.expiresAt > System.currentTimeMillis()) {
                return Optional.of(entry.value);
            }
            return Optional.empty();
        }
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     * @param memberId 회원 ID
     */
    public void deleteRefreshToken(String memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            redisTemplate.delete(key);
            log.info("Refresh Token 삭제 (로그아웃): {}", memberId);
        } catch (Exception e) {
            InMemoryTokenConfig.getTokenStore().remove(key);
            log.warn("Redis 연결 실패, 인메모리에서 Refresh Token 삭제: {}", memberId);
        }
    }

    /**
     * Refresh Token 검증 및 갱신
     * @param memberId 회원 ID
     * @param refreshToken 확인할 Refresh Token
     * @return 일치 여부
     */
    public boolean validateAndUpdate(String memberId, String currentRefreshToken, String newRefreshToken) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        String savedToken = null;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            savedToken = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // Redis 연결 실패 시 인메모리에서 조회
            log.debug("Redis 연결 실패, 인메모리에서 조회: {}", memberId);
            InMemoryTokenConfig.TokenEntry entry = InMemoryTokenConfig.getTokenStore().get(key);
            if (entry != null && entry.expiresAt > System.currentTimeMillis()) {
                savedToken = entry.value;
            }
        }

        if (savedToken == null) {
            log.error("Refresh Token 없음 (로그아웃 상태): {}", memberId);
            return false;
        }

        if (!savedToken.equals(currentRefreshToken)) {
            log.error("Refresh Token 불일치: {}", memberId);
            return false;
        }

        // 새로운 Refresh Token으로 갱신
        saveRefreshToken(memberId, newRefreshToken);

        return true;
    }
}

