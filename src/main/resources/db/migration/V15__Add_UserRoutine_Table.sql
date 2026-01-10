-- =====================================================
-- 사용자 루틴 설정 테이블 추가
-- Version: 15
-- Description: 기상시간, 다짐, 매일 갈 장소 등 사용자 루틴 설정 저장
-- =====================================================

CREATE TABLE `user_routine` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,

    -- 루틴 유형
    `routine_type` VARCHAR(30) NOT NULL COMMENT 'WAKE_UP_TIME, DAILY_PLACE, WEEKLY_RESOLUTION, MONTHLY_RESOLUTION 등',

    -- 주기 설정
    `period_type` VARCHAR(10) NOT NULL COMMENT 'DAILY, WEEKLY, MONTHLY, NONE',
    `period_start` DATE NULL COMMENT '해당 주기 시작일',
    `period_end` DATE NULL COMMENT '해당 주기 종료일',

    -- 값 저장 (유연하게 여러 타입)
    `value_text` VARCHAR(500) NULL COMMENT '텍스트 값 (다짐, 장소명 등)',
    `value_time` TIME NULL COMMENT '시간 값 (기상시간 등)',
    `value_number` INT NULL COMMENT '숫자 값 (목표 횟수 등)',
    `value_latitude` DECIMAL(10, 8) NULL COMMENT '위도 (장소용)',
    `value_longitude` DECIMAL(11, 8) NULL COMMENT '경도 (장소용)',

    -- 알림 설정
    `notification_enabled` BOOLEAN DEFAULT FALSE COMMENT '알림 활성화 여부',
    `notification_time` TIME NULL COMMENT '알림 받을 시간',

    -- 상태
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '현재 적용 중인 설정인지',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 인덱스
    INDEX `idx_user_routine_user` (`user_id`),
    INDEX `idx_user_routine_type` (`routine_type`),
    INDEX `idx_user_routine_period` (`period_type`, `period_start`),
    INDEX `idx_user_routine_active` (`user_id`, `is_active`),

    -- 외래키
    CONSTRAINT `fk_user_routine_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 루틴 설정';
