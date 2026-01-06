-- 댓글 대댓글 관계 컬럼 추가
ALTER TABLE comment ADD COLUMN parent_id BIGINT NULL COMMENT '부모 댓글 ID (대댓글인 경우)';

-- 부모 댓글 외래키 제약조건
ALTER TABLE comment ADD CONSTRAINT fk_comment_parent
    FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE;

-- 부모 댓글 ID 인덱스
CREATE INDEX idx_comment_parent_id ON comment(parent_id);

-- 사용자 FCM 토큰 컬럼 추가
ALTER TABLE user ADD COLUMN fcm_token VARCHAR(500) NULL COMMENT 'FCM 푸시 알림 토큰';
