-- =============================================================================
-- V43: 다중 카드 결제수단과 회차 시도 이력 마이에스큐엘
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

UPDATE toss_billing_methods m
    INNER JOIN (
        SELECT user_id, MAX(id) AS id
        FROM toss_billing_methods
        WHERE active = TRUE
        GROUP BY user_id
    ) t ON m.id = t.id
SET m.is_default = TRUE
WHERE m.is_default = FALSE;

ALTER TABLE toss_billing_methods
    MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '삭제하지 않은 사용 가능 카드 여부. 여러 장 동시 활성 가능',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디. 활성 카드는 사용자당 여러 장 가능',
    MODIFY COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '구독 자동결제에 쓸 기본 수단 여부';

CREATE INDEX idx_toss_billing_methods_user_active_default
    ON toss_billing_methods (user_id, active, is_default);

CREATE TABLE IF NOT EXISTS toss_billing_charge_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회차 결제 시도 기본키',
    charge_id BIGINT NOT NULL COMMENT '소속 청구 회차 아이디 외래키',
    billing_method_id BIGINT NOT NULL COMMENT '이번 시도에 사용한 결제수단 아이디 외래키',
    attempt_no INT NOT NULL COMMENT '회차 내 시도 순번. 1부터 증가',
    toss_order_id VARCHAR(220) NOT NULL COMMENT '토스에 보낸 주문번호. 회차 orderId에 시도 번호를 붙인 값',
    payment_key VARCHAR(200) NULL COMMENT '토스 결제키. 승인 성공 시 저장',
    status VARCHAR(30) NOT NULL COMMENT '상태. 대기, 성공, 실패',
    fail_message VARCHAR(500) NULL COMMENT '실패 시 안내 메시지',
    raw_response TEXT NULL COMMENT '토스 에이피아이 원문 응답',
    approved_at DATETIME(6) NULL COMMENT '토스 승인 완료 시각',
    created_at DATETIME(6) NOT NULL COMMENT '시도 행 생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각',
    CONSTRAINT uk_toss_billing_charge_attempts_charge_no UNIQUE (charge_id, attempt_no),
    CONSTRAINT uk_toss_billing_charge_attempts_toss_order UNIQUE (toss_order_id),
    CONSTRAINT fk_toss_billing_charge_attempts_charge
        FOREIGN KEY (charge_id) REFERENCES toss_billing_charges (id),
    CONSTRAINT fk_toss_billing_charge_attempts_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id)
) COMMENT='빌링 회차 내 카드별 결제 시도 이력';

CREATE INDEX idx_toss_billing_charge_attempts_charge_id
    ON toss_billing_charge_attempts (charge_id);
