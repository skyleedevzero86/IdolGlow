ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL;

COMMENT ON COLUMN users.password_hash IS '로컬 가입 비밀번호(BCrypt). OAuth 전용 계정은 NULL';

DROP INDEX IF EXISTS idx_users_email;

CREATE UNIQUE INDEX uq_users_email ON users (email);

CREATE UNIQUE INDEX uq_users_nickname ON users (nickname);
