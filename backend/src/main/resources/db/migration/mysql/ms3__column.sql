-- =============================================================================
-- column: 컬럼·제약·인덱스
-- -----
-- 목적
-- - 컬럼 추가·변경, 제약, 인덱스를 한 파일에 모은다.
-- 대상
-- - ms
-- 주의
-- - 새 데이터베이스는 setting, create, column, comment, insert, select 순서로 실행한다
-- - 이미 반영된 데이터베이스에는 이 묶음을 다시 실행하지 않는다
-- =============================================================================

-- 이메일 로그인·조회용
CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_user_oauths_user_id ON user_oauths (user_id);

CREATE INDEX idx_user_oauths_provider_provider_id ON user_oauths (provider, provider_id);

CREATE INDEX idx_user_oauths_email ON user_oauths (email);

CREATE INDEX idx_product_option_product_id ON product_option (product_id);

CREATE INDEX idx_product_option_option_id ON product_option (option_id);

CREATE INDEX idx_product_tag_tag_name ON product_tag (tag_name);

CREATE INDEX idx_product_tag_product_id ON product_tag (product_id);

CREATE INDEX idx_reservation_slots_product_id ON reservation_slots (product_id);

CREATE INDEX idx_reservations_user_visit_time ON reservations (user_id, visit_date, visit_start_time);

CREATE INDEX idx_reservations_reservation_slot_id ON reservations (reservation_slot_id);

CREATE INDEX idx_product_reviews_product_id ON product_reviews (product_id);

CREATE INDEX idx_product_reviews_user_id ON product_reviews (user_id);

CREATE INDEX idx_images_aggregate ON images (aggregate_type, aggregate_id, sort_order);

CREATE INDEX idx_user_survey_user_id ON user_survey (user_id);

CREATE INDEX idx_user_survey_places_survey_id ON user_survey_places (user_survey_id);

CREATE INDEX idx_wishes_user_id ON wishes (user_id);

CREATE INDEX idx_wishes_product_id ON wishes (product_id);

CREATE INDEX idx_schedule_user_start ON schedules (user_id, start_at);

-- 예약·결제·알림·보안 확장
-- ---
-- users: ADMIN 역할 추가
-- ---
ALTER TABLE users DROP CHECK chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- ---
-- reservation_slots: PENDING 예약이 슬롯을 점유할 때 hold 정보
-- hold_reservation_id: 잠금을 건 예약 PK
-- hold_expires_at: hold 만료 시각
-- ---
ALTER TABLE reservation_slots ADD COLUMN hold_reservation_id BIGINT NULL COMMENT 'hold를 건 reservations.id';

ALTER TABLE reservation_slots ADD COLUMN hold_expires_at DATETIME(6) NULL COMMENT 'hold 만료 시각';

ALTER TABLE reservation_slots
    ADD CONSTRAINT fk_reservation_slots_hold_reservation FOREIGN KEY (hold_reservation_id) REFERENCES reservations (id);

-- hold 만료 스캔·조회용
CREATE INDEX idx_reservation_slots_hold ON reservation_slots (hold_reservation_id, hold_expires_at);

-- ---
-- reservations: 결제·만료·취소 도메인 필드
-- ---
ALTER TABLE reservations ADD COLUMN expires_at DATETIME(6) NULL COMMENT 'PENDING 예약 자동 만료 시각';

ALTER TABLE reservations ADD COLUMN confirmed_at DATETIME(6) NULL COMMENT '결제 성공 후 확정 시각';

ALTER TABLE reservations ADD COLUMN canceled_at DATETIME(6) NULL COMMENT '취소 시각';

ALTER TABLE reservations ADD COLUMN cancel_reason VARCHAR(40) NULL COMMENT 'USER_REQUESTED/PAYMENT_FAILED/PAYMENT_EXPIRED/ADMIN_CANCELED';

ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_cancel_reason CHECK (
        cancel_reason IS NULL OR cancel_reason IN ('USER_REQUESTED', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED', 'ADMIN_CANCELED')
    );

-- 만료 배치·상태 조회용
CREATE INDEX idx_reservations_status_expires_at ON reservations (status, expires_at);

-- 상태별 후처리·모니터링용
CREATE INDEX idx_payments_status_updated_at ON payments (status, updated_at);

-- 목록 조회 미읽음 우선 등 용
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at);

-- user_oauths 프로필 필드
-- OAuth 제공자가 넘긴 표시 이름
ALTER TABLE user_oauths
    ADD COLUMN profile_name VARCHAR(255) NULL COMMENT 'OAuth 프로필 표시 이름';

-- 프로필 썸네일·아바타 URL
ALTER TABLE user_oauths
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT 'OAuth 프로필 이미지 URL';

ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT '사용자 지정 프로필 이미지 URL' AFTER nickname;

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '로컬 가입 비밀번호 BCrypt, OAuth만 연동 시 NULL';

DROP INDEX idx_users_email ON users;

CREATE UNIQUE INDEX uq_users_email ON users (email);

CREATE UNIQUE INDEX uq_users_nickname ON users (nickname);

-- 리뷰 신뢰도: 예약 연동, 도움돼요, 신고/숨김

