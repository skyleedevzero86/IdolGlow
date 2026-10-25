-- =============================================================================
-- insert: 기본 데이터
-- -----
-- 목적
-- - 초기 데이터 입력과 기존 행 값 보정을 한 파일에 모은다.
-- 대상
-- - ps
-- 주의
-- - 새 데이터베이스는 setting, create, column, comment, insert, select 순서로 실행한다
-- - 이미 반영된 데이터베이스에는 이 묶음을 다시 실행하지 않는다
-- =============================================================================

-- -
-- 더미 데이터
-- images 테이블이 비어있을 때만 1건 삽입한다.
-- -
INSERT INTO images (
    aggregate_type,
    aggregate_id,
    original_filename,
    unique_filename,
    extension,
    file_size,
    url,
    sort_order,
    created_at,
    updated_at
)
SELECT
    'PRODUCT',
    1,
    'sample-product-image.jpg',
    'sample-product-image-0001.jpg',
    'jpg',
    1024,
    'https://mock-cloud.example/images/sample-product-image-0001.jpg',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM images);

-- 기존 결제 행에 내부 번호와 가맹점 주문 번호를 채운다
UPDATE payments SET payment_no = 'LEGACY-' || CAST(id AS VARCHAR(30)) WHERE payment_no IS NULL;

UPDATE payments SET order_id = payment_reference WHERE order_id IS NULL;

-- -
-- 시드: 도메인 + 샘플 배너/팝업/메인이미지
-- -
insert into tb_domain_list (domain_id, domain_nm, domain_path, domain_dc, use_at, sort_seq) values
('kr', '한국어', '/kr', '한국어 메인사이트', 'Y', 1),
('en', 'English', '/en', 'English site', 'Y', 2),
('tour', '관광', '/tour', '관광 사이트', 'Y', 3)
on conflict (domain_id) do nothing;

insert into tb_banner (banner_id, domain_id, banner_nm, link_url, banner_dc, sort_ordr, reflct_at, frst_register_id) values
('BNR_001', 'kr', '샘플 배너 1', 'https://example.com', '샘플 배너입니다', 1, 'Y', 'admin'),
('BNR_002', 'kr', '샘플 배너 2', 'https://example.com/2', '두번째 배너', 2, 'Y', 'admin'),
('BNR_003', 'en', 'English Banner', 'https://example.com/en', 'English site banner', 1, 'Y', 'admin')
on conflict (banner_id) do nothing;

insert into tb_popup_manage (popup_id, domain_id, popup_sj_nm, file_url, link_target, ntce_bgnde, ntce_endde, ntce_at, stopvew_setup_at, frst_register_id) values
('POP_001', 'kr', '안내 팝업', 'https://example.com/popup', '_blank', '202603010000', '202612312359', 'Y', 'Y', 'admin'),
('POP_002', 'kr', '이벤트 팝업', 'https://example.com/event-popup', '_self', '202606010000', '202608312359', 'Y', 'N', 'admin')
on conflict (popup_id) do nothing;

insert into tb_main_image (image_id, domain_id, image_nm, image_dc, reflct_at, frst_register_id) values
('IMG_001', 'kr', '메인 이미지 1', '메인 슬라이드 이미지', 'Y', 'admin'),
('IMG_002', 'kr', '메인 이미지 2', '서브 슬라이드 이미지', 'Y', 'admin')
on conflict (image_id) do nothing;

-- MANAGER 역할 제거. 기존 MANAGER 행은 ADMIN으로 승격.
UPDATE users SET role = 'ADMIN' WHERE role = 'MANAGER';

UPDATE products SET is_recommended = COALESCE(is_recommended, FALSE);

UPDATE products SET recommendation_score = COALESCE(recommendation_score, 0);

UPDATE products
SET is_recommended = COALESCE(is_recommended, FALSE);

UPDATE products
SET recommendation_score = COALESCE(recommendation_score, 0);

INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT v.name, v.rate, v.currency, v.lat, v.lng, v.sort_order, v.airport_hub
FROM (
    VALUES
        ('IdolGlow 공항 환전', 9.33, 'JPY', 37.4602, 126.4407, 0, TRUE),
        ('명동 대사관', 9.23, 'JPY', 37.5635, 126.9826, 1, FALSE),
        ('명동 제1교류센터', 9.23, 'JPY', 37.5640, 126.9815, 2, FALSE),
        ('환전 카페 (구 입핀상)', 9.23, 'JPY', 37.5632, 126.9850, 3, FALSE),
        ('명동 돈 상자', 9.2283, 'JPY', 37.5628, 126.9838, 4, FALSE)
) AS v(name, rate, currency, lat, lng, sort_order, airport_hub)
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch);

