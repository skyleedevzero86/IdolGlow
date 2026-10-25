-- =============================================================================
-- column: 컬럼·제약·인덱스
-- -----
-- 목적
-- - 컬럼 추가·변경, 제약, 인덱스를 한 파일에 모은다.
-- 대상
-- - ps
-- 주의
-- - 새 데이터베이스는 setting, create, column, comment, insert, select 순서로 실행한다
-- - 이미 반영된 데이터베이스에는 이 묶음을 다시 실행하지 않는다
-- =============================================================================

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

-- 예약 보류, 결제, 알림, 회원 권한 관련 변경
-- -
-- users: 허용되는 role 값 범위를 넓힌다
-- -
ALTER TABLE users DROP CONSTRAINT chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- -
-- reservation_slots: 홀드 상태 컬럼 추가
-- -
ALTER TABLE reservation_slots ADD COLUMN hold_reservation_id BIGINT;

ALTER TABLE reservation_slots ADD COLUMN hold_expires_at TIMESTAMP;

ALTER TABLE reservation_slots
    ADD CONSTRAINT fk_reservation_slots_hold_reservation FOREIGN KEY (hold_reservation_id) REFERENCES reservations (id);

CREATE INDEX idx_reservation_slots_hold ON reservation_slots (hold_reservation_id, hold_expires_at);

-- -
-- reservations: 만료/확정/취소 메타 추가
-- -
ALTER TABLE reservations ADD COLUMN expires_at TIMESTAMP;

ALTER TABLE reservations ADD COLUMN confirmed_at TIMESTAMP;

ALTER TABLE reservations ADD COLUMN canceled_at TIMESTAMP;

ALTER TABLE reservations ADD COLUMN cancel_reason VARCHAR(40);

ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_cancel_reason CHECK (
        cancel_reason IS NULL OR cancel_reason IN ('USER_REQUESTED', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED', 'ADMIN_CANCELED')
    );

CREATE INDEX idx_reservations_status_expires_at ON reservations (status, expires_at);

CREATE INDEX idx_payments_status_updated_at ON payments (status, updated_at);

CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at);

CREATE INDEX IF NOT EXISTS idx_images_aggregate ON images (aggregate_type, aggregate_id, sort_order);

ALTER TABLE user_oauths
    ADD COLUMN profile_name VARCHAR(255);

ALTER TABLE user_oauths
    ADD COLUMN profile_image_url VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_images_aggregate ON images (aggregate_type, aggregate_id, sort_order);

ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NULL;

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL;

DROP INDEX IF EXISTS idx_users_email;

CREATE UNIQUE INDEX uq_users_email ON users (email);

CREATE UNIQUE INDEX uq_users_nickname ON users (nickname);

-- 리뷰 신뢰도: 예약 연동, 도움돼요, 신고/숨김

ALTER TABLE product_reviews ADD COLUMN reservation_id BIGINT NULL;

ALTER TABLE product_reviews ADD COLUMN helpful_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE product_reviews ADD COLUMN hidden_at TIMESTAMP NULL;

ALTER TABLE product_reviews ADD COLUMN hidden_reason VARCHAR(80) NULL;

ALTER TABLE product_reviews ADD CONSTRAINT fk_product_reviews_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations (id);

CREATE UNIQUE INDEX uk_product_reviews_reservation_id ON product_reviews (reservation_id);

CREATE INDEX idx_helpful_review_id ON product_review_helpful_votes (review_id);

CREATE INDEX idx_report_review_id ON product_review_reports (review_id);

CREATE INDEX idx_rsw_slot ON reservation_slot_waitlist (reservation_slot_id);

CREATE INDEX idx_rsw_user ON reservation_slot_waitlist (user_id);

-- 결제대행사 연동에 맞춘 결제 스키마 확장
-- -
-- 결제 제공자와 상태 코드 목록을 애플리케이션 규칙에 맞게 다시 정한다
-- -
ALTER TABLE payments DROP CONSTRAINT chk_payments_provider;

