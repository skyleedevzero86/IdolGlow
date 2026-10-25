-- 최초 DB 생성 시 docker-entrypoint-initdb.d 에서 자동 실행
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_search;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
