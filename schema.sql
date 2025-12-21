-- =====================================================
-- Replant Database Schema
-- MariaDB 10.11+
-- =====================================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS replant
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE replant;

-- =====================================================
-- 기존 테이블 삭제 (역순으로)
-- =====================================================
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `chat_room`;
DROP TABLE IF EXISTS `user_recommendation`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `post`;
DROP TABLE IF EXISTS `mission_qna_answer`;
DROP TABLE IF EXISTS `mission_qna`;
DROP TABLE IF EXISTS `mission_review`;
DROP TABLE IF EXISTS `verification_vote`;
DROP TABLE IF EXISTS `mission_verification`;
DROP TABLE IF EXISTS `verification_post`;
DROP TABLE IF EXISTS `user_badge`;
DROP TABLE IF EXISTS `user_mission`;
DROP TABLE IF EXISTS `custom_mission`;
DROP TABLE IF EXISTS `mission`;
DROP TABLE IF EXISTS `reant`;
DROP TABLE IF EXISTS `user_oauth`;
DROP TABLE IF EXISTS `user`;

-- =====================================================
-- 1. USER DOMAIN
-- =====================================================

CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `nickname` VARCHAR(50) NOT NULL,
    `birth_date` DATE NULL,
    `gender` VARCHAR(10) NULL,
    `profile_img` VARCHAR(500) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_oauth` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(20) NOT NULL,
    `provider_id` VARCHAR(255) NOT NULL,
    `access_token` VARCHAR(500) NULL,
    `refresh_token` VARCHAR(500) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_provider_provider_id` (`provider`, `provider_id`),
    CONSTRAINT `fk_user_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reant` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `name` VARCHAR(50) NOT NULL DEFAULT '리앤트',
    `level` INT NOT NULL DEFAULT 1,
    `exp` INT NOT NULL DEFAULT 0,
    `stage` VARCHAR(20) NOT NULL DEFAULT 'EGG',
    `appearance` JSON NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_reant_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. MISSION DOMAIN
-- =====================================================

CREATE TABLE `mission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `verification_type` VARCHAR(20) NOT NULL,
    `gps_latitude` DECIMAL(10,8) NULL,
    `gps_longitude` DECIMAL(11,8) NULL,
    `gps_radius_meters` INT NULL DEFAULT 100,
    `required_minutes` INT NULL,
    `exp_reward` INT NOT NULL DEFAULT 10,
    `badge_duration_days` INT NOT NULL DEFAULT 3,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `custom_mission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `creator_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NOT NULL,
    `duration_days` INT NOT NULL,
    `is_public` BOOLEAN NOT NULL DEFAULT FALSE,
    `verification_type` VARCHAR(20) NOT NULL,
    `gps_latitude` DECIMAL(10,8) NULL,
    `gps_longitude` DECIMAL(11,8) NULL,
    `gps_radius_meters` INT NULL DEFAULT 100,
    `required_minutes` INT NULL,
    `exp_reward` INT NOT NULL DEFAULT 10,
    `badge_duration_days` INT NOT NULL DEFAULT 3,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_custom_mission_creator` FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_mission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `assigned_at` TIMESTAMP NOT NULL,
    `due_date` TIMESTAMP NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_mission_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_mission_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_mission_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. BADGE DOMAIN
-- =====================================================

CREATE TABLE `user_badge` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `user_mission_id` BIGINT NOT NULL UNIQUE,
    `issued_at` TIMESTAMP NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_badge_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_badge_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_badge_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_badge_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 4. VERIFICATION DOMAIN
-- =====================================================

