-- ============================================================================
-- V2 MySQL 기준
-- ----------------------------------------------------------------------------
-- 목적
-- 1) 기존 운영 DB에서 누락 시 무중단 보정
-- 2) 문항형 설문 설문지 정의 + 사용자 응답 저장 모델 신규 도입
--
-- - 테이블은 책임 단위별로 분리
-- - FK + 인덱스로 조회 성능/정합성 보장
-- ============================================================================

-- 가격 컬럼 호환 보정
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS base_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT '상품 기본가(옵션 합산과 별도)';

-- - 설문 템플릿 제목/설명/활성 여부 저장
-- - 운영 중 무중단 설문 버전 교체를 지원
CREATE TABLE IF NOT EXISTS survey_form (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- - 문항 타입/필수 여부/표시 순서를 정규화하여 저장
-- - 설문 삭제 시 문항도 자동 정리되도록 적용
CREATE TABLE IF NOT EXISTS survey_question (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT NULL,
    question_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_question_form FOREIGN KEY (survey_form_id) REFERENCES survey_form(id) ON DELETE CASCADE
);

-- - 객관식 문항 선택지를 독립 저장해 재사용/정렬/검증 단순화
CREATE TABLE IF NOT EXISTS survey_question_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_question_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    option_text VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_question_option_question FOREIGN KEY (survey_question_id) REFERENCES survey_question(id) ON DELETE CASCADE
);

-- - 사용자 제출 헤더 누가 어떤 설문에 응답했는지 저장
-- - user FK를 통해 제출 주체 정합성 보장
CREATE TABLE IF NOT EXISTS survey_submission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_submission_form FOREIGN KEY (survey_form_id) REFERENCES survey_form(id) ON DELETE CASCADE,
    CONSTRAINT fk_survey_submission_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- - 문항 단위 답변 저장
-- - 주관식과 객관식을 분리해 확장성 확보
CREATE TABLE IF NOT EXISTS survey_answer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_submission_id BIGINT NOT NULL,
    survey_question_id BIGINT NOT NULL,
    answer_text TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_answer_submission FOREIGN KEY (survey_submission_id) REFERENCES survey_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_survey_answer_question FOREIGN KEY (survey_question_id) REFERENCES survey_question(id) ON DELETE CASCADE
);

-- - 객관식/복수선택의 최종 선택값 스냅샷
-- - 저장으로 과거 응답 재현성 보장
CREATE TABLE IF NOT EXISTS survey_answer_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_answer_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    option_text VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_answer_option_answer FOREIGN KEY (survey_answer_id) REFERENCES survey_answer(id) ON DELETE CASCADE
);

ALTER TABLE survey_form
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '설문지 PK',
    MODIFY COLUMN title VARCHAR(200) NOT NULL COMMENT '설문지 제목',
    MODIFY COLUMN description TEXT NULL COMMENT '설문지 설명',
    MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부(true=현재 운영 설문)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';
ALTER TABLE survey_form COMMENT = '설문지 메타 정보(제목/설명/활성 상태)';

ALTER TABLE survey_question
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '문항 PK',
    MODIFY COLUMN survey_form_id BIGINT NOT NULL COMMENT '소속 설문지 ID(FK)',
    MODIFY COLUMN display_order INT NOT NULL COMMENT '설문 화면 노출 순서',
    MODIFY COLUMN title VARCHAR(300) NOT NULL COMMENT '문항 제목',
    MODIFY COLUMN description TEXT NULL COMMENT '문항 부가 설명',
    MODIFY COLUMN question_type VARCHAR(30) NOT NULL COMMENT '문항 타입(TEXT/SINGLE_CHOICE/MULTIPLE_CHOICE)',
    MODIFY COLUMN required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '필수 응답 여부',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';
ALTER TABLE survey_question COMMENT = '설문지에 속한 문항 정의';

ALTER TABLE survey_question_option
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택지 PK',
    MODIFY COLUMN survey_question_id BIGINT NOT NULL COMMENT '소속 문항 ID(FK)',
    MODIFY COLUMN display_order INT NOT NULL COMMENT '선택지 노출 순서',
    MODIFY COLUMN option_text VARCHAR(300) NOT NULL COMMENT '선택지 문구',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';
ALTER TABLE survey_question_option COMMENT = '객관식 문항의 선택지 집합';

ALTER TABLE survey_submission
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '제출 PK',
    MODIFY COLUMN survey_form_id BIGINT NOT NULL COMMENT '응답한 설문지 ID(FK)',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '응답 사용자 ID(FK)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '제출 생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '제출 수정 시각';
ALTER TABLE survey_submission COMMENT = '사용자 설문 제출 헤더';

ALTER TABLE survey_answer
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '응답 PK',
    MODIFY COLUMN survey_submission_id BIGINT NOT NULL COMMENT '소속 제출 ID(FK)',
    MODIFY COLUMN survey_question_id BIGINT NOT NULL COMMENT '응답 대상 문항 ID(FK)',
    MODIFY COLUMN answer_text TEXT NULL COMMENT '주관식 응답 텍스트',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';
ALTER TABLE survey_answer COMMENT = '제출 내 문항별 응답 본문';

ALTER TABLE survey_answer_option
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택 응답 PK',
    MODIFY COLUMN survey_answer_id BIGINT NOT NULL COMMENT '소속 응답 ID(FK)',
    MODIFY COLUMN display_order INT NOT NULL COMMENT '선택 순서(복수선택 지원)',
    MODIFY COLUMN option_text VARCHAR(300) NOT NULL COMMENT '선택된 문구 스냅샷',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';
ALTER TABLE survey_answer_option COMMENT = '객관식/복수선택 응답의 선택값';

-- - 관리자 조회 폼/문항 편집 + 사용자 조회 내 제출 이력  경로의
--   조인/필터 컬럼을 우선 인덱싱해 응답시간 안정화 목적
CREATE INDEX idx_survey_question_form_id ON survey_question(survey_form_id);
CREATE INDEX idx_survey_question_option_question_id ON survey_question_option(survey_question_id);
CREATE INDEX idx_survey_submission_form_id ON survey_submission(survey_form_id);
CREATE INDEX idx_survey_submission_user_id ON survey_submission(user_id);
CREATE INDEX idx_survey_answer_submission_id ON survey_answer(survey_submission_id);
CREATE INDEX idx_survey_answer_question_id ON survey_answer(survey_question_id);
CREATE INDEX idx_survey_answer_option_answer_id ON survey_answer_option(survey_answer_id);
