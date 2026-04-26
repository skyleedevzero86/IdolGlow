-- JPY 외 통화도 표에서 조회되도록 동일 지점 데모 행 추가(실제 영업 환율과 무관한 예시 값).
INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT x.name, x.rate, x.currency, x.lat, x.lng, x.sort_order, x.airport_hub
FROM (
    SELECT 'IdolGlow 공항 환전' AS name, 1480.0 AS rate, 'EUR' AS currency, 37.4602 AS lat, 126.4407 AS lng, 0 AS sort_order, TRUE AS airport_hub
    UNION ALL
    SELECT '명동 대사관', 1465.0, 'EUR', 37.5635, 126.9826, 1, FALSE
    UNION ALL
    SELECT '명동 제1교류센터', 1465.0, 'EUR', 37.5640, 126.9815, 2, FALSE
    UNION ALL
    SELECT '환전 카페 (구 입핀상)', 1465.0, 'EUR', 37.5632, 126.9850, 3, FALSE
    UNION ALL
    SELECT '명동 돈 상자', 1462.5, 'EUR', 37.5628, 126.9838, 4, FALSE
) x
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch WHERE currency = 'EUR');

INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT x.name, x.rate, x.currency, x.lat, x.lng, x.sort_order, x.airport_hub
FROM (
    SELECT 'IdolGlow 공항 환전' AS name, 1370.0 AS rate, 'USD' AS currency, 37.4602 AS lat, 126.4407 AS lng, 0 AS sort_order, TRUE AS airport_hub
    UNION ALL
    SELECT '명동 대사관', 1355.0, 'USD', 37.5635, 126.9826, 1, FALSE
    UNION ALL
    SELECT '명동 제1교류센터', 1355.0, 'USD', 37.5640, 126.9815, 2, FALSE
    UNION ALL
    SELECT '환전 카페 (구 입핀상)', 1355.0, 'USD', 37.5632, 126.9850, 3, FALSE
    UNION ALL
    SELECT '명동 돈 상자', 1352.8, 'USD', 37.5628, 126.9838, 4, FALSE
) x
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch WHERE currency = 'USD');

INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT x.name, x.rate, x.currency, x.lat, x.lng, x.sort_order, x.airport_hub
FROM (
    SELECT 'IdolGlow 공항 환전' AS name, 192.0 AS rate, 'CNY' AS currency, 37.4602 AS lat, 126.4407 AS lng, 0 AS sort_order, TRUE AS airport_hub
    UNION ALL
    SELECT '명동 대사관', 189.0, 'CNY', 37.5635, 126.9826, 1, FALSE
    UNION ALL
    SELECT '명동 제1교류센터', 189.0, 'CNY', 37.5640, 126.9815, 2, FALSE
    UNION ALL
    SELECT '환전 카페 (구 입핀상)', 189.0, 'CNY', 37.5632, 126.9850, 3, FALSE
    UNION ALL
    SELECT '명동 돈 상자', 188.5, 'CNY', 37.5628, 126.9838, 4, FALSE
) x
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch WHERE currency = 'CNY');