-- JPY 외 통화도 표에서 조회되도록 동일 지점 데모 행 추가 실제 영업 환율과 무관한 예시 값.
INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT v.name, v.rate, v.currency, v.lat, v.lng, v.sort_order, v.airport_hub
FROM (
    VALUES
        ('IdolGlow 공항 환전', 1480.0, 'EUR', 37.4602, 126.4407, 0, TRUE),
        ('명동 대사관', 1465.0, 'EUR', 37.5635, 126.9826, 1, FALSE),
        ('명동 제1교류센터', 1465.0, 'EUR', 37.5640, 126.9815, 2, FALSE),
        ('환전 카페 (구 입핀상)', 1465.0, 'EUR', 37.5632, 126.9850, 3, FALSE),
        ('명동 돈 상자', 1462.5, 'EUR', 37.5628, 126.9838, 4, FALSE)
) AS v(name, rate, currency, lat, lng, sort_order, airport_hub)
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch WHERE currency = 'EUR');

INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT v.name, v.rate, v.currency, v.lat, v.lng, v.sort_order, v.airport_hub
FROM (
    VALUES
        ('IdolGlow 공항 환전', 1370.0, 'USD', 37.4602, 126.4407, 0, TRUE),
        ('명동 대사관', 1355.0, 'USD', 37.5635, 126.9826, 1, FALSE),
        ('명동 제1교류센터', 1355.0, 'USD', 37.5640, 126.9815, 2, FALSE),
        ('환전 카페 (구 입핀상)', 1355.0, 'USD', 37.5632, 126.9850, 3, FALSE),
        ('명동 돈 상자', 1352.8, 'USD', 37.5628, 126.9838, 4, FALSE)
) AS v(name, rate, currency, lat, lng, sort_order, airport_hub)
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch WHERE currency = 'USD');

INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT v.name, v.rate, v.currency, v.lat, v.lng, v.sort_order, v.airport_hub
FROM (
    VALUES
        ('IdolGlow 공항 환전', 192.0, 'CNY', 37.4602, 126.4407, 0, TRUE),
        ('명동 대사관', 189.0, 'CNY', 37.5635, 126.9826, 1, FALSE),
        ('명동 제1교류센터', 189.0, 'CNY', 37.5640, 126.9815, 2, FALSE),
        ('환전 카페 (구 입핀상)', 189.0, 'CNY', 37.5632, 126.9850, 3, FALSE),
        ('명동 돈 상자', 188.5, 'CNY', 37.5628, 126.9838, 4, FALSE)
) AS v(name, rate, currency, lat, lng, sort_order, airport_hub)
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch WHERE currency = 'CNY');

INSERT INTO survey_form_description (survey_form_id, markdown, created_at, updated_at)
SELECT f.id, f.description, COALESCE(f.created_at, CURRENT_TIMESTAMP), COALESCE(f.updated_at, CURRENT_TIMESTAMP)
FROM survey_form f
WHERE f.description IS NOT NULL
  AND TRIM(f.description) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM survey_form_description d
      WHERE d.survey_form_id = f.id
  );

UPDATE survey_form
SET status = 'SCHEDULED'
WHERE status = 'PLANNED';

UPDATE images
SET url = '/exchange-article-reviews-thumb.png'
WHERE url LIKE 'https://mock-cloud.example/%';

UPDATE toss_billing_methods
SET billing_key_hash = 'legacy-' || id::text
WHERE billing_key_hash IS NULL;

UPDATE toss_billing_methods m
SET is_default = TRUE
WHERE m.id IN (
    SELECT id FROM (
        SELECT MAX(id) AS id
        FROM toss_billing_methods
        WHERE active = TRUE
        GROUP BY user_id
    ) t
)
AND m.is_default = FALSE;

UPDATE payment_refunds
SET first_requested_at = created_at
WHERE first_requested_at IS NULL;

UPDATE toss_billing_methods
SET deletion_status = 'ACTIVE'
WHERE deletion_status IS NULL;
