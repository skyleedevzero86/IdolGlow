-- =============================================================================
-- V47: 빌링키 삭제 상태
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

-- 삭제 대기·완료·확정 실패를 카드 행에 남긴다. 기존 카드는 사용 중(ACTIVE)이다.
ALTER TABLE toss_billing_methods
    ADD COLUMN deletion_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '빌링키 삭제 상태. ACTIVE, DELETE_PENDING, DELETED, DELETE_FAILED',
    ADD COLUMN deletion_requested_at DATETIME(6) NULL COMMENT '사용자가 카드 삭제를 요청한 시각',
    ADD COLUMN deletion_completed_at DATETIME(6) NULL COMMENT '토스 삭제 성공 또는 이미 없음으로 확정한 시각',
    ADD COLUMN deletion_last_attempt_at DATETIME(6) NULL COMMENT '토스 삭제 API를 마지막으로 호출한 시각',
    ADD COLUMN deletion_attempt_count INT NOT NULL DEFAULT 0 COMMENT '토스 삭제 API 호출 횟수',
    ADD COLUMN deletion_failure_message VARCHAR(500) NULL COMMENT '확정 거절 시 안내 메시지. 500자 이내';

UPDATE toss_billing_methods
SET deletion_status = 'ACTIVE'
WHERE deletion_status IS NULL;

CREATE INDEX idx_toss_billing_methods_deletion_pending
    ON toss_billing_methods (deletion_status, deletion_last_attempt_at);
