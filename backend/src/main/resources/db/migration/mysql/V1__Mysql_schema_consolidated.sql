-- MySQL 전체 스키마
-- 새 데이터베이스를 처음 만들 때만 사용한다. 이미 같은 스키마가 반영된 환경에는 실행하지 않는다.

-- 초기 스키마
---
-- users: 애플리케이션 회원
---
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회원 PK',
    email VARCHAR(255) NOT NULL COMMENT '로그인과 식별에 쓰는 이메일. 유니크 인덱스',
    nickname VARCHAR(10) NOT NULL COMMENT '표시 닉네임',
    role VARCHAR(255) NOT NULL COMMENT '권한 역할. USER는 일반, ADMIN은 운영자',
    last_login_at DATETIME(6) NULL COMMENT '마지막 로그인 시각',
    CONSTRAINT chk_users_role CHECK (role IN ('USER'))
) COMMENT='서비스 회원. OAuth 연동은 user_oauths 참조';

-- 이메일 로그인·조회용
CREATE INDEX idx_users_email ON users (email);

---
-- user_oauths: OAuth 제공자별 계정 연결
---
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

---
-- options: 상품에 붙는 옵션
---
CREATE TABLE options (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '옵션 PK',
    name VARCHAR(120) NOT NULL COMMENT '옵션명',
    description TEXT NOT NULL COMMENT '옵션 상세 설명',
    price DECIMAL(38, 2) NOT NULL COMMENT '옵션 가격',
    location VARCHAR(200) NOT NULL COMMENT '옵션 관련 위치 표시 문자열',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각'
) COMMENT='상품 옵션 마스터(가격·설명·위치 문구)';

---
-- products: 상품 마스터
---
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '상품 PK',
    name VARCHAR(120) NOT NULL COMMENT '상품명',
    description TEXT NOT NULL COMMENT '상품 설명',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각'
) COMMENT='상품 기본 정보';

---
-- product_option: 상품-옵션 N:M
---
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

---
-- product_tag: 상품 태그
---
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

---
-- product_locations: 상품 대표 위치
---
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

---
-- reservation_slots: 상품별 예약 가능 슬롯
---
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

---
-- reservations: 사용자 예약
---
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

---
-- product_reviews: 상품 리뷰
---
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

---
-- images: 집계 타입별 첨부 이미지 메타
---
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

---
-- user_survey: 온보딩/취향 설문
---
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

---
-- user_survey_places: 설문에 선택한 장소 목록
---
CREATE TABLE user_survey_places (
    user_survey_id BIGINT NOT NULL COMMENT 'user_survey.id FK',
    place VARCHAR(100) NOT NULL COMMENT '장소명',
    CONSTRAINT fk_user_survey_places_survey FOREIGN KEY (user_survey_id) REFERENCES user_survey (id)
) COMMENT='설문에 기입한 방문 희망 장소(복수 행)';

CREATE INDEX idx_user_survey_places_survey_id ON user_survey_places (user_survey_id);

---
-- wishes: 상품 찜
---
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

---
-- schedules: 마이페이지 일정
---
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

-- 예약·결제·알림·보안 확장
---
-- users: ADMIN 역할 추가
---
ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

---
-- reservation_slots: PENDING 예약이 슬롯을 점유할 때 hold 정보
-- hold_reservation_id: 잠금을 건 예약 PK
-- hold_expires_at: hold 만료 시각
---
ALTER TABLE reservation_slots ADD COLUMN hold_reservation_id BIGINT NULL COMMENT 'hold를 건 reservations.id';
ALTER TABLE reservation_slots ADD COLUMN hold_expires_at DATETIME(6) NULL COMMENT 'hold 만료 시각';
ALTER TABLE reservation_slots
    ADD CONSTRAINT fk_reservation_slots_hold_reservation FOREIGN KEY (hold_reservation_id) REFERENCES reservations (id);

-- hold 만료 스캔·조회용
CREATE INDEX idx_reservation_slots_hold ON reservation_slots (hold_reservation_id, hold_expires_at);

---
-- reservations: 결제·만료·취소 도메인 필드
---
ALTER TABLE reservations ADD COLUMN expires_at DATETIME(6) NULL COMMENT 'PENDING 예약 자동 만료 시각';
ALTER TABLE reservations ADD COLUMN confirmed_at DATETIME(6) NULL COMMENT '결제 성공 후 확정 시각';
ALTER TABLE reservations ADD COLUMN canceled_at DATETIME(6) NULL COMMENT '취소 시각';
ALTER TABLE reservations ADD COLUMN cancel_reason VARCHAR(40) NULL COMMENT 'USER_REQUESTED/PAYMENT_FAILED/PAYMENT_EXPIRED/ADMIN_CANCELED';
ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_cancel_reason CHECK (
        cancel_reason IS NULL OR cancel_reason IN ('USER_REQUESTED', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED', 'ADMIN_CANCELED')
    );

-- 만료 배치·상태 조회용
CREATE INDEX idx_reservations_status_expires_at ON reservations (status, expires_at);

---
-- payments: 예약 1건당 1건 결제
---
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '결제 PK',
    reservation_id BIGINT NOT NULL COMMENT 'reservations.id FK(유니크)',
    provider VARCHAR(20) NOT NULL COMMENT '결제 제공자(현재 MOCK)',
    payment_reference VARCHAR(80) NOT NULL COMMENT '외부 시스템이나 알림에서 쓰는 참조 번호',
    amount DECIMAL(15, 2) NOT NULL COMMENT '결제 금액',
    status VARCHAR(20) NOT NULL COMMENT 'PENDING/SUCCEEDED/FAILED/CANCELED/EXPIRED',
    approved_at DATETIME(6) NULL COMMENT '승인 시각',
    failed_at DATETIME(6) NULL COMMENT '실패 시각',
    expired_at DATETIME(6) NULL COMMENT '만료 시각',
    failure_reason VARCHAR(255) NULL COMMENT '실패 사유 메시지',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_payments_reservation_id UNIQUE (reservation_id),
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT fk_payments_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT chk_payments_provider CHECK (provider IN ('MOCK')),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'EXPIRED'))
) COMMENT='예약 연동 결제(모의 결제)';

