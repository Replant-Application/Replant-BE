-- =====================================================
-- V4: Reset Mission Data and Seed with Correct Verification Types
-- 유저 정보는 유지하고 미션 관련 데이터만 초기화
-- =====================================================

-- 외래키 체크 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 미션 관련 테이블 초기화 (유저 데이터 유지)
DELETE FROM chat_message;
DELETE FROM chat_room;
DELETE FROM user_recommendation;
DELETE FROM notification;
DELETE FROM comment;
DELETE FROM post;
DELETE FROM mission_qna_answer;
DELETE FROM mission_qna;
DELETE FROM mission_review;
DELETE FROM verification_vote;
DELETE FROM mission_verification;
DELETE FROM verification_post;
DELETE FROM user_badge;
DELETE FROM user_mission;
DELETE FROM custom_mission;
DELETE FROM mission;

-- 외래키 체크 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 미션 데이터 삽입
-- 인증 타입:
--   GPS: 특정 위치에서 인증 (gps 좌표 필요)
--   TIME: 일정 시간 활동 후 인증 (required_minutes 필요)
--   COMMUNITY: 커뮤니티 투표로 인증 (좋아요 3개)
-- =====================================================

INSERT INTO mission (id, title, description, type, verification_type, gps_latitude, gps_longitude, gps_radius_meters, required_minutes, exp_reward, badge_duration_days, is_active) VALUES
-- 일간 미션 (DAILY)
(1, '30분 산책하기', '동네를 30분 동안 산책하며 마음을 정리해보세요.', 'DAILY', 'TIME', NULL, NULL, NULL, 30, 15, 3, TRUE),
(2, '물 8잔 마시기', '하루 동안 물 8잔(2L)을 마시고 인증해주세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
(3, '10분 명상하기', '조용한 곳에서 10분간 명상을 해보세요.', 'DAILY', 'TIME', NULL, NULL, NULL, 10, 10, 3, TRUE),
(4, '영어 단어 10개 외우기', '오늘 새로운 영어 단어 10개를 외워보세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
(5, '아침 식사하기', '건강한 아침 식사를 하고 사진으로 인증해주세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
(6, '일기 쓰기', '오늘 하루를 돌아보며 일기를 작성해주세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
(7, '스트레칭 10분', '10분간 스트레칭으로 몸을 풀어주세요.', 'DAILY', 'TIME', NULL, NULL, NULL, 10, 10, 3, TRUE),

-- 주간 미션 (WEEKLY)
(8, '책 1권 읽기', '이번 주에 책 1권을 완독하고 감상을 공유해주세요.', 'WEEKLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 50, 7, TRUE),
(9, '헬스장 3회 방문', '이번 주에 헬스장을 3회 방문해주세요.', 'WEEKLY', 'GPS', 37.5665, 126.9780, 500, NULL, 50, 7, TRUE),
(10, '새로운 요리 도전', '이번 주에 처음 만들어보는 요리에 도전해보세요.', 'WEEKLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 40, 7, TRUE),
(11, '친구와 통화하기', '오랫동안 연락 못했던 친구와 통화해보세요.', 'WEEKLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 30, 7, TRUE),
(12, '주 3회 운동하기', '이번 주에 3회 이상 운동을 해주세요. 각 30분 이상.', 'WEEKLY', 'TIME', NULL, NULL, NULL, 90, 50, 7, TRUE),

-- 월간 미션 (MONTHLY)
(13, '새로운 취미 시작', '이번 달에 새로운 취미를 시작해보세요.', 'MONTHLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 100, 21, TRUE),
(14, '5만원 저축하기', '이번 달에 5만원을 저축하고 인증해주세요.', 'MONTHLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 80, 21, TRUE),
(15, '봉사활동 참여', '이번 달에 봉사활동에 참여해보세요.', 'MONTHLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 120, 21, TRUE);
