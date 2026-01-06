-- 기상 미션 설정 테이블
CREATE TABLE wakeup_mission_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    time_slot VARCHAR(20) NOT NULL COMMENT 'SLOT_6_8, SLOT_8_10, SLOT_10_12',
    week_number INT NOT NULL COMMENT '해당 연도의 주차',
    year INT NOT NULL COMMENT '연도',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    CONSTRAINT fk_wakeup_setting_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_week_year (user_id, week_number, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 추가
CREATE INDEX idx_wakeup_setting_user_week ON wakeup_mission_setting(user_id, week_number, year);
