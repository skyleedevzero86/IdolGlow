ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT '사용자 지정 프로필 이미지 URL' AFTER nickname;
