-- =====================================================
-- 사용자 루틴 테이블 확장
-- Version: 19
-- Description: 루틴에 제목, 설명, 시작/종료 시간 필드 추가
--              새로운 루틴 타입(STUDY_TIME, GYM_LOCATION, LIBRARY_LOCATION) 지원
-- =====================================================

-- 제목 필드 추가 (사용자가 지정한 이름)
ALTER TABLE `user_routine`
    ADD COLUMN `title` VARCHAR(100) NULL COMMENT '루틴 제목 (예: 우리동네 헬스장)' AFTER `period_end`;

-- 설명 필드 추가
ALTER TABLE `user_routine`
    ADD COLUMN `description` VARCHAR(500) NULL COMMENT '루틴 설명' AFTER `title`;

-- 시작 시간 필드 추가 (기존 value_time은 유지하되 호환용으로)
ALTER TABLE `user_routine`
    ADD COLUMN `value_time_start` TIME NULL COMMENT '시작 시간 (기상시간, 공부시작시간 등)' AFTER `value_text`;

-- 종료 시간 필드 추가
ALTER TABLE `user_routine`
    ADD COLUMN `value_time_end` TIME NULL COMMENT '종료 시간 (공부종료시간 등)' AFTER `value_time_start`;

-- 기존 데이터 마이그레이션: value_time → value_time_start로 복사
UPDATE `user_routine`
SET `value_time_start` = `value_time`
WHERE `value_time` IS NOT NULL;
