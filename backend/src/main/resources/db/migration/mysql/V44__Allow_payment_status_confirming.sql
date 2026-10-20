-- =============================================================================
-- V44: 결제 상태에 승인 처리 중(CONFIRMING) 추가
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
