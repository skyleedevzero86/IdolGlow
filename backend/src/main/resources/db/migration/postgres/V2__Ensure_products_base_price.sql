-- 기존 DB에 products.base_price가 없을 때만 추가한다.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS base_price NUMERIC(19, 2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN products.base_price IS '상품 기본가(옵션 합산과 별도)';
