-- =============================================================================
-- Flyway V13 — 리뷰 부가 엔티티에 updated_at 추가 (공통 베이스 엔티티 정합)
-- =============================================================================
-- product_review_helpful_votes: 리뷰 "도움이 돼요" 투표
-- product_review_reports       : 리뷰 신고
-- JPA BaseEntity 등에서 요구하는 updated_at이 최초 DDL에 빠진 경우를 보정한다.
-- 기존 행: ADD 시점의 DEFAULT로 채움. 이후: 애플리케이션(@LastModifiedDate 등)이 갱신.
-- =============================================================================

-- 도움돼요 투표 행의 마지막 수정 시각(감사·스키마 검증용)
ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 신고 행의 마지막 수정 시각(감사·스키마 검증용)
ALTER TABLE product_review_reports
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
