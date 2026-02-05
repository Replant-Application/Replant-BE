package com.app.replant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Replant Backend Application
 *
 * 환경 설정은 .env 파일에서 자동으로 로드됩니다.
 * spring-dotenv 라이브러리를 사용하여 ${env.변수명} 형식으로 접근 가능합니다.
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
@EnableCaching
public class ReplantApplication {

    public static void main(String[] args) {
        System.out.println("=== Replant Backend Starting ===");
        System.out.println("환경변수는 .env 파일에서 자동으로 로드됩니다.");
        System.out.println("================================");

        SpringApplication.run(ReplantApplication.class, args);
    }

}