ALTER TABLE payments DROP CONSTRAINT chk_payments_status;

-- -
-- payments: 대행사 연동용 컬럼을 추가한다
-- -
ALTER TABLE payments ADD COLUMN payment_no VARCHAR(64) NULL;

ALTER TABLE payments ADD COLUMN order_id VARCHAR(200) NULL;

ALTER TABLE payments ADD COLUMN payment_key VARCHAR(200) NULL;

ALTER TABLE payments ADD COLUMN order_name VARCHAR(255) NULL;

ALTER TABLE payments ADD COLUMN supplied_amount NUMERIC(15, 2) NULL;

ALTER TABLE payments ADD COLUMN vat NUMERIC(15, 2) NULL;

ALTER TABLE payments ADD COLUMN tax_free_amount NUMERIC(15, 2) NULL;

ALTER TABLE payments ADD COLUMN currency VARCHAR(10) NULL;

ALTER TABLE payments ADD COLUMN gateway_method VARCHAR(50) NULL;

ALTER TABLE payments ADD COLUMN gateway_type VARCHAR(50) NULL;

ALTER TABLE payments ADD COLUMN external_status VARCHAR(50) NULL;

ALTER TABLE payments ADD COLUMN requested_at TIMESTAMP NULL;

ALTER TABLE payments ADD COLUMN last_transaction_key VARCHAR(200) NULL;

ALTER TABLE payments ADD COLUMN fail_code VARCHAR(100) NULL;

ALTER TABLE payments ADD COLUMN canceled_at TIMESTAMP NULL;

ALTER TABLE payments ADD COLUMN cancel_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;

ALTER TABLE payments ADD COLUMN card_company VARCHAR(100) NULL;

ALTER TABLE payments ADD COLUMN card_number VARCHAR(50) NULL;

ALTER TABLE payments ADD COLUMN installment_plan_months INT NULL;

ALTER TABLE payments ADD COLUMN is_interest_free BOOLEAN NULL;

ALTER TABLE payments ADD COLUMN virtual_account_bank VARCHAR(100) NULL;

ALTER TABLE payments ADD COLUMN virtual_account_number VARCHAR(100) NULL;

ALTER TABLE payments ADD COLUMN virtual_account_due_date TIMESTAMP NULL;

ALTER TABLE payments ADD COLUMN easy_pay_provider VARCHAR(100) NULL;

ALTER TABLE payments ADD COLUMN raw_response_json TEXT NULL;

ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) NULL;

ALTER TABLE payments ALTER COLUMN payment_no SET NOT NULL;

ALTER TABLE payments ALTER COLUMN order_id SET NOT NULL;

-- 내부 결제 번호, 가맹점 주문 번호, 대행사 결제 키, 승인 시 중복 방지 키는 각각 유일해야 한다
ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_no UNIQUE (payment_no);

ALTER TABLE payments ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);

ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_key UNIQUE (payment_key);

ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('MOCK', 'TOSS'));

ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (
    status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'EXPIRED', 'REFUNDED', 'PARTIAL_CANCELED')
);

-- 결제 상태와 키로 빠르게 찾기 위한 인덱스
CREATE INDEX idx_payments_external_status ON payments (external_status);

CREATE INDEX idx_payments_payment_key ON payments (payment_key);

CREATE INDEX idx_payment_logs_payment_id ON payment_logs (payment_id);

CREATE INDEX idx_payment_logs_order_id ON payment_logs (order_id);

CREATE INDEX idx_payment_logs_created_at ON payment_logs (created_at);

CREATE INDEX idx_payment_refunds_payment_id ON payment_refunds (payment_id);

CREATE INDEX idx_payment_refunds_reservation_id ON payment_refunds (reservation_id);

CREATE INDEX idx_payment_refunds_status ON payment_refunds (status);

-- 알림 설정 테이블 + 만료 예정 알림 타입
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_type;

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

