-- 토스(및 기타 PG) 연동 확장: payments 확장, payment_logs, payment_refunds

ALTER TABLE payments DROP CONSTRAINT chk_payments_provider;
ALTER TABLE payments DROP CONSTRAINT chk_payments_status;

ALTER TABLE payments ADD COLUMN payment_no VARCHAR(64) NULL COMMENT '내부 결제 번호(유니크)';
ALTER TABLE payments ADD COLUMN order_id VARCHAR(200) NULL COMMENT '가맹점 주문번호(토스 orderId, 유니크)';
ALTER TABLE payments ADD COLUMN payment_key VARCHAR(200) NULL COMMENT '토스 paymentKey(승인 후 저장)';

ALTER TABLE payments ADD COLUMN order_name VARCHAR(255) NULL COMMENT '주문명(토스 orderName)';
ALTER TABLE payments ADD COLUMN supplied_amount DECIMAL(15, 2) NULL COMMENT '공급가액(토스 suppliedAmount)';
ALTER TABLE payments ADD COLUMN vat DECIMAL(15, 2) NULL COMMENT '부가세(토스 vat)';
ALTER TABLE payments ADD COLUMN tax_free_amount DECIMAL(15, 2) NULL COMMENT '비과세 금액(토스 taxFreeAmount)';
ALTER TABLE payments ADD COLUMN currency VARCHAR(10) NULL COMMENT '통화 코드(예: KRW)';
ALTER TABLE payments ADD COLUMN gateway_method VARCHAR(50) NULL COMMENT 'PG 결제수단 요약(토스 method)';
ALTER TABLE payments ADD COLUMN gateway_type VARCHAR(50) NULL COMMENT '결제 유형(토스 type: NORMAL 등)';
ALTER TABLE payments ADD COLUMN external_status VARCHAR(50) NULL COMMENT 'PG 원본 결제 상태(토스 status 등)';

ALTER TABLE payments ADD COLUMN requested_at DATETIME(6) NULL COMMENT '결제 요청 시각(토스 requestedAt)';
ALTER TABLE payments ADD COLUMN last_transaction_key VARCHAR(200) NULL COMMENT '마지막 거래 키(토스 lastTransactionKey)';
ALTER TABLE payments ADD COLUMN fail_code VARCHAR(100) NULL COMMENT 'PG/토스 실패 코드';

ALTER TABLE payments ADD COLUMN canceled_at DATETIME(6) NULL COMMENT '전액 취소 완료 시각(내부 마킹)';
ALTER TABLE payments ADD COLUMN cancel_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '누적 취소(환불) 금액';

ALTER TABLE payments ADD COLUMN card_company VARCHAR(100) NULL COMMENT '카드사(토스 card.company)';
ALTER TABLE payments ADD COLUMN card_number VARCHAR(50) NULL COMMENT '마스킹 카드번호(토스 card.number)';
ALTER TABLE payments ADD COLUMN installment_plan_months INT NULL COMMENT '할부 개월 수';
ALTER TABLE payments ADD COLUMN is_interest_free TINYINT(1) NULL COMMENT '무이자 여부';

ALTER TABLE payments ADD COLUMN virtual_account_bank VARCHAR(100) NULL COMMENT '가상계좌 은행';
ALTER TABLE payments ADD COLUMN virtual_account_number VARCHAR(100) NULL COMMENT '가상계좌 번호';
ALTER TABLE payments ADD COLUMN virtual_account_due_date DATETIME(6) NULL COMMENT '가상계좌 입금 만료 시각';

ALTER TABLE payments ADD COLUMN easy_pay_provider VARCHAR(100) NULL COMMENT '간편결제사(토스 easyPay.provider)';

ALTER TABLE payments ADD COLUMN raw_response_json JSON NULL COMMENT 'PG 승인/오류 응답 원문(JSON)';
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) NULL COMMENT '승인 API Idempotency-Key(중복 요청 방지, 유니크)';

UPDATE payments SET payment_no = CONCAT('LEGACY-', id) WHERE payment_no IS NULL;
UPDATE payments SET order_id = payment_reference WHERE order_id IS NULL;

ALTER TABLE payments MODIFY COLUMN payment_no VARCHAR(64) NOT NULL COMMENT '내부 결제 번호(유니크)';
ALTER TABLE payments MODIFY COLUMN order_id VARCHAR(200) NOT NULL COMMENT '가맹점 주문번호(토스 orderId, 유니크)';

ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_no UNIQUE (payment_no);
ALTER TABLE payments ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);
ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_key UNIQUE (payment_key);
ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('MOCK', 'TOSS'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (
    status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'EXPIRED', 'REFUNDED', 'PARTIAL_CANCELED')
);

