-- =====================================================
-- 돌발 미션용 미션 추가 (359번부터)
-- 기존 미션 데이터가 있는 경우를 대비해 INSERT IGNORE 사용
-- =====================================================

INSERT IGNORE INTO mission (id, title, description, mission_type, category, verification_type, required_minutes, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level, deadline_days, duration_days, is_public, is_promoted, is_challenge, challenge_days, start_time, end_time, creator_id) VALUES
-- 돌발 미션: 기상
(359, '기상하기', '아침에 일어나서 하루를 시작해보세요! 10분 안에 인증해주세요.', 'OFFICIAL', 'DAILY_LIFE', 'COMMUNITY', NULL, 10, 3, TRUE, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL1', 1, NULL, FALSE, FALSE, FALSE, NULL, NULL, NULL, NULL),
-- 돌발 미션: 아침 식사
(360, '아침 식사하기', '건강한 아침 식사를 하고 사진으로 인증해주세요.', 'OFFICIAL', 'HEALTH', 'COMMUNITY', NULL, 10, 3, TRUE, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL1', 1, NULL, FALSE, FALSE, FALSE, NULL, NULL, NULL, NULL),
-- 돌발 미션: 점심 식사
(361, '점심 식사하기', '건강한 점심 식사를 하고 사진으로 인증해주세요.', 'OFFICIAL', 'HEALTH', 'COMMUNITY', NULL, 10, 3, TRUE, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL1', 1, NULL, FALSE, FALSE, FALSE, NULL, NULL, NULL, NULL),
-- 돌발 미션: 저녁 식사
(362, '저녁 식사하기', '건강한 저녁 식사를 하고 사진으로 인증해주세요.', 'OFFICIAL', 'HEALTH', 'COMMUNITY', NULL, 10, 3, TRUE, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL1', 1, NULL, FALSE, FALSE, FALSE, NULL, NULL, NULL, NULL),
-- 돌발 미션: 감성일기
(363, '감성일기 쓰기', '오늘 하루를 돌아보며 감성일기를 작성해주세요.', 'OFFICIAL', 'GROWTH', 'COMMUNITY', NULL, 10, 3, TRUE, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL1', 1, NULL, FALSE, FALSE, FALSE, NULL, NULL, NULL, NULL);
