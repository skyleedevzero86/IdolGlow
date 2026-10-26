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
-- - 테이블과 컬럼 설명은 이 파일에 둔다
-- =============================================================================

CREATE TABLE rag_chunks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '문단 PK',
    source_type VARCHAR(32) NOT NULL COMMENT '출처 종류. WEBZINE_ARTICLE, WEBZINE_SECTION, PRODUCT, OPTION',
    source_id BIGINT NOT NULL COMMENT '출처 테이블의 PK',
    link_key VARCHAR(400) NOT NULL COMMENT '원문 화면으로 가는 키',
    content TEXT NOT NULL COMMENT '검색 본문',
    content_hash VARCHAR(64) NOT NULL COMMENT '본문 SHA-256. 같으면 임베딩을 다시 하지 않는다',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_rag_chunk_source UNIQUE (source_type, source_id),
    FULLTEXT INDEX ft_rag_chunks_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품, 옵션, 웹진 문단. 가격과 예약 슬롯은 본문에 없다';

CREATE TABLE rag_chunk_embeddings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '임베딩 PK',
    chunk_id BIGINT NOT NULL COMMENT 'rag_chunks.id',
    model VARCHAR(80) NOT NULL COMMENT '임베딩 모델 이름',
    dims INT NOT NULL COMMENT '벡터 차원 수',
    vector_bytes LONGBLOB NOT NULL COMMENT '임베딩 바이트',
    content_hash VARCHAR(64) NOT NULL COMMENT '임베딩에 쓴 본문 해시',
    CONSTRAINT uk_rag_chunk_embedding_chunk UNIQUE (chunk_id),
    CONSTRAINT fk_rag_chunk_embedding_chunk FOREIGN KEY (chunk_id) REFERENCES rag_chunks (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='문단 임베딩 바이트. 유사도는 애플리케이션에서 계산한다';