CREATE TABLE `verification_post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `user_mission_id` BIGINT NOT NULL UNIQUE,
    `content` TEXT NOT NULL,
    `image_urls` JSON NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `approve_count` INT NOT NULL DEFAULT 0,
    `reject_count` INT NOT NULL DEFAULT 0,
    `verified_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_verification_post_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_verification_post_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mission_verification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_mission_id` BIGINT NOT NULL UNIQUE,
    `verification_post_id` BIGINT NULL UNIQUE,
    `gps_latitude` DECIMAL(10,8) NULL,
    `gps_longitude` DECIMAL(11,8) NULL,
    `gps_distance_meters` INT NULL,
    `time_started_at` TIMESTAMP NULL,
    `time_ended_at` TIMESTAMP NULL,
    `time_actual_minutes` INT NULL,
    `verified_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mission_verification_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_verification_post` FOREIGN KEY (`verification_post_id`) REFERENCES `verification_post`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `verification_vote` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `verification_post_id` BIGINT NOT NULL,
    `voter_id` BIGINT NOT NULL,
    `vote` VARCHAR(10) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_post_voter` (`verification_post_id`, `voter_id`),
    CONSTRAINT `fk_verification_vote_post` FOREIGN KEY (`verification_post_id`) REFERENCES `verification_post`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_verification_vote_voter` FOREIGN KEY (`voter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 5. REVIEW & QNA DOMAIN
-- =====================================================

CREATE TABLE `mission_review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `mission_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `content` VARCHAR(200) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_mission_user` (`mission_id`, `user_id`),
    CONSTRAINT `fk_mission_review_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_review_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_review_badge` FOREIGN KEY (`badge_id`) REFERENCES `user_badge`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mission_qna` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `mission_id` BIGINT NOT NULL,
    `questioner_id` BIGINT NOT NULL,
    `question` TEXT NOT NULL,
    `is_resolved` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mission_qna_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_qna_questioner` FOREIGN KEY (`questioner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mission_qna_answer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `qna_id` BIGINT NOT NULL,
    `answerer_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_accepted` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mission_qna_answer_qna` FOREIGN KEY (`qna_id`) REFERENCES `mission_qna`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_qna_answer_answerer` FOREIGN KEY (`answerer_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_qna_answer_badge` FOREIGN KEY (`badge_id`) REFERENCES `user_badge`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 6. COMMUNITY DOMAIN
-- =====================================================

CREATE TABLE `post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `title` VARCHAR(100) NULL,
    `content` TEXT NOT NULL,
    `image_urls` JSON NULL,
    `has_valid_badge` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_post_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 7. SOCIAL DOMAIN
-- =====================================================

CREATE TABLE `user_recommendation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `recommended_user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `user_mission_id` BIGINT NOT NULL,
    `match_reason` JSON NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_recommendation_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_recommendation_recommended` FOREIGN KEY (`recommended_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_recommendation_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_recommendation_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_recommendation_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `recommendation_id` BIGINT NOT NULL UNIQUE,
    `user1_id` BIGINT NOT NULL,
    `user2_id` BIGINT NOT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `last_message_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_chat_room_recommendation` FOREIGN KEY (`recommendation_id`) REFERENCES `user_recommendation`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_room_user1` FOREIGN KEY (`user1_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_room_user2` FOREIGN KEY (`user2_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_chat_message_room` FOREIGN KEY (`room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 8. NOTIFICATION DOMAIN
-- =====================================================

CREATE TABLE `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `content` TEXT NULL,
    `reference_type` VARCHAR(20) NULL,
    `reference_id` BIGINT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 9. INDEXES
-- =====================================================

CREATE INDEX `idx_user_mission_user_status` ON `user_mission`(`user_id`, `status`);
CREATE INDEX `idx_user_mission_due_date` ON `user_mission`(`due_date`);
CREATE INDEX `idx_user_badge_user_expires` ON `user_badge`(`user_id`, `expires_at`);
CREATE INDEX `idx_verification_post_status` ON `verification_post`(`status`);
CREATE INDEX `idx_post_user` ON `post`(`user_id`);
CREATE INDEX `idx_post_mission` ON `post`(`mission_id`);
CREATE INDEX `idx_comment_post` ON `comment`(`post_id`);
CREATE INDEX `idx_chat_message_room` ON `chat_message`(`room_id`);
CREATE INDEX `idx_notification_user_read` ON `notification`(`user_id`, `is_read`);
CREATE INDEX `idx_user_recommendation_user_status` ON `user_recommendation`(`user_id`, `status`);