CREATE INDEX idx_payments_external_status ON payments (external_status);

ALTER TABLE payments COMMENT = '예약별 결제 정보 및 상태 이력. PG(토스) 승인·환불·원문·멱등 키 컬럼 포함(V10 확장)';

CREATE TABLE payment_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    PRIMARY KEY (id),
    payment_id BIGINT NULL COMMENT 'payments.id FK(삭제 시 NULL)',
    order_id VARCHAR(200) NULL COMMENT '주문번호 조회용(토스 orderId)',
    payment_key VARCHAR(200) NULL COMMENT '토스 paymentKey',
    log_type VARCHAR(50) NOT NULL COMMENT 'CONFIRM_*/CANCEL_*/WEBHOOK_*/MOCK_WEBHOOK/SYSTEM',
    step VARCHAR(50) NULL COMMENT 'CLIENT / SERVER / TOSS_API',
    request_url VARCHAR(500) NULL COMMENT '호출 URL 경로',
    http_method VARCHAR(20) NULL COMMENT 'HTTP 메서드',
    http_status INT NULL COMMENT 'HTTP 응답 상태 코드',
    request_body JSON NULL COMMENT '요청 본문(JSON 등)',
    response_body JSON NULL COMMENT '응답 본문(JSON 등)',
    error_code VARCHAR(100) NULL COMMENT '에러 코드',
    error_message VARCHAR(1000) NULL COMMENT '에러 메시지',
    stack_trace TEXT NULL COMMENT '예외 스택(내부 오류 시)',
    client_ip VARCHAR(100) NULL COMMENT '클라이언트 IP',
    user_agent VARCHAR(500) NULL COMMENT 'User-Agent',
    trace_id VARCHAR(100) NULL COMMENT '요청 추적 ID(X-Trace-Id 등)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_payment_logs_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_logs_type CHECK (
        log_type IN (
            'CONFIRM_REQUEST', 'CONFIRM_RESPONSE', 'CONFIRM_ERROR',
            'CANCEL_REQUEST', 'CANCEL_RESPONSE', 'CANCEL_ERROR',
            'WEBHOOK_RECEIVED', 'WEBHOOK_REJECTED', 'MOCK_WEBHOOK', 'SYSTEM'
        )
    ),
    CONSTRAINT chk_payment_logs_step CHECK (step IS NULL OR step IN ('CLIENT', 'SERVER', 'TOSS_API'))
) COMMENT = '결제 승인·취소·웹훅 요청/응답 및 오류 감사 로그';

CREATE INDEX idx_payment_logs_payment_id ON payment_logs (payment_id);
CREATE INDEX idx_payment_logs_order_id ON payment_logs (order_id);
CREATE INDEX idx_payment_logs_created_at ON payment_logs (created_at);

CREATE TABLE payment_refunds (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '환불 이력 PK',
    PRIMARY KEY (id),
    payment_id BIGINT NOT NULL COMMENT 'payments.id FK',
    reservation_id BIGINT NOT NULL COMMENT 'reservations.id FK(조회 편의)',
    cancel_amount DECIMAL(15, 2) NOT NULL COMMENT '해당 요청 취소(환불) 금액',
    cancel_reason VARCHAR(500) NOT NULL COMMENT '취소 사유(사용자/운영/시스템 문구)',
    status VARCHAR(30) NOT NULL COMMENT 'PENDING / SUCCEEDED / FAILED',
    requested_by VARCHAR(20) NOT NULL COMMENT 'USER / ADMIN / SYSTEM / WEBHOOK',
    external_transaction_key VARCHAR(200) NULL COMMENT 'PG 거래 키(토스 transactionKey 등)',
    fail_code VARCHAR(100) NULL COMMENT 'PG 실패 코드',
    fail_message VARCHAR(500) NULL COMMENT 'PG 실패 메시지',
    idempotency_key VARCHAR(100) NULL COMMENT '취소 API Idempotency-Key(유니크)',
    raw_response_json JSON NULL COMMENT 'PG 취소 응답 원문(JSON)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_refunds_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT uk_payment_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_refunds_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_payment_refunds_requested_by CHECK (requested_by IN ('USER', 'ADMIN', 'SYSTEM', 'WEBHOOK'))
) COMMENT = 'PG 환불(취소) 시도 이력. CS·재처리·멱등 추적';

CREATE INDEX idx_payment_refunds_payment_id ON payment_refunds (payment_id);
CREATE INDEX idx_payment_refunds_reservation_id ON payment_refunds (reservation_id);
CREATE INDEX idx_payment_refunds_status ON payment_refunds (status);
