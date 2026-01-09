-- V10: 미션세트(투두리스트) 테이블 추가

-- 미션세트 테이블
CREATE TABLE IF NOT EXISTS mission_set (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    added_count INT NOT NULL DEFAULT 0,
    average_rating DOUBLE NOT NULL DEFAULT 0.0,
    review_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    CONSTRAINT fk_mission_set_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 미션세트-미션 연결 테이블
CREATE TABLE IF NOT EXISTS mission_set_mission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_set_id BIGINT NOT NULL,
    mission_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msm_mission_set FOREIGN KEY (mission_set_id) REFERENCES mission_set(id) ON DELETE CASCADE,
    CONSTRAINT fk_msm_mission FOREIGN KEY (mission_id) REFERENCES mission(id) ON DELETE CASCADE,
    CONSTRAINT uk_mission_set_mission UNIQUE (mission_set_id, mission_id)
);

-- 인덱스 추가
CREATE INDEX idx_mission_set_creator ON mission_set(creator_id);
CREATE INDEX idx_mission_set_is_public ON mission_set(is_public);
CREATE INDEX idx_mission_set_added_count ON mission_set(added_count);
CREATE INDEX idx_msm_mission_set ON mission_set_mission(mission_set_id);
CREATE INDEX idx_msm_mission ON mission_set_mission(mission_id);
