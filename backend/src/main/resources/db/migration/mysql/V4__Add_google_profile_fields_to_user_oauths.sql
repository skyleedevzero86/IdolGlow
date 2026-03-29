-- =============================================================================
-- V4 (MySQL): user_oauths 프로필 필드
-- Google OAuth 등에서 내려주는 표시 이름·프로필 이미지 URL을 저장한다.
-- =============================================================================

-- OAuth 제공자가 넘긴 표시 이름(닉네임과 별도)
ALTER TABLE user_oauths
    ADD COLUMN profile_name VARCHAR(255) NULL COMMENT 'OAuth 프로필 표시 이름';

-- 프로필 썸네일·아바타 URL
ALTER TABLE user_oauths
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT 'OAuth 프로필 이미지 URL';