ALTER TABLE product_reviews
    ADD COLUMN reservation_id BIGINT NULL COMMENT '방문 완료 예약 FK 인증 리뷰',
    ADD COLUMN helpful_count BIGINT NOT NULL DEFAULT 0 COMMENT '도움돼요 수',
    ADD COLUMN hidden_at DATETIME(6) NULL COMMENT '비공개 처리 시각',
    ADD COLUMN hidden_reason VARCHAR(80) NULL COMMENT '숨김 사유';

ALTER TABLE product_reviews
    ADD CONSTRAINT fk_product_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id);

CREATE UNIQUE INDEX uk_product_reviews_reservation_id ON product_reviews (reservation_id);

CREATE INDEX idx_helpful_review_id ON product_review_helpful_votes (review_id);

CREATE INDEX idx_report_review_id ON product_review_reports (review_id);

CREATE INDEX idx_rsw_slot ON reservation_slot_waitlist (reservation_slot_id);

CREATE INDEX idx_rsw_user ON reservation_slot_waitlist (user_id);

-- 결제대행사 연동: payments 확장과 payment_logs, payment_refunds

ALTER TABLE payments DROP CONSTRAINT chk_payments_provider;

ALTER TABLE payments DROP CONSTRAINT chk_payments_status;

ALTER TABLE payments ADD COLUMN payment_no VARCHAR(64) NULL COMMENT '내부 결제 번호 유니크';

ALTER TABLE payments ADD COLUMN order_id VARCHAR(200) NULL COMMENT '가맹점 주문 식별자. 대행사 orderId와 맞추며 유일해야 한다';

ALTER TABLE payments ADD COLUMN payment_key VARCHAR(200) NULL COMMENT '결제대행사가 내려준 결제 건 고유 키. 승인이 끝난 뒤 채운다.';

ALTER TABLE payments ADD COLUMN order_name VARCHAR(255) NULL COMMENT '주문명. 대행사 orderName에 대응';

ALTER TABLE payments ADD COLUMN supplied_amount DECIMAL(15, 2) NULL COMMENT '공급가액';

ALTER TABLE payments ADD COLUMN vat DECIMAL(15, 2) NULL COMMENT '부가세';

ALTER TABLE payments ADD COLUMN tax_free_amount DECIMAL(15, 2) NULL COMMENT '비과세 금액';

ALTER TABLE payments ADD COLUMN currency VARCHAR(10) NULL COMMENT '통화 코드. ISO 4217 규칙을 따른다.';

ALTER TABLE payments ADD COLUMN gateway_method VARCHAR(50) NULL COMMENT '결제 수단 요약. 대행사 method에 대응';

ALTER TABLE payments ADD COLUMN gateway_type VARCHAR(50) NULL COMMENT '결제 유형 코드. NORMAL 등';

ALTER TABLE payments ADD COLUMN external_status VARCHAR(50) NULL COMMENT '대행사가 내려준 결제 상태 문자열';

ALTER TABLE payments ADD COLUMN requested_at DATETIME(6) NULL COMMENT '결제 요청 시각';

ALTER TABLE payments ADD COLUMN last_transaction_key VARCHAR(200) NULL COMMENT '마지막 거래 식별 키';

ALTER TABLE payments ADD COLUMN fail_code VARCHAR(100) NULL COMMENT '실패 코드';

ALTER TABLE payments ADD COLUMN canceled_at DATETIME(6) NULL COMMENT '전액 취소 완료 시각 내부 마킹';

ALTER TABLE payments ADD COLUMN cancel_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '누적 취소 환불 금액';

ALTER TABLE payments ADD COLUMN card_company VARCHAR(100) NULL COMMENT '카드사 이름';

ALTER TABLE payments ADD COLUMN card_number VARCHAR(50) NULL COMMENT '마스킹된 카드번호';

ALTER TABLE payments ADD COLUMN installment_plan_months INT NULL COMMENT '할부 개월 수';

ALTER TABLE payments ADD COLUMN is_interest_free TINYINT(1) NULL COMMENT '무이자 여부';

ALTER TABLE payments ADD COLUMN virtual_account_bank VARCHAR(100) NULL COMMENT '가상계좌 은행';

ALTER TABLE payments ADD COLUMN virtual_account_number VARCHAR(100) NULL COMMENT '가상계좌 번호';

ALTER TABLE payments ADD COLUMN virtual_account_due_date DATETIME(6) NULL COMMENT '가상계좌 입금 만료 시각';

ALTER TABLE payments ADD COLUMN easy_pay_provider VARCHAR(100) NULL COMMENT '간편결제 제공자';

ALTER TABLE payments ADD COLUMN raw_response_json JSON NULL COMMENT '승인 또는 오류 응답 본문 JSON';

ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) NULL COMMENT '승인 요청마다 다른 중복 방지 키. 유일해야 한다';

ALTER TABLE payments MODIFY COLUMN payment_no VARCHAR(64) NOT NULL COMMENT '내부 결제 번호 유니크';

ALTER TABLE payments MODIFY COLUMN order_id VARCHAR(200) NOT NULL COMMENT '가맹점 주문 식별자. 대행사 orderId와 맞추며 유일해야 한다';

ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_no UNIQUE (payment_no);

