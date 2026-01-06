-- 다이어리 테이블
CREATE TABLE diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    emotion VARCHAR(50) NOT NULL COMMENT '감정 (happy, sad, angry 등)',
    content TEXT NOT NULL COMMENT '일기 내용',
    weather VARCHAR(50) COMMENT '날씨',
    location VARCHAR(100) COMMENT '위치',
    image_urls JSON COMMENT '이미지 URL 목록',
    is_private BOOLEAN NOT NULL DEFAULT FALSE COMMENT '비공개 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    CONSTRAINT fk_diary_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 추가
CREATE INDEX idx_diary_user_date ON diary(user_id, date);
CREATE INDEX idx_diary_user_created ON diary(user_id, created_at DESC);
