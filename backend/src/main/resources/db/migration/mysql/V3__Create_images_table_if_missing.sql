-- =============================================================================
-- V3 (MySQL): images 테이블 보정 및 샘플 1건
-- 기존 DB에 V1 이력은 있으나 images가 빠진 경우를 IF NOT EXISTS로 복구한다.
-- 개발·스키마 확인용으로 테이블이 비어 있을 때만 샘플 행을 넣는다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 PK',
    aggregate_type VARCHAR(30) NOT NULL COMMENT 'PRODUCT/OPTION/USER/PRODUCT_REVIEW',
    aggregate_id BIGINT NOT NULL COMMENT '소속 엔티티 PK',
    original_filename VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    unique_filename VARCHAR(255) NOT NULL COMMENT '스토리지 저장용 유니크 파일명',
    extension VARCHAR(20) NOT NULL COMMENT '확장자',
    file_size BIGINT NOT NULL COMMENT '파일 크기(byte)',
    url VARCHAR(500) NOT NULL COMMENT '접근 URL',
    sort_order INT NOT NULL COMMENT '동일 aggregate 내 정렬(0 이상)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_images_aggregate_type CHECK (aggregate_type IN ('PRODUCT', 'OPTION', 'USER', 'PRODUCT_REVIEW')),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_images_file_size CHECK (file_size > 0),
    INDEX idx_images_aggregate (aggregate_type, aggregate_id, sort_order)
) COMMENT='도메인 엔티티별 이미지 메타(V3 보정)';

-- -----------------------------------------------------------------------------
-- 샘플 1건: products.id=1이 존재할 때만 의미 있음(없어도 스키마 검증용 더미)
-- -----------------------------------------------------------------------------
INSERT INTO images (
    aggregate_type,
    aggregate_id,
    original_filename,
    unique_filename,
    extension,
    file_size,
    url,
    sort_order,
    created_at,
    updated_at
)
SELECT
    'PRODUCT',
    1,
    'sample-product-image.jpg',
    'sample-product-image-0001.jpg',
    'jpg',
    1024,
    'https://mock-cloud.example/images/sample-product-image-0001.jpg',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM images);
