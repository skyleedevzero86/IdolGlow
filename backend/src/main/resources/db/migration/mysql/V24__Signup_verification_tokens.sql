-- =============================================================================
-- Flyway V24  - 가입·계정 검증 토큰
-- =============================================================================
-- 목적
--   이메일 가입 인증, 계정 확인 등 일회성 토큰을 저장한다.
--   SignupVerificationToken 엔티티와 동일 스키마.
-- 인덱스
--   uk_signup_verification_tokens_token: 토큰 단건 조회
--   idx_signup_verification_tokens_email_type_status_created: 최신 행 조회
-- =============================================================================

CREATE TABLE IF NOT EXISTS signup_verification_tokens (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '행 PK',
    token          VARCHAR(120)  NOT NULL COMMENT '일회성 검증 토큰(전역 유일)',
    type           VARCHAR(40)   NOT NULL COMMENT 'SignupVerificationType 문자열',
    email          VARCHAR(190)  NOT NULL COMMENT '검증 대상 이메일',
    username       VARCHAR(120)  NULL COMMENT '닉네임 등(선택)',
    user_id        BIGINT        NULL COMMENT '확인 후 연결된 users.id(선택)',
    status         VARCHAR(30)   NOT NULL COMMENT 'SignupVerificationStatus 문자열',
    requested_ip   VARCHAR(64)   NOT NULL COMMENT '발급 요청 IP',
    confirmed_ip   VARCHAR(64)   NULL COMMENT '검증 완료 시 IP(선택)',
    expires_at     DATETIME(6)   NOT NULL COMMENT '만료 시각',
    created_at     DATETIME(6)   NOT NULL COMMENT '행 생성 시각',
    confirmed_at   DATETIME(6)   NULL COMMENT '검증·사용 완료 시각(선택)',
    detail         VARCHAR(500)  NULL COMMENT '거절 사유 등(선택)',
    CONSTRAINT uk_signup_verification_tokens_token UNIQUE (token)
) COMMENT='가입·계정 확인용 검증 토큰 저장소';

CREATE INDEX idx_signup_verification_tokens_email_type_status_created
    ON signup_verification_tokens (email, type, status, created_at DESC);
