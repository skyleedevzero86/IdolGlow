-- =============================================================================
-- V43: 다중 카드 결제수단과 회차 시도 이력 포스트그레스
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

UPDATE toss_billing_methods m
SET is_default = TRUE
WHERE m.id IN (
    SELECT id FROM (
        SELECT MAX(id) AS id
        FROM toss_billing_methods
        WHERE active = TRUE
        GROUP BY user_id
    ) t
)
AND m.is_default = FALSE;

CREATE INDEX IF NOT EXISTS idx_toss_billing_methods_user_active_default
    ON toss_billing_methods (user_id, active, is_default);

COMMENT ON COLUMN toss_billing_methods.user_id IS '회원 아이디. 활성 카드는 사용자당 여러 장 가능';
COMMENT ON COLUMN toss_billing_methods.active IS '삭제하지 않은 사용 가능 카드 여부. 여러 장 동시 활성 가능';
COMMENT ON COLUMN toss_billing_methods.is_default IS '구독 자동결제에 쓸 기본 수단 여부';

CREATE TABLE IF NOT EXISTS toss_billing_charge_attempts (
    id BIGSERIAL PRIMARY KEY,
    charge_id BIGINT NOT NULL,
    billing_method_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    toss_order_id VARCHAR(220) NOT NULL,
    payment_key VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    fail_message VARCHAR(500),
    raw_response TEXT,
    approved_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_toss_billing_charge_attempts_charge_no UNIQUE (charge_id, attempt_no),
    CONSTRAINT uk_toss_billing_charge_attempts_toss_order UNIQUE (toss_order_id),
    CONSTRAINT fk_toss_billing_charge_attempts_charge
        FOREIGN KEY (charge_id) REFERENCES toss_billing_charges (id),
    CONSTRAINT fk_toss_billing_charge_attempts_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id)
);

CREATE INDEX IF NOT EXISTS idx_toss_billing_charge_attempts_charge_id
    ON toss_billing_charge_attempts (charge_id);

COMMENT ON TABLE toss_billing_charge_attempts IS '빌링 회차 내 카드별 결제 시도 이력';
COMMENT ON COLUMN toss_billing_charge_attempts.id IS '회차 결제 시도 기본키';
COMMENT ON COLUMN toss_billing_charge_attempts.charge_id IS '소속 청구 회차 아이디 외래키';
COMMENT ON COLUMN toss_billing_charge_attempts.billing_method_id IS '이번 시도에 사용한 결제수단 아이디 외래키';
COMMENT ON COLUMN toss_billing_charge_attempts.attempt_no IS '회차 내 시도 순번. 1부터 증가';
COMMENT ON COLUMN toss_billing_charge_attempts.toss_order_id IS '토스에 보낸 주문번호. 회차 orderId에 시도 번호를 붙인 값';
COMMENT ON COLUMN toss_billing_charge_attempts.payment_key IS '토스 결제키. 승인 성공 시 저장';
COMMENT ON COLUMN toss_billing_charge_attempts.status IS '상태. 대기, 성공, 실패';
COMMENT ON COLUMN toss_billing_charge_attempts.fail_message IS '실패 시 안내 메시지';
COMMENT ON COLUMN toss_billing_charge_attempts.raw_response IS '토스 에이피아이 원문 응답';
COMMENT ON COLUMN toss_billing_charge_attempts.approved_at IS '토스 승인 완료 시각';
COMMENT ON COLUMN toss_billing_charge_attempts.created_at IS '시도 행 생성 시각';
COMMENT ON COLUMN toss_billing_charge_attempts.updated_at IS '행 수정 시각';
