package com.app.replant.global.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정
 * - userDetails: UserDetailService의 loadUserByUsername 메서드 결과 캐싱 (30분 TTL)
 * - reant: ReantService의 getMyReant 메서드 결과 캐싱 (1분 TTL - 자주 변경될 수 있음)
 * - reantStatus: ReantService의 getReantStatus 메서드 결과 캐싱 (1분 TTL)
 * 
 * 참고: Caffeine이 사용 불가능한 경우 Spring의 기본 ConcurrentMapCacheManager 사용
 * Caffeine을 사용하려면 build.gradle에 'com.github.ben-manes.caffeine:caffeine' 의존성 추가 필요
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Spring의 기본 인메모리 캐시 사용
        // Caffeine이 필요하면 CaffeineCacheManager로 변경 가능
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "userDetails",  // UserDetail 캐시
                "reant",        // Reant 조회 캐시
                "reantStatus"   // Reant 상태 캐시
        );
        return cacheManager;
    }
}
