-- =============================================================================
-- Flyway V25 - users 임시 비밀번호 발급 시각
-- =============================================================================
-- User 엔티티 temporary_password_issued_at 과 동일.
-- MySQL 은 ADD COLUMN IF NOT EXISTS 가 버전에 따라 없을 수 있어 단순 ADD 만 사용한다.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN temporary_password_issued_at DATETIME(6) NULL
        COMMENT '플랫폼 임시 비밀번호를 마지막으로 발급한 시각. 미발급이면 NULL';
