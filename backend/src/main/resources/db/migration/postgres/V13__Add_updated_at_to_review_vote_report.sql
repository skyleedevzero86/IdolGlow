-- =============================================================================
-- Flyway V13 (PostgreSQL) — 리뷰 부가 엔티티에 updated_at 추가
-- =============================================================================
-- 대상: product_review_helpful_votes(도움돼요), product_review_reports(신고)
-- 목적: BaseEntity/JPA 스키마와 DDL 일치. TIMESTAMP(6) = 마이크로초 정밀도.
-- 백필: DEFAULT CURRENT_TIMESTAMP(6). 이후 갱신은 애플리케이션 레이어.
-- =============================================================================

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE product_review_reports
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