-- 상태별 후처리·모니터링용
CREATE INDEX idx_payments_status_updated_at ON payments (status, updated_at);

---
-- notifications: 사용자 알림
---
CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '알림 PK',
    user_id BIGINT NOT NULL COMMENT '수신자 users.id',
    type VARCHAR(40) NOT NULL COMMENT 'RESERVATION_CONFIRMED/RESERVATION_CANCELED/PAYMENT_FAILED/PAYMENT_EXPIRED',
    title VARCHAR(120) NOT NULL COMMENT '알림 제목',
    message VARCHAR(500) NOT NULL COMMENT '알림 본문',
    link VARCHAR(255) NULL COMMENT '이동 URL(옵션)',
    read_at DATETIME(6) NULL COMMENT '읽은 시각(NULL이면 미읽음)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_notifications_type CHECK (type IN ('RESERVATION_CONFIRMED', 'RESERVATION_CANCELED', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED'))
) COMMENT='회원별 인앱 알림';

-- 목록 조회(미읽음 우선 등)용
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, read_at, created_at);

-- images 테이블을 보장하고 개발용 샘플 한 건을 넣는다
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
) COMMENT='엔티티별 첨부 이미지 메타데이터';

---
-- 샘플 1건: products.id=1이 존재할 때만 의미 있음
---
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

-- user_oauths 프로필 필드
-- OAuth 제공자가 넘긴 표시 이름
ALTER TABLE user_oauths
    ADD COLUMN profile_name VARCHAR(255) NULL COMMENT 'OAuth 프로필 표시 이름';

-- 프로필 썸네일·아바타 URL
ALTER TABLE user_oauths
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT 'OAuth 프로필 이미지 URL';

-- images 테이블 존재 보장
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
) COMMENT='엔티티별 첨부 이미지 메타데이터';

ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT '사용자 지정 프로필 이미지 URL' AFTER nickname;

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL COMMENT '로컬 가입 비밀번호(BCrypt), OAuth만 연동 시 NULL';

DROP INDEX idx_users_email ON users;

CREATE UNIQUE INDEX uq_users_email ON users (email);

CREATE UNIQUE INDEX uq_users_nickname ON users (nickname);

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

-- 매진 슬롯 웨이팅: 취소·만료·결제 실패 등으로 빈자리가 나면 대기자에게 알림

CREATE TABLE reservation_slot_waitlist (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'users.id',
    reservation_slot_id BIGINT NOT NULL COMMENT 'reservation_slots.id',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_rsw_user_slot UNIQUE (user_id, reservation_slot_id),
    CONSTRAINT fk_rsw_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_rsw_slot FOREIGN KEY (reservation_slot_id) REFERENCES reservation_slots (id) ON DELETE CASCADE
);

CREATE INDEX idx_rsw_slot ON reservation_slot_waitlist (reservation_slot_id);
CREATE INDEX idx_rsw_user ON reservation_slot_waitlist (user_id);

-- 결제대행사 연동: payments 확장과 payment_logs, payment_refunds

ALTER TABLE payments DROP CONSTRAINT chk_payments_provider;
ALTER TABLE payments DROP CONSTRAINT chk_payments_status;

ALTER TABLE payments ADD COLUMN payment_no VARCHAR(64) NULL COMMENT '내부 결제 번호(유니크)';
ALTER TABLE payments ADD COLUMN order_id VARCHAR(200) NULL COMMENT '가맹점 주문 식별자. 대행사 orderId와 맞추며 유일해야 한다';
ALTER TABLE payments ADD COLUMN payment_key VARCHAR(200) NULL COMMENT '결제대행사가 내려준 결제 건 고유 키. 승인이 끝난 뒤 채운다.';

ALTER TABLE payments ADD COLUMN order_name VARCHAR(255) NULL COMMENT '주문명. 대행사 orderName에 대응';
ALTER TABLE payments ADD COLUMN supplied_amount DECIMAL(15, 2) NULL COMMENT '공급가액';
ALTER TABLE payments ADD COLUMN vat DECIMAL(15, 2) NULL COMMENT '부가세';
ALTER TABLE payments ADD COLUMN tax_free_amount DECIMAL(15, 2) NULL COMMENT '비과세 금액';
ALTER TABLE payments ADD COLUMN currency VARCHAR(10) NULL COMMENT '통화 코드. ISO 4217 규칙을 따른다.';
ALTER TABLE payments ADD COLUMN gateway_method VARCHAR(50) NULL COMMENT '결제 수단 요약. 대행사 method에 대응';
ALTER TABLE payments ADD COLUMN gateway_type VARCHAR(50) NULL COMMENT '결제 유형 코드. NORMAL 등';
ALTER TABLE payments ADD COLUMN external_status VARCHAR(50) NULL COMMENT '대행사가 내려준 결제 상태 문자열';

