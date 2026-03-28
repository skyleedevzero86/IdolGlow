-- =============================================================================
-- V1 (MySQL): 초기 스키마
-- 사용자·OAuth·상품·옵션·태그·위치·예약 슬롯·예약·리뷰·이미지·설문·찜·일정
-- PostgreSQL용 migration(V1)과 동등한 도메인 구조를 MySQL 문법으로 정의한다.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- users: 애플리케이션 회원
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회원 PK',
    email VARCHAR(255) NOT NULL COMMENT '로그인·식별용 이메일(인덱스)',
    nickname VARCHAR(10) NOT NULL COMMENT '표시 닉네임',
    role VARCHAR(255) NOT NULL COMMENT '권한 역할(V1: USER만 허용, V2에서 ADMIN 추가)',
    last_login_at DATETIME(6) NULL COMMENT '마지막 로그인 시각',
    CONSTRAINT chk_users_role CHECK (role IN ('USER'))
) COMMENT='서비스 회원. OAuth 연동은 user_oauths 참조';

-- 이메일 로그인·조회용
CREATE INDEX idx_users_email ON users (email);

-- -----------------------------------------------------------------------------
-- user_oauths: OAuth 제공자별 계정 연결
-- -----------------------------------------------------------------------------
CREATE TABLE user_oauths (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '연동 행 PK',
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    provider VARCHAR(255) NOT NULL COMMENT 'OAuth 제공자(GOOGLE, TEST)',
    provider_id VARCHAR(255) NOT NULL COMMENT '제공자 측 사용자 식별자',
    email VARCHAR(255) NOT NULL COMMENT '제공자가 넘긴 이메일',
    CONSTRAINT chk_user_oauths_provider CHECK (provider IN ('GOOGLE', 'TEST'))
) COMMENT='외부 OAuth 계정과 users 매핑';

CREATE INDEX idx_user_oauths_user_id ON user_oauths (user_id);
CREATE INDEX idx_user_oauths_provider_provider_id ON user_oauths (provider, provider_id);
CREATE INDEX idx_user_oauths_email ON user_oauths (email);

-- -----------------------------------------------------------------------------
-- options: 상품에 붙는 옵션(가격·장소 등)
-- -----------------------------------------------------------------------------
CREATE TABLE options (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '옵션 PK',
    name VARCHAR(120) NOT NULL COMMENT '옵션명',
    description TEXT NOT NULL COMMENT '옵션 상세 설명',
    price DECIMAL(38, 2) NOT NULL COMMENT '옵션 가격',
    location VARCHAR(200) NOT NULL COMMENT '옵션 관련 위치 표시 문자열',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각'
) COMMENT='상품 옵션 마스터(가격·설명·위치 문구)';

-- -----------------------------------------------------------------------------
-- products: 상품 마스터
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '상품 PK',
    name VARCHAR(120) NOT NULL COMMENT '상품명',
    description TEXT NOT NULL COMMENT '상품 설명',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각'
) COMMENT='상품 기본 정보';

-- -----------------------------------------------------------------------------
-- product_option: 상품-옵션 N:M
-- -----------------------------------------------------------------------------
CREATE TABLE product_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '연결 행 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    option_id BIGINT NOT NULL COMMENT 'options.id FK',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_product_option UNIQUE (product_id, option_id),
    CONSTRAINT fk_product_option_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_option_option FOREIGN KEY (option_id) REFERENCES options (id)
) COMMENT='상품에 포함된 옵션 연결';

CREATE INDEX idx_product_option_product_id ON product_option (product_id);
CREATE INDEX idx_product_option_option_id ON product_option (option_id);

-- -----------------------------------------------------------------------------
-- product_tag: 상품 태그
-- -----------------------------------------------------------------------------
CREATE TABLE product_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '태그 행 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    tag_name VARCHAR(50) NOT NULL COMMENT '태그 문자열',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_product_tag UNIQUE (product_id, tag_name),
    CONSTRAINT fk_product_tag_product FOREIGN KEY (product_id) REFERENCES products (id)
) COMMENT='상품별 검색·분류용 태그';

CREATE INDEX idx_product_tag_tag_name ON product_tag (tag_name);
CREATE INDEX idx_product_tag_product_id ON product_tag (product_id);

