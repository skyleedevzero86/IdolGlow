-- =============================================================================
-- V39: 토스 빌링 재시도·해지예약 컬럼
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

COMMENT ON COLUMN toss_billing_subscriptions.fail_count IS '연속 결제 실패 횟수. 1회는 하루 뒤, 2회는 이틀 뒤 재시도, 3회는 일시중지';
COMMENT ON COLUMN toss_billing_subscriptions.retry_at IS '다음 재시도 시각. 비어 있으면 다음결제일 기준으로 도래 판단';
COMMENT ON COLUMN toss_billing_subscriptions.ends_at IS '해지 예약 시 혜택 유지 종료 시각. 보통 다음 결제일. 이후 해지완료';
