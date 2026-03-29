-- =============================================================================
-- V12 PostgreSQL 관리자 감사 로그
-- =============================================================================
-- 목적
--   관리자가 수행한 예약 취소, 슬롯 일괄 생성·삭제, 상품·옵션 삭제 등을 한 행으로 남긴다
--   운영 추적과 사후 조사에 쓰이며 애플리케이션 AdminAuditService 가 INSERT 한다
-- 인덱스
--   created_at 내림차순으로 최근 로그 조회
--   admin_user_id 로 특정 관리자 필터
-- =============================================================================

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id             BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_user_id  BIGINT        NOT NULL,
    action_code    VARCHAR(64)   NOT NULL,
    target_type    VARCHAR(64)   NOT NULL,
    target_id      BIGINT,
    detail         VARCHAR(2000),
    created_at     TIMESTAMP(6)  NOT NULL,
    CONSTRAINT fk_admin_audit_logs_user FOREIGN KEY (admin_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin_user ON admin_audit_logs (admin_user_id);

COMMENT ON TABLE admin_audit_logs IS '관리자 운영 액션 감사 로그';

COMMENT ON COLUMN admin_audit_logs.id IS '로그 PK 자동 증가';
COMMENT ON COLUMN admin_audit_logs.admin_user_id IS '행동한 관리자 users.id FK';
COMMENT ON COLUMN admin_audit_logs.action_code IS '비즈니스 액션 코드 예 RESERVATION_CANCEL SLOTS_BULK_CREATE';
COMMENT ON COLUMN admin_audit_logs.target_type IS '대상 도메인 유형 예 RESERVATION PRODUCT RESERVATION_SLOT OPTION';
COMMENT ON COLUMN admin_audit_logs.target_id IS '대상 엔티티 PK 없으면 null';
COMMENT ON COLUMN admin_audit_logs.detail IS '추가 맥락 짧은 JSON 또는 문장 최대 2000자';
COMMENT ON COLUMN admin_audit_logs.created_at IS '기록 시각';

COMMENT ON INDEX idx_admin_audit_logs_created_at IS '최근 로그 목록 조회용';
COMMENT ON INDEX idx_admin_audit_logs_admin_user IS '관리자별 로그 조회용';
