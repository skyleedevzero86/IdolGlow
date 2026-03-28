ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '로컬 가입 비밀번호(BCrypt), OAuth만 연동 시 NULL';

DROP INDEX idx_users_email ON users;

CREATE UNIQUE INDEX uq_users_email ON users (email);

CREATE UNIQUE INDEX uq_users_nickname ON users (nickname);
