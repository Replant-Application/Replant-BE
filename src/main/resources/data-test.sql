-- Replant 테스트 데이터
-- 비밀번호는 모두 'test1234' (BCrypt 암호화)

-- 테스트 사용자 1: 일반 유저
INSERT INTO user (id, email, nickname, password, phone, birth_date, gender, profile_img, role, status,
                  total_missions_completed, total_exp_gained, login_streak, created_at, updated_at)
VALUES (1, 'test@replant.com', '테스트유저', '$2a$10$N1K3JKl9cYZ8nQ3P4kE5XOxGZVJy5RfHN3vI8yR7pT5uE2mL0sK3G',
        '010-1234-5678', '1995-05-15', 'MALE', NULL, 'USER', 'ACTIVE',
        5, 250, 3, NOW(), NOW());

-- 테스트 사용자 2: 관리자
INSERT INTO user (id, email, nickname, password, phone, birth_date, gender, profile_img, role, status,
                  total_missions_completed, total_exp_gained, login_streak, created_at, updated_at)
VALUES (2, 'admin@replant.com', '관리자', '$2a$10$N1K3JKl9cYZ8nQ3P4kE5XOxGZVJy5RfHN3vI8yR7pT5uE2mL0sK3G',
        '010-9999-9999', '1990-01-01', 'FEMALE', NULL, 'ADMIN', 'ACTIVE',
        0, 0, 1, NOW(), NOW());

-- 테스트 사용자 3: OAuth 유저 (Google)
INSERT INTO user (id, email, nickname, password, phone, birth_date, gender, profile_img, role, status,
                  total_missions_completed, total_exp_gained, login_streak, created_at, updated_at)
VALUES (3, 'oauth.test@gmail.com', 'OAuth테스터', NULL,
        NULL, '1998-03-20', 'MALE', NULL, 'USER', 'ACTIVE',
        10, 500, 7, NOW(), NOW());

-- 테스트 사용자 1의 Reant
INSERT INTO reant (id, user_id, name, level, exp, stage, max_level, mood, health, hunger, created_at, updated_at)
VALUES (1, 1, '리앤티', 3, 50, 'EGG', 100, 85, 90, 20, NOW(), NOW());

-- 테스트 사용자 2의 Reant
INSERT INTO reant (id, user_id, name, level, exp, stage, max_level, mood, health, hunger, created_at, updated_at)
VALUES (2, 2, '관리봇', 1, 0, 'EGG', 100, 100, 100, 0, NOW(), NOW());

-- 테스트 사용자 3의 Reant
INSERT INTO reant (id, user_id, name, level, exp, stage, max_level, mood, health, hunger, created_at, updated_at)
VALUES (3, 3, '구글이', 6, 120, 'BABY', 100, 95, 95, 15, NOW(), NOW());

-- OAuth 연동 정보 (테스트 사용자 3)
INSERT INTO user_oauth (id, user_id, provider, provider_id, email, name, created_at, updated_at)
VALUES (1, 3, 'GOOGLE', 'google-test-id-123456', 'oauth.test@gmail.com', 'OAuth테스터', NOW(), NOW());

-- 시퀀스 초기화 (H2 데이터베이스용)
ALTER TABLE user ALTER COLUMN id RESTART WITH 4;
ALTER TABLE reant ALTER COLUMN id RESTART WITH 4;
ALTER TABLE user_oauth ALTER COLUMN id RESTART WITH 2;
