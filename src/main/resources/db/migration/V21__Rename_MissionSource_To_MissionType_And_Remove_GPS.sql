-- V21: mission_source를 mission_type으로 변경하고 GPS 관련 컬럼 삭제

-- 1. mission_source 컬럼을 mission_type으로 이름 변경 (이미 있으면 스킵)
-- MySQL에서는 CHANGE COLUMN 사용
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND COLUMN_NAME = 'mission_source');
SET @sql = IF(@column_exists > 0,
    'ALTER TABLE mission CHANGE COLUMN mission_source mission_type VARCHAR(20) NOT NULL DEFAULT ''CUSTOM''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. GPS 관련 컬럼 삭제 (존재하면 삭제)
SET @gps_lat_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND COLUMN_NAME = 'gps_latitude');
SET @sql = IF(@gps_lat_exists > 0, 'ALTER TABLE mission DROP COLUMN gps_latitude', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @gps_lon_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND COLUMN_NAME = 'gps_longitude');
SET @sql = IF(@gps_lon_exists > 0, 'ALTER TABLE mission DROP COLUMN gps_longitude', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @gps_radius_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND COLUMN_NAME = 'gps_radius_meters');
SET @sql = IF(@gps_radius_exists > 0, 'ALTER TABLE mission DROP COLUMN gps_radius_meters', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. type 컬럼 삭제 (DAILY, WEEKLY 등 - 존재하면 삭제)
SET @type_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND COLUMN_NAME = 'type');
SET @sql = IF(@type_exists > 0, 'ALTER TABLE mission DROP COLUMN type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 기존 인덱스 삭제 후 새 인덱스 생성
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND INDEX_NAME = 'idx_mission_source');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE mission DROP INDEX idx_mission_source', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새 인덱스 생성 (없으면)
SET @new_idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mission' AND INDEX_NAME = 'idx_mission_type');
SET @sql = IF(@new_idx_exists = 0, 'CREATE INDEX idx_mission_type ON mission(mission_type)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. custom_mission 테이블에서도 GPS 컬럼 삭제 (존재하면)
SET @cm_gps_lat_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'custom_mission' AND COLUMN_NAME = 'gps_latitude');
SET @sql = IF(@cm_gps_lat_exists > 0, 'ALTER TABLE custom_mission DROP COLUMN gps_latitude', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cm_gps_lon_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'custom_mission' AND COLUMN_NAME = 'gps_longitude');
SET @sql = IF(@cm_gps_lon_exists > 0, 'ALTER TABLE custom_mission DROP COLUMN gps_longitude', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cm_gps_radius_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'custom_mission' AND COLUMN_NAME = 'gps_radius_meters');
SET @sql = IF(@cm_gps_radius_exists > 0, 'ALTER TABLE custom_mission DROP COLUMN gps_radius_meters', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