ALTER TABLE payments ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);

ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_key UNIQUE (payment_key);

ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('MOCK', 'TOSS'));

ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (
    status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'EXPIRED', 'REFUNDED', 'PARTIAL_CANCELED')
);

CREATE INDEX idx_payments_external_status ON payments (external_status);

ALTER TABLE payments COMMENT = '예약 단위 결제 정보와 상태 이력. 결제대행사 승인·환불·응답 본문·중복 방지 키를 담는다.';

CREATE INDEX idx_payment_logs_payment_id ON payment_logs (payment_id);

CREATE INDEX idx_payment_logs_order_id ON payment_logs (order_id);

CREATE INDEX idx_payment_logs_created_at ON payment_logs (created_at);

CREATE INDEX idx_payment_refunds_payment_id ON payment_refunds (payment_id);

CREATE INDEX idx_payment_refunds_reservation_id ON payment_refunds (reservation_id);

CREATE INDEX idx_payment_refunds_status ON payment_refunds (status);

-- 알림 설정 테이블 + 만료 예정 알림 타입
ALTER TABLE notifications DROP CHECK chk_notifications_type;

ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type CHECK (
    type IN (
        'RESERVATION_CONFIRMED',
        'RESERVATION_CANCELED',
        'PAYMENT_FAILED',
        'PAYMENT_EXPIRED',
        'RESERVATION_SLOT_AVAILABLE',
        'RESERVATION_EXPIRING_SOON'
    )
);

CREATE INDEX idx_np_user_id ON notification_preferences (user_id);

CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs (created_at DESC);

CREATE INDEX idx_admin_audit_logs_admin_user ON admin_audit_logs (admin_user_id);

-- 리뷰 반응·신고 테이블에 수정 시각 컬럼이 없으면 추가한다. ORM 검증과 데이터 정합성을 위해 필요하다.
-- 기존 행은 마이그레이션 시점의 CURRENT_TIMESTAMP 6 로 채우고, 이후 변경 시 애플리케이션에서 갱신한다.

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각 감사·낙관적 락 등 공통 엔티티 필드';

ALTER TABLE product_review_reports
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각 감사·낙관적 락 등 공통 엔티티 필드';

-- 이슈별 기사 목록 조회
create index idx_webzine_articles_issue_id on webzine_articles(issue_id);

create index idx_newsletters_published_at on newsletters(published_at);

create index idx_email_subscriptions_active on email_subscriptions(active);

create index idx_subscription_dispatch_histories_dispatched_at on subscription_dispatch_histories(dispatched_at);

create index editor_documents_updated_at_idx
    on editor_documents (updated_at desc);

create index editor_documents_publication_status_idx
    on editor_documents (publication_status, updated_at desc);

create index editor_documents_url_slug_idx
    on editor_documents (url_slug);

create index editor_assets_uploaded_at_idx
    on editor_assets (uploaded_at desc);

-- 플랫폼 인증용 users 확장·비밀번호 이력·역할
ALTER TABLE users DROP CHECK chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
    COMMENT '계정 승인·정지 상태 PENDING, APPROVED, REJECTED, SUSPENDED, WITHDRAWN';

ALTER TABLE users ADD COLUMN platform_username VARCHAR(50) NULL
    COMMENT '플랫폼 가입 시 로그인명 계정 복구 시 이메일과 함께 사용';

ALTER TABLE users ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0
    COMMENT '연속 로그인 실패 횟수';

ALTER TABLE users ADD COLUMN account_locked_at TIMESTAMP NULL
    COMMENT '계정 잠금 처리 시각';

ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP NULL
    COMMENT '마지막 비밀번호 변경 시각';

ALTER TABLE users ADD COLUMN last_password_change_date DATE NULL
    COMMENT '당일 비밀번호 변경 횟수 집계 기준일';

ALTER TABLE users ADD COLUMN password_change_daily_count INT NOT NULL DEFAULT 0
    COMMENT '기준일 당일 비밀번호 변경 횟수';

CREATE UNIQUE INDEX uq_users_platform_username ON users (platform_username);

CREATE INDEX idx_user_password_history_user_created ON user_password_history (user_id, created_at DESC);

ALTER TABLE users DROP CHECK chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users MODIFY COLUMN role VARCHAR(255) NOT NULL COMMENT '권한 역할 USER: 일반, ADMIN: 관리자';

create index idx_subscription_dispatch_schedules_active on subscription_dispatch_schedules (active);

CREATE INDEX idx_auth_verification_audit_logs_created_at
    ON auth_verification_audit_logs (created_at DESC);

CREATE INDEX idx_auth_verification_audit_logs_type_created
    ON auth_verification_audit_logs (verification_type, created_at DESC);

CREATE INDEX idx_signup_verification_tokens_email_type_status_created
    ON signup_verification_tokens (email, type, status, created_at DESC);

