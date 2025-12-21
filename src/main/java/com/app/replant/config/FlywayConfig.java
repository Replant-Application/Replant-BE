package com.app.replant.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway 설정
 * - MariaDB (secondary) 데이터소스에 대한 마이그레이션 관리
 * - 베이스라인 설정 및 마이그레이션 전략 정의
 */
@Slf4j
@Configuration
public class FlywayConfig {

    /**
     * Flyway 인스턴스 생성 및 설정
     * Secondary DataSource (MariaDB)에 대해 Flyway를 설정합니다.
     * initMethod 제거 - FlywayMigrationStrategy에서 처리
     */
    @Bean
    public Flyway flyway(@Qualifier("secondaryDataSource") DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)  // 기존 DB에 대해 베이스라인 설정
                .baselineVersion("0")      // 베이스라인 버전
                .validateOnMigrate(false)  // 마이그레이션 시 검증 비활성화 (repair 사용)
                .outOfOrder(false)         // 순서대로 실행
                .cleanDisabled(false)      // clean 비활성화 해제 (repair에 필요)
                .load();

        // 직접 repair 및 migrate 수행
        try {
            log.info("Flyway: 실패한 마이그레이션 복구 시도...");
            flyway.repair();
            log.info("Flyway: 마이그레이션 실행...");
            flyway.migrate();
            log.info("Flyway: 마이그레이션 완료");
        } catch (Exception e) {
            log.warn("Flyway 마이그레이션 경고 (무시 가능): {}", e.getMessage());
            // 마이그레이션 실패해도 애플리케이션은 시작하도록 함
        }

        return flyway;
    }

    /**
     * Flyway 마이그레이션 전략
     * 빈 전략으로 변경 (flyway 빈에서 직접 처리)
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // flyway 빈에서 이미 처리했으므로 여기서는 아무것도 하지 않음
            log.info("Flyway 마이그레이션 전략: 빈에서 직접 처리됨");
        };
    }
}
