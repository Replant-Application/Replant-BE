package com.app.replant.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
     */
    @Bean
    public Flyway flyway(@Qualifier("secondaryDataSource") DataSource dataSource) {
        // flyway_schema_history 테이블 초기화
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // flyway_schema_history에서 모든 기록 삭제하고 baseline만 다시 삽입
            stmt.execute("DELETE FROM flyway_schema_history");

            // baseline을 V9로 설정 (V6~V9는 수동 마이그레이션으로 처리)
            String insertBaseline =
                "INSERT INTO flyway_schema_history " +
                "(installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES " +
                "(1, '9', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'admin', 0, 1)";
            stmt.execute(insertBaseline);

            log.info("Flyway: baseline V9 설정 완료 - 마이그레이션 스킵");
        } catch (Exception e) {
            log.warn("Flyway baseline 설정 중 오류 (무시 가능): {}", e.getMessage());
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("9")
                .validateOnMigrate(false)
                .outOfOrder(false)
                .cleanDisabled(true)
                .load();

        // 마이그레이션 스킵 (이미 baseline V9로 설정됨)
        log.info("Flyway: 마이그레이션 스킵 (baseline V9)");

        return flyway;
    }

    /**
     * Flyway 마이그레이션 전략
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Flyway 마이그레이션 전략: 빈에서 직접 처리됨");
        };
    }
}