-- users 임시 비밀번호 발급 시각
-- User 엔티티 temporary_password_issued_at 과 동일.
-- MySQL 은 ADD COLUMN IF NOT EXISTS 가 버전에 따라 없을 수 있어 단순 ADD 만 사용한다.
ALTER TABLE users
    ADD COLUMN temporary_password_issued_at DATETIME(6) NULL
        COMMENT '플랫폼 임시 비밀번호를 마지막으로 발급한 시각. 미발급이면 NULL';

-- users: 다음 로그인에서 비밀번호를 바꾸게 할지 여부
-- 기본값은 false 이다.
ALTER TABLE users
    ADD COLUMN temporary_password_required BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '임시 비밀번호 발급 등으로 비밀번호 변경이 필요하면 1';

-- ============================================================================
-- 통합 마이그레이션 MySQL
-- ----------------------------------------------------------------------------
-- 목적
-- 1 products.base_price 컬럼 누락 시 idempotent 보정
-- 2 문항형 설문 Form → Question → Option / Submission → Answer 스키마 도입
-- 3 상품 추천 운영 플래그/점수 + latest-in-korea 수동 큐레이션 테이블
--
-- 설계 메모
-- - 테이블 책임 단위 분리 + FK·인덱스로 정합성·조회 성능 확보
-- - CREATE IF NOT EXISTS 로 배포 리스크 최소화
-- - 추천 컬럼·latest-korea: ADD + UPDATE 백필 + MODIFY 로 NOT NULL 정합
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [A] products — 가격 컬럼
-- ----------------------------------------------------------------------------

-- base_price 는 통합 스키마의 products 에 이미 있다. MySQL 은 ADD COLUMN 를 지원하지 않아 다시 추가하지 않는다.

-- ----------------------------------------------------------------------------
-- [A2] products — 추천 운영 컬럼 + latest-in-korea 큐레이션
-- ----------------------------------------------------------------------------

ALTER TABLE products
    ADD COLUMN is_recommended BOOLEAN NOT NULL DEFAULT FALSE COMMENT '관리자 추천 노출 여부';

ALTER TABLE products
    ADD COLUMN recommendation_score INT NOT NULL DEFAULT 0 COMMENT '관리자 수동 추천 점수 높을수록 우선';

ALTER TABLE products
    MODIFY COLUMN is_recommended BOOLEAN NOT NULL DEFAULT FALSE COMMENT '관리자 추천 노출 여부';

ALTER TABLE products
    MODIFY COLUMN recommendation_score INT NOT NULL DEFAULT 0 COMMENT '관리자 수동 추천 점수 높을수록 우선';

CREATE INDEX idx_latest_korea_product_id
    ON product_latest_korea_recommendation(product_id);

-- ----------------------------------------------------------------------------
-- [C] structured survey — 테이블·컬럼 코멘트 MySQL: MODIFY + TABLE COMMENT
-- ----------------------------------------------------------------------------

ALTER TABLE survey_form
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '설문지 PK',
    MODIFY COLUMN title VARCHAR(200) NOT NULL COMMENT '설문지 제목',
    MODIFY COLUMN description TEXT NULL COMMENT '설문지 설명',
    MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부 true=현재 운영 설문',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';

ALTER TABLE survey_form COMMENT = '설문지 메타 정보 제목/설명/활성 상태';

ALTER TABLE survey_question
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '문항 PK',
    MODIFY COLUMN survey_form_id BIGINT NOT NULL COMMENT '소속 설문지 ID FK',
    MODIFY COLUMN display_order INT NOT NULL COMMENT '설문 화면 노출 순서',
    MODIFY COLUMN title VARCHAR(300) NOT NULL COMMENT '문항 제목',
    MODIFY COLUMN description TEXT NULL COMMENT '문항 부가 설명',
    MODIFY COLUMN question_type VARCHAR(30) NOT NULL COMMENT '문항 타입 TEXT/SINGLE_CHOICE/MULTIPLE_CHOICE',
    MODIFY COLUMN required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '필수 응답 여부',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';

ALTER TABLE survey_question COMMENT = '설문지에 속한 문항 정의';

ALTER TABLE survey_question_option
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택지 PK',
    MODIFY COLUMN survey_question_id BIGINT NOT NULL COMMENT '소속 문항 ID FK',
    MODIFY COLUMN display_order INT NOT NULL COMMENT '선택지 노출 순서',
    MODIFY COLUMN option_text VARCHAR(300) NOT NULL COMMENT '선택지 문구',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';

ALTER TABLE survey_question_option COMMENT = '객관식 문항의 선택지 집합';

ALTER TABLE survey_submission
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '제출 PK',
    MODIFY COLUMN survey_form_id BIGINT NOT NULL COMMENT '응답한 설문지 ID FK',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '응답 사용자 ID FK',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '제출 생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '제출 수정 시각';

ALTER TABLE survey_submission COMMENT = '사용자 설문 제출 헤더';

ALTER TABLE survey_answer
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '응답 PK',
    MODIFY COLUMN survey_submission_id BIGINT NOT NULL COMMENT '소속 제출 ID FK',
    MODIFY COLUMN survey_question_id BIGINT NOT NULL COMMENT '응답 대상 문항 ID FK',
    MODIFY COLUMN answer_text TEXT NULL COMMENT '주관식 응답 텍스트',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';

