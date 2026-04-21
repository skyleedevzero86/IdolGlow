-- =============================================================================
-- Flyway V26  - users 임시 비밀번호 강제 변경 플래그
-- =============================================================================
-- User.temporary_password_required 와 일치. 기존 행은 DEFAULT false.
-- =============================================================================

alter table users
    add column if not exists temporary_password_required boolean not null default false;

COMMENT ON COLUMN users.temporary_password_required IS '임시 비밀번호 발급 등으로 비밀번호 변경이 필요하면 true';
