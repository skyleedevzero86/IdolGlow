-- =============================================================================
-- V12 MySQL 관리자 감사 로그
-- =============================================================================
-- 관리자 예약 취소, 슬롯 일괄 생성, 상품 옵션 삭제 등 운영 액션을 남긴다
-- 조회는 관리자 API 로 최근 행 위주로 사용한다
-- =============================================================================

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '로그 PK',
    admin_user_id  BIGINT        NOT NULL COMMENT '관리자 users.id',
    action_code    VARCHAR(64)   NOT NULL COMMENT '액션 코드 예약 취소 슬롯 생성',
    target_type    VARCHAR(64)   NOT NULL COMMENT '대상 유형 예약 슬롯 상품',
    target_id      BIGINT        NULL COMMENT '대상 엔티티 PK 없으면 null',
    detail         VARCHAR(2000) NULL COMMENT '추가 설명 JSON 또는 짧은 문장',
    created_at     DATETIME(6)   NOT NULL COMMENT '기록 시각',
    CONSTRAINT fk_admin_audit_logs_user FOREIGN KEY (admin_user_id) REFERENCES users (id)
) COMMENT='관리자 운영 액션 감사 로그';

CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs (created_at DESC);
CREATE INDEX idx_admin_audit_logs_admin_user ON admin_audit_logs (admin_user_id);