ALTER TABLE survey_answer COMMENT = '제출 내 문항별 응답 본문';

ALTER TABLE survey_answer_option
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택 응답 PK',
    MODIFY COLUMN survey_answer_id BIGINT NOT NULL COMMENT '소속 응답 ID FK',
    MODIFY COLUMN display_order INT NOT NULL COMMENT '선택 순서 복수선택 지원',
    MODIFY COLUMN option_text VARCHAR(300) NOT NULL COMMENT '선택된 문구 스냅샷',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';

ALTER TABLE survey_answer_option COMMENT = '객관식/복수선택 응답의 선택값';

-- ----------------------------------------------------------------------------
-- [D] structured survey — 인덱스 FK 조인·목록 조회
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

CREATE INDEX idx_exchange_branch_currency ON exchange_branch (currency);

CREATE INDEX idx_exchange_alert_user ON exchange_alert (user_id);

ALTER TABLE survey_form
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PLANNED' COMMENT '설문 진행 상태 PLANNED=계획, SCHEDULED=예정, IN_PROGRESS=진행',
    ADD COLUMN primary_category VARCHAR(40) NOT NULL DEFAULT 'ALL' COMMENT '설문 1뎁스 카테고리',
    ADD COLUMN secondary_category VARCHAR(40) NULL COMMENT '설문 2뎁스 카테고리';

CREATE INDEX idx_survey_form_description_tag_description_id
    ON survey_form_description_tag (survey_form_description_id);

ALTER TABLE survey_form
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' COMMENT '설문 진행 상태 SCHEDULED=예정, IN_PROGRESS=진행, COMPLETED=완료';

CREATE INDEX idx_search_keyword_stats_count
    ON search_keyword_stats (search_count DESC, last_searched_at DESC);

CREATE INDEX idx_toss_billing_methods_user_id
    ON toss_billing_methods (user_id);

CREATE INDEX idx_toss_billing_subscriptions_next
    ON toss_billing_subscriptions (status, next_billing_at);

CREATE INDEX idx_toss_billing_charges_user_id
    ON toss_billing_charges (user_id);

-- =============================================================================
-- 토스 빌링 재시도·해지예약 컬럼
-- -----
-- 목적
-- - 결제 실패 시 실패횟수와 재시도시각으로 재시도 간격 제어. 1분마다 재결제 방지
-- - 해지 예약용 종료시각 추가
-- 대상
-- - toss_billing_subscriptions
-- =============================================================================

ALTER TABLE toss_billing_subscriptions
    ADD COLUMN fail_count INT NOT NULL DEFAULT 0
        COMMENT '연속 결제 실패 횟수. 1회는 하루 뒤, 2회는 이틀 뒤 재시도, 3회는 일시중지';

ALTER TABLE toss_billing_subscriptions
    ADD COLUMN retry_at DATETIME(6) NULL
        COMMENT '다음 재시도 시각. 비어 있으면 다음결제일 기준으로 도래 판단';

ALTER TABLE toss_billing_subscriptions
    ADD COLUMN ends_at DATETIME(6) NULL
        COMMENT '해지 예약 시 혜택 유지 종료 시각. 보통 다음 결제일. 이후 해지완료';

CREATE INDEX idx_toss_billing_subscriptions_retry
    ON toss_billing_subscriptions (status, retry_at);

-- =============================================================================
-- 예약 글로우패스 멤버십 할인 금액
-- -----
-- 목적
-- - 정기결제 글로우패스 활성 회원 예약 10퍼센트 할인액 기록
-- - 환불 시 실제 결제액과 할인액 추적에 사용
-- 대상
-- - reservations.membership_discount
-- =============================================================================

ALTER TABLE reservations
    ADD COLUMN membership_discount DECIMAL(15, 2) NOT NULL DEFAULT 0
        COMMENT '글로우패스 멤버 할인액 원 단위. 비멤버는 0, 멤버는 정상가에서 실결제액을 뺀 값';

SET @drop_uk := (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE toss_billing_methods DROP INDEX uk_toss_billing_methods_billing_key',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND index_name = 'uk_toss_billing_methods_billing_key'
);

PREPARE stmt FROM @drop_uk;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;

-- 암호문은 평문보다 길어서 길이를 1000으로 확장
ALTER TABLE toss_billing_methods
    MODIFY COLUMN billing_key VARCHAR(1000) NOT NULL
        COMMENT '토스 빌링키 암호화 저장값. 평문 카드정보 대체값';

SET @add_hash := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE toss_billing_methods ADD COLUMN billing_key_hash VARCHAR 64 NULL COMMENT ''빌링키 평문 해시. 암호문이 매번 달라도 동일 키 중복 등록 방지''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND column_name = 'billing_key_hash'
);

PREPARE stmt2 FROM @add_hash;

EXECUTE stmt2;

DEALLOCATE PREPARE stmt2;

ALTER TABLE toss_billing_methods
    MODIFY COLUMN billing_key_hash VARCHAR(64) NOT NULL
        COMMENT '빌링키 평문 해시. 암호문이 매번 달라도 동일 키 중복 등록 방지';

