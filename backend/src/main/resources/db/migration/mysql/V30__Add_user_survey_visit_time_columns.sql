ALTER TABLE user_survey
    ADD COLUMN IF NOT EXISTS visit_start_time VARCHAR(5),
    ADD COLUMN IF NOT EXISTS visit_end_time VARCHAR(5);
