-- =============================================================================
-- 부하 측정용 데이터 만들기
-- -----
-- 목적
-- - 상품이 많을 때 상품 목록 조회와 실행 계획이 어떻게 달라지는지 재기 위한 데이터를 만든다
-- 대상
-- - 포스트그레스 / 측정 전용 DB idolglow_perf
-- 만드는 양, n = 상품 수
-- - 회원      n × 2
-- - 찜        회원마다 15개
-- - 리뷰      회원마다 5개, 별점 1 ~ 5
-- - 태그      상품마다 3개, 태그 20종 중에서
-- - 예약 슬롯 상품마다 10개, 내일부터 5일 동안 하루 2개
-- 실행
-- - psql -d idolglow_perf -v n=10000 -f seed-products.sql
-- 주의
-- - 맨 앞에서 측정 데이터 표를 TRUNCATE 한다. 개발 DB 에서는 절대 실행하지 않는다
-- - 같은 n 으로 다시 돌리면 같은 데이터가 만들어진다
-- =============================================================================

\set ON_ERROR_STOP on


-- -----------------------------------------------------------------------------
-- 1. 비우기
-- -----------------------------------------------------------------------------
-- 앞서 넣은 측정 데이터를 지우고 번호를 1부터 다시 시작한다
TRUNCATE wishes, product_reviews, reservation_slots, product_tag, product_option, product_locations, products, options, users RESTART IDENTITY CASCADE;


-- -----------------------------------------------------------------------------
-- 2. 회원과 옵션
-- -----------------------------------------------------------------------------
-- 회원: 찜과 리뷰를 남길 사람들
INSERT INTO users (email, nickname, role)
SELECT 'perf' || i || '@idolglow.test', 'p' || i, 'USER'
FROM generate_series(1, :n * 2) AS i;

-- 옵션: 모든 상품이 나눠 쓰는 5개, 가격 1만 ~ 5만 원
INSERT INTO options (name, description, price, location, created_at, updated_at)
SELECT '옵션 ' || i, '측정용 옵션 ' || i, 10000 * i, '서울', now(), now()
FROM generate_series(1, 5) AS i;


-- -----------------------------------------------------------------------------
-- 3. 상품
-- -----------------------------------------------------------------------------
-- 상품: 지역 8가지 × 분류 8가지를 섞은 이름이라 키워드 검색이 일부 상품에만 걸린다
INSERT INTO products (name, description, created_at, updated_at, base_price)
SELECT
    (ARRAY['홍대', '성수', '강남', '명동', '이태원', '잠실', '여의도', '종로'])[1 + i % 8]
        || ' ' || (ARRAY['메이크업', '헤어', '포토', '댄스', '보컬', '네일', '한복', '굿즈'])[1 + (i / 8) % 8]
        || ' 체험 ' || i,
    '측정용 상품 설명 ' || i || ' 아이돌 스타일 체험 코스',
    now() - (i || ' minutes')::interval,
    now(),
    10000 + (i % 50) * 1000
FROM generate_series(1, :n) AS i;

-- 상품 옵션: 상품마다 1 ~ 3개
INSERT INTO product_option (product_id, option_id, created_at, updated_at)
SELECT p, o, now(), now()
FROM generate_series(1, :n) AS p
CROSS JOIN LATERAL generate_series(1, 1 + p % 3) AS o;

-- 상품 위치: 상품마다 1개, 서울 안쪽 좌표
INSERT INTO product_locations (product_id, name, latitude, longitude, kakao_place_id, created_at, updated_at)
SELECT p, '측정 장소 ' || p, 37.45 + (p % 200) * 0.001, 126.85 + (p % 300) * 0.001, 'perf-' || p, now(), now()
FROM generate_series(1, :n) AS p;

-- 상품 태그: 상품마다 3개
INSERT INTO product_tag (product_id, tag_name, created_at, updated_at)
SELECT p, '태그' || ((p + k * 7) % 20), now(), now()
FROM generate_series(1, :n) AS p
CROSS JOIN generate_series(0, 2) AS k;


-- -----------------------------------------------------------------------------
-- 4. 찜과 리뷰
-- -----------------------------------------------------------------------------
-- 찜: 회원마다 상품 15개
-- 104729 는 소수라서 한 회원 안에서 같은 상품이 두 번 나오지 않는다
INSERT INTO wishes (user_id, product_id, created_at, updated_at)
SELECT u, 1 + ((u * 7919 + k * 104729) % :n), now(), now()
FROM generate_series(1, :n * 2) AS u
CROSS JOIN generate_series(0, 14) AS k
ON CONFLICT DO NOTHING;

-- 리뷰: 회원마다 상품 5개, 별점은 1 ~ 5 를 돌아가며
INSERT INTO product_reviews (product_id, user_id, rating, content, created_at, updated_at)
SELECT 1 + ((u * 6007 + k * 104723) % :n), u, 1 + (u + k) % 5, '측정용 리뷰', now(), now()
FROM generate_series(1, :n * 2) AS u
CROSS JOIN generate_series(0, 4) AS k
ON CONFLICT DO NOTHING;


-- -----------------------------------------------------------------------------
-- 5. 예약 슬롯
-- -----------------------------------------------------------------------------
-- 상품마다 내일부터 5일 동안 10시와 13시, 1시간짜리 슬롯
INSERT INTO reservation_slots (product_id, reservation_date, start_time, end_time, is_booked, created_at, updated_at)
SELECT p, current_date + d, make_time(10 + h * 3, 0, 0), make_time(11 + h * 3, 0, 0), false, now(), now()
FROM generate_series(1, :n) AS p
CROSS JOIN generate_series(1, 5) AS d
CROSS JOIN generate_series(0, 1) AS h;


-- -----------------------------------------------------------------------------
-- 6. 마무리
-- -----------------------------------------------------------------------------
-- 통계를 새로 모아 실행 계획이 실제 데이터 크기를 보게 한다
ANALYZE;

-- 만들어진 건수 확인
SELECT 'products' AS t, count(*) FROM products
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'wishes', count(*) FROM wishes
UNION ALL SELECT 'product_reviews', count(*) FROM product_reviews
UNION ALL SELECT 'product_tag', count(*) FROM product_tag
UNION ALL SELECT 'reservation_slots', count(*) FROM reservation_slots;
