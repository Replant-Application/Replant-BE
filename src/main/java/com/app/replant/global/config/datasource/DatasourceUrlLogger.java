package com.app.replant.global.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 기동 시 Secondary DataSource(실제 사용 DB) JDBC URL 로그 출력
 * 공개 투두리스트 0건 이슈 등 DB 연결 확인용
 */
@Slf4j
@Component
public class DatasourceUrlLogger implements ApplicationRunner {

    private final DataSource secondaryDataSource;

    public DatasourceUrlLogger(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.secondaryDataSource = secondaryDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (secondaryDataSource instanceof HikariDataSource hikari) {
            String url = hikari.getJdbcUrl();
            // 비밀번호 제외하고 호스트/DB만 로그 (예: jdbc:mysql://host:port/dbname?params)
            String safe = url != null ? url.replaceAll("([?&]password=)[^&]*", "$1***") : "null";
            log.info("[DB 연결 확인] Secondary DataSource JDBC URL: {}", safe);
        }
    }
}
