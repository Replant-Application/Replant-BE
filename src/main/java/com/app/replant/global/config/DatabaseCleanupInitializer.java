package com.app.replant.global.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 데이터베이스 초기화 (EntityManagerFactory 생성 전 실행)
 * Hibernate가 스키마를 검증하기 전에 고아 외래키를 제거합니다.
 */
@Slf4j
@Component("databaseCleanupInitializer")
@Order(1) // 다른 Bean보다 먼저 실행
@RequiredArgsConstructor
public class DatabaseCleanupInitializer implements InitializingBean {

    @Qualifier("secondaryDataSource")
    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        cleanupOrphanedForeignKeys();
    }

    /** S2077: SQL 식별자로 사용 전 화이트리스트 검증 (동적 SQL 안전 처리) */
    private static boolean isSafeIdentifier(String s) {
        return s != null && !s.isEmpty() && s.matches("^[a-zA-Z0-9_]+$");
    }

    @PostConstruct
    public void cleanupOrphanedForeignKeys() {
        log.info("=== 데이터베이스 정리 시작 (고아 외래키 제거) ===");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 외래키 체크 비활성화
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 삭제된 테이블 목록
            String[] deletedTables = {"verification_post", "custom_mission", "user_routine", 
                                     "wakeup_mission_setting", "wakeup_mission_Setting", "user_recommendation"};

            String selectFkSql = "SELECT DISTINCT CONSTRAINT_NAME, TABLE_NAME " +
                    "FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE REFERENCED_TABLE_NAME = ? AND TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME IS NOT NULL";
            for (String deletedTable : deletedTables) {
                try (PreparedStatement ps = conn.prepareStatement(selectFkSql)) {
                    ps.setString(1, deletedTable);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String constraintName = rs.getString("CONSTRAINT_NAME");
                            String tableName = rs.getString("TABLE_NAME");
                            if (!isSafeIdentifier(tableName) || !isSafeIdentifier(constraintName)) continue;
                            try {
                                stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
                                log.info("고아 외래키 삭제: {}.{}", tableName, constraintName);
                            } catch (Exception e) {
                                log.debug("외래키 삭제 실패 (이미 삭제되었을 수 있음): {}.{} - {}", 
                                        tableName, constraintName, e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("외래키 검색 실패: {} - {}", deletedTable, e.getMessage());
                }
            }

            // 알려진 문제 외래키 직접 삭제 시도
            String[] knownOrphanedKeys = {"FKk6jowxq7o9ysklnigijjbwa7u"};
            String findFkSql = "SELECT DISTINCT TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE CONSTRAINT_NAME = ? AND TABLE_SCHEMA = DATABASE() LIMIT 1";
            for (String fkName : knownOrphanedKeys) {
                if (!isSafeIdentifier(fkName)) continue;
                try (PreparedStatement ps = conn.prepareStatement(findFkSql)) {
                    ps.setString(1, fkName);
                    try (ResultSet fkRs = ps.executeQuery()) {
                        if (fkRs.next()) {
                            String tableName = fkRs.getString("TABLE_NAME");
                            if (isSafeIdentifier(tableName)) {
                                stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + fkName + "`");
                                log.info("알려진 고아 외래키 삭제: {}.{}", tableName, fkName);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("외래키 {} 삭제 시도 실패: {}", fkName, e.getMessage());
                }
            }

            // 존재하지 않는 테이블을 참조하는 모든 외래키 찾아서 삭제
            String findOrphanedFkSql = "SELECT DISTINCT kcu.CONSTRAINT_NAME, kcu.TABLE_NAME " +
                    "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                    "LEFT JOIN information_schema.TABLES t " +
                    "ON kcu.REFERENCED_TABLE_NAME = t.TABLE_NAME AND t.TABLE_SCHEMA = DATABASE() " +
                    "WHERE kcu.TABLE_SCHEMA = DATABASE() AND kcu.REFERENCED_TABLE_NAME IS NOT NULL " +
                    "AND t.TABLE_NAME IS NULL AND kcu.CONSTRAINT_NAME IS NOT NULL";
            try (ResultSet orphanedRs = stmt.executeQuery(findOrphanedFkSql)) {
                while (orphanedRs.next()) {
                    String constraintName = orphanedRs.getString("CONSTRAINT_NAME");
                    String tableName = orphanedRs.getString("TABLE_NAME");
                    if (!isSafeIdentifier(tableName) || !isSafeIdentifier(constraintName)) continue;
                    try {
                        stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
                        log.info("고아 외래키 자동 삭제: {}.{}", tableName, constraintName);
                    } catch (Exception e) {
                        log.debug("외래키 삭제 실패: {}.{} - {}", tableName, constraintName, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("고아 외래키 자동 검색 실패: {}", e.getMessage());
            }

            // spontaneous_mission 테이블의 잘못된 데이터 정리 (user 테이블에 존재하지 않는 user_id)
            try {
                ResultSet tableRs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'spontaneous_mission'"
                );
                boolean tableExists = tableRs.next() && tableRs.getInt(1) > 0;
                tableRs.close();
                
                if (tableExists) {
                    // 먼저 user_id가 NULL이거나 존재하지 않는 user_id를 가진 레코드 삭제
                    String cleanupSql = 
                        "DELETE sm FROM spontaneous_mission sm " +
                        "WHERE sm.user_id IS NULL OR NOT EXISTS (" +
                        "  SELECT 1 FROM `user` u WHERE u.id = sm.user_id" +
                        ")";
                    int deletedCount = stmt.executeUpdate(cleanupSql);
                    if (deletedCount > 0) {
                        log.info("spontaneous_mission 테이블에서 {}개의 잘못된 데이터 삭제 (NULL이거나 존재하지 않는 user_id)", deletedCount);
                    }
                    
                    // 삭제된 사용자(del_flag = TRUE)를 참조하는 레코드도 삭제
                    String cleanupDeletedUsersSql = 
                        "DELETE sm FROM spontaneous_mission sm " +
                        "WHERE EXISTS (" +
                        "  SELECT 1 FROM `user` u WHERE u.id = sm.user_id AND (u.del_flag = TRUE OR u.del_flag IS NULL)" +
                        ")";
                    int deletedUserCount = stmt.executeUpdate(cleanupDeletedUsersSql);
                    if (deletedUserCount > 0) {
                        log.info("spontaneous_mission 테이블에서 {}개의 데이터 삭제 (삭제된 사용자 참조)", deletedUserCount);
                    }
                }
            } catch (Exception e) {
                log.warn("spontaneous_mission 데이터 정리 실패: {}", e.getMessage(), e);
            }

            // 외래키 체크 활성화
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            log.info("=== 데이터베이스 정리 완료 ===");
        } catch (Exception e) {
            log.error("데이터베이스 정리 중 오류 발생: {}", e.getMessage(), e);
            // 오류가 발생해도 애플리케이션 시작은 계속 진행 (수동 마이그레이션에서 처리 가능)
        }
    }
}
