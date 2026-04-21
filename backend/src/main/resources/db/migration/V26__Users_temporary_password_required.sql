-- =============================================================================
-- Flyway V26 - users 임시 비밀번호 강제 변경 플래그
-- =============================================================================
-- 목적
--   User.temporaryPasswordRequired 와 매핑한다.
--   true 이면 다음 로그인 시 비밀번호 변경을 요구하는 등 플랫폼 정책에 사용한다.
--   기존 사용자는 false 로 둔다.
-- =============================================================================

alter table users
    add column if not exists temporary_password_required boolean not null default false;

comment on column users.temporary_password_required is '임시 비밀번호 발급 등으로 인해 비밀번호 변경이 필요하면 true';
