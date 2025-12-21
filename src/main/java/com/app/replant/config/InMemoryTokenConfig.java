package com.app.replant.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis가 없을 때 사용할 인메모리 토큰 저장소 설정
 * Redis 연결 실패 시 자동으로 인메모리 방식으로 대체됨
 */
@Configuration
public class InMemoryTokenConfig {

    private static final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private static final Map<String, CodeEntry> verificationCodeStore = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        // 만료된 토큰 정리 (1시간마다)
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            tokenStore.entrySet().removeIf(e -> e.getValue().expiresAt < now);
            verificationCodeStore.entrySet().removeIf(e -> e.getValue().expiresAt < now);
        }, 1, 1, TimeUnit.HOURS);
    }

    public static Map<String, TokenEntry> getTokenStore() {
        return tokenStore;
    }

    public static Map<String, CodeEntry> getVerificationCodeStore() {
        return verificationCodeStore;
    }

    public static class TokenEntry {
        public final String value;
        public final long expiresAt;

        public TokenEntry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    public static class CodeEntry {
        public final String value;
        public final long expiresAt;

        public CodeEntry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
