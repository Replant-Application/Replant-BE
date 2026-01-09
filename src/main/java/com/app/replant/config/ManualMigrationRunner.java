package com.app.replant.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 수동 마이그레이션 실행기
 * Flyway가 처리하지 못하는 V6, V7 마이그레이션을 직접 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualMigrationRunner implements CommandLineRunner {

    @Qualifier("secondaryDataSource")
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 수동 마이그레이션 시작 ===");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // V6 마이그레이션 필요 여부 확인 (post 테이블에 post_type 컬럼 존재 여부)
            boolean needV6 = !columnExists(stmt, "post", "post_type");

            if (needV6) {
                log.info("V6 마이그레이션 실행 중...");
                executeV6Migration(conn);
                log.info("V6 마이그레이션 완료");
            } else {
                log.info("V6 마이그레이션 스킵 (이미 적용됨)");
            }

            // V7 마이그레이션 필요 여부 확인 (mission 테이블에 mission_source 컬럼 존재 여부)
            boolean needV7 = !columnExists(stmt, "mission", "mission_source");

            if (needV7) {
                log.info("V7 마이그레이션 실행 중...");
                executeV7Migration(conn);
                log.info("V7 마이그레이션 완료");
            } else {
                log.info("V7 마이그레이션 스킵 (이미 적용됨)");
            }

            // V8 마이그레이션: user_mission.mission_source 컬럼 크기 수정
            log.info("V8 마이그레이션 실행 중...");
            executeV8Migration(conn);
            log.info("V8 마이그레이션 완료");

            // V9 마이그레이션: diary 테이블에 created_at, updated_at 컬럼 추가
            boolean needV9 = !columnExists(stmt, "diary", "created_at");
            if (needV9) {
                log.info("V9 마이그레이션 실행 중...");
                executeV9Migration(conn);
                log.info("V9 마이그레이션 완료");
            } else {
                log.info("V9 마이그레이션 스킵 (이미 적용됨)");
            }

        } catch (Exception e) {
            log.error("마이그레이션 실행 중 오류 발생: {}", e.getMessage(), e);
        }

        log.info("=== 수동 마이그레이션 종료 ===");
    }

    private boolean columnExists(Statement stmt, String tableName, String columnName) {
        try {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "' " +
                "AND COLUMN_NAME = '" + columnName + "'"
            );
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            log.warn("컬럼 존재 여부 확인 실패: {}", e.getMessage());
        }
        return false;
    }

    private void executeV6Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V6: Post + VerificationPost 통합

            // 1. 백업 테이블 생성
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_verification_post` AS SELECT * FROM `verification_post`");
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_post` AS SELECT * FROM `post`");
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_comment` AS SELECT * FROM `comment`");
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_verification_vote` AS SELECT * FROM `verification_vote`");

            // 2. 기존 테이블 삭제
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            executeIgnore(stmt, "DROP TABLE IF EXISTS verification_vote");
            executeIgnore(stmt, "DROP TABLE IF EXISTS comment");
            executeIgnore(stmt, "DROP TABLE IF EXISTS verification_post");
            executeIgnore(stmt, "DROP TABLE IF EXISTS post");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            // 3. 통합된 post 테이블 생성
            stmt.execute(
                "CREATE TABLE `post` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "`post_type` VARCHAR(20) NOT NULL DEFAULT 'GENERAL'," +
                "`user_id` BIGINT NOT NULL," +
                "`mission_id` BIGINT NULL," +
                "`custom_mission_id` BIGINT NULL," +
                "`mission_source` VARCHAR(20) NULL," +
                "`user_mission_id` BIGINT NULL," +
                "`title` VARCHAR(100) NULL," +
                "`content` TEXT NOT NULL," +
                "`image_urls` JSON NULL," +
                "`has_valid_badge` BOOLEAN DEFAULT FALSE," +
                "`del_flag` BOOLEAN NOT NULL DEFAULT FALSE," +
                "`status` VARCHAR(20) NULL," +
                "`approve_count` INT DEFAULT 0," +
                "`reject_count` INT DEFAULT 0," +
                "`verified_at` TIMESTAMP NULL," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "`updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX `idx_post_type` (`post_type`)," +
                "INDEX `idx_post_user_id` (`user_id`)," +
                "INDEX `idx_post_status` (`status`)," +
                "INDEX `idx_post_mission_id` (`mission_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            // 4. comment 테이블 재생성
            stmt.execute(
                "CREATE TABLE `comment` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "`post_id` BIGINT NOT NULL," +
                "`user_id` BIGINT NOT NULL," +
                "`parent_id` BIGINT NULL," +
                "`content` TEXT NOT NULL," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "`updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX `idx_comment_post_id` (`post_id`)," +
                "INDEX `idx_comment_user_id` (`user_id`)," +
                "INDEX `idx_comment_parent_id` (`parent_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            // 5. verification_vote 테이블 재생성
            stmt.execute(
                "CREATE TABLE `verification_vote` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "`post_id` BIGINT NOT NULL," +
                "`user_id` BIGINT NOT NULL," +
                "`vote_type` VARCHAR(20) NOT NULL," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY `uk_verification_vote` (`post_id`, `user_id`)," +
                "INDEX `idx_verification_vote_post` (`post_id`)," +
                "INDEX `idx_verification_vote_user` (`user_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            // 6. 백업 데이터 복원 (일반 게시글)
            executeIgnore(stmt,
                "INSERT INTO `post` (`post_type`, `user_id`, `mission_id`, `custom_mission_id`, `title`, `content`, " +
                "`image_urls`, `has_valid_badge`, `del_flag`, `created_at`, `updated_at`) " +
                "SELECT 'GENERAL', `user_id`, `mission_id`, `custom_mission_id`, `title`, `content`, " +
                "`image_urls`, `has_valid_badge`, `del_flag`, `created_at`, `updated_at` " +
                "FROM `_backup_post`"
            );

            // 7. 백업 데이터 복원 (인증 게시글)
            executeIgnore(stmt,
                "INSERT INTO `post` (`post_type`, `user_id`, `mission_id`, `custom_mission_id`, `user_mission_id`, " +
                "`content`, `image_urls`, `status`, `approve_count`, `reject_count`, `verified_at`, `created_at`, `updated_at`) " +
                "SELECT 'VERIFICATION', vp.`user_id`, um.`mission_id`, um.`custom_mission_id`, vp.`user_mission_id`, " +
                "vp.`content`, vp.`image_urls`, vp.`status`, vp.`approve_count`, vp.`reject_count`, " +
                "vp.`verified_at`, vp.`created_at`, vp.`updated_at` " +
                "FROM `_backup_verification_post` vp " +
                "LEFT JOIN `user_mission` um ON vp.`user_mission_id` = um.`id`"
            );

            log.info("V6 마이그레이션: Post + VerificationPost 통합 완료");
        }
    }

    private void executeV7Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V7: Mission + CustomMission 통합

            // 1. 백업 테이블 생성
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_mission` AS SELECT * FROM `mission`");
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_custom_mission` AS SELECT * FROM `custom_mission`");
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_mission_age_ranges` AS SELECT * FROM `mission_age_ranges`");
            executeIgnore(stmt, "CREATE TABLE IF NOT EXISTS `_backup_user_mission` AS SELECT * FROM `user_mission`");

            // 2. user_mission에 mission_source 컬럼 추가 (이미 없다면)
            if (!columnExists(stmt, "user_mission", "mission_source")) {
                stmt.execute("ALTER TABLE `user_mission` ADD COLUMN `mission_source` VARCHAR(20) NULL");
            }

            // 3. 기존 미션 테이블에 mission_source 컬럼 추가 (테이블 재생성 대신)
            stmt.execute("ALTER TABLE `mission` ADD COLUMN `mission_source` VARCHAR(20) NOT NULL DEFAULT 'OFFICIAL'");
            stmt.execute("ALTER TABLE `mission` ADD COLUMN `creator_id` BIGINT NULL");
            stmt.execute("ALTER TABLE `mission` ADD COLUMN `duration_days` INT NULL");
            stmt.execute("ALTER TABLE `mission` ADD COLUMN `is_public` BOOLEAN DEFAULT FALSE");

            // 4. custom_mission 데이터를 mission 테이블로 통합
            executeIgnore(stmt,
                "INSERT INTO `mission` (`mission_source`, `creator_id`, `title`, `description`, `type`, " +
                "`verification_type`, `gps_latitude`, `gps_longitude`, `gps_radius_meters`, `required_minutes`, " +
                "`exp_reward`, `badge_duration_days`, `is_active`, `created_at`, `worry_type`, " +
                "`difficulty_level`, `duration_days`, `is_public`) " +
                "SELECT 'CUSTOM', `creator_id`, `title`, `description`, COALESCE(`mission_type`, 'DAILY'), " +
                "`verification_type`, `gps_latitude`, `gps_longitude`, `gps_radius_meters`, `required_minutes`, " +
                "`exp_reward`, `badge_duration_days`, `is_active`, `created_at`, `worry_type`, " +
                "`difficulty_level`, `duration_days`, `is_public` " +
                "FROM `_backup_custom_mission`"
            );

            // 5. user_mission의 custom_mission_id를 mission_id로 매핑 업데이트
            executeIgnore(stmt,
                "UPDATE `user_mission` um " +
                "JOIN `_backup_custom_mission` cm ON um.`custom_mission_id` = cm.`id` " +
                "JOIN `mission` m ON m.`mission_source` = 'CUSTOM' AND m.`creator_id` = cm.`creator_id` " +
                "AND m.`title` = cm.`title` AND m.`created_at` = cm.`created_at` " +
                "SET um.`mission_id` = m.`id`, um.`mission_source` = 'CUSTOM' " +
                "WHERE um.`custom_mission_id` IS NOT NULL"
            );

            // 6. mission_source가 없는 user_mission을 OFFICIAL로 설정
            stmt.execute(
                "UPDATE `user_mission` SET `mission_source` = 'OFFICIAL' " +
                "WHERE `mission_source` IS NULL AND `mission_id` IS NOT NULL"
            );

            log.info("V7 마이그레이션: Mission + CustomMission 통합 완료");
        }
    }

    private void executeV8Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V8: user_mission.mission_source 컬럼 크기 수정

            // 1. mission_source 컬럼 크기 확장 (Data truncated 오류 수정)
            executeIgnore(stmt, "ALTER TABLE `user_mission` MODIFY COLUMN `mission_source` VARCHAR(20) NULL");

            // 2. 기존 NULL 값을 OFFICIAL로 업데이트
            executeIgnore(stmt,
                "UPDATE `user_mission` SET `mission_source` = 'OFFICIAL' " +
                "WHERE `mission_source` IS NULL AND `mission_id` IS NOT NULL"
            );

            log.info("V8 마이그레이션: user_mission.mission_source 컬럼 크기 수정 완료");
        }
    }

    private void executeV9Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V9: diary 테이블에 created_at, updated_at 컬럼 추가

            // 1. created_at 컬럼 추가
            executeIgnore(stmt,
                "ALTER TABLE `diary` ADD COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
            );

            // 2. updated_at 컬럼 추가
            executeIgnore(stmt,
                "ALTER TABLE `diary` ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
            );

            // 3. 기존 데이터의 created_at을 date 기준으로 설정
            executeIgnore(stmt,
                "UPDATE `diary` SET `created_at` = TIMESTAMP(`date`) WHERE `created_at` IS NULL OR `created_at` = '0000-00-00 00:00:00'"
            );

            log.info("V9 마이그레이션: diary 테이블 created_at, updated_at 컬럼 추가 완료");
        }
    }

    private void executeIgnore(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (Exception e) {
            log.debug("SQL 실행 (무시): {} - {}", sql.substring(0, Math.min(50, sql.length())), e.getMessage());
        }
    }
}
