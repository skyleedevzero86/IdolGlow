-- =============================================================================
-- V10: PG(토스) 연동 스키마 확장
--   - payments: 가맹점 주문번호·paymentKey·PG 메타·실패/취소·원문 JSON·멱등 키
--   - payment_logs: 승인/취소/웹훅 요청·응답·오류 감사 추적
--   - payment_refunds: 환불(취소) 시도 이력(CS·재처리)
-- 기존 행: payment_no / order_id 백필 후 NOT NULL. provider·status 체크 제약 갱신.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 기존 CHECK 제약 제거 후 MOCK·TOSS 및 REFUNDED·PARTIAL_CANCELED 반영
-- -----------------------------------------------------------------------------
ALTER TABLE payments DROP CONSTRAINT chk_payments_provider;
ALTER TABLE payments DROP CONSTRAINT chk_payments_status;

-- -----------------------------------------------------------------------------
-- payments: PG 연동 컬럼 추가
-- -----------------------------------------------------------------------------
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

ALTER TABLE payments ADD COLUMN raw_response_json LONGVARCHAR NULL;
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) NULL;

-- 기존 결제 행: 내부 번호·주문번호 백필(마이그레이션 일회)
UPDATE payments SET payment_no = 'LEGACY-' || CAST(id AS VARCHAR(30)) WHERE payment_no IS NULL;
UPDATE payments SET order_id = payment_reference WHERE order_id IS NULL;

ALTER TABLE payments ALTER COLUMN payment_no SET NOT NULL;
ALTER TABLE payments ALTER COLUMN order_id SET NOT NULL;

-- 유니크: 내부 결제번호·가맹점 주문번호·토스 paymentKey·승인 멱등키
ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_no UNIQUE (payment_no);
ALTER TABLE payments ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);
ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_key UNIQUE (payment_key);
ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('MOCK', 'TOSS'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (
    status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'EXPIRED', 'REFUNDED', 'PARTIAL_CANCELED')
);

-- PG 상태·키 조회용
CREATE INDEX idx_payments_external_status ON payments (external_status);
CREATE INDEX idx_payments_payment_key ON payments (payment_key);

COMMENT ON TABLE payments IS '예약별 결제 정보 및 상태 이력. PG(토스) 승인·환불·원문·멱등 키 컬럼 포함(V10 확장)';
COMMENT ON COLUMN payments.payment_no IS '내부 결제 번호(유니크)';
COMMENT ON COLUMN payments.order_id IS '가맹점 주문번호(토스 orderId, 유니크)';
COMMENT ON COLUMN payments.payment_key IS '토스 paymentKey(승인 후 저장)';
COMMENT ON COLUMN payments.order_name IS '주문명(토스 orderName)';
COMMENT ON COLUMN payments.supplied_amount IS '공급가액(토스 suppliedAmount)';
COMMENT ON COLUMN payments.vat IS '부가세(토스 vat)';
COMMENT ON COLUMN payments.tax_free_amount IS '비과세 금액(토스 taxFreeAmount)';
COMMENT ON COLUMN payments.currency IS '통화 코드(예: KRW)';
COMMENT ON COLUMN payments.gateway_method IS 'PG 결제수단 요약(토스 method)';
COMMENT ON COLUMN payments.gateway_type IS '결제 유형(토스 type: NORMAL 등)';
COMMENT ON COLUMN payments.external_status IS 'PG 원본 결제 상태(토스 status 등)';
COMMENT ON COLUMN payments.requested_at IS '결제 요청 시각(토스 requestedAt)';
COMMENT ON COLUMN payments.last_transaction_key IS '마지막 거래 키(토스 lastTransactionKey)';
COMMENT ON COLUMN payments.fail_code IS 'PG/토스 실패 코드';
COMMENT ON COLUMN payments.canceled_at IS '전액 취소 완료 시각(내부 마킹)';
COMMENT ON COLUMN payments.cancel_amount IS '누적 취소(환불) 금액';
COMMENT ON COLUMN payments.card_company IS '카드사(토스 card.company)';
COMMENT ON COLUMN payments.card_number IS '마스킹 카드번호(토스 card.number)';
COMMENT ON COLUMN payments.installment_plan_months IS '할부 개월 수';
COMMENT ON COLUMN payments.is_interest_free IS '무이자 여부';
COMMENT ON COLUMN payments.virtual_account_bank IS '가상계좌 은행';
COMMENT ON COLUMN payments.virtual_account_number IS '가상계좌 번호';
COMMENT ON COLUMN payments.virtual_account_due_date IS '가상계좌 입금 만료 시각';
COMMENT ON COLUMN payments.easy_pay_provider IS '간편결제사(토스 easyPay.provider)';
COMMENT ON COLUMN payments.raw_response_json IS 'PG 승인/오류 응답 원문(JSON)';
COMMENT ON COLUMN payments.idempotency_key IS '승인 API Idempotency-Key(중복 요청 방지, 유니크)';