SET @add_uk := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE toss_billing_methods ADD CONSTRAINT uk_toss_billing_methods_billing_key_hash UNIQUE (billing_key_hash)',
        'SELECT 1'
    )
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND constraint_name = 'uk_toss_billing_methods_billing_key_hash'
);

PREPARE stmt3 FROM @add_uk;

EXECUTE stmt3;

DEALLOCATE PREPARE stmt3;

-- =============================================================================
-- 토스 빌링 테이블·컬럼 주석 보강 마이에스큐엘
-- -----
-- 목적
-- - 이미 적용된 스키마에 컬럼 역할 주석을 부여
-- - 신규 설치는 이전 버전에 주석이 포함되어 있으나, 기존 디비 호환을 위해 재적용
-- 대상
-- - toss_billing_methods, subscriptions, charges, shedlock, reservations.membership_discount
-- =============================================================================

ALTER TABLE toss_billing_methods
    COMMENT = '글로우패스 카드 자동결제 수단. 카드번호 원문과 씨브이씨는 미저장';

ALTER TABLE toss_billing_methods
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제수단 기본키',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디. 한 사용자에 활성 수단 1개 권장',
    MODIFY COLUMN customer_key VARCHAR(100) NOT NULL COMMENT '토스 구매자 식별키. idolglow-user-회원아이디 형식',
    MODIFY COLUMN billing_key VARCHAR(1000) NOT NULL COMMENT '토스 빌링키 암호화 저장값. 평문 카드정보 대체값',
    MODIFY COLUMN billing_key_hash VARCHAR(64) NOT NULL COMMENT '빌링키 평문 해시. 동일 키 중복 등록 방지',
    MODIFY COLUMN card_company VARCHAR(50) NULL COMMENT '카드사명 표시용. 예: 현대, 국민',
    MODIFY COLUMN card_number VARCHAR(40) NULL COMMENT '마스킹된 카드번호. 예: 43301234****123*',
    MODIFY COLUMN card_type VARCHAR(30) NULL COMMENT '카드 종류. 예: 신용, 체크',
    MODIFY COLUMN owner_type VARCHAR(30) NULL COMMENT '소유 구분. 예: 개인, 법인',
    MODIFY COLUMN authenticated_at DATETIME(6) NULL COMMENT '토스 카드 인증 등록 완료 시각',
    MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '현재 사용 중 여부. 재등록 시 기존 행은 비활성',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각';

ALTER TABLE toss_billing_subscriptions
    COMMENT = '글로우패스 월간 구독. 회원과 요금제 조합당 1건';

ALTER TABLE toss_billing_subscriptions
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '구독 기본키',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디',
    MODIFY COLUMN billing_method_id BIGINT NOT NULL COMMENT '결제에 쓸 결제수단 아이디 외래키',
    MODIFY COLUMN plan_code VARCHAR(50) NOT NULL DEFAULT 'MONTHLY' COMMENT '요금제 코드. 현재 월간 고정',
    MODIFY COLUMN order_name VARCHAR(200) NOT NULL COMMENT '토스와 영수증 주문명. 예: 아이돌글로우 월간 구독',
    MODIFY COLUMN amount BIGINT NOT NULL COMMENT '매월 청구 금액 원 단위. 기본 4900',
    MODIFY COLUMN status VARCHAR(30) NOT NULL COMMENT '상태. 활성, 일시중지, 해지예약, 해지완료',
    MODIFY COLUMN next_billing_at DATETIME(6) NULL COMMENT '다음 자동결제 예정 시각. 스케줄러 도래 판단 기준',
    MODIFY COLUMN last_charged_at DATETIME(6) NULL COMMENT '최근 결제 성공 시각',
    MODIFY COLUMN fail_count INT NOT NULL DEFAULT 0 COMMENT '연속 결제 실패 횟수. 1회는 하루 뒤, 2회는 이틀 뒤 재시도, 3회는 일시중지',
    MODIFY COLUMN retry_at DATETIME(6) NULL COMMENT '다음 재시도 시각. 비어 있으면 다음결제일 기준 도래',
    MODIFY COLUMN ends_at DATETIME(6) NULL COMMENT '해지 예약 시 혜택 유지 종료 시각. 이후 해지완료',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각';

ALTER TABLE toss_billing_charges
    COMMENT = '빌링 자동결제 회차별 승인 이력. 주문번호로 이중청구 방지';

ALTER TABLE toss_billing_charges
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '회차 결제 기본키',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디',
    MODIFY COLUMN subscription_id BIGINT NULL COMMENT '소속 구독 아이디. 단건 테스트 결제는 비울 수 있음',
    MODIFY COLUMN billing_method_id BIGINT NOT NULL COMMENT '실제 청구에 사용한 결제수단 아이디 외래키',
    MODIFY COLUMN order_id VARCHAR(200) NOT NULL COMMENT '주문번호. 구독회차는 구독아이디와 연월, 단건은 임의값',
    MODIFY COLUMN order_name VARCHAR(200) NOT NULL COMMENT '해당 회차 주문명',
    MODIFY COLUMN amount BIGINT NOT NULL COMMENT '이 회차 청구 금액 원 단위',
    MODIFY COLUMN payment_key VARCHAR(200) NULL COMMENT '토스 결제키. 승인 성공 시 저장',
    MODIFY COLUMN status VARCHAR(30) NOT NULL COMMENT '상태. 대기, 성공, 실패',
    MODIFY COLUMN raw_response TEXT NULL COMMENT '토스 에이피아이 원문 응답. 디버깅과 대사에 사용',
    MODIFY COLUMN fail_message VARCHAR(500) NULL COMMENT '실패 시 사용자와 운영 안내 메시지',
    MODIFY COLUMN approved_at DATETIME(6) NULL COMMENT '토스 승인 완료 시각',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '회차 행 생성 시각. 대기 상태로 먼저 저장하는 시점',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각';

