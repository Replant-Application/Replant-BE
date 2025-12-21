-- =====================================================
-- Replant Database Schema - Initial Migration
-- Version: 1.0.0
-- Description: Creates all tables for the Replant application
-- =====================================================

-- =====================================================
-- 1. Core Domain Tables (No Dependencies)
-- =====================================================

-- User Table
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `nickname` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NULL COMMENT 'NULL for OAuth users',
    `phone` VARCHAR(20) NULL,
    `birth_date` DATE NULL,
    `gender` VARCHAR(10) NULL COMMENT 'MALE, FEMALE',
    `profile_img` VARCHAR(500) NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER, GRADUATE, CONTRIBUTOR, ADMIN',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, SUSPENDED, DELETED',
    `last_login_at` TIMESTAMP NULL,
    `total_missions_completed` INT NOT NULL DEFAULT 0,
    `total_exp_gained` INT NOT NULL DEFAULT 0,
    `login_streak` INT NOT NULL DEFAULT 0,
    `last_login_date` DATE NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_email` (`email`),
    INDEX `idx_user_nickname` (`nickname`),
    INDEX `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정보';

-- Mission Table (System-defined missions)
CREATE TABLE `mission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'DAILY, WEEKLY, MONTHLY',
    `verification_type` VARCHAR(20) NOT NULL COMMENT 'COMMUNITY, GPS, TIME',
    `gps_latitude` DECIMAL(10,8) NULL,
    `gps_longitude` DECIMAL(11,8) NULL,
    `gps_radius_meters` INT NULL DEFAULT 100,
    `required_minutes` INT NULL,
    `exp_reward` INT NOT NULL DEFAULT 10,
    `badge_duration_days` INT NOT NULL DEFAULT 3,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_mission_type` (`type`),
    INDEX `idx_mission_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='시스템 미션';

-- Card Table (Legacy - for gamification)
CREATE TABLE `card` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `card_type` VARCHAR(20) NOT NULL COMMENT 'DAILY, WEEKLY, MONTHLY, SPECIAL',
    `image_url` VARCHAR(255) NULL,
    `points_required` INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카드 (레거시)';

-- =====================================================
-- 2. User-Related Tables
-- =====================================================

-- User OAuth Table
CREATE TABLE `user_oauth` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(20) NOT NULL COMMENT 'KAKAO, GOOGLE, APPLE, NAVER',
    `provider_id` VARCHAR(255) NOT NULL,
    `access_token` VARCHAR(500) NULL,
    `refresh_token` VARCHAR(500) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_provider_provider_id` UNIQUE (`provider`, `provider_id`),
    INDEX `idx_user_oauth_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 OAuth 정보';

-- Reant Table (Virtual Pet)
CREATE TABLE `reant` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `name` VARCHAR(50) NOT NULL DEFAULT '리앤트',
    `level` INT NOT NULL DEFAULT 1,
    `exp` INT NOT NULL DEFAULT 0,
    `stage` VARCHAR(20) NOT NULL DEFAULT 'EGG' COMMENT 'EGG, BABY, TEEN, ADULT',
    `max_level` INT NOT NULL DEFAULT 100,
    `mood` INT NOT NULL DEFAULT 100 COMMENT '0-100',
    `health` INT NOT NULL DEFAULT 100 COMMENT '0-100',
    `hunger` INT NOT NULL DEFAULT 0 COMMENT '0-100, higher means more hungry',
    `appearance` JSON NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_reant_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_reant_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가상 펫 (리앤트)';

-- Custom Mission Table (User-created missions)
CREATE TABLE `custom_mission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `creator_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NOT NULL,
    `duration_days` INT NOT NULL,
    `is_public` BOOLEAN NOT NULL DEFAULT FALSE,
    `verification_type` VARCHAR(20) NOT NULL COMMENT 'COMMUNITY, GPS, TIME',
    `gps_latitude` DECIMAL(10,8) NULL,
    `gps_longitude` DECIMAL(11,8) NULL,
    `gps_radius_meters` INT NULL DEFAULT 100,
    `required_minutes` INT NULL,
    `exp_reward` INT NOT NULL DEFAULT 10,
    `badge_duration_days` INT NOT NULL DEFAULT 3,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_custom_mission_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_custom_mission_creator_id` (`creator_id`),
    INDEX `idx_custom_mission_is_public` (`is_public`),
    INDEX `idx_custom_mission_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정의 미션';

-- =====================================================
-- 3. Mission Assignment and Verification Tables
-- =====================================================

-- User Mission Table (Mission assignments to users)
CREATE TABLE `user_mission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL COMMENT 'System mission reference',
    `custom_mission_id` BIGINT NULL COMMENT 'Custom mission reference',
    `assigned_at` TIMESTAMP NOT NULL,
    `due_date` TIMESTAMP NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED, PENDING, COMPLETED, EXPIRED',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_mission_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_mission_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_mission_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission` (`id`) ON DELETE CASCADE,
    INDEX `idx_user_mission_user_id` (`user_id`),
    INDEX `idx_user_mission_mission_id` (`mission_id`),
    INDEX `idx_user_mission_custom_mission_id` (`custom_mission_id`),
    INDEX `idx_user_mission_status` (`status`),
    INDEX `idx_user_mission_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 미션 할당';

-- Verification Post Table
CREATE TABLE `verification_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `user_mission_id` BIGINT NOT NULL UNIQUE,
    `content` TEXT NOT NULL,
    `image_urls` JSON NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    `approve_count` INT NOT NULL DEFAULT 0,
    `reject_count` INT NOT NULL DEFAULT 0,
    `verified_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_verification_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_verification_post_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission` (`id`) ON DELETE CASCADE,
    INDEX `idx_verification_post_user_id` (`user_id`),
    INDEX `idx_verification_post_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='미션 인증 게시물';

-- Mission Verification Table
CREATE TABLE `mission_verification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    CONSTRAINT `fk_mission_verification_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_verification_post` FOREIGN KEY (`verification_post_id`) REFERENCES `verification_post` (`id`) ON DELETE SET NULL,
    INDEX `idx_mission_verification_verified_at` (`verified_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='미션 인증 정보';

-- Verification Vote Table
CREATE TABLE `verification_vote` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `verification_post_id` BIGINT NOT NULL,
    `voter_id` BIGINT NOT NULL,
    `vote` VARCHAR(10) NOT NULL COMMENT 'APPROVE, REJECT',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_verification_vote_post` FOREIGN KEY (`verification_post_id`) REFERENCES `verification_post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_verification_vote_voter` FOREIGN KEY (`voter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_post_voter` UNIQUE (`verification_post_id`, `voter_id`),
    INDEX `idx_verification_vote_post_id` (`verification_post_id`),
    INDEX `idx_verification_vote_voter_id` (`voter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인증 투표';

-- =====================================================
-- 4. Badge and Achievement Tables
-- =====================================================

-- User Badge Table
CREATE TABLE `user_badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `user_mission_id` BIGINT NOT NULL UNIQUE,
    `issued_at` TIMESTAMP NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_badge_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_badge_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_badge_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_badge_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission` (`id`) ON DELETE CASCADE,
    INDEX `idx_user_badge_user_id` (`user_id`),
    INDEX `idx_user_badge_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 뱃지';

-- =====================================================
-- 5. Social and Community Tables
-- =====================================================

-- Mission Review Table
CREATE TABLE `mission_review` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `mission_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `content` VARCHAR(200) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mission_review_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_review_badge` FOREIGN KEY (`badge_id`) REFERENCES `user_badge` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_mission_user` UNIQUE (`mission_id`, `user_id`),
    INDEX `idx_mission_review_mission_id` (`mission_id`),
    INDEX `idx_mission_review_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='미션 리뷰';

-- Mission Q&A Table
CREATE TABLE `mission_qna` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `mission_id` BIGINT NOT NULL,
    `questioner_id` BIGINT NOT NULL,
    `question` TEXT NOT NULL,
    `is_resolved` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mission_qna_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_qna_questioner` FOREIGN KEY (`questioner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_mission_qna_mission_id` (`mission_id`),
    INDEX `idx_mission_qna_is_resolved` (`is_resolved`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='미션 Q&A';

-- Mission Q&A Answer Table
CREATE TABLE `mission_qna_answer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `qna_id` BIGINT NOT NULL,
    `answerer_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_accepted` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mission_qna_answer_qna` FOREIGN KEY (`qna_id`) REFERENCES `mission_qna` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_qna_answer_answerer` FOREIGN KEY (`answerer_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mission_qna_answer_badge` FOREIGN KEY (`badge_id`) REFERENCES `user_badge` (`id`) ON DELETE CASCADE,
    INDEX `idx_mission_qna_answer_qna_id` (`qna_id`),
    INDEX `idx_mission_qna_answer_answerer_id` (`answerer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='미션 Q&A 답변';

-- Post Table (Community posts)
CREATE TABLE `post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `title` VARCHAR(100) NULL,
    `content` TEXT NOT NULL,
    `image_urls` JSON NULL,
    `has_valid_badge` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_post_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission` (`id`) ON DELETE SET NULL,
    INDEX `idx_post_user_id` (`user_id`),
    INDEX `idx_post_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='커뮤니티 게시물';

-- Comment Table
CREATE TABLE `comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_comment_post_id` (`post_id`),
    INDEX `idx_comment_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='댓글';

-- =====================================================
-- 6. Recommendation and Chat Tables
-- =====================================================

-- User Recommendation Table
CREATE TABLE `user_recommendation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `recommended_user_id` BIGINT NOT NULL,
    `mission_id` BIGINT NULL,
    `custom_mission_id` BIGINT NULL,
    `user_mission_id` BIGINT NOT NULL,
    `match_reason` JSON NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, ACCEPTED, REJECTED, EXPIRED',
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_recommendation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_recommendation_recommended` FOREIGN KEY (`recommended_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_recommendation_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_recommendation_custom_mission` FOREIGN KEY (`custom_mission_id`) REFERENCES `custom_mission` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_recommendation_user_mission` FOREIGN KEY (`user_mission_id`) REFERENCES `user_mission` (`id`) ON DELETE CASCADE,
    INDEX `idx_user_recommendation_user_id` (`user_id`),
    INDEX `idx_user_recommendation_recommended_id` (`recommended_user_id`),
    INDEX `idx_user_recommendation_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 추천';

-- Chat Room Table
CREATE TABLE `chat_room` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recommendation_id` BIGINT NOT NULL UNIQUE,
    `user1_id` BIGINT NOT NULL,
    `user2_id` BIGINT NOT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `last_message_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_chat_room_recommendation` FOREIGN KEY (`recommendation_id`) REFERENCES `user_recommendation` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_room_user1` FOREIGN KEY (`user1_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_room_user2` FOREIGN KEY (`user2_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_chat_room_user1_id` (`user1_id`),
    INDEX `idx_chat_room_user2_id` (`user2_id`),
    INDEX `idx_chat_room_last_message_at` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅방';

-- Chat Message Table
CREATE TABLE `chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `room_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_chat_message_room` FOREIGN KEY (`room_id`) REFERENCES `chat_room` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_chat_message_room_id` (`room_id`),
    INDEX `idx_chat_message_sender_id` (`sender_id`),
    INDEX `idx_chat_message_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채팅 메시지';

-- =====================================================
-- 7. Notification Table
-- =====================================================

-- Notification Table
CREATE TABLE `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `content` TEXT NULL,
    `reference_type` VARCHAR(20) NULL,
    `reference_id` BIGINT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_notification_user_id` (`user_id`),
    INDEX `idx_notification_is_read` (`is_read`),
    INDEX `idx_notification_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림';

-- =====================================================
-- 8. Legacy Tables (Deprecated but kept for compatibility)
-- =====================================================

-- Member Table (DEPRECATED - Use User instead)
CREATE TABLE `member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `member_id` VARCHAR(255) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `member_name` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(255) NOT NULL,
    `birth_date` VARCHAR(255) NULL,
    `birth_back` VARCHAR(255) NULL,
    `status` VARCHAR(20) NOT NULL COMMENT 'ABLE, UNABLE, DISABLED, SUSPENDED',
    `authority` VARCHAR(20) NOT NULL COMMENT 'USER, ADMIN',
    `created_at` TIMESTAMP NULL,
    `updated_at` TIMESTAMP NULL,
    `last_login_at` TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 (레거시)';

-- Goal Table (Legacy)
CREATE TABLE `goal` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `member_id` BIGINT NULL,
    `goal_job` VARCHAR(20) NULL COMMENT 'STUDENT, OFFICE_WORKER, FREELANCER, ENTREPRENEUR, HOUSEWIFE, UNEMPLOYED, OTHER',
    `goal_start_date` DATE NULL,
    `goal_income` VARCHAR(255) NULL,
    `previous_goal_money` INT NULL,
    CONSTRAINT `fk_goal_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='목표 (레거시)';

-- Member Card Table (Legacy)
CREATE TABLE `member_card` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `member_id` BIGINT NOT NULL,
    `card_id` BIGINT NOT NULL,
    `acquired_at` TIMESTAMP NOT NULL,
    `is_new` BOOLEAN NULL DEFAULT TRUE,
    CONSTRAINT `fk_member_card_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_member_card_card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`) ON DELETE CASCADE,
    INDEX `idx_member_card_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 카드 (레거시)';

-- Refresh Token Table
CREATE TABLE `refresh_token` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `token_key` VARCHAR(255) NOT NULL UNIQUE COMMENT 'User identifier',
    `token_value` VARCHAR(255) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_refresh_token_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리프레시 토큰';

-- =====================================================
-- Migration Complete
-- =====================================================
