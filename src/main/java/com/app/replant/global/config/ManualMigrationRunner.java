package com.app.replant.global.config;

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
import java.sql.SQLException;
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

            // V7 마이그레이션 필요 여부 확인 (mission 테이블에 mission_source 또는 mission_type 컬럼 존재 여부)
            // mission_type이 이미 있으면 V7/V21이 이미 완료된 것임
            boolean needV7 = !columnExists(stmt, "mission", "mission_source") && !columnExists(stmt, "mission", "mission_type");

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

            // V16 마이그레이션: mission.difficulty_level 컬럼 크기 수정
            log.info("V16 마이그레이션 실행 중...");
            executeV16Migration(conn);
            log.info("V16 마이그레이션 완료");

            // V17 마이그레이션: custom_mission.is_promoted 기본값 설정
            log.info("V17 마이그레이션 실행 중...");
            executeV17Migration(conn);
            log.info("V17 마이그레이션 완료");

            // V18 마이그레이션: mission.mission_source DEFAULT 값 제거 및 creator_id 기반 CUSTOM 설정
            // mission_source가 있어야만 실행 (이미 mission_type으로 변경되었으면 스킵)
            if (columnExists(stmt, "mission", "mission_source")) {
                log.info("V18 마이그레이션 실행 중...");
                executeV18Migration(conn);
                log.info("V18 마이그레이션 완료");
            } else {
                log.info("V18 마이그레이션 스킵 (mission_source 컬럼 없음 - 이미 mission_type으로 변경됨)");
            }

            // V21 마이그레이션: mission_source를 mission_type으로 변경, GPS 컬럼 삭제, type 컬럼 삭제
            boolean needV21 = columnExists(stmt, "mission", "mission_source") &&
                              !columnExists(stmt, "mission", "mission_type");
            if (needV21) {
                log.info("V21 마이그레이션 실행 중...");
                executeV21Migration(conn);
                log.info("V21 마이그레이션 완료");
            } else {
                log.info("V21 마이그레이션 스킵 (이미 적용됨)");
            }

            // mission_source 컬럼이 남아있으면 삭제 (mission_type으로 이미 변경된 경우)
            if (columnExists(stmt, "mission", "mission_source") && columnExists(stmt, "mission", "mission_type")) {
                log.info("mission_source 컬럼 정리 중 (mission_type으로 대체됨)...");
                try (Statement cleanupStmt = conn.createStatement()) {
                    cleanupStmt.execute("ALTER TABLE `mission` DROP COLUMN `mission_source`");
                }
                log.info("mission_source 컬럼 삭제 완료");
            }

            // V22: comment 테이블에 deleted_at 컬럼 추가
            if (!columnExists(stmt, "comment", "deleted_at")) {
                log.info("V22 마이그레이션 실행 중: comment.deleted_at 컬럼 추가...");
                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute("ALTER TABLE `comment` ADD COLUMN `deleted_at` DATETIME NULL");
                }
                log.info("V22 마이그레이션 완료: comment.deleted_at 컬럼 추가됨");
            }

            // V23: notification 테이블에 updated_at 컬럼 추가 (BaseEntity 상속)
            if (!columnExists(stmt, "notification", "updated_at")) {
                log.info("V23 마이그레이션 실행 중: notification.updated_at 컬럼 추가...");
                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute("ALTER TABLE `notification` ADD COLUMN `updated_at` DATETIME NULL");
                }
                log.info("V23 마이그레이션 완료: notification.updated_at 컬럼 추가됨");
            }

            // V24: diary 테이블에 deleted_at 컬럼 추가 (SoftDeletableEntity 상속)
            if (!columnExists(stmt, "diary", "deleted_at")) {
                log.info("V24 마이그레이션 실행 중: diary.deleted_at 컬럼 추가...");
                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute("ALTER TABLE `diary` ADD COLUMN `deleted_at` DATETIME NULL");
                }
                log.info("V24 마이그레이션 완료: diary.deleted_at 컬럼 추가됨");
            }

            // V25: 인증글의 title이 NULL인 경우 미션 제목으로 업데이트
            log.info("V25 마이그레이션 실행 중: 인증글 title 업데이트...");
            try (Statement updateStmt = conn.createStatement()) {
                int updatedCount = updateStmt.executeUpdate(
                    "UPDATE post p " +
                    "INNER JOIN user_mission um ON p.user_mission_id = um.id " +
                    "INNER JOIN mission m ON um.mission_id = m.id " +
                    "SET p.title = m.title " +
                    "WHERE p.post_type = 'VERIFICATION' " +
                    "  AND (p.title IS NULL OR p.title = '')"
                );
                log.info("V25 마이그레이션 완료: {}개의 인증글 title 업데이트됨", updatedCount);
            } catch (Exception e) {
                log.warn("V25 마이그레이션 실행 중 오류 (무시 가능): {}", e.getMessage());
            }

            // V26: Mission 테이블에서 사용되지 않는 컬럼 제거
            log.info("V26 마이그레이션 실행 중: Mission 테이블 미사용 컬럼 제거...");
            executeV26Migration(conn);
            log.info("V26 마이그레이션 완료");

            // V27: 사용되지 않는 테이블 삭제
            log.info("V27 마이그레이션 실행 중: 사용되지 않는 테이블 삭제...");
            executeV27Migration(conn);
            log.info("V27 마이그레이션 완료");

            // V28: User 테이블에 돌발 미션 설정 관련 컬럼 추가
            log.info("V28 마이그레이션 실행 중: User 테이블 돌발 미션 설정 컬럼 추가...");
            executeV28Migration(conn);
            log.info("V28 마이그레이션 완료");

            // V29: UserMission 테이블에 돌발 미션 구분 컬럼 추가
            log.info("V29 마이그레이션 실행 중: UserMission 테이블 돌발 미션 구분 컬럼 추가...");
            executeV29Migration(conn);
            log.info("V29 마이그레이션 완료");

            // V30: todolist 테이블에 is_public 컬럼 추가
            log.info("V30 마이그레이션 실행 중: todolist 테이블 is_public 컬럼 추가...");
            executeV30Migration(conn);
            log.info("V30 마이그레이션 완료");

            // V31: post 테이블에 completion_rate 컬럼 추가
            log.info("V31 마이그레이션 실행 중: post 테이블 completion_rate 컬럼 추가...");
            executeV31Migration(conn);
            log.info("V31 마이그레이션 완료");

            // V32: todolist_review 테이블에서 content 컬럼 제거
            log.info("V32 마이그레이션 실행 중: todolist_review 테이블 content 컬럼 제거...");
            executeV32Migration(conn);
            log.info("V32 마이그레이션 완료");

            // V33: user 테이블에 spontaneous_mission_setup_at 컬럼 추가 (돌발 미션 설정 시점)
            log.info("V33 마이그레이션 실행 중: user 테이블 spontaneous_mission_setup_at 컬럼 추가...");
            executeV33Migration(conn);
            log.info("V33 마이그레이션 완료");

            // V34: 시간 형식 정규화 (H:mm → HH:mm)
            log.info("V34 마이그레이션 실행 중: 시간 형식 정규화...");
            executeV34Migration(conn);
            log.info("V34 마이그레이션 완료");

            // V35: 기상 미션 및 식사 미션 추가
            log.info("V35 마이그레이션 실행 중: 돌발 미션 시드 데이터 추가...");
            executeV35Migration(conn);
            log.info("V35 마이그레이션 완료");

            // V36: meal_log 테이블 생성 (식사 인증 전용)
            log.info("V36 마이그레이션 실행 중: meal_log 테이블 생성...");
            executeV36Migration(conn);
            log.info("V36 마이그레이션 완료");

            // V37: spontaneous_mission 테이블 생성 (돌발 미션 전용)
            log.info("V37 마이그레이션 실행 중: spontaneous_mission 테이블 생성...");
            executeV37Migration(conn);
            log.info("V37 마이그레이션 완료");

            // V38: user_mission의 is_spontaneous=true 데이터를 spontaneous_mission으로 마이그레이션
            log.info("V38 마이그레이션 실행 중: user_mission 돌발 미션 데이터 마이그레이션...");
            executeV38Migration(conn);
            log.info("V38 마이그레이션 완료");

            // V39: user_mission 테이블의 is_spontaneous 컬럼 제거
            log.info("V39 마이그레이션 실행 중: user_mission.is_spontaneous 컬럼 제거...");
            executeV39Migration(conn);
            log.info("V39 마이그레이션 완료");

            // V40: chat_log 테이블의 user_message 컬럼을 nullable로 변경 (선제 메시지 지원)
            log.info("V40 마이그레이션 실행 중: chat_log.user_message 컬럼 nullable 변경...");
            executeV40Migration(conn);
            log.info("V40 마이그레이션 완료");

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

    private boolean tableExists(Statement stmt, String tableName) {
        try {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "'"
            );
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            log.warn("테이블 존재 여부 확인 실패: {}", e.getMessage());
        }
        return false;
    }

    private boolean indexExists(Statement stmt, String tableName, String indexName) {
        try {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "' " +
                "AND INDEX_NAME = '" + indexName + "'"
            );
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            log.warn("인덱스 존재 여부 확인 실패: {}", e.getMessage());
        }
        return false;
    }

    private String getAlterTableSqlForColumn(String tableName, String columnName) {
        // 컬럼 타입 정의 (위치 지정 없이 추가 - 더 안전함)
        switch (columnName) {
            case "user_id":
                return "ALTER TABLE " + tableName + " ADD COLUMN user_id BIGINT NOT NULL";
            case "mission_type":
                return "ALTER TABLE " + tableName + " ADD COLUMN mission_type VARCHAR(20) NOT NULL";
            case "title":
                return "ALTER TABLE " + tableName + " ADD COLUMN title VARCHAR(100) NOT NULL DEFAULT '돌발 미션'";
            case "description":
                return "ALTER TABLE " + tableName + " ADD COLUMN description TEXT NOT NULL DEFAULT '돌발 미션입니다.'";
            case "assigned_at":
                return "ALTER TABLE " + tableName + " ADD COLUMN assigned_at DATETIME NOT NULL";
            case "deadline_at":
                return "ALTER TABLE " + tableName + " ADD COLUMN deadline_at DATETIME NOT NULL";
            case "deadline_minutes":
                return "ALTER TABLE " + tableName + " ADD COLUMN deadline_minutes INT NULL";
            case "status":
                return "ALTER TABLE " + tableName + " ADD COLUMN status VARCHAR(20) NOT NULL";
            case "verified_at":
                return "ALTER TABLE " + tableName + " ADD COLUMN verified_at DATETIME NULL";
            case "post_id":
                return "ALTER TABLE " + tableName + " ADD COLUMN post_id BIGINT NULL";
            case "meal_log_id":
                return "ALTER TABLE " + tableName + " ADD COLUMN meal_log_id BIGINT NULL";
            case "exp_reward":
                return "ALTER TABLE " + tableName + " ADD COLUMN exp_reward INT NOT NULL DEFAULT 10";
            case "created_at":
                return "ALTER TABLE " + tableName + " ADD COLUMN created_at DATETIME NOT NULL";
            default:
                return null;
        }
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
                "INSERT INTO `post` (`post_type`, `user_id`, `mission_id`, `title`, `content`, " +
                "`image_urls`, `has_valid_badge`, `del_flag`, `created_at`, `updated_at`) " +
                "SELECT 'GENERAL', `user_id`, `mission_id`, `title`, `content`, " +
                "`image_urls`, `has_valid_badge`, `del_flag`, `created_at`, `updated_at` " +
                "FROM `_backup_post`"
            );

            // 7. 백업 데이터 복원 (인증 게시글)
            executeIgnore(stmt,
                "INSERT INTO `post` (`post_type`, `user_id`, `mission_id`, `user_mission_id`, " +
                "`content`, `image_urls`, `status`, `approve_count`, `reject_count`, `verified_at`, `created_at`, `updated_at`) " +
                "SELECT 'VERIFICATION', vp.`user_id`, um.`mission_id`, vp.`user_mission_id`, " +
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

    private void executeV16Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V16: mission.difficulty_level 컬럼 크기 수정 (커스텀 미션 생성 시 Data truncated 오류 수정)
            executeIgnore(stmt, "ALTER TABLE `mission` MODIFY COLUMN `difficulty_level` VARCHAR(10)");
            log.info("V16 마이그레이션: mission.difficulty_level 컬럼 크기 수정 완료");
        }
    }

    private void executeV17Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V17: custom_mission.is_promoted 컬럼 기본값 설정
            executeIgnore(stmt, "ALTER TABLE `custom_mission` MODIFY COLUMN `is_promoted` BOOLEAN NOT NULL DEFAULT FALSE");
            log.info("V17 마이그레이션: custom_mission.is_promoted 기본값 설정 완료");
        }
    }

    private void executeV18Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V18: mission.mission_source DEFAULT 값 제거 및 creator_id 기반 CUSTOM 설정

            // 1. mission_source 컬럼에서 DEFAULT 값 제거 (애플리케이션에서 명시적으로 설정)
            executeIgnore(stmt, "ALTER TABLE `mission` MODIFY COLUMN `mission_source` VARCHAR(20) NOT NULL");

            // 2. creator_id가 있는 미션 중 mission_source가 OFFICIAL인 것을 CUSTOM으로 수정
            int updated = stmt.executeUpdate(
                "UPDATE `mission` SET `mission_source` = 'CUSTOM' " +
                "WHERE `creator_id` IS NOT NULL AND `mission_source` = 'OFFICIAL'"
            );
            if (updated > 0) {
                log.info("V18 마이그레이션: {} 개의 미션이 CUSTOM으로 변경됨", updated);
            }

            log.info("V18 마이그레이션: mission.mission_source DEFAULT 값 제거 완료");
        }
    }

    private void executeV21Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V21: mission_source를 mission_type으로 변경, GPS 컬럼 삭제, type 컬럼 삭제

            // 1. mission_source 컬럼을 mission_type으로 이름 변경
            executeIgnore(stmt,
                "ALTER TABLE `mission` CHANGE COLUMN `mission_source` `mission_type` VARCHAR(20) NOT NULL"
            );
            log.info("V21 마이그레이션: mission_source를 mission_type으로 변경 완료");

            // 2. GPS 관련 컬럼 삭제
            executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `gps_latitude`");
            executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `gps_longitude`");
            executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `gps_radius_meters`");
            log.info("V21 마이그레이션: GPS 관련 컬럼 삭제 완료");

            // 3. type 컬럼 삭제 (DAILY, WEEKLY 등 - 더 이상 사용하지 않음)
            executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `type`");
            log.info("V21 마이그레이션: type 컬럼 삭제 완료");

            // 4. 기존 인덱스 삭제 후 새 인덱스 생성
            executeIgnore(stmt, "ALTER TABLE `mission` DROP INDEX `idx_mission_source`");
            executeIgnore(stmt, "CREATE INDEX `idx_mission_type` ON `mission`(`mission_type`)");
            log.info("V21 마이그레이션: 인덱스 업데이트 완료");

            // 5. custom_mission 테이블에서도 GPS 컬럼 삭제 (존재하면)
            executeIgnore(stmt, "ALTER TABLE `custom_mission` DROP COLUMN `gps_latitude`");
            executeIgnore(stmt, "ALTER TABLE `custom_mission` DROP COLUMN `gps_longitude`");
            executeIgnore(stmt, "ALTER TABLE `custom_mission` DROP COLUMN `gps_radius_meters`");
            log.info("V21 마이그레이션: custom_mission GPS 관련 컬럼 삭제 완료");

            log.info("V21 마이그레이션: mission_source → mission_type 변경 및 GPS 컬럼 삭제 완료");
        }
    }

    private void executeIgnore(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (Exception e) {
            log.debug("SQL 실행 (무시): {} - {}", sql.substring(0, Math.min(50, sql.length())), e.getMessage());
        }
    }

    private void executeV25Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V25: todolist_mission 테이블에 시간대 배치 필드 추가
            executeIgnore(stmt, "ALTER TABLE `todolist_mission` ADD COLUMN `scheduled_start_time` TIME NULL");
            executeIgnore(stmt, "ALTER TABLE `todolist_mission` ADD COLUMN `scheduled_end_time` TIME NULL");
            log.info("V25 마이그레이션: todolist_mission 시간대 필드 추가 완료");
        }
    }

    private void executeV22Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V22: SoftDelete를 위한 deleted_at 컬럼 추가

            // 1. comment 테이블에 deleted_at, target_type, target_id 컬럼 추가
            executeIgnore(stmt, "ALTER TABLE `comment` ADD COLUMN `deleted_at` TIMESTAMP NULL");
            executeIgnore(stmt, "ALTER TABLE `comment` ADD COLUMN `target_type` VARCHAR(50) NULL");
            executeIgnore(stmt, "ALTER TABLE `comment` ADD COLUMN `target_id` BIGINT NULL");
            log.info("V22 마이그레이션: comment 테이블에 컬럼 추가 완료");

            // 2. mission_set 테이블 생성 (없으면)
            executeIgnore(stmt,
                "CREATE TABLE IF NOT EXISTS `mission_set` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "`creator_id` BIGINT NOT NULL," +
                "`title` VARCHAR(100) NOT NULL," +
                "`description` TEXT NULL," +
                "`is_public` BOOLEAN NOT NULL DEFAULT FALSE," +
                "`added_count` INT NOT NULL DEFAULT 0," +
                "`average_rating` DOUBLE NOT NULL DEFAULT 0," +
                "`review_count` INT NOT NULL DEFAULT 0," +
                "`is_active` BOOLEAN NOT NULL DEFAULT TRUE," +
                "`set_type` VARCHAR(20) NULL," +
                "`completed_count` INT NULL," +
                "`total_count` INT NULL," +
                "`todolist_status` VARCHAR(20) NULL," +
                "`deleted_at` TIMESTAMP NULL," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "`updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX `idx_mission_set_creator` (`creator_id`)," +
                "INDEX `idx_mission_set_is_public` (`is_public`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            // 3. 기존 mission_set 테이블에 deleted_at 컬럼 추가 (이미 테이블이 있을 경우)
            executeIgnore(stmt, "ALTER TABLE `mission_set` ADD COLUMN `deleted_at` TIMESTAMP NULL");
            log.info("V22 마이그레이션: mission_set 테이블 설정 완료");

            // 4. mission_set_mission 테이블 생성 (없으면)
            executeIgnore(stmt,
                "CREATE TABLE IF NOT EXISTS `mission_set_mission` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "`mission_set_id` BIGINT NOT NULL," +
                "`mission_id` BIGINT NULL," +
                "`display_order` INT NOT NULL DEFAULT 0," +
                "`is_completed` BOOLEAN NOT NULL DEFAULT FALSE," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "INDEX `idx_mission_set_mission_set` (`mission_set_id`)," +
                "INDEX `idx_mission_set_mission_mission` (`mission_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
            log.info("V22 마이그레이션: mission_set_mission 테이블 설정 완료");

            // 5. diary 테이블에 deleted_at 컬럼 추가
            executeIgnore(stmt, "ALTER TABLE `diary` ADD COLUMN `deleted_at` TIMESTAMP NULL");
            log.info("V22 마이그레이션: diary 테이블에 deleted_at 추가 완료");

            // 6. notification 테이블에 deleted_at 컬럼 추가
            executeIgnore(stmt, "ALTER TABLE `notification` ADD COLUMN `deleted_at` TIMESTAMP NULL");
            log.info("V22 마이그레이션: notification 테이블에 deleted_at 추가 완료");

            log.info("V22 마이그레이션: SoftDelete 컬럼 추가 완료");
        }
    }

    private void executeV26Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V26: Mission 테이블에서 사용되지 않는 컬럼 제거
            
            // 1. challenged 컬럼 삭제
            if (columnExists(stmt, "mission", "challenged")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `challenged`");
                log.info("V26 마이그레이션: challenged 컬럼 삭제 완료");
            }

            // 2. challenge_count 컬럼 삭제
            if (columnExists(stmt, "mission", "challenge_count")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `challenge_count`");
                log.info("V26 마이그레이션: challenge_count 컬럼 삭제 완료");
            }

            // 3. GPS 관련 컬럼 삭제 (혹시 남아있을 수 있음)
            if (columnExists(stmt, "mission", "gps_latitude")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `gps_latitude`");
                log.info("V26 마이그레이션: gps_latitude 컬럼 삭제 완료");
            }
            if (columnExists(stmt, "mission", "gps_longitude")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `gps_longitude`");
                log.info("V26 마이그레이션: gps_longitude 컬럼 삭제 완료");
            }
            if (columnExists(stmt, "mission", "gps_radius_meters")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `gps_radius_meters`");
                log.info("V26 마이그레이션: gps_radius_meters 컬럼 삭제 완료");
            }

            // 4. type 컬럼 삭제 (혹시 남아있을 수 있음)
            if (columnExists(stmt, "mission", "type")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `type`");
                log.info("V26 마이그레이션: type 컬럼 삭제 완료");
            }

            // 5. mission_source 컬럼 삭제 (mission_type으로 이미 변경됨)
            if (columnExists(stmt, "mission", "mission_source")) {
                executeIgnore(stmt, "ALTER TABLE `mission` DROP COLUMN `mission_source`");
                log.info("V26 마이그레이션: mission_source 컬럼 삭제 완료");
            }

            log.info("V26 마이그레이션: Mission 테이블 미사용 컬럼 제거 완료");
        }
    }

    private void executeV27Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V27: 사용되지 않는 테이블 삭제
            
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 먼저 삭제된 테이블을 참조하는 외래키 제거
            dropOrphanedForeignKeys(stmt);

            // 1. custom_mission 테이블 삭제 (이미 Mission 테이블로 통합됨)
            if (tableExists(stmt, "custom_mission")) {
                executeIgnore(stmt, "DROP TABLE IF EXISTS `custom_mission`");
                log.info("V27 마이그레이션: custom_mission 테이블 삭제 완료");
            }

            // 2. user_routine 테이블 삭제
            if (tableExists(stmt, "user_routine")) {
                executeIgnore(stmt, "DROP TABLE IF EXISTS `user_routine`");
                log.info("V27 마이그레이션: user_routine 테이블 삭제 완료");
            }

            // 3. wakeup_mission_setting 테이블 삭제 (대소문자 구분 없이 확인)
            if (tableExists(stmt, "wakeup_mission_setting") || tableExists(stmt, "wakeup_mission_Setting")) {
                executeIgnore(stmt, "DROP TABLE IF EXISTS `wakeup_mission_setting`");
                executeIgnore(stmt, "DROP TABLE IF EXISTS `wakeup_mission_Setting`");
                log.info("V27 마이그레이션: wakeup_mission_setting 테이블 삭제 완료");
            }

            // 4. user_recommendation 테이블 삭제
            if (tableExists(stmt, "user_recommendation")) {
                executeIgnore(stmt, "DROP TABLE IF EXISTS `user_recommendation`");
                log.info("V27 마이그레이션: user_recommendation 테이블 삭제 완료");
            }

            // 5. verification_post 테이블 삭제 (이미 Post 테이블로 통합됨)
            if (tableExists(stmt, "verification_post")) {
                executeIgnore(stmt, "DROP TABLE IF EXISTS `verification_post`");
                log.info("V27 마이그레이션: verification_post 테이블 삭제 완료");
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            // 6. post 테이블에서 custom_mission_id 컬럼 제거 (이미 Mission 테이블로 통합됨)
            if (columnExists(stmt, "post", "custom_mission_id")) {
                executeIgnore(stmt, "ALTER TABLE `post` DROP COLUMN `custom_mission_id`");
                log.info("V27 마이그레이션: post.custom_mission_id 컬럼 삭제 완료");
            }

            log.info("V27 마이그레이션: 사용되지 않는 테이블 삭제 완료");
        }
    }

    /**
     * 삭제된 테이블을 참조하는 고아 외래키 제거
     */
    private void dropOrphanedForeignKeys(Statement stmt) {
        try {
            // 삭제된 테이블 목록
            String[] deletedTables = {"verification_post", "custom_mission", "user_routine", 
                                     "wakeup_mission_setting", "wakeup_mission_Setting", "user_recommendation"};

            for (String deletedTable : deletedTables) {
                // 해당 테이블을 참조하는 외래키 찾기
                String sql = "SELECT DISTINCT CONSTRAINT_NAME, TABLE_NAME " +
                            "FROM information_schema.KEY_COLUMN_USAGE " +
                            "WHERE REFERENCED_TABLE_NAME = '" + deletedTable + "' " +
                            "AND TABLE_SCHEMA = DATABASE() " +
                            "AND CONSTRAINT_NAME IS NOT NULL";
                
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String constraintName = rs.getString("CONSTRAINT_NAME");
                    String tableName = rs.getString("TABLE_NAME");
                    
                    try {
                        stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
                        log.info("V27 마이그레이션: 고아 외래키 삭제 - {}.{}", tableName, constraintName);
                    } catch (Exception e) {
                        log.debug("외래키 삭제 실패 (이미 삭제되었을 수 있음): {}.{} - {}", 
                                tableName, constraintName, e.getMessage());
                    }
                }
            }

            // 알려진 문제 외래키 직접 삭제 시도
            String[] knownOrphanedKeys = {"FKk6jowxq7o9ysklnigijjbwa7u"};
            for (String fkName : knownOrphanedKeys) {
                // 어떤 테이블에 있는지 찾기
                String findFkSql = "SELECT DISTINCT TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                                  "WHERE CONSTRAINT_NAME = '" + fkName + "' " +
                                  "AND TABLE_SCHEMA = DATABASE() LIMIT 1";
                try {
                    ResultSet fkRs = stmt.executeQuery(findFkSql);
                    if (fkRs.next()) {
                        String tableName = fkRs.getString("TABLE_NAME");
                        executeIgnore(stmt, "ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + fkName + "`");
                        log.info("V27 마이그레이션: 알려진 고아 외래키 삭제 - {}.{}", tableName, fkName);
                    }
                } catch (Exception e) {
                    log.debug("외래키 {} 삭제 시도 실패 (이미 삭제되었을 수 있음): {}", fkName, e.getMessage());
                }
            }

            // 존재하지 않는 테이블을 참조하는 모든 외래키 찾아서 삭제
            String findOrphanedFkSql = "SELECT DISTINCT kcu.CONSTRAINT_NAME, kcu.TABLE_NAME " +
                                      "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                                      "LEFT JOIN information_schema.TABLES t " +
                                      "ON kcu.REFERENCED_TABLE_NAME = t.TABLE_NAME " +
                                      "AND t.TABLE_SCHEMA = DATABASE() " +
                                      "WHERE kcu.TABLE_SCHEMA = DATABASE() " +
                                      "AND kcu.REFERENCED_TABLE_NAME IS NOT NULL " +
                                      "AND t.TABLE_NAME IS NULL " +
                                      "AND kcu.CONSTRAINT_NAME IS NOT NULL";
            try {
                ResultSet orphanedRs = stmt.executeQuery(findOrphanedFkSql);
                while (orphanedRs.next()) {
                    String constraintName = orphanedRs.getString("CONSTRAINT_NAME");
                    String tableName = orphanedRs.getString("TABLE_NAME");
                    try {
                        executeIgnore(stmt, "ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
                        log.info("V27 마이그레이션: 고아 외래키 자동 삭제 - {}.{}", tableName, constraintName);
                    } catch (Exception e) {
                        log.debug("외래키 삭제 실패: {}.{} - {}", tableName, constraintName, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("고아 외래키 자동 검색 실패: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("고아 외래키 정리 중 오류 (무시 가능): {}", e.getMessage());
        }
    }

    private void executeV28Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V28: User 테이블에 돌발 미션 설정 관련 컬럼 추가

            // 1. is_spontaneous_mission_setup_completed 컬럼 처리
            boolean hasOldColumn = columnExists(stmt, "user", "is_onboarding_completed");
            boolean hasNewColumn = columnExists(stmt, "user", "is_spontaneous_mission_setup_completed");
            
            if (hasOldColumn && !hasNewColumn) {
                // 기존 컬럼을 새 이름으로 변경
                executeIgnore(stmt,
                    "ALTER TABLE `user` CHANGE COLUMN `is_onboarding_completed` `is_spontaneous_mission_setup_completed` BOOLEAN NOT NULL DEFAULT FALSE"
                );
                log.info("V28 마이그레이션: is_onboarding_completed를 is_spontaneous_mission_setup_completed로 변경 완료");
            } else if (hasOldColumn && hasNewColumn) {
                // 두 컬럼이 모두 존재하면 기존 컬럼 삭제
                executeIgnore(stmt,
                    "ALTER TABLE `user` DROP COLUMN `is_onboarding_completed`"
                );
                log.info("V28 마이그레이션: is_onboarding_completed 컬럼 삭제 완료 (중복 컬럼 정리)");
            } else if (!hasNewColumn) {
                // 새 컬럼이 없으면 추가
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `is_spontaneous_mission_setup_completed` BOOLEAN NOT NULL DEFAULT FALSE"
                );
                log.info("V28 마이그레이션: is_spontaneous_mission_setup_completed 컬럼 추가 완료");
            }

            // 2. sleep_time 컬럼 추가
            if (!columnExists(stmt, "user", "sleep_time")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `sleep_time` VARCHAR(5) NULL"
                );
                log.info("V28 마이그레이션: sleep_time 컬럼 추가 완료");
            }

            // 3. wake_time 컬럼 추가
            if (!columnExists(stmt, "user", "wake_time")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `wake_time` VARCHAR(5) NULL"
                );
                log.info("V28 마이그레이션: wake_time 컬럼 추가 완료");
            }

            // 4. 식사 시간 컬럼 추가
            if (!columnExists(stmt, "user", "breakfast_time")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `breakfast_time` VARCHAR(5) NULL"
                );
                log.info("V28 마이그레이션: breakfast_time 컬럼 추가 완료");
            }

            if (!columnExists(stmt, "user", "lunch_time")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `lunch_time` VARCHAR(5) NULL"
                );
                log.info("V28 마이그레이션: lunch_time 컬럼 추가 완료");
            }

            if (!columnExists(stmt, "user", "dinner_time")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `dinner_time` VARCHAR(5) NULL"
                );
                log.info("V28 마이그레이션: dinner_time 컬럼 추가 완료");
            }

            log.info("V28 마이그레이션: User 테이블 돌발 미션 설정 컬럼 추가 완료");
        }
    }

    private void executeV29Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V29: UserMission 테이블에 돌발 미션 구분 컬럼 추가

            if (!columnExists(stmt, "user_mission", "is_spontaneous")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user_mission` ADD COLUMN `is_spontaneous` BOOLEAN NOT NULL DEFAULT FALSE"
                );
                log.info("V29 마이그레이션: user_mission.is_spontaneous 컬럼 추가 완료");
            } else {
                // 컬럼이 이미 존재하는 경우 기본값이 없을 수 있으므로 기본값 추가
                executeIgnore(stmt,
                    "ALTER TABLE `user_mission` MODIFY COLUMN `is_spontaneous` BOOLEAN NOT NULL DEFAULT FALSE"
                );
                log.info("V29 마이그레이션: user_mission.is_spontaneous 컬럼 기본값 설정 완료");
            }

            log.info("V29 마이그레이션: UserMission 테이블 돌발 미션 구분 컬럼 추가 완료");
        }
    }

    private void executeV30Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V30: todolist 테이블에 is_public 컬럼 추가

            if (!columnExists(stmt, "todolist", "is_public")) {
                executeIgnore(stmt,
                    "ALTER TABLE `todolist` ADD COLUMN `is_public` BOOLEAN NOT NULL DEFAULT FALSE"
                );
                log.info("V30 마이그레이션: todolist.is_public 컬럼 추가 완료");
            } else {
                log.info("V30 마이그레이션: todolist.is_public 컬럼이 이미 존재함");
            }

            log.info("V30 마이그레이션: todolist 테이블 is_public 컬럼 추가 완료");
        }
    }

    private void executeV31Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V31: post 테이블에 completion_rate 컬럼 추가

            if (!columnExists(stmt, "post", "completion_rate")) {
                executeIgnore(stmt,
                    "ALTER TABLE `post` ADD COLUMN `completion_rate` INT NULL COMMENT '완료 정도 (0-100, VERIFICATION 타입만 사용)'"
                );
                log.info("V31 마이그레이션: post.completion_rate 컬럼 추가 완료");
            } else {
                log.info("V31 마이그레이션: post.completion_rate 컬럼이 이미 존재함");
            }

            log.info("V31 마이그레이션: post 테이블 completion_rate 컬럼 추가 완료");
        }
    }

    private void executeV32Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V32: todolist_review 테이블에서 content 컬럼 제거

            if (columnExists(stmt, "todolist_review", "content")) {
                // 기존 리뷰의 content를 NULL로 업데이트 (안전을 위해)
                executeIgnore(stmt,
                    "UPDATE todolist_review SET content = NULL WHERE content IS NOT NULL"
                );
                log.info("V32 마이그레이션: 기존 리뷰의 content를 NULL로 업데이트 완료");

                // content 컬럼 제거
                executeIgnore(stmt,
                    "ALTER TABLE todolist_review DROP COLUMN content"
                );
                log.info("V32 마이그레이션: todolist_review.content 컬럼 제거 완료");
            } else {
                log.info("V32 마이그레이션: todolist_review.content 컬럼이 이미 존재하지 않음");
            }

            log.info("V32 마이그레이션: todolist_review 테이블 content 컬럼 제거 완료");
        }
    }

    private void executeV33Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V33: user 테이블에 spontaneous_mission_setup_at 컬럼 추가 (돌발 미션 설정 시점)
            // 기존에 updatedAt을 사용하던 악용 방지 로직을 전용 필드로 분리

            if (!columnExists(stmt, "user", "spontaneous_mission_setup_at")) {
                executeIgnore(stmt,
                    "ALTER TABLE `user` ADD COLUMN `spontaneous_mission_setup_at` DATETIME NULL " +
                    "COMMENT '돌발 미션 설정 시점 (악용 방지용 - 설정 당일에는 미션 할당 안 함)'"
                );
                log.info("V33 마이그레이션: user.spontaneous_mission_setup_at 컬럼 추가 완료");
                
                // 기존에 돌발 미션 설정을 완료한 사용자의 경우, 설정 시점을 어제로 설정 (미션 할당 가능하도록)
                executeIgnore(stmt,
                    "UPDATE `user` SET `spontaneous_mission_setup_at` = DATE_SUB(NOW(), INTERVAL 1 DAY) " +
                    "WHERE `is_spontaneous_mission_setup_completed` = TRUE AND `spontaneous_mission_setup_at` IS NULL"
                );
                log.info("V33 마이그레이션: 기존 설정 완료 사용자의 spontaneous_mission_setup_at을 어제로 초기화 완료");
            } else {
                log.info("V33 마이그레이션: user.spontaneous_mission_setup_at 컬럼이 이미 존재함");
            }

            log.info("V33 마이그레이션: user 테이블 spontaneous_mission_setup_at 컬럼 추가 완료");
        }
    }

    private void executeV34Migration(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            // V34: 시간 형식 정규화 (H:mm → HH:mm)
            // DB에 "7:00", "0:42" 같이 1자리 시간으로 저장된 데이터를 "07:00", "00:42"로 정규화
            
            log.info("V34 마이그레이션: 시간 형식 정규화 시작 (H:mm → HH:mm)");
            
            // wake_time 정규화: "7:00" → "07:00"
            executeIgnore(stmt,
                "UPDATE `user` SET `wake_time` = LPAD(`wake_time`, 5, '0') " +
                "WHERE `wake_time` IS NOT NULL AND LENGTH(`wake_time`) = 4"
            );
            log.info("V34 마이그레이션: wake_time 정규화 완료");
            
            // sleep_time 정규화
            executeIgnore(stmt,
                "UPDATE `user` SET `sleep_time` = LPAD(`sleep_time`, 5, '0') " +
                "WHERE `sleep_time` IS NOT NULL AND LENGTH(`sleep_time`) = 4"
            );
            log.info("V34 마이그레이션: sleep_time 정규화 완료");
            
            // breakfast_time 정규화
            executeIgnore(stmt,
                "UPDATE `user` SET `breakfast_time` = LPAD(`breakfast_time`, 5, '0') " +
                "WHERE `breakfast_time` IS NOT NULL AND LENGTH(`breakfast_time`) = 4"
            );
            log.info("V34 마이그레이션: breakfast_time 정규화 완료");
            
            // lunch_time 정규화
            executeIgnore(stmt,
                "UPDATE `user` SET `lunch_time` = LPAD(`lunch_time`, 5, '0') " +
                "WHERE `lunch_time` IS NOT NULL AND LENGTH(`lunch_time`) = 4"
            );
            log.info("V34 마이그레이션: lunch_time 정규화 완료");
            
            // dinner_time 정규화
            executeIgnore(stmt,
                "UPDATE `user` SET `dinner_time` = LPAD(`dinner_time`, 5, '0') " +
                "WHERE `dinner_time` IS NOT NULL AND LENGTH(`dinner_time`) = 4"
            );
            log.info("V34 마이그레이션: dinner_time 정규화 완료");
            
            log.info("V34 마이그레이션: 시간 형식 정규화 완료");
        }
    }
    
    /**
     * V35 마이그레이션: 돌발 미션 시드 데이터 추가 (기상, 아침, 점심, 저녁 식사 미션)
     */
    private void executeV35Migration(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 기상 미션 추가 (없으면 INSERT)
            executeIgnore(stmt,
                "INSERT INTO `mission` (`title`, `description`, `category`, `mission_type`, " +
                "`is_active`, `difficulty_level`, `exp_reward`, `verification_type`, `created_at`) " +
                "SELECT '기상 미션', '일어나서 10분 안에 인증 버튼을 눌러주세요!', 'DAILY_LIFE', 'OFFICIAL', " +
                "true, 'EASY', 10, 'BUTTON', NOW() " +
                "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `mission` WHERE `title` = '기상 미션')"
            );
            log.info("V35 마이그레이션: 기상 미션 추가 완료");
            
            // 아침 식사 미션 추가
            executeIgnore(stmt,
                "INSERT INTO `mission` (`title`, `description`, `category`, `mission_type`, " +
                "`is_active`, `difficulty_level`, `exp_reward`, `verification_type`, `created_at`) " +
                "SELECT '아침 식사 인증', '오늘의 아침 식사를 사진과 함께 공유해주세요!', 'DAILY_LIFE', 'OFFICIAL', " +
                "true, 'EASY', 15, 'POST', NOW() " +
                "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `mission` WHERE `title` = '아침 식사 인증')"
            );
            log.info("V35 마이그레이션: 아침 식사 미션 추가 완료");
            
            // 점심 식사 미션 추가
            executeIgnore(stmt,
                "INSERT INTO `mission` (`title`, `description`, `category`, `mission_type`, " +
                "`is_active`, `difficulty_level`, `exp_reward`, `verification_type`, `created_at`) " +
                "SELECT '점심 식사 인증', '오늘의 점심 식사를 사진과 함께 공유해주세요!', 'DAILY_LIFE', 'OFFICIAL', " +
                "true, 'EASY', 15, 'POST', NOW() " +
                "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `mission` WHERE `title` = '점심 식사 인증')"
            );
            log.info("V35 마이그레이션: 점심 식사 미션 추가 완료");
            
            // 저녁 식사 미션 추가
            executeIgnore(stmt,
                "INSERT INTO `mission` (`title`, `description`, `category`, `mission_type`, " +
                "`is_active`, `difficulty_level`, `exp_reward`, `verification_type`, `created_at`) " +
                "SELECT '저녁 식사 인증', '오늘의 저녁 식사를 사진과 함께 공유해주세요!', 'DAILY_LIFE', 'OFFICIAL', " +
                "true, 'EASY', 15, 'POST', NOW() " +
                "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `mission` WHERE `title` = '저녁 식사 인증')"
            );
            log.info("V35 마이그레이션: 저녁 식사 미션 추가 완료");
            
            log.info("V35 마이그레이션: 돌발 미션 시드 데이터 추가 완료");
        }
    }

    /**
     * V36 마이그레이션: meal_log 테이블 생성 (식사 인증 전용 테이블)
     */
    private void executeV36Migration(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // meal_log 테이블이 없으면 생성
            if (!tableExists(stmt, "meal_log")) {
                executeIgnore(stmt,
                    "CREATE TABLE `meal_log` (" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "  `user_id` BIGINT NOT NULL," +
                    "  `meal_type` VARCHAR(20) NOT NULL," +
                    "  `meal_date` DATE NOT NULL," +
                    "  `title` VARCHAR(255)," +
                    "  `description` TEXT," +
                    "  `rating` INT," +
                    "  `post_id` BIGINT," +
                    "  `status` VARCHAR(20) NOT NULL," +
                    "  `assigned_at` DATETIME," +
                    "  `verified_at` DATETIME," +
                    "  `deadline_at` DATETIME," +
                    "  `exp_reward` INT DEFAULT 15," +
                    "  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "  PRIMARY KEY (`id`)," +
                    "  UNIQUE KEY `uk_user_meal_date` (`user_id`, `meal_type`, `meal_date`)," +
                    "  INDEX `idx_meal_log_user_date` (`user_id`, `meal_date`)," +
                    "  INDEX `idx_meal_log_status` (`status`)," +
                    "  CONSTRAINT `fk_meal_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE," +
                    "  CONSTRAINT `fk_meal_log_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE SET NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
                );
                log.info("V36 마이그레이션: meal_log 테이블 생성 완료");
            } else {
                log.info("V36 마이그레이션: meal_log 테이블이 이미 존재합니다.");
            }
        }
    }

    /**
     * V37 마이그레이션: spontaneous_mission 테이블 생성 (돌발 미션 전용)
     */
    private void executeV37Migration(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 테이블 존재 여부 확인
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'spontaneous_mission'"
            );
            boolean tableExists = rs.next() && rs.getInt(1) > 0;

            if (!tableExists) {
                // spontaneous_mission 테이블 생성
                stmt.execute(
                    "CREATE TABLE `spontaneous_mission` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                    "`user_id` BIGINT NOT NULL, " +
                    "`mission_type` VARCHAR(20) NOT NULL, " +
                    "`title` VARCHAR(100) NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`assigned_at` DATETIME NOT NULL, " +
                    "`deadline_at` DATETIME NOT NULL, " +
                    "`status` VARCHAR(20) NOT NULL, " +
                    "`verified_at` DATETIME NULL, " +
                    "`meal_log_id` BIGINT NULL, " +
                    "`post_id` BIGINT NULL, " +
                    "`exp_reward` INT NOT NULL DEFAULT 10, " +
                    "`created_at` DATETIME NOT NULL, " +
                    "PRIMARY KEY (`id`), " +
                    "INDEX `idx_spontaneous_user_date` (`user_id`, `assigned_at`), " +
                    "INDEX `idx_spontaneous_status` (`status`), " +
                    "INDEX `idx_spontaneous_type` (`mission_type`), " +
                    "FOREIGN KEY (`user_id`) REFERENCES `user`(`id`), " +
                    "FOREIGN KEY (`meal_log_id`) REFERENCES `meal_log`(`id`), " +
                    "FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
                );
                log.info("V37 마이그레이션: spontaneous_mission 테이블 생성 완료");
            } else {
                log.info("V37 마이그레이션: spontaneous_mission 테이블이 이미 존재합니다.");
                
                // title과 description 컬럼이 없으면 추가
                if (!columnExists(stmt, "spontaneous_mission", "title")) {
                    try {
                        stmt.execute("ALTER TABLE `spontaneous_mission` ADD COLUMN `title` VARCHAR(100) NOT NULL DEFAULT '돌발 미션'");
                        log.info("V37 마이그레이션: title 컬럼 추가 완료");
                    } catch (SQLException e) {
                        log.warn("V37 마이그레이션: title 컬럼 추가 실패: {}", e.getMessage());
                    }
                }
                
                if (!columnExists(stmt, "spontaneous_mission", "description")) {
                    try {
                        stmt.execute("ALTER TABLE `spontaneous_mission` ADD COLUMN `description` TEXT NOT NULL DEFAULT '돌발 미션입니다.'");
                        log.info("V37 마이그레이션: description 컬럼 추가 완료");
                    } catch (SQLException e) {
                        log.warn("V37 마이그레이션: description 컬럼 추가 실패: {}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * V38 마이그레이션: user_mission의 is_spontaneous=true 데이터를 spontaneous_mission으로 마이그레이션
     */
    private void executeV38Migration(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // spontaneous_mission 테이블이 존재하는지 확인
            boolean tableExists = tableExists(stmt, "spontaneous_mission");
            
            if (!tableExists) {
                log.info("V38 마이그레이션: spontaneous_mission 테이블이 없습니다. V37 마이그레이션을 먼저 실행해야 합니다.");
                return;
            }
            
            // 필요한 컬럼들이 있는지 확인하고 없으면 추가
            String[] requiredColumns = {
                "user_id", "mission_type", "title", "description", "assigned_at", "deadline_at", "deadline_minutes", "status", 
                "verified_at", "post_id", "meal_log_id", "exp_reward", "created_at"
            };
            
            for (String columnName : requiredColumns) {
                if (!columnExists(stmt, "spontaneous_mission", columnName)) {
                    log.info("V38 마이그레이션: spontaneous_mission 테이블에 {} 컬럼이 없습니다. 컬럼 추가 중...", columnName);
                    try {
                        String alterSql = getAlterTableSqlForColumn("spontaneous_mission", columnName);
                        if (alterSql != null) {
                            stmt.execute(alterSql);
                            log.info("V38 마이그레이션: {} 컬럼 추가 완료", columnName);
                        }
                    } catch (SQLException e) {
                        log.warn("V38 마이그레이션: {} 컬럼 추가 실패: {}", columnName, e.getMessage());
                    }
                }
            }
            
            // 인덱스 추가 (user_id가 있는 경우에만)
            if (columnExists(stmt, "spontaneous_mission", "user_id") && 
                columnExists(stmt, "spontaneous_mission", "assigned_at")) {
                try {
                    // 인덱스가 이미 존재하는지 확인
                    if (!indexExists(stmt, "spontaneous_mission", "idx_spontaneous_user_date")) {
                        stmt.execute("CREATE INDEX idx_spontaneous_user_date ON spontaneous_mission (user_id, assigned_at)");
                        log.info("V38 마이그레이션: 인덱스 추가 완료");
                    }
                } catch (SQLException e) {
                    log.warn("V38 마이그레이션: 인덱스 추가 실패: {}", e.getMessage());
                }
            }
            
            // 이미 마이그레이션되었는지 확인 (spontaneous_mission에 데이터가 있고 user_mission에 is_spontaneous=true가 없으면 완료)
            ResultSet checkRs = stmt.executeQuery(
                "SELECT COUNT(*) FROM user_mission WHERE is_spontaneous = TRUE"
            );
            int remainingCount = checkRs.next() ? checkRs.getInt(1) : 0;
            
            if (remainingCount == 0) {
                log.info("V38 마이그레이션: 마이그레이션할 데이터가 없습니다. (이미 완료됨)");
                return;
            }
            
            log.info("V38 마이그레이션: {}개의 돌발 미션 데이터 마이그레이션 시작", remainingCount);
            
            // 먼저 잘못된 데이터 정리: user 테이블에 존재하지 않는 user_id를 가진 spontaneous_mission 삭제
            String preCleanupSql = 
                "DELETE FROM spontaneous_mission sm " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM `user` u WHERE u.id = sm.user_id" +
                ")";
            int preDeletedCount = stmt.executeUpdate(preCleanupSql);
            if (preDeletedCount > 0) {
                log.warn("V38 마이그레이션: {}개의 잘못된 spontaneous_mission 데이터 사전 삭제 (존재하지 않는 user_id)", preDeletedCount);
            }
            
            // user_mission의 is_spontaneous=true 데이터를 spontaneous_mission으로 마이그레이션
            // 미션 제목을 기반으로 SpontaneousMissionType 결정
            String migrationSql = 
                "INSERT INTO spontaneous_mission (" +
                "  user_id, mission_type, title, description, assigned_at, deadline_at, deadline_minutes, status, " +
                "  verified_at, post_id, meal_log_id, exp_reward, created_at" +
                ") " +
                "SELECT " +
                "  um.user_id, " +
                "  CASE " +
                "    WHEN m.title LIKE '%기상%' OR m.title LIKE '%일어나%' THEN 'WAKE_UP' " +
                "    WHEN m.title LIKE '%아침%식사%' OR m.title LIKE '%아침%밥%' THEN 'MEAL_BREAKFAST' " +
                "    WHEN m.title LIKE '%점심%식사%' OR m.title LIKE '%점심%밥%' THEN 'MEAL_LUNCH' " +
                "    WHEN m.title LIKE '%저녁%식사%' OR m.title LIKE '%저녁%밥%' THEN 'MEAL_DINNER' " +
                "    WHEN m.title LIKE '%일기%' OR m.title LIKE '%감성%' THEN 'DIARY' " +
                "    ELSE 'WAKE_UP' " +  // 기본값
                "  END AS mission_type, " +
                "  COALESCE(m.title, '돌발 미션') AS title, " +
                "  COALESCE(m.description, '돌발 미션입니다.') AS description, " +
                "  um.assigned_at, " +
                "  CASE " +
                "    WHEN m.title LIKE '%기상%' OR m.title LIKE '%일어나%' THEN DATE_ADD(um.assigned_at, INTERVAL 10 MINUTE) " +
                "    WHEN m.title LIKE '%일기%' OR m.title LIKE '%감성%' THEN DATE_ADD(um.assigned_at, INTERVAL 1440 MINUTE) " +
                "    ELSE DATE_ADD(um.assigned_at, INTERVAL 120 MINUTE) " +  // 식사 미션: 2시간
                "  END AS deadline_at, " +
                "  CASE " +
                "    WHEN m.title LIKE '%기상%' OR m.title LIKE '%일어나%' THEN 10 " +
                "    WHEN m.title LIKE '%일기%' OR m.title LIKE '%감성%' THEN 1440 " +
                "    ELSE 120 " +  // 식사 미션: 120분
                "  END AS deadline_minutes, " +
                "  CASE " +
                "    WHEN um.status = 'ASSIGNED' THEN 'ASSIGNED' " +
                "    WHEN um.status = 'COMPLETED' THEN 'COMPLETED' " +
                "    WHEN um.status = 'FAILED' OR um.status = 'EXPIRED' THEN 'FAILED' " +
                "    ELSE 'ASSIGNED' " +
                "  END AS status, " +
                "  CASE " +
                "    WHEN um.status = 'COMPLETED' THEN COALESCE(mv.verified_at, um.assigned_at) " +
                "    ELSE NULL " +
                "  END AS verified_at, " +
                "  p.id AS post_id, " +
                "  NULL AS meal_log_id, " +  // meal_log 연결은 나중에 수동으로 처리 필요
                "  10 AS exp_reward, " +
                "  um.created_at " +
                "FROM user_mission um " +
                "INNER JOIN mission m ON um.mission_id = m.id " +
                "INNER JOIN `user` u ON u.id = um.user_id " +  // 유효한 user_id만 포함
                "LEFT JOIN mission_verification mv ON mv.user_mission_id = um.id " +
                "LEFT JOIN post p ON p.user_mission_id = um.id " +
                "WHERE um.is_spontaneous = TRUE " +
                "AND (u.del_flag = FALSE OR u.del_flag IS NULL) " +  // 삭제되지 않은 사용자만
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM spontaneous_mission sm " +
                "  WHERE sm.user_id = um.user_id " +
                "  AND DATE(sm.assigned_at) = DATE(um.assigned_at) " +
                "  AND sm.mission_type = CASE " +
                "    WHEN m.title LIKE '%기상%' OR m.title LIKE '%일어나%' THEN 'WAKE_UP' " +
                "    WHEN m.title LIKE '%아침%식사%' OR m.title LIKE '%아침%밥%' THEN 'MEAL_BREAKFAST' " +
                "    WHEN m.title LIKE '%점심%식사%' OR m.title LIKE '%점심%밥%' THEN 'MEAL_LUNCH' " +
                "    WHEN m.title LIKE '%저녁%식사%' OR m.title LIKE '%저녁%밥%' THEN 'MEAL_DINNER' " +
                "    WHEN m.title LIKE '%일기%' OR m.title LIKE '%감성%' THEN 'DIARY' " +
                "    ELSE 'WAKE_UP' " +
                "  END" +
                ")";
            
            int migratedCount = stmt.executeUpdate(migrationSql);
            log.info("V38 마이그레이션: {}개의 돌발 미션 데이터 마이그레이션 완료", migratedCount);
            
            // 마이그레이션된 데이터의 meal_log 연결 처리 (식사 미션의 경우)
            // meal_log와 user_mission의 연결은 복잡하므로, 식사 미션의 경우 meal_log를 찾아서 연결
            String updateMealLogSql = 
                "UPDATE spontaneous_mission sm " +
                "INNER JOIN user_mission um ON sm.user_id = um.user_id " +
                "  AND DATE(sm.assigned_at) = DATE(um.assigned_at) " +
                "INNER JOIN mission m ON um.mission_id = m.id " +
                "LEFT JOIN meal_log ml ON ml.user_id = um.user_id " +
                "  AND ml.meal_date = DATE(um.assigned_at) " +
                "  AND (" +
                "    (sm.mission_type = 'MEAL_BREAKFAST' AND ml.meal_type = 'BREAKFAST') OR " +
                "    (sm.mission_type = 'MEAL_LUNCH' AND ml.meal_type = 'LUNCH') OR " +
                "    (sm.mission_type = 'MEAL_DINNER' AND ml.meal_type = 'DINNER') " +
                "  ) " +
                "SET sm.meal_log_id = ml.id " +
                "WHERE sm.mission_type IN ('MEAL_BREAKFAST', 'MEAL_LUNCH', 'MEAL_DINNER') " +
                "AND sm.meal_log_id IS NULL " +
                "AND ml.id IS NOT NULL";
            
            int mealLogUpdated = stmt.executeUpdate(updateMealLogSql);
            if (mealLogUpdated > 0) {
                log.info("V38 마이그레이션: {}개의 식사 미션에 meal_log 연결 완료", mealLogUpdated);
            }
            
            // 잘못된 데이터 정리: user 테이블에 존재하지 않는 user_id를 가진 spontaneous_mission 삭제
            String cleanupSql = 
                "DELETE FROM spontaneous_mission sm " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM `user` u WHERE u.id = sm.user_id" +
                ")";
            int deletedCount = stmt.executeUpdate(cleanupSql);
            if (deletedCount > 0) {
                log.warn("V38 마이그레이션: {}개의 잘못된 spontaneous_mission 데이터 삭제 (존재하지 않는 user_id)", deletedCount);
            }
            
        } catch (Exception e) {
            log.error("V38 마이그레이션 실행 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * V39 마이그레이션: user_mission 테이블의 is_spontaneous 컬럼 제거
     */
    private void executeV39Migration(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 컬럼 존재 여부 확인
            if (columnExists(stmt, "user_mission", "is_spontaneous")) {
                // 외래키 제약조건이 있을 수 있으므로 안전하게 제거
                try {
                    stmt.execute("ALTER TABLE `user_mission` DROP COLUMN `is_spontaneous`");
                    log.info("V39 마이그레이션: user_mission.is_spontaneous 컬럼 제거 완료");
                } catch (SQLException e) {
                    // 컬럼이 다른 곳에서 참조되고 있을 수 있으므로 경고만 출력
                    log.warn("V39 마이그레이션: user_mission.is_spontaneous 컬럼 제거 실패 (이미 제거되었거나 참조 중): {}", e.getMessage());
                }
            } else {
                log.info("V39 마이그레이션: user_mission.is_spontaneous 컬럼이 이미 존재하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("V39 마이그레이션 실행 중 오류 발생: {}", e.getMessage(), e);
            // 컬럼 제거 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    private void executeV40Migration(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // chat_log 테이블의 user_message 컬럼을 nullable로 변경
            // 선제 메시지(proactive message)는 사용자 메시지가 없으므로 null이어야 함
            try {
                stmt.execute("ALTER TABLE `chat_log` MODIFY COLUMN `user_message` TEXT NULL");
                log.info("V40 마이그레이션: chat_log.user_message 컬럼 nullable 변경 완료");
            } catch (SQLException e) {
                // 이미 nullable이거나 다른 문제가 있을 수 있음
                log.warn("V40 마이그레이션: chat_log.user_message 컬럼 nullable 변경 실패: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("V40 마이그레이션 실행 중 오류 발생: {}", e.getMessage(), e);
            // 컬럼 변경 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
}