-- -----------------------------------------------------------------------------
-- payment_logs: 승인·취소·웹훅 등 요청/응답/예외 기록 (운영 디버깅·감사)
-- -----------------------------------------------------------------------------
CREATE TABLE payment_logs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    payment_id BIGINT NULL,
    order_id VARCHAR(200) NULL,
    payment_key VARCHAR(200) NULL,
    log_type VARCHAR(50) NOT NULL,
    step VARCHAR(50) NULL,
    request_url VARCHAR(500) NULL,
    http_method VARCHAR(20) NULL,
    http_status INT NULL,
    request_body LONGVARCHAR NULL,
    response_body LONGVARCHAR NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    stack_trace LONGVARCHAR NULL,
    client_ip VARCHAR(100) NULL,
    user_agent VARCHAR(500) NULL,
    trace_id VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_logs_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_logs_type CHECK (
        log_type IN (
            'CONFIRM_REQUEST', 'CONFIRM_RESPONSE', 'CONFIRM_ERROR',
            'CANCEL_REQUEST', 'CANCEL_RESPONSE', 'CANCEL_ERROR',
            'WEBHOOK_RECEIVED', 'WEBHOOK_REJECTED', 'MOCK_WEBHOOK', 'SYSTEM'
        )
    ),
    CONSTRAINT chk_payment_logs_step CHECK (step IS NULL OR step IN ('CLIENT', 'SERVER', 'TOSS_API'))
);

CREATE INDEX idx_payment_logs_payment_id ON payment_logs (payment_id);
CREATE INDEX idx_payment_logs_order_id ON payment_logs (order_id);
CREATE INDEX idx_payment_logs_created_at ON payment_logs (created_at);

COMMENT ON TABLE payment_logs IS '결제 승인·취소·웹훅 요청/응답 및 오류 감사 로그';
COMMENT ON COLUMN payment_logs.id IS '로그 PK';
COMMENT ON COLUMN payment_logs.payment_id IS 'payments.id FK(삭제 시 NULL)';
COMMENT ON COLUMN payment_logs.order_id IS '주문번호 조회용(토스 orderId)';
COMMENT ON COLUMN payment_logs.payment_key IS '토스 paymentKey';
COMMENT ON COLUMN payment_logs.log_type IS 'CONFIRM_*/CANCEL_*/WEBHOOK_*/MOCK_WEBHOOK/SYSTEM';
COMMENT ON COLUMN payment_logs.step IS 'CLIENT / SERVER / TOSS_API';
COMMENT ON COLUMN payment_logs.request_url IS '호출 URL 경로';
COMMENT ON COLUMN payment_logs.http_method IS 'HTTP 메서드';
COMMENT ON COLUMN payment_logs.http_status IS 'HTTP 응답 상태 코드';
COMMENT ON COLUMN payment_logs.request_body IS '요청 본문(JSON 등)';
COMMENT ON COLUMN payment_logs.response_body IS '응답 본문(JSON 등)';
COMMENT ON COLUMN payment_logs.error_code IS '에러 코드';
COMMENT ON COLUMN payment_logs.error_message IS '에러 메시지';
COMMENT ON COLUMN payment_logs.stack_trace IS '예외 스택(내부 오류 시)';
COMMENT ON COLUMN payment_logs.client_ip IS '클라이언트 IP';
COMMENT ON COLUMN payment_logs.user_agent IS 'User-Agent';
COMMENT ON COLUMN payment_logs.trace_id IS '요청 추적 ID(X-Trace-Id 등)';
COMMENT ON COLUMN payment_logs.created_at IS '생성 시각';
COMMENT ON COLUMN payment_logs.updated_at IS '수정 시각';

-- -----------------------------------------------------------------------------
-- payment_refunds: PG 취소(환불) API 호출 단위 이력 — 실패 시 재시도·CS 대응
-- -----------------------------------------------------------------------------
CREATE TABLE payment_refunds (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    cancel_amount NUMERIC(15, 2) NOT NULL,
    cancel_reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_by VARCHAR(20) NOT NULL,
    external_transaction_key VARCHAR(200) NULL,
    fail_code VARCHAR(100) NULL,
    fail_message VARCHAR(500) NULL,
    idempotency_key VARCHAR(100) NULL,
    raw_response_json LONGVARCHAR NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_refunds_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT uk_payment_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_refunds_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_payment_refunds_requested_by CHECK (requested_by IN ('USER', 'ADMIN', 'SYSTEM', 'WEBHOOK'))
);

CREATE INDEX idx_payment_refunds_payment_id ON payment_refunds (payment_id);
CREATE INDEX idx_payment_refunds_reservation_id ON payment_refunds (reservation_id);
CREATE INDEX idx_payment_refunds_status ON payment_refunds (status);

COMMENT ON TABLE payment_refunds IS 'PG 환불(취소) 시도 이력. CS·재처리·멱등 추적';
COMMENT ON COLUMN payment_refunds.id IS '환불 이력 PK';
COMMENT ON COLUMN payment_refunds.payment_id IS 'payments.id FK';
COMMENT ON COLUMN payment_refunds.reservation_id IS 'reservations.id FK(조회 편의)';
COMMENT ON COLUMN payment_refunds.cancel_amount IS '해당 요청 취소(환불) 금액';
COMMENT ON COLUMN payment_refunds.cancel_reason IS '취소 사유(사용자/운영/시스템 문구)';
COMMENT ON COLUMN payment_refunds.status IS 'PENDING / SUCCEEDED / FAILED';
COMMENT ON COLUMN payment_refunds.requested_by IS 'USER / ADMIN / SYSTEM / WEBHOOK';
COMMENT ON COLUMN payment_refunds.external_transaction_key IS 'PG 거래 키(토스 transactionKey 등)';
COMMENT ON COLUMN payment_refunds.fail_code IS 'PG 실패 코드';
COMMENT ON COLUMN payment_refunds.fail_message IS 'PG 실패 메시지';
COMMENT ON COLUMN payment_refunds.idempotency_key IS '취소 API Idempotency-Key(유니크)';
COMMENT ON COLUMN payment_refunds.raw_response_json IS 'PG 취소 응답 원문(JSON)';
COMMENT ON COLUMN payment_refunds.created_at IS '생성 시각';
COMMENT ON COLUMN payment_refunds.updated_at IS '수정 시각';