ALTER TABLE shedlock
    COMMENT = '스케줄러 분산 락';

ALTER TABLE shedlock
    MODIFY COLUMN name VARCHAR(64) NOT NULL COMMENT '잠금 이름. 예: 토스빌링자동결제',
    MODIFY COLUMN lock_until TIMESTAMP(6) NOT NULL COMMENT '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능',
    MODIFY COLUMN locked_at TIMESTAMP(6) NOT NULL COMMENT '락을 잡은 시각',
    MODIFY COLUMN locked_by VARCHAR(255) NOT NULL COMMENT '락을 잡은 호스트 또는 인스턴스 식별자';

ALTER TABLE reservations
    MODIFY COLUMN membership_discount DECIMAL(15, 2) NOT NULL DEFAULT 0
        COMMENT '글로우패스 멤버 할인액 원 단위. 비멤버는 0, 멤버는 정상가에서 실결제액을 뺀 값';

-- =============================================================================
-- 다중 카드 결제수단과 회차 시도 이력 마이에스큐엘
-- -----
-- 목적
-- - 사용자당 카드 여러 장 등록. 기본 결제수단 플래그
-- - 같은 청구 회차에 카드별 결제 시도 이력을 분리 저장
-- 대상
-- - toss_billing_methods.is_default, toss_billing_charge_attempts
-- 연관
-- - TossBillingService 다중 카드, BillingChargeExecutor 시도별 orderId
-- =============================================================================

SET @add_default := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE toss_billing_methods ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''구독 자동결제에 쓸 기본 수단 여부''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND column_name = 'is_default'
);

PREPARE stmt_default FROM @add_default;

EXECUTE stmt_default;

DEALLOCATE PREPARE stmt_default;

ALTER TABLE toss_billing_methods
    MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '삭제하지 않은 사용 가능 카드 여부. 여러 장 동시 활성 가능',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디. 활성 카드는 사용자당 여러 장 가능',
    MODIFY COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '구독 자동결제에 쓸 기본 수단 여부';

CREATE INDEX idx_toss_billing_methods_user_active_default
    ON toss_billing_methods (user_id, active, is_default);

CREATE INDEX idx_toss_billing_charge_attempts_charge_id
    ON toss_billing_charge_attempts (charge_id);

-- =============================================================================
-- 결제 상태에 승인 처리 중 CONFIRMING 추가
-- -----
-- 목적
-- - 토스 승인 HTTP를 트랜잭션 밖에서 부르는 동안 결제 상태를 CONFIRMING으로 둔다
-- - 타임아웃 뒤 조회 스케줄러가 같은 상태로 남은 건을 맞춘다
-- 대상
-- - MySQL payments.chk_payments_status
-- 연관
-- - TossPaymentConfirmTxService, TossConfirmReconcileScheduler
-- =============================================================================

ALTER TABLE payments DROP CHECK chk_payments_status;

ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (
    status IN (
        'PENDING',
        'CONFIRMING',
        'SUCCEEDED',
        'FAILED',
        'CANCELED',
        'EXPIRED',
        'REFUNDED',
        'PARTIAL_CANCELED'
    )
);

-- =============================================================================
-- 결제 환불 처리 중 상태와 환불 첫 요청 시각
-- -----
-- 목적
-- - payments.status에 REFUNDING을 허용한다
-- - payment_refunds.first_requested_at을 두어 첫 환불 요청 시각을 남긴다
-- 대상
-- - MySQL / payments.chk_payments_status
-- - MySQL / payment_refunds.first_requested_at
-- 주의
-- - 기존 환불 행의 first_requested_at은 created_at으로 채운다
-- - 환불 API·스케줄러는 이 마이그레이션에서 바꾸지 않는다
-- 연관
-- - PaymentStatus.REFUNDING, Payment.markRefunding, PaymentRefund.firstRequestedAt
-- =============================================================================

ALTER TABLE payments DROP CHECK chk_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status CHECK (
        status IN (
            'PENDING',
            'CONFIRMING',
            'SUCCEEDED',
            'FAILED',
            'CANCELED',
            'EXPIRED',
            'REFUNDED',
            'PARTIAL_CANCELED',
            'REFUNDING'
        )
    );

-- 환불 이력에 첫 요청 시각을 남긴다. 기존 행은 생성 시각 created_at 으로 채운다.
ALTER TABLE payment_refunds
    ADD COLUMN first_requested_at DATETIME(6) NULL COMMENT '환불 첫 요청 시각. 기존 행은 created_at으로 채운다. NULL 허용.';

