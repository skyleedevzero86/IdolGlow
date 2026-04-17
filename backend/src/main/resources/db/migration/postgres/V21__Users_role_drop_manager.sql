-- =============================================================================
-- V21 (PostgreSQL 전용 경로): MANAGER 역할 제거. 기존 MANAGER 행은 ADMIN으로 승격.
-- =============================================================================

UPDATE users SET role = 'ADMIN' WHERE role = 'MANAGER';

ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

COMMENT ON COLUMN users.role IS '권한 역할(USER: 일반, ADMIN: 관리자)';
