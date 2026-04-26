ALTER TABLE user_survey
    ADD COLUMN IF NOT EXISTS visit_start_time VARCHAR(5),
    ADD COLUMN IF NOT EXISTS visit_end_time VARCHAR(5);

COMMENT ON COLUMN user_survey.visit_start_time IS '방문 희망 시작 시각(HH:mm, 예: 09:00)';
COMMENT ON COLUMN user_survey.visit_end_time IS '방문 희망 종료 시각(HH:mm, 예: 24:00)';