-- 사용자 기준 설정 조회·배치 스캔용
CREATE INDEX IF NOT EXISTS idx_np_user_id ON notification_preferences (user_id);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin_user ON admin_audit_logs (admin_user_id);

-- 리뷰 반응·신고 테이블에 수정 시각 컬럼이 없으면 추가한다. 애플리케이션과 스키마를 맞추기 위함이다.
-- 기존 데이터는 마이그레이션 시점 시각으로 채우고, 이후 값은 애플리케이션 감사 로직이 갱신한다.

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE product_review_reports
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 이슈별 기사 목록 조회용
create index if not exists idx_webzine_articles_issue_id on webzine_articles(issue_id);

create index if not exists idx_newsletters_published_at on newsletters(published_at);

create index if not exists idx_email_subscriptions_active on email_subscriptions(active);

create index if not exists idx_subscription_dispatch_histories_dispatched_at on subscription_dispatch_histories(dispatched_at);

create index if not exists idx_admin_audit_logs_created_at on admin_audit_logs (created_at desc);

create index if not exists idx_admin_audit_logs_admin_user on admin_audit_logs (admin_user_id);

create index if not exists idx_webzine_articles_issue_id on webzine_articles(issue_id);

create index if not exists idx_newsletters_published_at on newsletters (published_at);

create index if not exists idx_email_subscriptions_active on email_subscriptions(active);

create index if not exists idx_subscription_dispatch_histories_dispatched_at
    on subscription_dispatch_histories(dispatched_at);

create index if not exists editor_documents_updated_at_idx
    on editor_documents (updated_at desc);

create index if not exists editor_documents_publication_status_idx
    on editor_documents (publication_status, updated_at desc);

create index if not exists editor_documents_url_slug_idx
    on editor_documents (url_slug);

create index if not exists editor_assets_uploaded_at_idx
    on editor_assets (uploaded_at desc);

-- -
-- pg_search 가 설치된 경우에만 BM25 검색 인덱스를 시도한다
-- 확장이 없으면 애플리케이션은 ILIKE 기반 검색으로 동작한다
-- -
do $pgsearch$
begin
    if exists (select 1 from pg_extension where extname = 'pg_search') then
        execute '
            create index if not exists editor_documents_search_idx
            on editor_documents
            using bm25 (id, title, author, markdown)
            with (key_field = ''id'')
        ';
    end if;
end
$pgsearch$;

-- 플랫폼 인증용 users 확장·비밀번호 이력·역할
ALTER TABLE users DROP CONSTRAINT chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE users ADD COLUMN IF NOT EXISTS platform_username VARCHAR(50) NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS login_fail_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked_at TIMESTAMP NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_password_change_date DATE NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_daily_count INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_platform_username ON users (platform_username);

CREATE INDEX IF NOT EXISTS idx_user_password_history_user_created
    ON user_password_history (user_id, created_at DESC);

ALTER TABLE users DROP CONSTRAINT chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

create index if not exists idx_subscription_dispatch_schedules_active
    on subscription_dispatch_schedules(active);

create index if not exists idx_auth_verification_audit_logs_created_at
    on auth_verification_audit_logs (created_at desc);

create index if not exists idx_auth_verification_audit_logs_type_created
    on auth_verification_audit_logs (verification_type, created_at desc);

create index if not exists idx_signup_verification_tokens_email_type_status_created
    on signup_verification_tokens (email, type, status, created_at desc);

-- users: 임시 비밀번호를 마지막으로 발급한 시각
-- 강제 변경 플래그와 함께 쓰이며, 재발급 정책에 참고한다.
alter table users
    add column if not exists temporary_password_issued_at timestamp(6);

-- users: 다음 로그인에서 비밀번호를 바꾸게 할지 여부
-- 기본값은 false 이다.
alter table users
    add column if not exists temporary_password_required boolean not null default false;

ALTER TABLE reservation_slots
    ADD COLUMN IF NOT EXISTS admin_note TEXT;

ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS admin_memo TEXT;

