-- 리뷰 도움돼요/신고 엔티티에 BaseEntity(JPA) 기준 updated_at 컬럼이 스키마에 없어 DDL 검증(validate) 또는 감사 일관성이 깨질 수 있어 보정한다.
-- 기존 행은 마이그레이션 시점의 CURRENT_TIMESTAMP(6)로 채우고, 이후 변경 시 애플리케이션에서 갱신한다.

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각(감사·낙관적 락 등 공통 엔티티 필드)';

ALTER TABLE product_review_reports
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각(감사·낙관적 락 등 공통 엔티티 필드)';