-- -----------------------------------------------------------------------------
-- product_locations: 상품 대표 위치(지도 1건)
-- -----------------------------------------------------------------------------
CREATE TABLE product_locations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '위치 행 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    name VARCHAR(120) NOT NULL COMMENT '장소 표시명',
    latitude DECIMAL(10, 7) NOT NULL COMMENT '위도',
    longitude DECIMAL(10, 7) NOT NULL COMMENT '경도',
    road_address_name VARCHAR(255) NULL COMMENT '도로명 주소',
    address_name VARCHAR(255) NULL COMMENT '지번/일반 주소',
    kakao_place_id VARCHAR(40) NOT NULL COMMENT '카카오 장소 ID',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_product_locations_product UNIQUE (product_id),
    CONSTRAINT fk_product_locations_product FOREIGN KEY (product_id) REFERENCES products (id)
) COMMENT='상품당 하나의 지도/주소 정보';

-- -----------------------------------------------------------------------------
-- reservation_slots: 상품별 예약 가능 슬롯(날짜·시간대)
-- -----------------------------------------------------------------------------
CREATE TABLE reservation_slots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '슬롯 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    reservation_date DATE NOT NULL COMMENT '슬롯 날짜',
    start_time TIME NOT NULL COMMENT '시작 시각',
    end_time TIME NOT NULL COMMENT '종료 시각(시작보다 커야 함)',
    is_booked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '예약 확정 등으로 점유 여부',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_reservation_slot_product_date_start UNIQUE (product_id, reservation_date, start_time),
    CONSTRAINT fk_reservation_slots_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_reservation_slots_time CHECK (end_time > start_time)
) COMMENT='예약 가능 시간 슬롯';

CREATE INDEX idx_reservation_slots_product_id ON reservation_slots (product_id);

-- -----------------------------------------------------------------------------
-- reservations: 사용자 예약(슬롯·상태·금액)
-- -----------------------------------------------------------------------------
CREATE TABLE reservations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '예약 PK',
    reservation_slot_id BIGINT NOT NULL COMMENT 'reservation_slots.id FK',
    user_id BIGINT NOT NULL COMMENT '예약자 users.id(앱 레벨 정합성)',
    visit_date DATE NOT NULL COMMENT '방문일',
    visit_start_time TIME NOT NULL COMMENT '방문 시작 시각',
    visit_end_time TIME NOT NULL COMMENT '방문 종료 시각',
    total_price DECIMAL(15, 2) NOT NULL COMMENT '예약 총액',
    status VARCHAR(20) NOT NULL COMMENT 'PREBOOK/PENDING/BOOKED/COMPLETED/CANCELED',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_reservations_reservation_slot FOREIGN KEY (reservation_slot_id) REFERENCES reservation_slots (id),
    CONSTRAINT chk_reservations_status CHECK (status IN ('PREBOOK', 'PENDING', 'BOOKED', 'COMPLETED', 'CANCELED'))
) COMMENT='회원의 예약 건';

CREATE INDEX idx_reservations_user_visit_time ON reservations (user_id, visit_date, visit_start_time);
CREATE INDEX idx_reservations_reservation_slot_id ON reservations (reservation_slot_id);

-- -----------------------------------------------------------------------------
-- product_reviews: 상품 리뷰(회원당 1건)
-- -----------------------------------------------------------------------------
CREATE TABLE product_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '리뷰 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    user_id BIGINT NOT NULL COMMENT '작성자 users.id',
    rating INT NOT NULL COMMENT '별점 1~5',
    content VARCHAR(2000) NOT NULL COMMENT '리뷰 본문',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_product_review_user UNIQUE (product_id, user_id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_product_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) COMMENT='상품별 사용자 리뷰(상품·사용자 유니크)';

CREATE INDEX idx_product_reviews_product_id ON product_reviews (product_id);
CREATE INDEX idx_product_reviews_user_id ON product_reviews (user_id);

