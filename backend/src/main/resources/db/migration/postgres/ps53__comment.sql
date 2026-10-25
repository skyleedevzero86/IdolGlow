-- =============================================================================
-- comment: 근거 검색 문단
-- -----
-- 목적
-- - 근거 검색 문단과 임베딩 컬럼 설명을 둔다
-- 대상
-- - ps
-- 주의
-- - 테이블 생성 다음에 실행한다
-- =============================================================================

COMMENT ON TABLE rag_chunks IS '상품, 옵션, 웹진 문단. 가격과 예약 슬롯은 본문에 없다';
COMMENT ON COLUMN rag_chunks.id IS '문단 PK';
COMMENT ON COLUMN rag_chunks.source_type IS '출처 종류. WEBZINE_ARTICLE, WEBZINE_SECTION, PRODUCT, OPTION';
COMMENT ON COLUMN rag_chunks.source_id IS '출처 테이블의 PK';
COMMENT ON COLUMN rag_chunks.link_key IS '원문 화면으로 가는 키';
COMMENT ON COLUMN rag_chunks.content IS '검색 본문';
COMMENT ON COLUMN rag_chunks.content_hash IS '본문 SHA-256. 같으면 임베딩을 다시 하지 않는다';
COMMENT ON COLUMN rag_chunks.created_at IS '생성 시각';
COMMENT ON COLUMN rag_chunks.updated_at IS '수정 시각';

COMMENT ON TABLE rag_chunk_embeddings IS '문단 임베딩 바이트. 유사도는 애플리케이션에서 계산한다';
COMMENT ON COLUMN rag_chunk_embeddings.id IS '임베딩 PK';
COMMENT ON COLUMN rag_chunk_embeddings.chunk_id IS 'rag_chunks.id';
COMMENT ON COLUMN rag_chunk_embeddings.model IS '임베딩 모델 이름';
COMMENT ON COLUMN rag_chunk_embeddings.dims IS '벡터 차원 수';
COMMENT ON COLUMN rag_chunk_embeddings.vector_bytes IS '임베딩 바이트';
COMMENT ON COLUMN rag_chunk_embeddings.content_hash IS '임베딩에 쓴 본문 해시';
