CREATE TABLE IF NOT EXISTS survey_form_description (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL COMMENT '소속 설문지 ID(FK)',
    markdown TEXT NOT NULL COMMENT '설문 설명 마크다운',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_form_description_form
        FOREIGN KEY (survey_form_id)
        REFERENCES survey_form (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_survey_form_description_form UNIQUE (survey_form_id)
) COMMENT='설문 설명 마크다운 본문';

CREATE TABLE IF NOT EXISTS survey_form_description_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_description_id BIGINT NOT NULL COMMENT '소속 설문 설명 ID(FK)',
    display_order INT NOT NULL COMMENT '태그 노출 순서',
    tag_name VARCHAR(100) NOT NULL COMMENT '태그명',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_form_description_tag_description
        FOREIGN KEY (survey_form_description_id)
        REFERENCES survey_form_description (id)
        ON DELETE CASCADE
) COMMENT='설문 설명 태그 목록';

CREATE INDEX idx_survey_form_description_tag_description_id
    ON survey_form_description_tag (survey_form_description_id);

INSERT INTO survey_form_description (survey_form_id, markdown, created_at, updated_at)
SELECT f.id, f.description, COALESCE(f.created_at, CURRENT_TIMESTAMP(6)), COALESCE(f.updated_at, CURRENT_TIMESTAMP(6))
FROM survey_form f
WHERE f.description IS NOT NULL
  AND TRIM(f.description) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM survey_form_description d
      WHERE d.survey_form_id = f.id
  );
