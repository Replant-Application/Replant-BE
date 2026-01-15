package com.app.replant.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
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

            for (String deletedTable : deletedTables) {
                // 해당 테이블을 참조하는 외래키 찾기
                String sql = "SELECT DISTINCT CONSTRAINT_NAME, TABLE_NAME " +
                            "FROM information_schema.KEY_COLUMN_USAGE " +
                            "WHERE REFERENCED_TABLE_NAME = '" + deletedTable + "' " +
                            "AND TABLE_SCHEMA = DATABASE() " +
                            "AND CONSTRAINT_NAME IS NOT NULL";
                
                try {
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        String constraintName = rs.getString("CONSTRAINT_NAME");
                        String tableName = rs.getString("TABLE_NAME");
                        
                        try {
                            stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
                            log.info("고아 외래키 삭제: {}.{}", tableName, constraintName);
                        } catch (Exception e) {
                            log.debug("외래키 삭제 실패 (이미 삭제되었을 수 있음): {}.{} - {}", 
                                    tableName, constraintName, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.debug("외래키 검색 실패: {} - {}", deletedTable, e.getMessage());
                }
            }

            // 알려진 문제 외래키 직접 삭제 시도
            String[] knownOrphanedKeys = {"FKk6jowxq7o9ysklnigijjbwa7u"};
            for (String fkName : knownOrphanedKeys) {
                String findFkSql = "SELECT DISTINCT TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                                  "WHERE CONSTRAINT_NAME = '" + fkName + "' " +
                                  "AND TABLE_SCHEMA = DATABASE() LIMIT 1";
                try {
                    ResultSet fkRs = stmt.executeQuery(findFkSql);
                    if (fkRs.next()) {
                        String tableName = fkRs.getString("TABLE_NAME");
                        stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + fkName + "`");
                        log.info("알려진 고아 외래키 삭제: {}.{}", tableName, fkName);
                    }
                } catch (Exception e) {
                    log.debug("외래키 {} 삭제 시도 실패: {}", fkName, e.getMessage());
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
                        stmt.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
                        log.info("고아 외래키 자동 삭제: {}.{}", tableName, constraintName);
                    } catch (Exception e) {
                        log.debug("외래키 삭제 실패: {}.{} - {}", tableName, constraintName, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("고아 외래키 자동 검색 실패: {}", e.getMessage());
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
