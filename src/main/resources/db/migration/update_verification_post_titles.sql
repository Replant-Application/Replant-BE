-- 인증글의 title이 NULL인 경우 미션 제목으로 업데이트
-- 기존 데이터 마이그레이션 스크립트

UPDATE post p
INNER JOIN user_mission um ON p.user_mission_id = um.id
INNER JOIN mission m ON um.mission_id = m.id
SET p.title = m.title
WHERE p.post_type = 'VERIFICATION'
  AND (p.title IS NULL OR p.title = '');
