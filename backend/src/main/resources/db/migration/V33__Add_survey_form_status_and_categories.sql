ALTER TABLE survey_form
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    ADD COLUMN IF NOT EXISTS primary_category VARCHAR(40) NOT NULL DEFAULT 'ALL',
    ADD COLUMN IF NOT EXISTS secondary_category VARCHAR(40);

COMMENT ON COLUMN survey_form.status IS '설문 진행 상태(PLANNED=계획, SCHEDULED=예정, IN_PROGRESS=진행)';
COMMENT ON COLUMN survey_form.primary_category IS '설문 1뎁스 카테고리';
COMMENT ON COLUMN survey_form.secondary_category IS '설문 2뎁스 카테고리';
