package com.app.replant.global.infrastructure.service.token;

import com.app.replant.global.config.InMemoryTokenConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * AccessToken 블랙리스트를 Redis로 관리하는 서비스
 * 로그아웃 시 AccessToken을 블랙리스트에 등록하여 토큰의 남은 유효기간만큼 저장
 * Redis 연결 실패 시 인메모리 저장소로 자동 폴백
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Redis 연결 상태 확인
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * AccessToken을 블랙리스트에 등록
     * @param accessToken 블랙리스트에 등록할 AccessToken
     * @param ttlSeconds 토큰의 남은 유효기간 (초 단위)
     */
    public void addToBlacklist(String accessToken, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            log.debug("토큰이 이미 만료되어 블랙리스트에 추가하지 않음: {}", accessToken.substring(0, Math.min(20, accessToken.length())));
            return;
        }

        String key = BLACKLIST_PREFIX + accessToken;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            redisTemplate.opsForValue().set(
                    key,
                    "blacklisted",
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
            log.info("AccessToken 블랙리스트 등록 완료 (TTL: {}초)", ttlSeconds);
        } catch (Exception e) {
            // Redis 연결 실패 시 인메모리로 저장
            long expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000);
            InMemoryTokenConfig.getTokenStore().put(key, new InMemoryTokenConfig.TokenEntry("blacklisted", expiresAt));
            log.warn("Redis 연결 실패 ({}), 인메모리에 블랙리스트 저장 (TTL: {}초)", e.getClass().getSimpleName(), ttlSeconds);
        }
    }

    /**
     * AccessToken이 블랙리스트에 있는지 확인
     * @param accessToken 확인할 AccessToken
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            String value = redisTemplate.opsForValue().get(key);
            return value != null;
        } catch (Exception e) {
            // Redis 연결 실패 시 인메모리에서 조회
            log.debug("Redis 연결 실패, 인메모리에서 블랙리스트 조회");
            InMemoryTokenConfig.TokenEntry entry = InMemoryTokenConfig.getTokenStore().get(key);
            if (entry != null && entry.expiresAt > System.currentTimeMillis()) {
                return true;
            }
            return false;
        }
    }

    /**
     * AccessToken을 블랙리스트에서 제거 (필요시 사용)
     * @param accessToken 제거할 AccessToken
     */
    public void removeFromBlacklist(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;

        try {
            if (!isRedisAvailable()) {
                throw new RedisConnectionFailureException("Redis unavailable");
            }
            redisTemplate.delete(key);
            log.info("AccessToken 블랙리스트에서 제거 완료");
        } catch (Exception e) {
            InMemoryTokenConfig.getTokenStore().remove(key);
            log.warn("Redis 연결 실패, 인메모리에서 블랙리스트 제거");
        }
    }
}