ALTER TABLE payments ADD COLUMN requested_at DATETIME(6) NULL COMMENT '결제 요청 시각';
ALTER TABLE payments ADD COLUMN last_transaction_key VARCHAR(200) NULL COMMENT '마지막 거래 식별 키';
ALTER TABLE payments ADD COLUMN fail_code VARCHAR(100) NULL COMMENT '실패 코드';

ALTER TABLE payments ADD COLUMN canceled_at DATETIME(6) NULL COMMENT '전액 취소 완료 시각(내부 마킹)';
ALTER TABLE payments ADD COLUMN cancel_amount DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '누적 취소(환불) 금액';

ALTER TABLE payments ADD COLUMN card_company VARCHAR(100) NULL COMMENT '카드사 이름';
ALTER TABLE payments ADD COLUMN card_number VARCHAR(50) NULL COMMENT '마스킹된 카드번호';
ALTER TABLE payments ADD COLUMN installment_plan_months INT NULL COMMENT '할부 개월 수';
ALTER TABLE payments ADD COLUMN is_interest_free TINYINT(1) NULL COMMENT '무이자 여부';

ALTER TABLE payments ADD COLUMN virtual_account_bank VARCHAR(100) NULL COMMENT '가상계좌 은행';
ALTER TABLE payments ADD COLUMN virtual_account_number VARCHAR(100) NULL COMMENT '가상계좌 번호';
ALTER TABLE payments ADD COLUMN virtual_account_due_date DATETIME(6) NULL COMMENT '가상계좌 입금 만료 시각';

ALTER TABLE payments ADD COLUMN easy_pay_provider VARCHAR(100) NULL COMMENT '간편결제 제공자';

ALTER TABLE payments ADD COLUMN raw_response_json JSON NULL COMMENT '승인 또는 오류 응답 본문 JSON';
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) NULL COMMENT '승인 요청마다 다른 중복 방지 키. 유일해야 한다';

UPDATE payments SET payment_no = CONCAT('LEGACY-', id) WHERE payment_no IS NULL;
UPDATE payments SET order_id = payment_reference WHERE order_id IS NULL;

ALTER TABLE payments MODIFY COLUMN payment_no VARCHAR(64) NOT NULL COMMENT '내부 결제 번호(유니크)';
ALTER TABLE payments MODIFY COLUMN order_id VARCHAR(200) NOT NULL COMMENT '가맹점 주문 식별자. 대행사 orderId와 맞추며 유일해야 한다';

ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_no UNIQUE (payment_no);
ALTER TABLE payments ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);
ALTER TABLE payments ADD CONSTRAINT uk_payments_payment_key UNIQUE (payment_key);
ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('MOCK', 'TOSS'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (
    status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'EXPIRED', 'REFUNDED', 'PARTIAL_CANCELED')
);

CREATE INDEX idx_payments_external_status ON payments (external_status);

ALTER TABLE payments COMMENT = '예약 단위 결제 정보와 상태 이력. 결제대행사 승인·환불·응답 본문·중복 방지 키를 담는다.';

CREATE TABLE payment_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    PRIMARY KEY (id),
    payment_id BIGINT NULL COMMENT 'payments.id FK(삭제 시 NULL)',
    order_id VARCHAR(200) NULL COMMENT '주문 식별자 검색용',
    payment_key VARCHAR(200) NULL COMMENT '결제대행사가 내려준 결제 건 고유 키',
    log_type VARCHAR(50) NOT NULL COMMENT 'CONFIRM_*/CANCEL_*/WEBHOOK_*/MOCK_WEBHOOK/SYSTEM',
    step VARCHAR(50) NULL COMMENT 'CLIENT / SERVER / TOSS_API',
    request_url VARCHAR(500) NULL COMMENT '호출 URL 경로',
    http_method VARCHAR(20) NULL COMMENT 'HTTP 메서드',
    http_status INT NULL COMMENT 'HTTP 응답 상태 코드',
    request_body JSON NULL COMMENT '요청 본문(JSON 등)',
    response_body JSON NULL COMMENT '응답 본문(JSON 등)',
    error_code VARCHAR(100) NULL COMMENT '에러 코드',
    error_message VARCHAR(1000) NULL COMMENT '에러 메시지',
    stack_trace TEXT NULL COMMENT '예외 스택(내부 오류 시)',
    client_ip VARCHAR(100) NULL COMMENT '클라이언트 IP',
    user_agent VARCHAR(500) NULL COMMENT 'User-Agent',
    trace_id VARCHAR(100) NULL COMMENT '요청 추적 ID(X-Trace-Id 등)',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_payment_logs_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_logs_type CHECK (
        log_type IN (
            'CONFIRM_REQUEST', 'CONFIRM_RESPONSE', 'CONFIRM_ERROR',
            'CANCEL_REQUEST', 'CANCEL_RESPONSE', 'CANCEL_ERROR',
            'WEBHOOK_RECEIVED', 'WEBHOOK_REJECTED', 'MOCK_WEBHOOK', 'SYSTEM'
        )
    ),
    CONSTRAINT chk_payment_logs_step CHECK (step IS NULL OR step IN ('CLIENT', 'SERVER', 'TOSS_API'))
) COMMENT = '결제 승인·취소·서버 알림 요청과 응답, 오류를 남긴다.';

