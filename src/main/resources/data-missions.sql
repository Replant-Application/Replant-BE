-- 샘플 미션 데이터 (20개)
-- 기존 데이터가 있으면 먼저 삭제
DELETE FROM mission_age_ranges;
DELETE FROM mission;

-- ========== 일간 미션 (DAILY) - 7개 ==========

-- 1. 취업준비 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (1, '이력서 한 줄 수정하기', '오늘 이력서에서 한 문장이라도 수정하거나 개선해보세요. 작은 변화가 큰 차이를 만듭니다.', 'DAILY', 'COMMUNITY', 10, 3, true, NOW(), 'JOB_PREPARATION', 'ALL', 'ALL', 'HOME', 'LEVEL1');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (1, 'EARLY_TWENTIES'), (1, 'MID_TWENTIES'), (1, 'LATE_TWENTIES');

-- 2. 복학 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (2, '오늘 배울 것 정리하기', '오늘 공부하거나 배울 내용을 미리 정리해보세요. 복학 전 학습 습관을 만들어가요.', 'DAILY', 'COMMUNITY', 10, 3, true, NOW(), 'RETURN_TO_SCHOOL', 'ALL', 'ALL', 'HOME', 'LEVEL1');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (2, 'EARLY_TWENTIES'), (2, 'MID_TWENTIES');

-- 3. 입시 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (3, '영어 단어 10개 외우기', '오늘 영어 단어 10개를 외워보세요. 매일 꾸준히 하면 3개월에 900개!', 'DAILY', 'COMMUNITY', 15, 3, true, NOW(), 'ENTRANCE_EXAM', 'ALL', 'ALL', 'HOME', 'LEVEL1');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (3, 'LATE_TEENS'), (3, 'EARLY_TWENTIES');

