-- =============================================================================
-- Flyway V25 - users 임시 비밀번호 발급 시각
-- =============================================================================
-- 목적
--   User.temporaryPasswordIssuedAt 과 매핑한다.
--   임시 비밀번호 발급 시각을 두면 재발급 쿨다운·만료 정책에 활용할 수 있다.
--   기존 행은 NULL 미발급 또는 레거시 데이터.
-- =============================================================================

alter table users
    add column if not exists temporary_password_issued_at timestamp(6);

comment on column users.temporary_password_issued_at is '플랫폼 임시 비밀번호를 마지막으로 발급한 시각. 미발급이면 NULL';
