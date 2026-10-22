-- =============================================================================
-- V46: 결제 환불 처리 중 상태와 환불 첫 요청 시각
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

-- 환불 이력에 첫 요청 시각을 남긴다. 기존 행은 생성 시각(created_at)으로 채운다.
ALTER TABLE payment_refunds
    ADD COLUMN first_requested_at DATETIME(6) NULL COMMENT '환불 첫 요청 시각. 기존 행은 created_at으로 채운다. NULL 허용.';

UPDATE payment_refunds
SET first_requested_at = created_at
WHERE first_requested_at IS NULL;
