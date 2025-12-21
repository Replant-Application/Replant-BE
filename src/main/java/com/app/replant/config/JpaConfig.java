package com.app.replant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정
 * JPA Auditing 활성화 (createdDate, modifiedDate 자동 관리)
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPAQueryFactory는 QueryDslConfig에서 관리
}
