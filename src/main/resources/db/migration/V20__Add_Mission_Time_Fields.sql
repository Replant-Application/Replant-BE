-- V20: 시간 미션용 시작/종료 시간 필드 추가
-- 시간(TIME) 인증 미션에서 특정 시간대에 수행해야 하는 미션을 위한 필드

-- mission 테이블에 추가
ALTER TABLE mission ADD COLUMN IF NOT EXISTS start_time VARCHAR(5) NULL COMMENT '시작 시간 (HH:mm 형식)';
ALTER TABLE mission ADD COLUMN IF NOT EXISTS end_time VARCHAR(5) NULL COMMENT '종료 시간 (HH:mm 형식)';

-- custom_mission 테이블에 추가
ALTER TABLE custom_mission ADD COLUMN IF NOT EXISTS start_time VARCHAR(5) NULL COMMENT '시작 시간 (HH:mm 형식)';
ALTER TABLE custom_mission ADD COLUMN IF NOT EXISTS end_time VARCHAR(5) NULL COMMENT '종료 시간 (HH:mm 형식)';