-- ============================================================================
-- 통합 마이그레이션 PostgreSQL
-- ----------------------------------------------------------------------------
-- 목적
-- 1 products.base_price 컬럼 누락 시 idempotent 보정
-- 2 문항형 설문 Form → Question → Option / Submission → Answer 스키마 도입
-- 3 상품 추천 운영 플래그/점수 + latest-in-korea 수동 큐레이션 테이블
--
-- 설계 메모
-- - CREATE IF NOT EXISTS 로 신규/기존 배포 모두 안전
-- - 설문 정규화: 관리자 문항 편집·응답 이력·통계/추천 확장에 대응
-- - 추천 컬럼·latest-korea 테이블: idempotent ADD + 백필 후 NOT NULL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [A] products — 가격 컬럼
-- ----------------------------------------------------------------------------

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS base_price NUMERIC(19, 2) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------------
-- [A2] products — 추천 운영 컬럼 + latest-in-korea 큐레이션
-- ----------------------------------------------------------------------------

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_recommended BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS recommendation_score INT NOT NULL DEFAULT 0;

ALTER TABLE products ALTER COLUMN is_recommended SET DEFAULT FALSE;

ALTER TABLE products ALTER COLUMN recommendation_score SET DEFAULT 0;

ALTER TABLE products ALTER COLUMN is_recommended SET NOT NULL;

ALTER TABLE products ALTER COLUMN recommendation_score SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_latest_korea_product_id
    ON product_latest_korea_recommendation(product_id);

-- ----------------------------------------------------------------------------
-- [D] structured survey — 인덱스 FK 조인·목록 조회
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_survey_question_form_id
    ON survey_question (survey_form_id);

CREATE INDEX IF NOT EXISTS idx_survey_question_option_question_id
    ON survey_question_option (survey_question_id);

CREATE INDEX IF NOT EXISTS idx_survey_submission_form_id
    ON survey_submission (survey_form_id);

CREATE INDEX IF NOT EXISTS idx_survey_submission_user_id
    ON survey_submission (user_id);

CREATE INDEX IF NOT EXISTS idx_survey_answer_submission_id
    ON survey_answer (survey_submission_id);

CREATE INDEX IF NOT EXISTS idx_survey_answer_question_id
    ON survey_answer (survey_question_id);

CREATE INDEX IF NOT EXISTS idx_survey_answer_option_answer_id
    ON survey_answer_option (survey_answer_id);

-- - 과거 Flyway 이력이 28 이상인 로컬/공유 DB에서도
-- - 현재 리포의 추천 기능 엔티티와 스키마 validate 불일치를 해소한다.
-- ============================================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_recommended BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS recommendation_score INT NOT NULL DEFAULT 0;

ALTER TABLE products
    ALTER COLUMN is_recommended SET DEFAULT FALSE;

ALTER TABLE products
    ALTER COLUMN recommendation_score SET DEFAULT 0;

ALTER TABLE products
    ALTER COLUMN is_recommended SET NOT NULL;

ALTER TABLE products
    ALTER COLUMN recommendation_score SET NOT NULL;

ALTER TABLE user_survey
    ADD COLUMN IF NOT EXISTS visit_start_time VARCHAR(5),
    ADD COLUMN IF NOT EXISTS visit_end_time VARCHAR(5);

CREATE INDEX IF NOT EXISTS idx_exchange_branch_currency ON exchange_branch (currency);

CREATE INDEX IF NOT EXISTS idx_exchange_alert_user ON exchange_alert (user_id);

ALTER TABLE survey_form
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    ADD COLUMN IF NOT EXISTS primary_category VARCHAR(40) NOT NULL DEFAULT 'ALL',
    ADD COLUMN IF NOT EXISTS secondary_category VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_survey_form_description_tag_description_id
    ON survey_form_description_tag (survey_form_description_id);

ALTER TABLE survey_form
    ALTER COLUMN status SET DEFAULT 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_search_keyword_stats_count
    ON search_keyword_stats (search_count DESC, last_searched_at DESC);

