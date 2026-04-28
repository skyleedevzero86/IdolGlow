-- JPY 외 통화도 표에서 조회되도록 동일 지점 데모 행 추가(실제 영업 환율과 무관한 예시 값).
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
