-- 기존 DB(V1만 적용된 스키마)에 products.base_price가 없을 때만 추가한다.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS base_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT '상품 기본가(옵션 합산과 별도)';
