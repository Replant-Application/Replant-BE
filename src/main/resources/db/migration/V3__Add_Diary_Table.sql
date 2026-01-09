-- 다이어리 테이블
CREATE TABLE diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    mood INT COMMENT '기분 값 (슬라이더 값, 예: 1-5)',
    emotions JSON COMMENT '선택된 감정들 (JSON 배열: ["행복", "기쁨", "사랑"])',
    emotion_factors JSON COMMENT '감정에 영향을 준 요인들 (JSON 배열: ["공부", "가족", "운동"])',
    content TEXT NOT NULL COMMENT '일기 내용',

    CONSTRAINT fk_diary_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 추가
CREATE INDEX idx_diary_user_date ON diary(user_id, date);
