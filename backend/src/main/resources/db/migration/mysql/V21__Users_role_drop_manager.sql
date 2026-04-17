-- =============================================================================
-- V21 (MySQL): MANAGER 역할 제거. 기존 MANAGER 행은 ADMIN으로 승격.
-- =============================================================================

UPDATE users SET role = 'ADMIN' WHERE role = 'MANAGER';

ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users MODIFY COLUMN role VARCHAR(255) NOT NULL COMMENT '권한 역할(USER: 일반, ADMIN: 관리자)';