CREATE INDEX IF NOT EXISTS idx_toss_billing_methods_user_id
    ON toss_billing_methods (user_id);

CREATE INDEX IF NOT EXISTS idx_toss_billing_subscriptions_next
    ON toss_billing_subscriptions (status, next_billing_at);

CREATE INDEX IF NOT EXISTS idx_toss_billing_charges_user_id
    ON toss_billing_charges (user_id);

-- =============================================================================
-- 토스 빌링 재시도·해지예약 컬럼
-- -----
-- 목적
-- - 결제 실패 시 실패횟수와 재시도시각으로 재시도 간격 제어
-- - 해지 예약용 종료시각 추가
-- =============================================================================

ALTER TABLE toss_billing_subscriptions
    ADD COLUMN IF NOT EXISTS fail_count INT NOT NULL DEFAULT 0;

ALTER TABLE toss_billing_subscriptions
    ADD COLUMN IF NOT EXISTS retry_at TIMESTAMP(6);

ALTER TABLE toss_billing_subscriptions
    ADD COLUMN IF NOT EXISTS ends_at TIMESTAMP(6);

CREATE INDEX IF NOT EXISTS idx_toss_billing_subscriptions_retry
    ON toss_billing_subscriptions (status, retry_at);

-- =============================================================================
-- 예약 글로우패스 멤버십 할인 금액
-- -----
-- 목적
-- - 정기결제 글로우패스 활성 회원 예약 10퍼센트 할인액 기록
-- =============================================================================

ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS membership_discount NUMERIC(15, 2) NOT NULL DEFAULT 0;

ALTER TABLE toss_billing_methods
    DROP CONSTRAINT IF EXISTS uk_toss_billing_methods_billing_key;

ALTER TABLE toss_billing_methods
    ALTER COLUMN billing_key TYPE VARCHAR(1000);

ALTER TABLE toss_billing_methods
    ADD COLUMN IF NOT EXISTS billing_key_hash VARCHAR(64);

ALTER TABLE toss_billing_methods
    ALTER COLUMN billing_key_hash SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_toss_billing_methods_billing_key_hash'
    ) THEN
        ALTER TABLE toss_billing_methods
            ADD CONSTRAINT uk_toss_billing_methods_billing_key_hash UNIQUE (billing_key_hash);
    END IF;
END $$;

-- =============================================================================
-- 다중 카드 결제수단과 회차 시도 이력 포스트그레스
-- -----
-- 목적
-- - 사용자당 카드 여러 장 등록. 기본 결제수단 플래그
-- - 같은 청구 회차에 카드별 결제 시도 이력을 분리 저장
-- 대상
-- - toss_billing_methods.is_default, toss_billing_charge_attempts
-- 연관
-- - TossBillingService 다중 카드, BillingChargeExecutor 시도별 orderId
-- =============================================================================

ALTER TABLE toss_billing_methods
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_toss_billing_methods_user_active_default
    ON toss_billing_methods (user_id, active, is_default);

CREATE INDEX IF NOT EXISTS idx_toss_billing_charge_attempts_charge_id
    ON toss_billing_charge_attempts (charge_id);

-- =============================================================================
-- 결제 상태에 승인 처리 중 CONFIRMING 추가
-- -----
-- 목적
-- - 토스 승인 HTTP를 트랜잭션 밖에서 부르는 동안 결제 상태를 CONFIRMING으로 둔다
-- - 타임아웃 뒤 조회 스케줄러가 같은 상태로 남은 건을 맞춘다
-- 대상
-- - PostgreSQL payments.chk_payments_status
-- 연관
-- - TossPaymentConfirmTxService, TossConfirmReconcileScheduler
-- =============================================================================

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_status;

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
-- - 포스트그레스 / payments.chk_payments_status
-- - 포스트그레스 / payment_refunds.first_requested_at
-- 주의
-- - 기존 환불 행의 first_requested_at은 created_at으로 채운다
-- - 환불 API·스케줄러는 이 마이그레이션에서 바꾸지 않는다
-- 연관
-- - PaymentStatus.REFUNDING, Payment.markRefunding, PaymentRefund.firstRequestedAt
-- =============================================================================

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_status;

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
    ADD COLUMN first_requested_at TIMESTAMP(6) NULL;

