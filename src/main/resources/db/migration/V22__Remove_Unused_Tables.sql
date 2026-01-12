-- V22: 미사용 테이블 및 컬럼 삭제
-- 삭제된 엔티티: Card, MemberCard, Goal, Member, Chat, CustomMission (Mission으로 통합), QnA

-- 1. QnA 관련 테이블 삭제
DROP TABLE IF EXISTS mission_qna_answer;
DROP TABLE IF EXISTS mission_qna;

-- 2. Chat 관련 테이블 삭제
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_room;

-- 3. Card 관련 테이블 삭제
DROP TABLE IF EXISTS member_card;
DROP TABLE IF EXISTS card;

-- 4. Goal 테이블 삭제
DROP TABLE IF EXISTS goal;

-- 5. Member 테이블 삭제 (User 테이블로 통합됨)
DROP TABLE IF EXISTS member;

-- 6. CustomMission 테이블 삭제 (Mission으로 통합됨 - V7에서 이미 처리되었을 수 있음)
DROP TABLE IF EXISTS custom_mission;

-- 7. user_recommendation 테이블에서 custom_mission_id 컬럼 삭제
ALTER TABLE user_recommendation DROP COLUMN IF EXISTS custom_mission_id;

-- 8. mission_age_ranges 테이블은 유지 (Mission 엔티티에서 사용 중)
