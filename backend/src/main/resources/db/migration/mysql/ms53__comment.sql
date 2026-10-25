-- =============================================================================
-- comment: 근거 검색 문단
-- -----
-- 목적
-- - 근거 검색 문단과 임베딩 컬럼 설명을 둔다
-- 대상
-- - ms
-- 주의
-- - 테이블 생성 다음에 실행한다
-- =============================================================================

ALTER TABLE rag_chunks COMMENT = '상품, 옵션, 웹진 문단. 가격과 예약 슬롯은 본문에 없다';
ALTER TABLE rag_chunks MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '문단 PK';
ALTER TABLE rag_chunks MODIFY COLUMN source_type VARCHAR(32) NOT NULL COMMENT '출처 종류. WEBZINE_ARTICLE, WEBZINE_SECTION, PRODUCT, OPTION';
ALTER TABLE rag_chunks MODIFY COLUMN source_id BIGINT NOT NULL COMMENT '출처 테이블의 PK';
ALTER TABLE rag_chunks MODIFY COLUMN link_key VARCHAR(400) NOT NULL COMMENT '원문 화면으로 가는 키';
ALTER TABLE rag_chunks MODIFY COLUMN content TEXT NOT NULL COMMENT '검색 본문';
ALTER TABLE rag_chunks MODIFY COLUMN content_hash VARCHAR(64) NOT NULL COMMENT '본문 SHA-256. 같으면 임베딩을 다시 하지 않는다';
ALTER TABLE rag_chunks MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 시각';
ALTER TABLE rag_chunks MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '수정 시각';

ALTER TABLE rag_chunk_embeddings COMMENT = '문단 임베딩 바이트. 유사도는 애플리케이션에서 계산한다';
ALTER TABLE rag_chunk_embeddings MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '임베딩 PK';
ALTER TABLE rag_chunk_embeddings MODIFY COLUMN chunk_id BIGINT NOT NULL COMMENT 'rag_chunks.id';
ALTER TABLE rag_chunk_embeddings MODIFY COLUMN model VARCHAR(80) NOT NULL COMMENT '임베딩 모델 이름';
ALTER TABLE rag_chunk_embeddings MODIFY COLUMN dims INT NOT NULL COMMENT '벡터 차원 수';
ALTER TABLE rag_chunk_embeddings MODIFY COLUMN vector_bytes LONGBLOB NOT NULL COMMENT '임베딩 바이트';
ALTER TABLE rag_chunk_embeddings MODIFY COLUMN content_hash VARCHAR(64) NOT NULL COMMENT '임베딩에 쓴 본문 해시';
