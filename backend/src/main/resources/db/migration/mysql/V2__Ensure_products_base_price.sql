-- ============================================================================
-- V2 통합 마이그레이션 (MySQL)
-- ----------------------------------------------------------------------------
-- 목적
--   1) products.base_price 컬럼 누락 시 idempotent 보정
--   2) 문항형 설문(Form → Question → Option / Submission → Answer) 스키마 도입
--   3) 상품 추천 운영 플래그/점수 + latest-in-korea 수동 큐레이션 테이블
--
-- 설계 메모
--   - 테이블 책임 단위 분리 + FK·인덱스로 정합성·조회 성능 확보
--   - CREATE IF NOT EXISTS 로 배포 리스크 최소화
--   - 추천 컬럼·latest-korea: ADD + UPDATE 백필 + MODIFY 로 NOT NULL 정합
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [A] products — 가격 컬럼
-- ----------------------------------------------------------------------------

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS base_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00
        COMMENT '상품 기본가(옵션 합산과 별도)';

-- ----------------------------------------------------------------------------
-- [A2] products — 추천 운영 컬럼 + latest-in-korea 큐레이션
-- ----------------------------------------------------------------------------

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_recommended BOOLEAN NOT NULL DEFAULT FALSE COMMENT '관리자 추천 노출 여부';

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS recommendation_score INT NOT NULL DEFAULT 0 COMMENT '관리자 수동 추천 점수(높을수록 우선)';

UPDATE products SET is_recommended = COALESCE(is_recommended, FALSE) WHERE is_recommended IS NULL;
UPDATE products SET recommendation_score = COALESCE(recommendation_score, 0) WHERE recommendation_score IS NULL;
ALTER TABLE products
    MODIFY COLUMN is_recommended BOOLEAN NOT NULL DEFAULT FALSE COMMENT '관리자 추천 노출 여부';
ALTER TABLE products
    MODIFY COLUMN recommendation_score INT NOT NULL DEFAULT 0 COMMENT '관리자 수동 추천 점수(높을수록 우선)';

CREATE TABLE IF NOT EXISTS product_latest_korea_recommendation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'PK',
    display_order INT NOT NULL COMMENT '화면 노출 순서(1부터 증가)',
    product_id BIGINT NOT NULL COMMENT '추천 대상 상품 ID',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_latest_korea_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_latest_korea_display_order UNIQUE (display_order),
    CONSTRAINT uk_latest_korea_product_id UNIQUE (product_id)
) COMMENT='latest-in-korea 추천 상품 수동 큐레이션 순서';

CREATE INDEX idx_latest_korea_product_id
    ON product_latest_korea_recommendation(product_id);

-- ----------------------------------------------------------------------------
-- [B] structured survey — 테이블 DDL (의존 순서)
-- ----------------------------------------------------------------------------

-- survey_form: 설문 템플릿(제목·설명·활성). active 로 버전 전환 운영 가능.
CREATE TABLE IF NOT EXISTS survey_form (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- survey_question: 문항 정의(순서·타입·필수). form 삭제 시 CASCADE.
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
    CONSTRAINT fk_survey_question_form
        FOREIGN KEY (survey_form_id)
        REFERENCES survey_form (id)
        ON DELETE CASCADE
);

-- survey_question_option: 객관식 선택지. display_order 로 UI 순서 고정.
CREATE TABLE IF NOT EXISTS survey_question_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_question_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    option_text VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_question_option_question
        FOREIGN KEY (survey_question_id)
        REFERENCES survey_question (id)
        ON DELETE CASCADE
);

-- survey_submission: 사용자 1회 제출 헤더(form + user FK).
CREATE TABLE IF NOT EXISTS survey_submission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_submission_form
        FOREIGN KEY (survey_form_id)
        REFERENCES survey_form (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_survey_submission_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

-- survey_answer: 문항별 응답 본문(TEXT는 answer_text, 객관식은 answer_option 확장).
CREATE TABLE IF NOT EXISTS survey_answer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_submission_id BIGINT NOT NULL,
    survey_question_id BIGINT NOT NULL,
    answer_text TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_answer_submission
        FOREIGN KEY (survey_submission_id)
        REFERENCES survey_submission (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_survey_answer_question
        FOREIGN KEY (survey_question_id)
        REFERENCES survey_question (id)
        ON DELETE CASCADE
);

-- survey_answer_option: 객관식·복수선택 스냅샷(option_text).
CREATE TABLE IF NOT EXISTS survey_answer_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_answer_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    option_text VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_answer_option_answer
        FOREIGN KEY (survey_answer_id)
        REFERENCES survey_answer (id)
        ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- [C] structured survey — 테이블·컬럼 코멘트 (MySQL: MODIFY + TABLE COMMENT)
-- ----------------------------------------------------------------------------

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

-- ----------------------------------------------------------------------------
-- [D] structured survey — 인덱스 (FK 조인·목록 조회)
-- ----------------------------------------------------------------------------

CREATE INDEX idx_survey_question_form_id
    ON survey_question (survey_form_id);

CREATE INDEX idx_survey_question_option_question_id
    ON survey_question_option (survey_question_id);

CREATE INDEX idx_survey_submission_form_id
    ON survey_submission (survey_form_id);

CREATE INDEX idx_survey_submission_user_id
    ON survey_submission (user_id);

CREATE INDEX idx_survey_answer_submission_id
    ON survey_answer (survey_submission_id);

CREATE INDEX idx_survey_answer_question_id
    ON survey_answer (survey_question_id);

CREATE INDEX idx_survey_answer_option_answer_id
    ON survey_answer_option (survey_answer_id);