-- =============================================================================
-- 빌링키 삭제 상태
-- -----
-- 목적
-- - 카드 비활성화와 토스 빌링키 삭제 성공·대기·확정 실패를 구분한다
-- 대상
-- - 포스트그레스 / toss_billing_methods 삭제 상태 컬럼
-- 주의
-- - 기존 행은 기본값 ACTIVE로 채운다
-- - 빌링키 해시 유니크는 유지한다. 암호문만 삭제 확정 후 표시 값으로 바꾼다
-- 연관
-- - BillingKeyDeletionService, TossBillingKeyDeletionScheduler, BILLING_DELETED 웹훅
-- =============================================================================

-- 삭제 대기·완료·확정 실패를 카드 행에 남긴다. 기존 카드는 사용 중 ACTIVE 이다.
ALTER TABLE toss_billing_methods
    ADD COLUMN IF NOT EXISTS deletion_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS deletion_completed_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS deletion_last_attempt_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS deletion_attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deletion_failure_message VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_toss_billing_methods_deletion_pending
    ON toss_billing_methods (deletion_status, deletion_last_attempt_at);

-- =============================================================================
-- 구독 발송 선점과 수신자별 결과
-- -----
-- 목적
-- - 발송 전에 SENDING으로 이력을 선점하고, 수신자별 성공·실패를 남긴다
-- 대상
-- - 포스트그레스 / subscription_dispatch_histories, subscription_dispatch_recipients
-- 주의
-- - 기존 이력의 성공·실패 수는 0이다. 발송 상태 값 길이는 그대로다
-- - PARTIAL_FAILED 14, SENDING 7 은 dispatch_status varchar 20 에 들어간다
-- 연관
-- - SubscriptionDispatchMailService, SubscriptionDispatchTxService
-- =============================================================================

-- 발송 중 선점 시각과 성공·실패 수를 이력에 남긴다. 기존 행은 0이다.
ALTER TABLE subscription_dispatch_histories
    ADD COLUMN IF NOT EXISTS success_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failure_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP(6);

CREATE INDEX IF NOT EXISTS idx_subscription_dispatch_histories_sending
    ON subscription_dispatch_histories (dispatch_status, last_attempt_at);

CREATE INDEX IF NOT EXISTS idx_subscription_dispatch_recipients_dispatch
    ON subscription_dispatch_recipients (dispatch_id);

CREATE INDEX IF NOT EXISTS idx_event_master_normalized_title
    ON event_master (normalized_title);

CREATE INDEX IF NOT EXISTS idx_event_source_ref_global_id
    ON event_source_ref (global_id);

-- =============================================================================
-- 운영 변경 요청·승인·되돌리기
-- -----
-- 목적
-- - 운영 변경을 요청, 승인, 적용, 되돌리기 상태로 남긴다
-- 대상
-- - 포스트그레스 / ops_change_request, users.role
-- 주의
-- - 요청자와 승인자는 같을 수 없다. 적용 전 상태는 before_snapshot에 남긴다
-- - OPERATOR는 요청·적용, APPROVER는 승인. ADMIN은 기존 관리 권한을 유지한다
-- 연관
-- - OpsChangeCommandService, EventMatchMergeHandler, AdminAuditService
-- =============================================================================

-- 제안자와 승인자를 역할로 나눈다. 기존 USER, ADMIN은 그대로 둔다.
ALTER TABLE users DROP CONSTRAINT chk_users_role;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'OPERATOR', 'APPROVER'));

CREATE INDEX IF NOT EXISTS idx_ops_change_request_status
    ON ops_change_request (status);
