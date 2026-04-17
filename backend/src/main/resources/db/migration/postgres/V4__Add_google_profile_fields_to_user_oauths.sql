ALTER TABLE user_oauths
    ADD COLUMN profile_name VARCHAR(255);

ALTER TABLE user_oauths
    ADD COLUMN profile_image_url VARCHAR(500);
