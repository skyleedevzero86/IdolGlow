-- 리뷰 신뢰도: 예약 연동, 도움돼요, 신고/숨김

ALTER TABLE product_reviews
    ADD COLUMN reservation_id BIGINT NULL COMMENT '방문 완료 예약 FK(인증 리뷰)',
    ADD COLUMN helpful_count BIGINT NOT NULL DEFAULT 0 COMMENT '도움돼요 수',
    ADD COLUMN hidden_at DATETIME(6) NULL COMMENT '비공개 처리 시각',
    ADD COLUMN hidden_reason VARCHAR(80) NULL COMMENT '숨김 사유';

ALTER TABLE product_reviews
    ADD CONSTRAINT fk_product_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id);

CREATE UNIQUE INDEX uk_product_reviews_reservation_id ON product_reviews (reservation_id);

CREATE TABLE product_review_helpful_votes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_review_helpful_user UNIQUE (review_id, user_id),
    CONSTRAINT fk_helpful_review FOREIGN KEY (review_id) REFERENCES product_reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_helpful_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='리뷰 도움돼요';

CREATE INDEX idx_helpful_review_id ON product_review_helpful_votes (review_id);

CREATE TABLE product_review_reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    reporter_user_id BIGINT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_review_report_user UNIQUE (review_id, reporter_user_id),
    CONSTRAINT fk_report_review FOREIGN KEY (review_id) REFERENCES product_reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id)
) COMMENT='리뷰 신고';

CREATE INDEX idx_report_review_id ON product_review_reports (review_id);