CREATE INDEX idx_payment_logs_payment_id ON payment_logs (payment_id);
CREATE INDEX idx_payment_logs_order_id ON payment_logs (order_id);
CREATE INDEX idx_payment_logs_created_at ON payment_logs (created_at);

CREATE TABLE payment_refunds (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '환불 이력 PK',
    PRIMARY KEY (id),
    payment_id BIGINT NOT NULL COMMENT 'payments.id FK',
    reservation_id BIGINT NOT NULL COMMENT 'reservations.id FK(조회 편의)',
    cancel_amount DECIMAL(15, 2) NOT NULL COMMENT '해당 요청 취소(환불) 금액',
    cancel_reason VARCHAR(500) NOT NULL COMMENT '취소 사유(사용자/운영/시스템 문구)',
    status VARCHAR(30) NOT NULL COMMENT 'PENDING / SUCCEEDED / FAILED',
    requested_by VARCHAR(20) NOT NULL COMMENT 'USER / ADMIN / SYSTEM / WEBHOOK',
    external_transaction_key VARCHAR(200) NULL COMMENT '대행사 거래 식별 키',
    fail_code VARCHAR(100) NULL COMMENT '실패 코드',
    fail_message VARCHAR(500) NULL COMMENT '실패 메시지',
    idempotency_key VARCHAR(100) NULL COMMENT '취소 요청 중복 방지 키. 유일해야 한다',
    raw_response_json JSON NULL COMMENT '취소 응답 본문 JSON',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_refunds_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT uk_payment_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_refunds_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_payment_refunds_requested_by CHECK (requested_by IN ('USER', 'ADMIN', 'SYSTEM', 'WEBHOOK'))
) COMMENT = '결제대행사 환불·취소 요청 이력. 고객 지원과 재시도 추적에 쓴다.';

CREATE INDEX idx_payment_refunds_payment_id ON payment_refunds (payment_id);
CREATE INDEX idx_payment_refunds_reservation_id ON payment_refunds (reservation_id);
CREATE INDEX idx_payment_refunds_status ON payment_refunds (status);

-- 알림 설정 테이블 + 만료 예정 알림 타입
ALTER TABLE notifications DROP CHECK chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type CHECK (
    type IN (
        'RESERVATION_CONFIRMED',
        'RESERVATION_CANCELED',
        'PAYMENT_FAILED',
        'PAYMENT_EXPIRED',
        'RESERVATION_SLOT_AVAILABLE',
        'RESERVATION_EXPIRING_SOON'
    )
);

-- 사용자별 알림 타입 수신 설정
CREATE TABLE notification_preferences (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '설정 PK',
    user_id      BIGINT        NOT NULL COMMENT '사용자 FK → users.id',
    type         VARCHAR(40)   NOT NULL COMMENT '알림 타입(notifications.type 과 동일 집합)',
    enabled      TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '수신 여부: 1=ON, 0=OFF',
    created_at   DATETIME(6)   NOT NULL COMMENT '생성 시각',
    updated_at   DATETIME(6)   NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_np_user_type UNIQUE (user_id, type),
    CONSTRAINT fk_np_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_np_type CHECK (
        type IN (
            'RESERVATION_CONFIRMED',
            'RESERVATION_CANCELED',
            'PAYMENT_FAILED',
            'PAYMENT_EXPIRED',
            'RESERVATION_SLOT_AVAILABLE',
            'RESERVATION_EXPIRING_SOON'
        )
    )
) COMMENT='사용자별 알림 타입 수신 ON/OFF';

CREATE INDEX idx_np_user_id ON notification_preferences (user_id);

-- MySQL 관리자 감사 로그
-- 관리자 예약 취소, 슬롯 일괄 생성, 상품 옵션 삭제 등 운영 액션을 남긴다
-- 조회는 관리자 API 로 최근 행 위주로 사용한다
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '로그 PK',
    admin_user_id  BIGINT        NOT NULL COMMENT '관리자 users.id',
    action_code    VARCHAR(64)   NOT NULL COMMENT '액션 코드 예약 취소 슬롯 생성',
    target_type    VARCHAR(64)   NOT NULL COMMENT '대상 유형 예약 슬롯 상품',
    target_id      BIGINT        NULL COMMENT '대상 엔티티 PK 없으면 null',
    detail         VARCHAR(2000) NULL COMMENT '추가 설명 JSON 또는 짧은 문장',
    created_at     DATETIME(6)   NOT NULL COMMENT '기록 시각',
    CONSTRAINT fk_admin_audit_logs_user FOREIGN KEY (admin_user_id) REFERENCES users (id)
) COMMENT='관리자 운영 액션 감사 로그';

CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs (created_at DESC);
CREATE INDEX idx_admin_audit_logs_admin_user ON admin_audit_logs (admin_user_id);

