-- =====================================================
-- PostLike 테이블 추가
-- Version: 12
-- Description: 게시글 좋아요 기능을 위한 테이블 생성
-- =====================================================

-- 1. post_like 테이블 생성
CREATE TABLE IF NOT EXISTS `post_like` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_post_like_post_user` (`post_id`, `user_id`),
    INDEX `idx_post_like_post` (`post_id`),
    INDEX `idx_post_like_user` (`user_id`),

    CONSTRAINT `fk_post_like_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글 좋아요';

-- 2. comment 테이블에 target_type, target_id 컬럼 추가 (일반화된 댓글 지원)
-- post_id가 이미 있으므로, 추가 대상 타입 지원용
ALTER TABLE `comment`
    ADD COLUMN IF NOT EXISTS `target_type` VARCHAR(20) NULL COMMENT 'POST, VERIFICATION, QNA, DIARY' AFTER `parent_id`,
    ADD COLUMN IF NOT EXISTS `target_id` BIGINT NULL COMMENT '대상 엔티티 ID' AFTER `target_type`;

-- 3. comment 테이블에 target 인덱스 추가
CREATE INDEX IF NOT EXISTS `idx_comment_target` ON `comment` (`target_type`, `target_id`);
