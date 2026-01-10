-- V16: Fix mission difficulty_level column size
-- 커스텀 미션 생성 시 Data truncated 오류 수정

ALTER TABLE mission MODIFY COLUMN difficulty_level VARCHAR(10);
