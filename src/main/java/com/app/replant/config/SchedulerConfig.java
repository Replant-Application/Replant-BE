package com.app.replant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * TaskScheduler 설정
 * 효율적인 리소스 관리와 동시 작업 처리를 위한 스레드 풀 설정
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    /**
     * TaskScheduler 빈 설정
     * - 스레드 풀 크기: 10 (동시 실행 가능한 작업 수)
     * - 스레드 이름 prefix: "spontaneous-mission-scheduler-"
     * - 대기 큐 크기: 100 (대기 중인 작업 수)
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 동시 실행 가능한 스레드 수
        scheduler.setThreadNamePrefix("spontaneous-mission-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 대기
        scheduler.setAwaitTerminationSeconds(60); // 종료 대기 시간 (초)
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.initialize();
        return scheduler;
    }
}
