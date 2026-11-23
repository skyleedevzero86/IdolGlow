-- =============================================================================
-- 상품 목록 실행 계획 보기
-- -----
-- 목적
-- - GET /products 한 번에 나가는 SQL 을 EXPLAIN ANALYZE BUFFERS 로 돌려 실제 시간과 읽은 블록 수를 본다
-- 대상
-- - 포스트그레스 / 개발 DB idolglow 또는 측정 DB idolglow_perf
-- 실행
-- - psql -d idolglow_perf -f explain-products.sql
-- 주의
-- - SELECT 만 실행하므로 데이터는 바뀌지 않는다
-- - SQL 모양은 ProductSearchQueryRepository, ProductDiscoveryQueryRepository, ProductQueryRepository 와 같게 맞췄다
-- =============================================================================

\set ON_ERROR_STOP on
\pset pager off

-- 화면에 보일 20개 상품 번호를 먼저 구해 둔다
SELECT string_agg(id::text, ',') AS ids FROM (SELECT id FROM products ORDER BY id DESC LIMIT 20) t \gset


\echo '===== Q1. 기본 정렬, 최신순 첫 페이지 ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id
FROM products p
WHERE 1 = 1
AND p.id < 9223372036854775807
ORDER BY p.id DESC
LIMIT 20;


\echo '===== Q2. 키워드 검색, 걸리는 상품이 많은 말 ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id
FROM products p
WHERE 1 = 1
AND (
  LOWER(p.name) LIKE '%성수%' ESCAPE '\'
  OR LOWER(p.description) LIKE '%성수%' ESCAPE '\'
  OR EXISTS (
    SELECT 1 FROM product_tag ptk
    WHERE ptk.product_id = p.id
    AND LOWER(ptk.tag_name) LIKE '%성수%' ESCAPE '\'
  )
)
AND p.id < 9223372036854775807
ORDER BY p.id DESC
LIMIT 20;


\echo '===== Q3. 키워드 검색, 아무 상품에도 없는 말 ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id
FROM products p
WHERE 1 = 1
AND (
  LOWER(p.name) LIKE '%없는검색어%' ESCAPE '\'
  OR LOWER(p.description) LIKE '%없는검색어%' ESCAPE '\'
  OR EXISTS (
    SELECT 1 FROM product_tag ptk
    WHERE ptk.product_id = p.id
    AND LOWER(ptk.tag_name) LIKE '%없는검색어%' ESCAPE '\'
  )
)
AND p.id < 9223372036854775807
ORDER BY p.id DESC
LIMIT 20;


\echo '===== Q4. 인기순 정렬 첫 페이지 ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.pid FROM (
    SELECT p.id AS pid,
        (SELECT COUNT(*) FROM wishes w WHERE w.product_id = p.id) AS wc,
        COALESCE((
            SELECT AVG(CAST(pr.rating AS DOUBLE PRECISION)) FROM product_reviews pr WHERE pr.product_id = p.id AND pr.hidden_at IS NULL
        ), 0) AS ar,
        COALESCE((
            SELECT COUNT(*) FROM product_reviews pr2 WHERE pr2.product_id = p.id AND pr2.hidden_at IS NULL
        ), 0) AS rc
    FROM products p
    WHERE 1 = 1
) t
ORDER BY t.wc DESC, t.ar DESC, t.rc DESC, t.pid DESC
LIMIT 20 OFFSET 0;


\echo '===== Q5. 찜 수와 리뷰 점수, 20개 상품 ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT w.product_id, count(w.id) FROM wishes w WHERE w.product_id IN (:ids) GROUP BY w.product_id;

EXPLAIN (ANALYZE, BUFFERS)
SELECT r.product_id, coalesce(avg(r.rating), 0), count(r.id)
FROM product_reviews r WHERE r.product_id IN (:ids) AND r.hidden_at IS NULL GROUP BY r.product_id;


\echo '===== Q6. 20개 상품 내용 채우기, 옵션과 위치를 함께 ====='
EXPLAIN (ANALYZE, BUFFERS)
SELECT DISTINCT p.*, po.*, o.*, pl.*
FROM products p
LEFT JOIN product_option po ON po.product_id = p.id
LEFT JOIN options o ON o.id = po.option_id
LEFT JOIN product_locations pl ON pl.product_id = p.id
WHERE p.id IN (:ids);

EXPLAIN (ANALYZE, BUFFERS)
SELECT pt.* FROM product_tag pt WHERE pt.product_id IN (:ids);
