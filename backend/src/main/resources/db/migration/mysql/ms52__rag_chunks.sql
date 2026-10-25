-- =============================================================================
-- rag chunks
-- -----
-- 목적
-- - 상품, 옵션, 웹진 문단과 임베딩을 저장한다
-- 대상
-- - ms
-- 주의
-- - 가격과 예약 슬롯은 본문에 넣지 않는다
-- - 키워드 검색은 ngram FULLTEXT를 쓴다. 토큰 길이 기본값은 2다
-- - 임베딩은 blob으로 저장하고 유사도는 애플리케이션에서 계산한다
-- =============================================================================

CREATE TABLE rag_chunks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    link_key VARCHAR(400) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_rag_chunk_source UNIQUE (source_type, source_id),
    FULLTEXT INDEX ft_rag_chunks_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rag_chunk_embeddings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chunk_id BIGINT NOT NULL,
    model VARCHAR(80) NOT NULL,
    dims INT NOT NULL,
    vector_bytes LONGBLOB NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    CONSTRAINT uk_rag_chunk_embedding_chunk UNIQUE (chunk_id),
    CONSTRAINT fk_rag_chunk_embedding_chunk FOREIGN KEY (chunk_id) REFERENCES rag_chunks (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
