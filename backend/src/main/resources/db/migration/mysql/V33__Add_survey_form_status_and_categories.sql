ALTER TABLE survey_form
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PLANNED' COMMENT '설문 진행 상태(PLANNED=계획, SCHEDULED=예정, IN_PROGRESS=진행)',
    ADD COLUMN primary_category VARCHAR(40) NOT NULL DEFAULT 'ALL' COMMENT '설문 1뎁스 카테고리',
    ADD COLUMN secondary_category VARCHAR(40) NULL COMMENT '설문 2뎁스 카테고리';
