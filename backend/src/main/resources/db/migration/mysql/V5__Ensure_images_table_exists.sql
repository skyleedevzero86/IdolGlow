-- =============================================================================
-- V5 (MySQL): images 테이블 존재 보장
-- flyway_schema_history는 최신인데 테이블만 누락된 경우(덤프 복원, 수동 DDL 등) 복구.
-- V3과 동일 구조, INSERT 없음(운영 데이터 오염 방지).
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
) COMMENT='도메인 엔티티별 이미지 메타(V5 idempotent 보정)';
