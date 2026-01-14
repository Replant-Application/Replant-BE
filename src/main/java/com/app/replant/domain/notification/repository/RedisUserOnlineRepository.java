package com.app.replant.domain.notification.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 사용자 온라인 상태 저장소
 * Key: user:online:{userId}
 * Value: "online" (상태 표시용)
 * TTL: 60초 (heartbeat로 갱신)
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisUserOnlineRepository {

    private static final String KEY_PREFIX = "user:online:";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60); // 기본 60초

    private final StringRedisTemplate redisTemplate;

    /**
     * 사용자 온라인 상태 저장
     * @param userId 사용자 ID
     * @param ttlSeconds TTL (초 단위)
     */
    public void setOnline(Long userId, long ttlSeconds) {
        try {
            String key = getKey(userId);
            Duration ttl = Duration.ofSeconds(ttlSeconds);
            redisTemplate.opsForValue().set(key, "online", ttl);
            log.debug("[Redis] 사용자 온라인 상태 저장 - userId: {}, TTL: {}초", userId, ttlSeconds);
        } catch (Exception e) {
            log.error("[Redis] 사용자 온라인 상태 저장 실패 - userId: {}", userId, e);
        }
    }

    /**
     * 사용자 온라인 상태 저장 (기본 TTL 사용)
     * @param userId 사용자 ID
     */
    public void setOnline(Long userId) {
        setOnline(userId, DEFAULT_TTL.getSeconds());
    }

    /**
     * 사용자 온라인 상태 확인
     * @param userId 사용자 ID
     * @return 온라인 여부
     */
    public boolean isOnline(Long userId) {
        try {
            String key = getKey(userId);
            Boolean exists = redisTemplate.hasKey(key);
            boolean online = Boolean.TRUE.equals(exists);
            log.debug("[Redis] 사용자 온라인 상태 확인 - userId: {}, online: {}", userId, online);
            return online;
        } catch (Exception e) {
            log.error("[Redis] 사용자 온라인 상태 확인 실패 - userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 사용자 오프라인 상태로 변경 (키 삭제)
     * @param userId 사용자 ID
     */
    public void setOffline(Long userId) {
        try {
            String key = getKey(userId);
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("[Redis] 사용자 오프라인 상태로 변경 - userId: {}", userId);
            } else {
                log.debug("[Redis] 사용자 오프라인 상태 변경 (키 없음) - userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("[Redis] 사용자 오프라인 상태 변경 실패 - userId: {}", userId, e);
        }
    }

    /**
     * TTL 갱신 (heartbeat)
     * @param userId 사용자 ID
     * @param ttlSeconds 새로운 TTL (초 단위)
     */
    public void refreshTTL(Long userId, long ttlSeconds) {
        try {
            String key = getKey(userId);
            Duration ttl = Duration.ofSeconds(ttlSeconds);
            Boolean exists = redisTemplate.hasKey(key);
            
            if (Boolean.TRUE.equals(exists)) {
                redisTemplate.expire(key, ttl);
                log.debug("[Redis] 사용자 온라인 상태 TTL 갱신 - userId: {}, TTL: {}초", userId, ttlSeconds);
            } else {
                // 키가 없으면 새로 생성
                setOnline(userId, ttlSeconds);
                log.debug("[Redis] 사용자 온라인 상태 재생성 - userId: {}, TTL: {}초", userId, ttlSeconds);
            }
        } catch (Exception e) {
            log.error("[Redis] 사용자 온라인 상태 TTL 갱신 실패 - userId: {}", userId, e);
        }
    }

    /**
     * TTL 갱신 (기본 TTL 사용)
     * @param userId 사용자 ID
     */
    public void refreshTTL(Long userId) {
        refreshTTL(userId, DEFAULT_TTL.getSeconds());
    }

    /**
     * 남은 TTL 조회
     * @param userId 사용자 ID
     * @return 남은 TTL (초), 키가 없으면 -1
     */
    public long getRemainingTTL(Long userId) {
        try {
            String key = getKey(userId);
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null && ttl > 0 ? ttl : -1;
        } catch (Exception e) {
            log.error("[Redis] 사용자 온라인 상태 TTL 조회 실패 - userId: {}", userId, e);
            return -1;
        }
    }

    /**
     * Redis 키 생성
     * @param userId 사용자 ID
     * @return Redis 키
     */
    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
