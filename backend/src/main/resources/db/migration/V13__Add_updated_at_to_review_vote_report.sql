-- 리뷰 도움돼요/신고 엔티티의 BaseEntity updated_at 컬럼 누락을 보정한다.
-- 기존 데이터는 마이그레이션 시점 시각으로 채우고, 이후 값은 애플리케이션 감사 로직이 갱신한다.

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE product_review_reports
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
