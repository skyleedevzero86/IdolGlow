CREATE TABLE IF NOT EXISTS search_keyword_stats (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 1,
    last_searched_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_search_keyword_stats_keyword UNIQUE (keyword)
);

CREATE INDEX IF NOT EXISTS idx_search_keyword_stats_count
    ON search_keyword_stats (search_count DESC, last_searched_at DESC);