-- -----------------------------------------------------------------------------
-- images: 집계 타입별 첨부 이미지 메타
-- -----------------------------------------------------------------------------
CREATE TABLE images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 PK',
    aggregate_type VARCHAR(30) NOT NULL COMMENT '소속 엔티티 종류(PRODUCT/OPTION/USER/PRODUCT_REVIEW)',
    aggregate_id BIGINT NOT NULL COMMENT '소속 엔티티 PK',
    original_filename VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    unique_filename VARCHAR(255) NOT NULL COMMENT '저장용 유니크 파일명',
    extension VARCHAR(20) NOT NULL COMMENT '확장자',
    file_size BIGINT NOT NULL COMMENT '파일 크기(바이트, 양수)',
    url VARCHAR(500) NOT NULL COMMENT '접근 URL',
    sort_order INT NOT NULL COMMENT '같은 aggregate 내 정렬 순서(0 이상)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_images_aggregate_type CHECK (aggregate_type IN ('PRODUCT', 'OPTION', 'USER', 'PRODUCT_REVIEW')),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_images_file_size CHECK (file_size > 0)
) COMMENT='도메인 엔티티별 저장 이미지 메타';

CREATE INDEX idx_images_aggregate ON images (aggregate_type, aggregate_id, sort_order);

-- -----------------------------------------------------------------------------
-- user_survey: 온보딩/취향 설문(회원당 1건)
-- -----------------------------------------------------------------------------
CREATE TABLE user_survey (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '설문 PK',
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    concept VARCHAR(30) NOT NULL COMMENT '선호 콘셉트 코드',
    idol_name VARCHAR(255) NOT NULL COMMENT '관심 아이돌/그룹명',
    visit_start_date DATE NOT NULL COMMENT '방문 희망 시작일',
    visit_end_date DATE NOT NULL COMMENT '방문 희망 종료일',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_user_survey_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_survey_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_user_survey_concept CHECK (concept IN ('GIRL_CRUSH', 'LOVELY_FRESH', 'ELEGANT_GLAM', 'DREAMY', 'HIGHTEEN', 'ETC')),
    CONSTRAINT chk_user_survey_visit_dates CHECK (visit_end_date >= visit_start_date)
) COMMENT='사용자 설문(콘셉트·아이돌명·방문 기간)';

CREATE INDEX idx_user_survey_user_id ON user_survey (user_id);

-- -----------------------------------------------------------------------------
-- user_survey_places: 설문에 선택한 장소 목록
-- -----------------------------------------------------------------------------
CREATE TABLE user_survey_places (
    user_survey_id BIGINT NOT NULL COMMENT 'user_survey.id FK',
    place VARCHAR(100) NOT NULL COMMENT '장소명',
    CONSTRAINT fk_user_survey_places_survey FOREIGN KEY (user_survey_id) REFERENCES user_survey (id)
) COMMENT='설문에 기입한 방문 희망 장소(복수 행)';

CREATE INDEX idx_user_survey_places_survey_id ON user_survey_places (user_survey_id);

-- -----------------------------------------------------------------------------
-- wishes: 상품 찜(회원·상품 유니크)
-- -----------------------------------------------------------------------------
CREATE TABLE wishes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '찜 PK',
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_user_product_wish UNIQUE (user_id, product_id),
    CONSTRAINT fk_wishes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_wishes_product FOREIGN KEY (product_id) REFERENCES products (id)
) COMMENT='사용자의 상품 찜';

CREATE INDEX idx_wishes_user_id ON wishes (user_id);
CREATE INDEX idx_wishes_product_id ON wishes (product_id);

-- -----------------------------------------------------------------------------
-- schedules: 마이페이지 일정(상품 연동, FK는 앱 레벨에서 관리)
-- -----------------------------------------------------------------------------
CREATE TABLE schedules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '일정 PK',
    user_id BIGINT NOT NULL COMMENT 'users.id',
    product_id BIGINT NOT NULL COMMENT 'products.id',
    title VARCHAR(100) NOT NULL COMMENT '일정 제목',
    start_at DATETIME(6) NOT NULL COMMENT '시작 시각',
    end_at DATETIME(6) NOT NULL COMMENT '종료 시각(시작 이상)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_schedules_time CHECK (end_at >= start_at)
) COMMENT='사용자 일정(상품 참조)';

CREATE INDEX idx_schedule_user_start ON schedules (user_id, start_at);
