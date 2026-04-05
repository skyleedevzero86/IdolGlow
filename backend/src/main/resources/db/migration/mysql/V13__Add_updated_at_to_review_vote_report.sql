-- =============================================================================
-- Flyway V13 (MySQL) — 리뷰 부가 엔티티에 updated_at 추가
-- =============================================================================
-- 대상: product_review_helpful_votes(도움돼요), product_review_reports(신고)
-- DATETIME(6): 마이크로초. INFORMATION_SCHEMA/COLUMN COMMENT로 메타 설명 유지.
-- 백필: CURRENT_TIMESTAMP(6). 이후 갱신은 애플리케이션에서 수행.
-- =============================================================================

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각(감사·공통 엔티티 필드)';

ALTER TABLE product_review_reports
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각(감사·공통 엔티티 필드)';
