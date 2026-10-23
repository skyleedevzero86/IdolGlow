-- =============================================================================
-- V47: 빌링키 삭제 상태
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

-- 삭제 대기·완료·확정 실패를 카드 행에 남긴다. 기존 카드는 사용 중(ACTIVE)이다.
ALTER TABLE toss_billing_methods
    ADD COLUMN IF NOT EXISTS deletion_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS deletion_completed_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS deletion_last_attempt_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS deletion_attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deletion_failure_message VARCHAR(500);

UPDATE toss_billing_methods
SET deletion_status = 'ACTIVE'
WHERE deletion_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_toss_billing_methods_deletion_pending
    ON toss_billing_methods (deletion_status, deletion_last_attempt_at);

COMMENT ON COLUMN toss_billing_methods.deletion_status IS '빌링키 삭제 상태. ACTIVE, DELETE_PENDING, DELETED, DELETE_FAILED';
COMMENT ON COLUMN toss_billing_methods.deletion_requested_at IS '사용자가 카드 삭제를 요청한 시각';
COMMENT ON COLUMN toss_billing_methods.deletion_completed_at IS '토스 삭제 성공 또는 이미 없음으로 확정한 시각';
COMMENT ON COLUMN toss_billing_methods.deletion_last_attempt_at IS '토스 삭제 API를 마지막으로 호출한 시각';
COMMENT ON COLUMN toss_billing_methods.deletion_attempt_count IS '토스 삭제 API 호출 횟수';
COMMENT ON COLUMN toss_billing_methods.deletion_failure_message IS '확정 거절 시 안내 메시지. 500자 이내';
