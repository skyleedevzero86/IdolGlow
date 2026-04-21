-- =============================================================================
-- Flyway V23- 인증 검증 감사 로그
-- =============================================================================
-- 목적
--   가입·비밀번호 찾기 등 이메일/계정 검증 시도를 한 행으로 남긴다.
--   AuthVerificationAuditLog 엔티티와 동일 스키마이며 관리자 API에서 조회한다.
-- 인덱스
--   created_at DESC: 최근 로그 페이지
--   verification_type + created_at: 유형별 최신순
-- =============================================================================

CREATE TABLE IF NOT EXISTS auth_verification_audit_logs (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '로그 PK',
    verification_type  VARCHAR(64)   NOT NULL COMMENT '검증 유형 코드(서비스 규약 문자열)',
    email              VARCHAR(190)  NULL COMMENT '시도에 사용된 이메일 주소',
    username           VARCHAR(120)  NULL COMMENT '시도에 사용된 사용자명 또는 닉네임',
    ip_address         VARCHAR(64)   NOT NULL COMMENT '요청 클라이언트 IP 주소',
    success            BOOLEAN       NOT NULL COMMENT '검증 성공 여부(1 성공, 0 실패)',
    detail             VARCHAR(500)  NULL COMMENT '실패 사유·메모 등 부가 설명',
    created_at         DATETIME(6)   NOT NULL COMMENT '로그가 기록된 시각'
) COMMENT='인증·검증(이메일 코드 등) 시도 감사 로그';

CREATE INDEX idx_auth_verification_audit_logs_created_at
    ON auth_verification_audit_logs (created_at DESC);

CREATE INDEX idx_auth_verification_audit_logs_type_created
    ON auth_verification_audit_logs (verification_type, created_at DESC);
