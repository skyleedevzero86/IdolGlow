-- =============================================================================
-- Flyway V25  - users 임시 비밀번호 발급 시각
-- =============================================================================
-- User 엔티티 temporary_password_issued_at 컬럼과 일치한다.
-- temporary_password_required 와 함께 쓰이며, 발급 이력 시각을 남긴다.
-- =============================================================================

alter table users
    add column if not exists temporary_password_issued_at timestamp(6);

COMMENT ON COLUMN users.temporary_password_issued_at IS '플랫폼 임시 비밀번호를 마지막으로 발급한 시각. 미발급이면 NULL';