-- 4. 자기관리 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (4, '아침 7시 전 기상하기', '아침 일찍 일어나면 하루가 길어집니다. 오늘 아침 7시 전에 일어나보세요!', 'DAILY', 'TIME', 20, 3, true, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (4, 'LATE_TEENS'), (4, 'EARLY_TWENTIES'), (4, 'MID_TWENTIES'), (4, 'LATE_TWENTIES');

-- 5. 자기관리 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (5, '물 2L 마시기', '하루 동안 물 2리터를 마셔보세요. 건강한 습관의 시작입니다.', 'DAILY', 'COMMUNITY', 10, 3, true, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL1');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (5, 'LATE_TEENS'), (5, 'EARLY_TWENTIES'), (5, 'MID_TWENTIES'), (5, 'LATE_TWENTIES'), (5, 'EARLY_THIRTIES');

-- 6. 취업준비 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (6, '채용공고 3개 확인하기', '오늘 관심 있는 채용공고 3개를 찾아보고 저장해보세요.', 'DAILY', 'COMMUNITY', 15, 3, true, NOW(), 'JOB_PREPARATION', 'ALL', 'ALL', 'HOME', 'LEVEL1');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (6, 'EARLY_TWENTIES'), (6, 'MID_TWENTIES'), (6, 'LATE_TWENTIES'), (6, 'EARLY_THIRTIES');

-- 7. 입시 - 일간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (7, '수학 문제 5개 풀기', '수학 문제 5개를 풀어보세요. 꾸준한 연습이 실력을 만듭니다.', 'DAILY', 'COMMUNITY', 15, 3, true, NOW(), 'ENTRANCE_EXAM', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (7, 'LATE_TEENS'), (7, 'EARLY_TWENTIES');

-- ========== 주간 미션 (WEEKLY) - 7개 ==========

-- 8. 취업준비 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (8, '자기소개서 한 편 완성하기', '이번 주에 자기소개서 한 편을 완성해보세요. 시작이 반입니다!', 'WEEKLY', 'COMMUNITY', 50, 7, true, NOW(), 'JOB_PREPARATION', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (8, 'EARLY_TWENTIES'), (8, 'MID_TWENTIES'), (8, 'LATE_TWENTIES');

-- 9. 복학 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (9, '전공 책 1장 읽기', '이번 주에 전공 책 한 장을 읽어보세요. 복학 준비의 시작이에요.', 'WEEKLY', 'COMMUNITY', 40, 7, true, NOW(), 'RETURN_TO_SCHOOL', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (9, 'EARLY_TWENTIES'), (9, 'MID_TWENTIES');

-- 10. 자기관리 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (10, '운동 3회 하기', '이번 주에 운동을 3번 해보세요. 헬스, 러닝, 홈트 모두 OK!', 'WEEKLY', 'COMMUNITY', 60, 7, true, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'OUTDOOR', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (10, 'LATE_TEENS'), (10, 'EARLY_TWENTIES'), (10, 'MID_TWENTIES'), (10, 'LATE_TWENTIES'), (10, 'EARLY_THIRTIES');

-- 11. 입시 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (11, '모의고사 1회 풀기', '이번 주에 모의고사 한 회를 풀어보세요. 실전 감각을 키워요!', 'WEEKLY', 'COMMUNITY', 70, 7, true, NOW(), 'ENTRANCE_EXAM', 'ALL', 'ALL', 'HOME', 'LEVEL3');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (11, 'LATE_TEENS'), (11, 'EARLY_TWENTIES');

-- 12. 취업준비 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (12, '면접 질문 5개 답변 준비하기', '자주 나오는 면접 질문 5개에 대한 답변을 준비해보세요.', 'WEEKLY', 'COMMUNITY', 45, 7, true, NOW(), 'JOB_PREPARATION', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (12, 'EARLY_TWENTIES'), (12, 'MID_TWENTIES'), (12, 'LATE_TWENTIES');

-- 13. 자기관리 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (13, '책 한 권 읽기', '이번 주에 책 한 권을 완독해보세요. 자기계발, 소설 모두 OK!', 'WEEKLY', 'COMMUNITY', 55, 7, true, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (13, 'LATE_TEENS'), (13, 'EARLY_TWENTIES'), (13, 'MID_TWENTIES'), (13, 'LATE_TWENTIES'), (13, 'EARLY_THIRTIES'), (13, 'MID_THIRTIES');

-- 14. 복학 - 주간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (14, '학교 도서관 방문하기', '이번 주에 학교 도서관을 방문해보세요. 캠퍼스 적응의 시작!', 'WEEKLY', 'GPS', 35, 7, true, NOW(), 'RETURN_TO_SCHOOL', 'ALL', 'ALL', 'INDOOR', 'LEVEL1');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (14, 'EARLY_TWENTIES'), (14, 'MID_TWENTIES');

-- ========== 월간 미션 (MONTHLY) - 6개 ==========

-- 15. 취업준비 - 월간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (15, '온라인 강의 하나 완강하기', '이번 달에 관심 있는 온라인 강의 하나를 완강해보세요.', 'MONTHLY', 'COMMUNITY', 150, 30, true, NOW(), 'JOB_PREPARATION', 'ALL', 'ALL', 'HOME', 'LEVEL3');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (15, 'EARLY_TWENTIES'), (15, 'MID_TWENTIES'), (15, 'LATE_TWENTIES'), (15, 'EARLY_THIRTIES');

-- 16. 자기관리 - 월간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (16, '새로운 취미 시작하기', '이번 달에 새로운 취미를 시작해보세요. 그림, 요리, 악기 등 무엇이든!', 'MONTHLY', 'COMMUNITY', 120, 30, true, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'HOME', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (16, 'LATE_TEENS'), (16, 'EARLY_TWENTIES'), (16, 'MID_TWENTIES'), (16, 'LATE_TWENTIES'), (16, 'EARLY_THIRTIES'), (16, 'MID_THIRTIES');

-- 17. 입시 - 월간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (17, '과목별 오답노트 만들기', '이번 달에 취약 과목 오답노트를 만들어보세요. 반복 실수를 줄여요!', 'MONTHLY', 'COMMUNITY', 130, 30, true, NOW(), 'ENTRANCE_EXAM', 'ALL', 'ALL', 'HOME', 'LEVEL3');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (17, 'LATE_TEENS'), (17, 'EARLY_TWENTIES');

-- 18. 복학 - 월간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (18, '선배와 커피챗 하기', '이번 달에 같은 과 선배와 커피챗을 해보세요. 조언을 들어보아요!', 'MONTHLY', 'COMMUNITY', 80, 30, true, NOW(), 'RETURN_TO_SCHOOL', 'ALL', 'ALL', 'OUTDOOR', 'LEVEL2');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (18, 'EARLY_TWENTIES'), (18, 'MID_TWENTIES');

-- 19. 취업준비 - 월간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (19, '포트폴리오 작품 하나 완성하기', '이번 달에 포트폴리오에 넣을 작품이나 프로젝트를 하나 완성해보세요.', 'MONTHLY', 'COMMUNITY', 200, 30, true, NOW(), 'JOB_PREPARATION', 'ALL', 'ALL', 'HOME', 'LEVEL3');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (19, 'EARLY_TWENTIES'), (19, 'MID_TWENTIES'), (19, 'LATE_TWENTIES');

-- 20. 자기관리 - 월간 미션
INSERT INTO mission (id, title, description, type, verification_type, exp_reward, badge_duration_days, is_active, created_at, worry_type, gender_type, region_type, place_type, difficulty_level)
VALUES (20, '30일 운동 챌린지 완료하기', '이번 달 매일 운동하는 30일 챌린지에 도전해보세요!', 'MONTHLY', 'COMMUNITY', 250, 30, true, NOW(), 'SELF_MANAGEMENT', 'ALL', 'ALL', 'OUTDOOR', 'LEVEL3');

INSERT INTO mission_age_ranges (mission_id, age_range) VALUES (20, 'LATE_TEENS'), (20, 'EARLY_TWENTIES'), (20, 'MID_TWENTIES'), (20, 'LATE_TWENTIES'), (20, 'EARLY_THIRTIES');
