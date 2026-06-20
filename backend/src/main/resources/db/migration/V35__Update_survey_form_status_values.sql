UPDATE survey_form
SET status = 'SCHEDULED'
WHERE status = 'PLANNED';

ALTER TABLE survey_form
    ALTER COLUMN status SET DEFAULT 'SCHEDULED';

COMMENT ON COLUMN survey_form.status IS '설문 진행 상태(SCHEDULED=예정, IN_PROGRESS=진행, COMPLETED=완료)';