-- 리뷰 반응·신고 테이블에 수정 시각 컬럼이 없으면 추가한다. ORM 검증과 데이터 정합성을 위해 필요하다.
-- 기존 행은 마이그레이션 시점의 CURRENT_TIMESTAMP(6)로 채우고, 이후 변경 시 애플리케이션에서 갱신한다.

ALTER TABLE product_review_helpful_votes
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각(감사·낙관적 락 등 공통 엔티티 필드)';

ALTER TABLE product_review_reports
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '레코드 마지막 수정 시각(감사·낙관적 락 등 공통 엔티티 필드)';

-- 기사·본문 섹션·갤러리·태그
-- PK: BIGINT AUTO_INCREMENT. FK는 별도 CONSTRAINT로 선언(순서: 자식 테이블 정의 후).
-- updated_at: ON UPDATE CURRENT_TIMESTAMP — DB에서 갱신 시각 자동 반영(앱과 이중 갱신 시 정책 통일 권장).
-- 웹진 호
create table if not exists webzine_issues (
    id bigint not null auto_increment primary key comment '이슈 PK',
    slug varchar(80) not null comment '공개 URL용 슬러그',
    volume int not null comment '권호(정수, 전역 유일)',
    issue_date date not null comment '발행일',
    cover_image_url varchar(500) not null comment '표지 이미지 URL',
    teaser varchar(1000) not null comment '목록/공유용 티저',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_issue_slug unique (slug),
    constraint uk_webzine_issue_volume unique (volume)
) comment='웹진 이슈(호) 메타';

-- 기사
create table if not exists webzine_articles (
    id bigint not null auto_increment primary key comment '기사 PK',
    issue_id bigint not null comment 'FK → webzine_issues.id',
    slug varchar(150) not null comment '이슈 내 기사 URL 슬러그',
    title varchar(200) not null comment '제목',
    kicker varchar(200) not null comment '상단 키커/톤',
    summary text not null comment '요약/리드',
    hero_image_url varchar(500) not null comment '상세 상단 히어로',
    card_image_url varchar(500) not null comment '목록 카드 이미지',
    category varchar(30) not null comment '분류 키',
    format_label varchar(60) not null comment '포맷 라벨(인터뷰 등)',
    author_name varchar(120) not null comment '표시용 저자명',
    author_email varchar(255) not null comment '연락/메타용 이메일',
    credit_line varchar(255) not null comment '사진·출처 크레딧',
    highlight_quote varchar(500) comment '강조 인용(옵션)',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_issue_slug unique (issue_id, slug),
    constraint fk_webzine_articles_issue foreign key (issue_id) references webzine_issues(id) on delete cascade
) comment='웹진 기사 본문 메타';

-- 이슈별 기사 목록 조회
create index idx_webzine_articles_issue_id on webzine_articles(issue_id);

