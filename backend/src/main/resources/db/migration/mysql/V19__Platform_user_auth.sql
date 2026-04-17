-- =============================================================================
-- V19 (MySQL): 플랫폼 인증용 users 확장·비밀번호 이력·역할(USER, ADMIN)
-- =============================================================================

ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
    COMMENT '계정 승인·정지 상태(PENDING, APPROVED, REJECTED, SUSPENDED, WITHDRAWN)';
ALTER TABLE users ADD COLUMN platform_username VARCHAR(50) NULL
    COMMENT '플랫폼 가입 시 로그인명(계정 복구 시 이메일과 함께 사용)';
ALTER TABLE users ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0
    COMMENT '연속 로그인 실패 횟수';
ALTER TABLE users ADD COLUMN account_locked_at TIMESTAMP NULL
    COMMENT '계정 잠금 처리 시각';
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP NULL
    COMMENT '마지막 비밀번호 변경 시각';
ALTER TABLE users ADD COLUMN last_password_change_date DATE NULL
    COMMENT '당일 비밀번호 변경 횟수 집계 기준일';
ALTER TABLE users ADD COLUMN password_change_daily_count INT NOT NULL DEFAULT 0
    COMMENT '기준일 당일 비밀번호 변경 횟수';

CREATE UNIQUE INDEX uq_users_platform_username ON users (platform_username);

CREATE TABLE user_password_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 행 PK' PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 등으로 인코딩된 비밀번호',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이력 저장 시각',
    CONSTRAINT fk_user_password_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) COMMENT = '과거 비밀번호 해시(재사용 방지 조회용)';

CREATE INDEX idx_user_password_history_user_created ON user_password_history (user_id, created_at DESC);
