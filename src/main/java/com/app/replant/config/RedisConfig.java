package com.app.replant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

/**
 * Redis 설정
 * Redis 연결 실패 시에도 애플리케이션이 정상 시작되도록 구성
 * 실제 Redis 작업 시 연결 실패하면 인메모리 폴백 사용
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration timeout;

    /**
     * RedisConnectionFactory - 연결 실패해도 Bean 생성 성공
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            
            // 비밀번호가 설정되어 있으면 설정
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
                log.info("Redis 비밀번호 설정됨");
            }

            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(timeout)
                    .build();

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
            factory.setValidateConnection(false);  // 연결 검증 비활성화 (시작 시 실패 방지)

            log.info("Redis 연결 설정 완료: {}:{}", redisHost, redisPort);
            return factory;
        } catch (Exception e) {
            log.warn("Redis 연결 설정 실패, 인메모리 폴백 사용 예정: {}", e.getMessage());
            // 기본 설정으로 반환 (실제 연결은 사용 시점에 시도)
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.setValidateConnection(false);
            return factory;
        }
    }

    /**
     * StringRedisTemplate 빈 (이메일 인증 코드 저장용)
     * Key: String, Value: String
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    /**
     * RedisTemplate 빈 (범용)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String 직렬화 (Key, Value 모두)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        return template;
    }
}

