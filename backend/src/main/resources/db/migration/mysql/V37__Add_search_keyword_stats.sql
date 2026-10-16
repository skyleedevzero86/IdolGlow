CREATE TABLE IF NOT EXISTS search_keyword_stats (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 1,
    last_searched_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_search_keyword_stats_keyword UNIQUE (keyword)
);

CREATE INDEX idx_search_keyword_stats_count
    ON search_keyword_stats (search_count DESC, last_searched_at DESC);
