-- =============================================================================
-- Flyway V26  - users 임시 비밀번호 강제 변경 플래그
-- =============================================================================
-- User.temporary_password_required 와 동일. 기존 행은 DEFAULT false.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN temporary_password_required BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '임시 비밀번호 발급 등으로 비밀번호 변경이 필요하면 1';
