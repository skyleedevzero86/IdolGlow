CREATE TABLE IF NOT EXISTS exchange_branch (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    rate DECIMAL(20, 8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    airport_hub BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE INDEX idx_exchange_branch_currency ON exchange_branch (currency);

CREATE TABLE IF NOT EXISTS exchange_alert (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    from_currency VARCHAR(8) NOT NULL,
    to_currency VARCHAR(8) NOT NULL,
    target_rate DECIMAL(20, 8) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_exchange_alert_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_exchange_alert_user ON exchange_alert (user_id);

INSERT INTO exchange_branch (name, rate, currency, lat, lng, sort_order, airport_hub)
SELECT x.name, x.rate, x.currency, x.lat, x.lng, x.sort_order, x.airport_hub
FROM (
    SELECT 'IdolGlow 공항 환전' AS name, 9.33 AS rate, 'JPY' AS currency, 37.4602 AS lat, 126.4407 AS lng, 0 AS sort_order, TRUE AS airport_hub
    UNION ALL
    SELECT '명동 대사관', 9.23, 'JPY', 37.5635, 126.9826, 1, FALSE
    UNION ALL
    SELECT '명동 제1교류센터', 9.23, 'JPY', 37.5640, 126.9815, 2, FALSE
    UNION ALL
    SELECT '환전 카페 (구 입핀상)', 9.23, 'JPY', 37.5632, 126.9850, 3, FALSE
    UNION ALL
    SELECT '명동 돈 상자', 9.2283, 'JPY', 37.5628, 126.9838, 4, FALSE
) x
WHERE NOT EXISTS (SELECT 1 FROM exchange_branch);