-- =============================================================================
-- 빌링키 삭제 상태
-- -----
-- 목적
-- - 카드 비활성화와 토스 빌링키 삭제 성공·대기·확정 실패를 구분한다
-- 대상
-- - 마이에스큐엘 / toss_billing_methods 삭제 상태 컬럼
-- 주의
-- - 기존 행은 기본값 ACTIVE로 채운다
-- - 빌링키 해시 유니크는 유지한다. 암호문만 삭제 확정 후 표시 값으로 바꾼다
-- 연관
-- - BillingKeyDeletionService, TossBillingKeyDeletionScheduler, BILLING_DELETED 웹훅
-- =============================================================================

-- 삭제 대기·완료·확정 실패를 카드 행에 남긴다. 기존 카드는 사용 중 ACTIVE 이다.
ALTER TABLE toss_billing_methods
    ADD COLUMN deletion_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '빌링키 삭제 상태. ACTIVE, DELETE_PENDING, DELETED, DELETE_FAILED',
    ADD COLUMN deletion_requested_at DATETIME(6) NULL COMMENT '사용자가 카드 삭제를 요청한 시각',
    ADD COLUMN deletion_completed_at DATETIME(6) NULL COMMENT '토스 삭제 성공 또는 이미 없음으로 확정한 시각',
    ADD COLUMN deletion_last_attempt_at DATETIME(6) NULL COMMENT '토스 삭제 API를 마지막으로 호출한 시각',
    ADD COLUMN deletion_attempt_count INT NOT NULL DEFAULT 0 COMMENT '토스 삭제 API 호출 횟수',
    ADD COLUMN deletion_failure_message VARCHAR(500) NULL COMMENT '확정 거절 시 안내 메시지. 500자 이내';

CREATE INDEX idx_toss_billing_methods_deletion_pending
    ON toss_billing_methods (deletion_status, deletion_last_attempt_at);

-- =============================================================================
-- 구독 발송 선점과 수신자별 결과
-- -----
-- 목적
-- - 발송 전에 SENDING으로 이력을 선점하고, 수신자별 성공·실패를 남긴다
-- 대상
-- - 마이에스큐엘 / subscription_dispatch_histories, subscription_dispatch_recipients
-- 주의
-- - 기존 이력의 성공·실패 수는 0이다. 발송 상태 값 길이는 그대로다
-- - PARTIAL_FAILED 14, SENDING 7 은 dispatch_status varchar 20 에 들어간다
-- 연관
-- - SubscriptionDispatchMailService, SubscriptionDispatchTxService
-- =============================================================================

-- 발송 중 선점 시각과 성공·실패 수를 이력에 남긴다. 기존 행은 0이다.
ALTER TABLE subscription_dispatch_histories
    ADD COLUMN success_count BIGINT NOT NULL DEFAULT 0 COMMENT '수신자별 발송 성공 수',
    ADD COLUMN failure_count BIGINT NOT NULL DEFAULT 0 COMMENT '수신자별 발송 실패 수',
    ADD COLUMN last_attempt_at DATETIME(6) NULL COMMENT '마지막으로 발송을 시도한 시각. 오래되면 안 보낸 수신자만 이어서 보낸다';

ALTER TABLE subscription_dispatch_histories
    MODIFY COLUMN dispatch_status VARCHAR(20) NOT NULL COMMENT '발송 상태. RECORDED, SENDING, SENT, PARTIAL_FAILED, FAILED';

CREATE INDEX idx_subscription_dispatch_histories_sending
    ON subscription_dispatch_histories (dispatch_status, last_attempt_at);

CREATE INDEX idx_subscription_dispatch_recipients_dispatch
    ON subscription_dispatch_recipients (dispatch_id);

CREATE INDEX idx_event_master_normalized_title
    ON event_master (normalized_title);

CREATE INDEX idx_event_source_ref_global_id
    ON event_source_ref (global_id);

-- =============================================================================
-- 운영 변경 요청·승인·되돌리기
-- -----
-- 목적
-- - 운영 변경을 요청, 승인, 적용, 되돌리기 상태로 남긴다
-- 대상
-- - 마이에스큐엘 / ops_change_request, users.role
-- 주의
-- - 요청자와 승인자는 같을 수 없다. 적용 전 상태는 before_snapshot에 남긴다
-- - OPERATOR는 요청·적용, APPROVER는 승인. ADMIN은 기존 관리 권한을 유지한다
-- 연관
-- - OpsChangeCommandService, EventMatchMergeHandler, AdminAuditService
-- =============================================================================

-- 제안자와 승인자를 역할로 나눈다. 기존 USER, ADMIN은 그대로 둔다.
ALTER TABLE users DROP CHECK chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'OPERATOR', 'APPROVER'));

ALTER TABLE users MODIFY COLUMN role VARCHAR(255) NOT NULL COMMENT '권한 역할. USER 일반, ADMIN 관리자, OPERATOR 운영 변경 요청, APPROVER 승인';

CREATE INDEX idx_ops_change_request_status
    ON ops_change_request (status);
