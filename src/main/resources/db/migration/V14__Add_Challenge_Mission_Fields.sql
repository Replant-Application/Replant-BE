-- =====================================================
-- 챌린지 미션 관련 필드 추가
-- Version: 14
-- Description: 미션 타입(챌린지/일반) 구분 필드 및 승격 필드 추가
-- =====================================================

-- 1. mission_source를 mission_type으로 변경 (엔티티와 일치시키기 위해)
ALTER TABLE `mission` CHANGE COLUMN `mission_source` `mission_type` VARCHAR(20) NOT NULL COMMENT 'OFFICIAL, CUSTOM';

-- 2. 챌린지 미션 여부 컬럼 추가
ALTER TABLE `mission` ADD COLUMN `is_challenge` BOOLEAN NULL DEFAULT FALSE COMMENT '챌린지 미션 여부';

-- 3. 챌린지 기간 컬럼 추가 (챌린지 미션일 때만 사용)
ALTER TABLE `mission` ADD COLUMN `challenge_days` INT NULL COMMENT '챌린지 기간 (일수)';

-- 4. 완료 기한 컬럼 추가 (일반 미션일 때만 사용)
ALTER TABLE `mission` ADD COLUMN `deadline_days` INT NULL COMMENT '완료 기한 (일수)';

-- 5. 공식 미션 승격 여부 컬럼 추가
ALTER TABLE `mission` ADD COLUMN `is_promoted` BOOLEAN NULL DEFAULT FALSE COMMENT '공식 미션으로 승격 여부';

-- 6. type 컬럼을 category로 이름 변경 (엔티티와 일치시키기 위해)
ALTER TABLE `mission` CHANGE COLUMN `type` `category` VARCHAR(20) NOT NULL COMMENT 'DAILY_LIFE, GROWTH, EXERCISE, STUDY, HEALTH, RELATIONSHIP';

-- 7. 기존 커스텀 미션에 대해 deadline_days 기본값 설정 (챌린지가 아닌 경우)
UPDATE `mission`
SET `deadline_days` = 3
WHERE `mission_type` = 'CUSTOM' AND `is_challenge` = FALSE AND `deadline_days` IS NULL;

-- 8. 인덱스 업데이트 (mission_source -> mission_type)
DROP INDEX IF EXISTS `idx_mission_source` ON `mission`;
CREATE INDEX `idx_mission_type` ON `mission` (`mission_type`);