-- 본문 섹션
create table if not exists webzine_article_sections (
    id bigint not null auto_increment primary key comment '섹션 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '렌더 순서',
    heading varchar(200) comment '소제목(옵션)',
    body text not null comment '본문(표현 형식은 앱 규약)',
    note varchar(1000) comment '각주·편집자 주(옵션)',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_section_order unique (article_id, display_order),
    constraint fk_webzine_article_sections_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 본문 블록(섹션)';

-- 갤러리 이미지
create table if not exists webzine_article_gallery_images (
    id bigint not null auto_increment primary key comment '갤러리 행 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '갤러리 내 순서',
    image_url varchar(500) not null comment '이미지 URL',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_gallery_order unique (article_id, display_order),
    constraint fk_webzine_article_gallery_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 갤러리 이미지(순서)';

-- 태그
create table if not exists webzine_article_tags (
    id bigint not null auto_increment primary key comment '태그 행 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '표시 순서',
    tag_name varchar(80) not null comment '태그 문자열',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_tag_name unique (article_id, tag_name),
    constraint fk_webzine_article_tags_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 태그';

-- Newsletter admin domain tables for MySQL
-- 목록/상세/수정 화면에서 같은 데이터를 재사용할 수 있도록 태그와 문단을 별도 테이블로 분리한다.

create table if not exists newsletters (
    id bigint not null auto_increment primary key comment '뉴스레터 PK',
    slug varchar(160) not null comment '공개 URL용 슬러그',
    title varchar(200) not null comment '뉴스레터 제목',
    category_label varchar(80) not null comment '관리 화면 카테고리 라벨',
    published_at date not null comment '게시일',
    image_url varchar(500) not null comment '대표 이미지 URL',
    summary text not null comment '목록/공유용 요약',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_newsletter_slug unique (slug)
) comment='뉴스레터 마스터';

create table if not exists newsletter_tags (
    id bigint not null auto_increment primary key comment '태그 PK',
    newsletter_id bigint not null comment 'FK → newsletters.id',
    display_order integer not null comment '표시 순서',
    tag_name varchar(80) not null comment '태그 문자열',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_newsletter_tag_name unique (newsletter_id, tag_name),
    constraint fk_newsletter_tags_newsletter foreign key (newsletter_id) references newsletters(id) on delete cascade
) comment='뉴스레터 태그';

create table if not exists newsletter_paragraphs (
    id bigint not null auto_increment primary key comment '문단 PK',
    newsletter_id bigint not null comment 'FK → newsletters.id',
    display_order integer not null comment '문단 순서',
    body text not null comment '본문 문단',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_newsletter_paragraph_order unique (newsletter_id, display_order),
    constraint fk_newsletter_paragraphs_newsletter foreign key (newsletter_id) references newsletters(id) on delete cascade
) comment='뉴스레터 본문 문단';

create index idx_newsletters_published_at on newsletters(published_at);

-- 구독자 및 콘텐츠 발송 이력 테이블
-- 이메일 구독 상태(동의/활성 여부)와 뉴스레터·웹진 발송 결과를 추적한다.
-- updated_at은 ON UPDATE CURRENT_TIMESTAMP로 DB에서 자동 갱신한다.
create table if not exists email_subscriptions (
    id bigint auto_increment primary key,
    email varchar(255) not null comment '구독자 이메일(전역 유일)',
    subscribed_newsletters boolean not null default true comment '뉴스레터 수신 여부',
    subscribed_issues boolean not null default true comment '웹진/이슈 수신 여부',
    consented_at timestamp not null comment '수신 동의 시각',
    subscribed_at timestamp not null comment '구독 등록 시각',
    subscription_source varchar(50) not null comment '구독 유입 채널',
    active boolean not null default true comment '활성 구독 상태',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_email_subscriptions_email unique (email)
) comment='구독자 마스터 테이블(수신 동의/활성 상태 포함)';

create table if not exists subscription_dispatch_histories (
    id bigint auto_increment primary key,
    content_type varchar(40) not null comment '콘텐츠 유형 코드. NEWSLETTER, WEBZINE 등 서비스에서 정의한 값.',
    content_slug varchar(160) not null comment '콘텐츠 고유 slug/키',
    content_title varchar(255) not null comment '발송 당시 제목 스냅샷',
    content_summary varchar(1000) comment '발송 당시 요약 스냅샷',
    dispatch_channel varchar(20) not null comment '발송 채널 코드. EMAIL 등',
    dispatch_status varchar(20) not null comment '발송 상태 코드. SENT, FAILED 등',
    recipient_count bigint not null comment '발송 대상자 수',
    content_created_at timestamp null comment '콘텐츠 원본 생성 시각',
    dispatched_at timestamp not null comment '실제 발송 시각',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_subscription_dispatch_content unique (content_type, content_slug, dispatch_channel)
) comment='콘텐츠 발송 이력 테이블';

create index idx_email_subscriptions_active on email_subscriptions(active);
create index idx_subscription_dispatch_histories_dispatched_at on subscription_dispatch_histories(dispatched_at);

-- 사이트 도메인, 배너, 팝업, 메인이미지
---
-- tb_domain_list: 멀티 사이트구분용 도메인 마스터
---
create table if not exists tb_domain_list (
    domain_id varchar(20) primary key comment '사이트 도메인 식별자. kr, en처럼 짧은 코드',
    domain_nm varchar(100) comment '표시명',
    domain_path varchar(200) comment 'URL 경로 프리픽스 등',
    domain_dc varchar(500) comment '설명',
    use_at char(1) default 'Y' comment '사용 여부 Y/N',
    actvty_at char(1) default 'Y' comment '활성 여부 Y/N',
    sort_seq int default 0 comment '정렬 순서'
) engine=InnoDB default charset=utf8mb4 comment='사이트·언어 도메인 목록(배너/팝업/메인이미지 공통 참조)';

---
-- tb_banner: 상단·슬롯 등 배너 콘텐츠
---
create table if not exists tb_banner (
    banner_id varchar(20) primary key comment '배너 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    banner_nm varchar(200) comment '배너명',
    link_url varchar(500) comment '클릭 시 이동 URL',
    banner_image varchar(500) comment '이미지 경로/URL',
    banner_image_file varchar(500) comment '원본 파일명 등',
    banner_dc varchar(1000) comment '설명',
    sort_ordr int default 0 comment '노출 정렬 순서',
    reflct_at char(1) default 'Y' comment '게시(반영) 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_banner_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='배너 관리';

---
-- tb_popup_manage: 기간·위치·크기 속성을 가진 팝업
---
create table if not exists tb_popup_manage (
    popup_id varchar(20) primary key comment '팝업 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    popup_sj_nm varchar(200) comment '팝업 제목',
    file_url varchar(500) comment '링크 URL',
    link_target varchar(20) default '_blank' comment 'a 태그 target',
    popup_img_path varchar(500) comment '팝업 이미지 경로',
    popup_file_nm varchar(200) comment '이미지 파일명',
    popup_vrticl_lc int default 0 comment '세로 위치',
    popup_width_lc int default 0 comment '가로 위치',
    popup_vrticl_size int default 400 comment '세로 크기(px)',
    popup_width_size int default 500 comment '가로 크기(px)',
    ntce_bgnde varchar(20) comment '게시 시작 시각. 문자열로 저장한다',
    ntce_endde varchar(20) comment '게시 종료',
    stopvew_setup_at char(1) default 'Y' comment '다시보지 않기 설정 Y/N',
    ntce_at char(1) default 'Y' comment '게시(알림) 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_popup_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='팝업 관리';

---
-- tb_main_image: 메인 영역 슬라이드/히어로 이미지
---
create table if not exists tb_main_image (
    image_id varchar(20) primary key comment '이미지 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    image_nm varchar(200) comment '이미지 제목',
    image varchar(500) comment '이미지 경로나 URL. 기존 운영 스키마와 맞추기 위해 컬럼 이름을 image 로 둔다',
    image_file varchar(500) comment '파일명',
    image_dc varchar(1000) comment '설명',
    reflct_at char(1) default 'Y' comment '게시(반영) 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_main_image_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='메인 이미지 관리';

---
-- 시드: 도메인 + 샘플 데이터
---
insert ignore into tb_domain_list (domain_id, domain_nm, domain_path, domain_dc, use_at, sort_seq) values
('kr', '한국어', '/kr', '한국어 메인사이트', 'Y', 1),
('en', 'English', '/en', 'English site', 'Y', 2),
('tour', '관광', '/tour', '관광 사이트', 'Y', 3);

insert ignore into tb_banner (banner_id, domain_id, banner_nm, link_url, banner_dc, sort_ordr, reflct_at, frst_register_id) values
('BNR_001', 'kr', '샘플 배너 1', 'https://example.com', '샘플 배너입니다', 1, 'Y', 'admin'),
('BNR_002', 'kr', '샘플 배너 2', 'https://example.com/2', '두번째 배너', 2, 'Y', 'admin'),
('BNR_003', 'en', 'English Banner', 'https://example.com/en', 'English site banner', 1, 'Y', 'admin');

insert ignore into tb_popup_manage (popup_id, domain_id, popup_sj_nm, file_url, link_target, ntce_bgnde, ntce_endde, ntce_at, stopvew_setup_at, frst_register_id) values
('POP_001', 'kr', '안내 팝업', 'https://example.com/popup', '_blank', '202603010000', '202612312359', 'Y', 'Y', 'admin'),
('POP_002', 'kr', '이벤트 팝업', 'https://example.com/event-popup', '_self', '202606010000', '202608312359', 'Y', 'N', 'admin');

insert ignore into tb_main_image (image_id, domain_id, image_nm, image_dc, reflct_at, frst_register_id) values
('IMG_001', 'kr', '메인 이미지 1', '메인 슬라이드 이미지', 'Y', 'admin'),
('IMG_002', 'kr', '메인 이미지 2', '서브 슬라이드 이미지', 'Y', 'admin');

-- MySQL
---
-- editor_documents
---
create table if not exists editor_documents (
    id char(36) not null primary key,
    title varchar(120) not null,
    author varchar(60) not null,
    markdown longtext not null,
    tags_json longtext not null default '[]',
    updated_at timestamp(6) not null,
    embedding text not null,
    publication_status varchar(20) not null default 'PUBLISHED',
    url_slug varchar(180),
    introduction text null,
    thumbnail_image_url text,
    view_count bigint not null default 0
) engine=InnoDB default charset=utf8mb4;

create index if not exists editor_documents_updated_at_idx
    on editor_documents (updated_at desc);

create index if not exists editor_documents_publication_status_idx
    on editor_documents (publication_status, updated_at desc);

create index if not exists editor_documents_url_slug_idx
    on editor_documents (url_slug);

---
-- editor_assets
---
create table if not exists editor_assets (
    id char(36) not null primary key,
    object_key varchar(255) not null,
    bucket_name varchar(80) not null,
    original_file_name varchar(255) not null,
    content_type varchar(160) not null,
    file_size bigint not null,
    uploaded_at timestamp(6) not null,
    constraint uk_editor_assets_object_key unique (object_key)
) engine=InnoDB default charset=utf8mb4;

create index if not exists editor_assets_uploaded_at_idx
    on editor_assets (uploaded_at desc);

-- 플랫폼 인증용 users 확장·비밀번호 이력·역할
ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
    COMMENT '계정 승인·정지 상태(PENDING, APPROVED, REJECTED, SUSPENDED, WITHDRAWN)';
ALTER TABLE users ADD COLUMN platform_username VARCHAR(50) NULL
    COMMENT '플랫폼 가입 시 로그인명(계정 복구 시 이메일과 함께 사용)';
ALTER TABLE users ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0
    COMMENT '연속 로그인 실패 횟수';
ALTER TABLE users ADD COLUMN account_locked_at TIMESTAMP NULL
    COMMENT '계정 잠금 처리 시각';
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP NULL
    COMMENT '마지막 비밀번호 변경 시각';
ALTER TABLE users ADD COLUMN last_password_change_date DATE NULL
    COMMENT '당일 비밀번호 변경 횟수 집계 기준일';
ALTER TABLE users ADD COLUMN password_change_daily_count INT NOT NULL DEFAULT 0
    COMMENT '기준일 당일 비밀번호 변경 횟수';

CREATE UNIQUE INDEX uq_users_platform_username ON users (platform_username);

CREATE TABLE user_password_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 행 PK' PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 등으로 인코딩된 비밀번호',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이력 저장 시각',
    CONSTRAINT fk_user_password_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) COMMENT = '과거 비밀번호 해시(재사용 방지 조회용)';

CREATE INDEX idx_user_password_history_user_created ON user_password_history (user_id, created_at DESC);

-- MANAGER 역할 제거. 기존 MANAGER 행은 ADMIN으로 승격.
UPDATE users SET role = 'ADMIN' WHERE role = 'MANAGER';

ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users MODIFY COLUMN role VARCHAR(255) NOT NULL COMMENT '권한 역할(USER: 일반, ADMIN: 관리자)';

-- 구독 콘텐츠 자동 발송 스케줄
-- 콘텐츠 유형(뉴스레터·웹진 등)별로 발송 주기·요일·시각을 정의한다.
-- content_type 당 스케줄은 한 행만 허용(UK). 스케줄 비활성 시 active=false.
-- 애플리케이션의 배치/스케줄러가 이 테이블을 읽어 발송 시점을 결정한다.
-- updated_at은 행 변경 시 DB가 자동 갱신한다(구독·발송 관련 테이블과 동일 패턴).
create table if not exists subscription_dispatch_schedules (
    id bigint not null auto_increment comment '스케줄 행 PK',
    content_type varchar(40) not null comment '콘텐츠 유형 코드. 유형당 스케줄은 한 행만 둔다',
    frequency_type varchar(20) not null comment '반복 주기 유형. DAILY, WEEKLY, MONTHLY 등',
    day_of_week varchar(20) null comment '주간 등 주기에 따른 요일(애플리케이션 규약 문자열). 해당 없으면 NULL',
    dispatch_hour int not null comment '발송 시각(시, 0–23)',
    dispatch_minute int not null comment '발송 시각(분, 0–59)',
    active boolean not null default true comment '스케줄 사용 여부(배치에서 false 행은 건너뜀)',
    created_at timestamp not null default current_timestamp comment '행 생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    primary key (id),
    constraint uk_subscription_dispatch_schedule_content_type unique (content_type)
) comment='구독 콘텐츠 자동 발송 스케줄(유형별 주기·시각)';

create index idx_subscription_dispatch_schedules_active on subscription_dispatch_schedules (active);

-- 인증 검증 감사 로그
CREATE TABLE IF NOT EXISTS auth_verification_audit_logs (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '로그 PK',
    verification_type  VARCHAR(64)   NOT NULL COMMENT '검증 유형 코드. 서비스에서 정한 문자열',
    email              VARCHAR(190)  NULL COMMENT '시도에 사용된 이메일 주소',
    username           VARCHAR(120)  NULL COMMENT '시도에 사용된 사용자명 또는 닉네임',
    ip_address         VARCHAR(64)   NOT NULL COMMENT '요청 클라이언트 IP 주소',
    success            BOOLEAN       NOT NULL COMMENT '검증 성공 여부(1 성공, 0 실패)',
    detail             VARCHAR(500)  NULL COMMENT '실패 사유·메모 등 부가 설명',
    created_at         DATETIME(6)   NOT NULL COMMENT '로그가 기록된 시각'
) COMMENT='인증·검증(이메일 코드 등) 시도 감사 로그';

CREATE INDEX idx_auth_verification_audit_logs_created_at
    ON auth_verification_audit_logs (created_at DESC);

CREATE INDEX idx_auth_verification_audit_logs_type_created
    ON auth_verification_audit_logs (verification_type, created_at DESC);

-- 계정 검증 토큰
CREATE TABLE IF NOT EXISTS signup_verification_tokens (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '행 PK',
    token          VARCHAR(120)  NOT NULL COMMENT '일회성 검증 토큰(전역 유일)',
    type           VARCHAR(40)   NOT NULL COMMENT '검증 유형 코드',
    email          VARCHAR(190)  NOT NULL COMMENT '검증 대상 이메일',
    username       VARCHAR(120)  NULL COMMENT '닉네임 등(선택)',
    user_id        BIGINT        NULL COMMENT '확인 후 연결된 users.id(선택)',
    status         VARCHAR(30)   NOT NULL COMMENT '토큰 상태 코드',
    requested_ip   VARCHAR(64)   NOT NULL COMMENT '발급 요청 IP',
    confirmed_ip   VARCHAR(64)   NULL COMMENT '검증 완료 시 IP(선택)',
    expires_at     DATETIME(6)   NOT NULL COMMENT '만료 시각',
    created_at     DATETIME(6)   NOT NULL COMMENT '행 생성 시각',
    confirmed_at   DATETIME(6)   NULL COMMENT '검증·사용 완료 시각(선택)',
    detail         VARCHAR(500)  NULL COMMENT '거절 사유 등(선택)',
    CONSTRAINT uk_signup_verification_tokens_token UNIQUE (token)
) COMMENT='가입·계정 확인용 검증 토큰 저장소';

CREATE INDEX idx_signup_verification_tokens_email_type_status_created
    ON signup_verification_tokens (email, type, status, created_at DESC);

-- users 임시 비밀번호 발급 시각
-- User 엔티티 temporary_password_issued_at 과 동일.
-- MySQL 은 ADD COLUMN IF NOT EXISTS 가 버전에 따라 없을 수 있어 단순 ADD 만 사용한다.
ALTER TABLE users
    ADD COLUMN temporary_password_issued_at DATETIME(6) NULL
        COMMENT '플랫폼 임시 비밀번호를 마지막으로 발급한 시각. 미발급이면 NULL';

-- users: 다음 로그인에서 비밀번호를 바꾸게 할지 여부
-- 기본값은 false 이다.
ALTER TABLE users
    ADD COLUMN temporary_password_required BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '임시 비밀번호 발급 등으로 비밀번호 변경이 필요하면 1';

