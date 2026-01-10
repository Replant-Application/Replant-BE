-- =====================================================
-- Post 테이블 단순화 - 좋아요 = 인증 시스템
-- Version: 13
-- Description: 불필요한 컬럼 제거 및 verification_vote 테이블 삭제
-- 좋아요가 인증 역할을 하므로 투표 테이블 불필요
-- =====================================================

-- 1. verification_vote 테이블 삭제 (좋아요로 대체)
-- 이미 삭제되었을 수 있으므로 조건부 실행
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'verification_vote');
SET @sql = IF(@table_exists > 0, 'DROP TABLE verification_vote', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. post 테이블 외래키 삭제 (존재하면)
SET @fk_mission = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND CONSTRAINT_NAME = 'fk_post_mission');
SET @sql = IF(@fk_mission > 0, 'ALTER TABLE post DROP FOREIGN KEY fk_post_mission', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_custom = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND CONSTRAINT_NAME = 'fk_post_custom_mission');
SET @sql = IF(@fk_custom > 0, 'ALTER TABLE post DROP FOREIGN KEY fk_post_custom_mission', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. post 테이블 인덱스 삭제 (존재하면)
SET @idx_mission = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_post_mission_id');
SET @sql = IF(@idx_mission > 0, 'ALTER TABLE post DROP INDEX idx_post_mission_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. post 테이블 불필요 컬럼 삭제 (존재하면)
SET @col_mission = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'mission_id');
SET @sql = IF(@col_mission > 0, 'ALTER TABLE post DROP COLUMN mission_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_custom = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'custom_mission_id');
SET @sql = IF(@col_custom > 0, 'ALTER TABLE post DROP COLUMN custom_mission_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_badge = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'has_valid_badge');
SET @sql = IF(@col_badge > 0, 'ALTER TABLE post DROP COLUMN has_valid_badge', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_approve = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'approve_count');
SET @sql = IF(@col_approve > 0, 'ALTER TABLE post DROP COLUMN approve_count', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_reject = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'reject_count');
SET @sql = IF(@col_reject > 0, 'ALTER TABLE post DROP COLUMN reject_count', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 백업 테이블 정리
DROP TABLE IF EXISTS `_backup_verification_post`;
DROP TABLE IF EXISTS `_backup_post`;
DROP TABLE IF EXISTS `_backup_comment`;
DROP TABLE IF EXISTS `_backup_verification_vote`;

-- =====================================================
-- 마이그레이션 완료
-- 최종 post 테이블 구조:
-- - id, post_type, user_id, user_mission_id
-- - title, content, image_urls
-- - del_flag, status, verified_at
-- - created_at, updated_at
--
-- 인증 로직: 좋아요 수 >= REQUIRED_LIKES(1) 이면 자동 인증
-- =====================================================
