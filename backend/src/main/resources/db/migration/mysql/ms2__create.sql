-- =============================================================================
-- create: 테이블 생성
-- -----
-- 목적
-- - 테이블과 시퀀스 생성을 한 파일에 모은다.
-- 대상
-- - ms
-- 주의
-- - 새 데이터베이스는 setting, create, column, comment, insert, select 순서로 실행한다
-- - 이미 반영된 데이터베이스에는 이 묶음을 다시 실행하지 않는다
-- =============================================================================

-- MySQL 전체 스키마 Flyway 단계를 단일 본에 반영, 예: products.base_price
-- 새 데이터베이스를 처음 만들 때만 사용한다. 이미 같은 스키마가 반영된 환경에는 실행하지 않는다.

-- 초기 스키마
-- ---
-- users: 애플리케이션 회원
-- ---
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회원 PK',
    email VARCHAR(255) NOT NULL COMMENT '로그인과 식별에 쓰는 이메일. 유니크 인덱스',
    nickname VARCHAR(10) NOT NULL COMMENT '표시 닉네임',
    role VARCHAR(255) NOT NULL COMMENT '권한 역할. USER는 일반, ADMIN은 운영자',
    last_login_at DATETIME(6) NULL COMMENT '마지막 로그인 시각',
    CONSTRAINT chk_users_role CHECK (role IN ('USER'))
) COMMENT='서비스 회원. OAuth 연동은 user_oauths 참조';

-- ---
-- user_oauths: OAuth 제공자별 계정 연결
-- ---
CREATE TABLE user_oauths (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '연동 행 PK',
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    provider VARCHAR(255) NOT NULL COMMENT 'OAuth 제공자 GOOGLE, TEST',
    provider_id VARCHAR(255) NOT NULL COMMENT '제공자 측 사용자 식별자',
    email VARCHAR(255) NOT NULL COMMENT '제공자가 넘긴 이메일',
    CONSTRAINT chk_user_oauths_provider CHECK (provider IN ('GOOGLE', 'TEST'))
) COMMENT='외부 OAuth 계정과 users 매핑';

-- ---
-- options: 상품에 붙는 옵션
-- ---
CREATE TABLE options (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '옵션 PK',
    name VARCHAR(120) NOT NULL COMMENT '옵션명',
    description TEXT NOT NULL COMMENT '옵션 상세 설명',
    price DECIMAL(38, 2) NOT NULL COMMENT '옵션 가격',
    location VARCHAR(200) NOT NULL COMMENT '옵션 관련 위치 표시 문자열',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각'
) COMMENT='상품 옵션 마스터 가격·설명·위치 문구';

-- ---
-- products: 상품 마스터
-- ---
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '상품 PK',
    name VARCHAR(120) NOT NULL COMMENT '상품명',
    description TEXT NOT NULL COMMENT '상품 설명',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    tour_attraction_picks_json TEXT NULL COMMENT 'Tour API 관광지 다중 선택 JSON',
    base_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT '상품 기본가 옵션 합산과 별도'
) COMMENT='상품 기본 정보';

-- ---
-- product_option: 상품-옵션 N:M
-- ---
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

-- ---
-- product_tag: 상품 태그
-- ---
CREATE TABLE product_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '태그 행 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    tag_name VARCHAR(50) NOT NULL COMMENT '태그 문자열',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_product_tag UNIQUE (product_id, tag_name),
    CONSTRAINT fk_product_tag_product FOREIGN KEY (product_id) REFERENCES products (id)
) COMMENT='상품별 검색·분류용 태그';

-- ---
-- product_locations: 상품 대표 위치
-- ---
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

-- ---
-- reservation_slots: 상품별 예약 가능 슬롯
-- ---
CREATE TABLE reservation_slots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '슬롯 PK',
    product_id BIGINT NOT NULL COMMENT 'products.id FK',
    reservation_date DATE NOT NULL COMMENT '슬롯 날짜',
    start_time TIME NOT NULL COMMENT '시작 시각',
    end_time TIME NOT NULL COMMENT '종료 시각 시작보다 커야 함',
    admin_note TEXT NULL COMMENT '운영자 메모',
    is_booked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '예약 확정 등으로 점유 여부',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT uk_reservation_slot_product_date_start UNIQUE (product_id, reservation_date, start_time),
    CONSTRAINT fk_reservation_slots_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_reservation_slots_time CHECK (end_time > start_time)
) COMMENT='예약 가능 시간 슬롯';

-- ---
-- reservations: 사용자 예약
-- ---
CREATE TABLE reservations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '예약 PK',
    reservation_slot_id BIGINT NOT NULL COMMENT 'reservation_slots.id FK',
    user_id BIGINT NOT NULL COMMENT '예약자 users.id 앱 레벨 정합성',
    visit_date DATE NOT NULL COMMENT '방문일',
    visit_start_time TIME NOT NULL COMMENT '방문 시작 시각',
    visit_end_time TIME NOT NULL COMMENT '방문 종료 시각',
    total_price DECIMAL(15, 2) NOT NULL COMMENT '예약 총액',
    status VARCHAR(20) NOT NULL COMMENT 'PREBOOK/PENDING/BOOKED/COMPLETED/CANCELED',
    admin_memo TEXT NULL COMMENT '관리자 메모',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_reservations_reservation_slot FOREIGN KEY (reservation_slot_id) REFERENCES reservation_slots (id),
    CONSTRAINT chk_reservations_status CHECK (status IN ('PREBOOK', 'PENDING', 'BOOKED', 'COMPLETED', 'CANCELED'))
) COMMENT='회원의 예약 건';

-- ---
-- product_reviews: 상품 리뷰
-- ---
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
) COMMENT='상품별 사용자 리뷰 상품·사용자 유니크';

-- ---
-- images: 집계 타입별 첨부 이미지 메타
-- ---
CREATE TABLE images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 PK',
    aggregate_type VARCHAR(30) NOT NULL COMMENT '소속 엔티티 종류 PRODUCT/OPTION/USER/PRODUCT_REVIEW',
    aggregate_id BIGINT NOT NULL COMMENT '소속 엔티티 PK',
    original_filename VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    unique_filename VARCHAR(255) NOT NULL COMMENT '저장용 유니크 파일명',
    extension VARCHAR(20) NOT NULL COMMENT '확장자',
    file_size BIGINT NOT NULL COMMENT '파일 크기 바이트, 양수',
    url VARCHAR(500) NOT NULL COMMENT '접근 URL',
    sort_order INT NOT NULL COMMENT '같은 aggregate 내 정렬 순서 0 이상',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_images_aggregate_type CHECK (aggregate_type IN ('PRODUCT', 'OPTION', 'USER', 'PRODUCT_REVIEW')),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_images_file_size CHECK (file_size > 0)
) COMMENT='도메인 엔티티별 저장 이미지 메타';

-- ---
-- user_survey: 온보딩/취향 설문
-- ---
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
) COMMENT='사용자 설문 콘셉트·아이돌명·방문 기간';

-- ---
-- user_survey_places: 설문에 선택한 장소 목록
-- ---
CREATE TABLE user_survey_places (
    user_survey_id BIGINT NOT NULL COMMENT 'user_survey.id FK',
    place VARCHAR(100) NOT NULL COMMENT '장소명',
    CONSTRAINT fk_user_survey_places_survey FOREIGN KEY (user_survey_id) REFERENCES user_survey (id)
) COMMENT='설문에 기입한 방문 희망 장소 복수 행';

-- ---
-- wishes: 상품 찜
-- ---
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

-- ---
-- schedules: 마이페이지 일정
-- ---
CREATE TABLE schedules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '일정 PK',
    user_id BIGINT NOT NULL COMMENT 'users.id',
    product_id BIGINT NOT NULL COMMENT 'products.id',
    title VARCHAR(100) NOT NULL COMMENT '일정 제목',
    start_at DATETIME(6) NOT NULL COMMENT '시작 시각',
    end_at DATETIME(6) NOT NULL COMMENT '종료 시각 시작 이상',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_schedules_time CHECK (end_at >= start_at)
) COMMENT='사용자 일정 상품 참조';

-- ---
-- payments: 예약 1건당 1건 결제
-- ---
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '결제 PK',
    reservation_id BIGINT NOT NULL COMMENT 'reservations.id FK 유니크',
    provider VARCHAR(20) NOT NULL COMMENT '결제 제공자 현재 MOCK',
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
) COMMENT='예약 연동 결제 모의 결제';

-- ---
-- notifications: 사용자 알림
-- ---
CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '알림 PK',
    user_id BIGINT NOT NULL COMMENT '수신자 users.id',
    type VARCHAR(40) NOT NULL COMMENT 'RESERVATION_CONFIRMED/RESERVATION_CANCELED/PAYMENT_FAILED/PAYMENT_EXPIRED',
    title VARCHAR(120) NOT NULL COMMENT '알림 제목',
    message VARCHAR(500) NOT NULL COMMENT '알림 본문',
    link VARCHAR(255) NULL COMMENT '이동 URL 옵션',
    read_at DATETIME(6) NULL COMMENT '읽은 시각 NULL이면 미읽음',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_notifications_type CHECK (type IN ('RESERVATION_CONFIRMED', 'RESERVATION_CANCELED', 'PAYMENT_FAILED', 'PAYMENT_EXPIRED'))
) COMMENT='회원별 인앱 알림';

-- images 테이블을 보장하고 개발용 샘플 한 건을 넣는다
CREATE TABLE IF NOT EXISTS images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 PK',
    aggregate_type VARCHAR(30) NOT NULL COMMENT 'PRODUCT/OPTION/USER/PRODUCT_REVIEW',
    aggregate_id BIGINT NOT NULL COMMENT '소속 엔티티 PK',
    original_filename VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    unique_filename VARCHAR(255) NOT NULL COMMENT '스토리지 저장용 유니크 파일명',
    extension VARCHAR(20) NOT NULL COMMENT '확장자',
    file_size BIGINT NOT NULL COMMENT '파일 크기 byte',
    url VARCHAR(500) NOT NULL COMMENT '접근 URL',
    sort_order INT NOT NULL COMMENT '동일 aggregate 내 정렬 0 이상',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_images_aggregate_type CHECK (aggregate_type IN ('PRODUCT', 'OPTION', 'USER', 'PRODUCT_REVIEW')),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_images_file_size CHECK (file_size > 0),
    INDEX idx_images_aggregate (aggregate_type, aggregate_id, sort_order)
) COMMENT='엔티티별 첨부 이미지 메타데이터';

-- images 테이블 존재 보장
CREATE TABLE IF NOT EXISTS images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 PK',
    aggregate_type VARCHAR(30) NOT NULL COMMENT 'PRODUCT/OPTION/USER/PRODUCT_REVIEW',
    aggregate_id BIGINT NOT NULL COMMENT '소속 엔티티 PK',
    original_filename VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    unique_filename VARCHAR(255) NOT NULL COMMENT '스토리지 저장용 유니크 파일명',
    extension VARCHAR(20) NOT NULL COMMENT '확장자',
    file_size BIGINT NOT NULL COMMENT '파일 크기 byte',
    url VARCHAR(500) NOT NULL COMMENT '접근 URL',
    sort_order INT NOT NULL COMMENT '동일 aggregate 내 정렬 0 이상',
    created_at DATETIME(6) NOT NULL COMMENT '생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '수정 시각',
    CONSTRAINT chk_images_aggregate_type CHECK (aggregate_type IN ('PRODUCT', 'OPTION', 'USER', 'PRODUCT_REVIEW')),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_images_file_size CHECK (file_size > 0),
    INDEX idx_images_aggregate (aggregate_type, aggregate_id, sort_order)
) COMMENT='엔티티별 첨부 이미지 메타데이터';

CREATE TABLE product_review_helpful_votes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_review_helpful_user UNIQUE (review_id, user_id),
    CONSTRAINT fk_helpful_review FOREIGN KEY (review_id) REFERENCES product_reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_helpful_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='리뷰 도움돼요';

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

CREATE TABLE payment_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    PRIMARY KEY (id),
    payment_id BIGINT NULL COMMENT 'payments.id FK 삭제 시 NULL',
    order_id VARCHAR(200) NULL COMMENT '주문 식별자 검색용',
    payment_key VARCHAR(200) NULL COMMENT '결제대행사가 내려준 결제 건 고유 키',
    log_type VARCHAR(50) NOT NULL COMMENT 'CONFIRM_*/CANCEL_*/WEBHOOK_*/MOCK_WEBHOOK/SYSTEM',
    step VARCHAR(50) NULL COMMENT 'CLIENT / SERVER / TOSS_API',
    request_url VARCHAR(500) NULL COMMENT '호출 URL 경로',
    http_method VARCHAR(20) NULL COMMENT 'HTTP 메서드',
    http_status INT NULL COMMENT 'HTTP 응답 상태 코드',
    request_body JSON NULL COMMENT '요청 본문 JSON 등',
    response_body JSON NULL COMMENT '응답 본문 JSON 등',
    error_code VARCHAR(100) NULL COMMENT '에러 코드',
    error_message VARCHAR(1000) NULL COMMENT '에러 메시지',
    stack_trace TEXT NULL COMMENT '예외 스택 내부 오류 시',
    client_ip VARCHAR(100) NULL COMMENT '클라이언트 IP',
    user_agent VARCHAR(500) NULL COMMENT 'User-Agent',
    trace_id VARCHAR(100) NULL COMMENT '요청 추적 ID X-Trace-Id 등',
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

CREATE TABLE payment_refunds (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '환불 이력 PK',
    PRIMARY KEY (id),
    payment_id BIGINT NOT NULL COMMENT 'payments.id FK',
    reservation_id BIGINT NOT NULL COMMENT 'reservations.id FK 조회 편의',
    cancel_amount DECIMAL(15, 2) NOT NULL COMMENT '해당 요청 취소 환불 금액',
    cancel_reason VARCHAR(500) NOT NULL COMMENT '취소 사유 사용자/운영/시스템 문구',
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

-- 사용자별 알림 타입 수신 설정
CREATE TABLE notification_preferences (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '설정 PK',
    user_id      BIGINT        NOT NULL COMMENT '사용자 FK → users.id',
    type         VARCHAR(40)   NOT NULL COMMENT '알림 타입 notifications.type 과 동일 집합',
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

-- 기사·본문 섹션·갤러리·태그
-- PK: BIGINT AUTO_INCREMENT. FK는 별도 CONSTRAINT로 선언 순서: 자식 테이블 정의 후.
-- updated_at: ON UPDATE CURRENT_TIMESTAMP — DB에서 갱신 시각 자동 반영 앱과 이중 갱신 시 정책 통일 권장.
-- 웹진 호
create table if not exists webzine_issues (
    id bigint not null auto_increment primary key comment '이슈 PK',
    slug varchar(80) not null comment '공개 URL용 슬러그',
    volume int not null comment '권호 정수, 전역 유일',
    issue_date date not null comment '발행일',
    cover_image_url varchar(500) not null comment '표지 이미지 URL',
    teaser varchar(1000) not null comment '목록/공유용 티저',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_issue_slug unique (slug),
    constraint uk_webzine_issue_volume unique (volume)
) comment='웹진 이슈 호 메타';

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
    format_label varchar(60) not null comment '포맷 라벨 인터뷰 등',
    author_name varchar(120) not null comment '표시용 저자명',
    author_email varchar(255) not null comment '연락/메타용 이메일',
    credit_line varchar(255) not null comment '사진·출처 크레딧',
    highlight_quote varchar(500) comment '강조 인용 옵션',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_issue_slug unique (issue_id, slug),
    constraint fk_webzine_articles_issue foreign key (issue_id) references webzine_issues(id) on delete cascade
) comment='웹진 기사 본문 메타';

-- 본문 섹션
create table if not exists webzine_article_sections (
    id bigint not null auto_increment primary key comment '섹션 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '렌더 순서',
    heading varchar(200) comment '소제목 옵션',
    body text not null comment '본문 표현 형식은 앱 규약',
    note varchar(1000) comment '각주·편집자 주 옵션',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_section_order unique (article_id, display_order),
    constraint fk_webzine_article_sections_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 본문 블록 섹션';

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
) comment='기사 갤러리 이미지 순서';

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

-- 구독자 및 콘텐츠 발송 이력 테이블
-- 이메일 구독 상태 동의/활성 여부 와 뉴스레터·웹진 발송 결과를 추적한다.
-- updated_at은 ON UPDATE CURRENT_TIMESTAMP로 DB에서 자동 갱신한다.
create table if not exists email_subscriptions (
    id bigint auto_increment primary key,
    email varchar(255) not null comment '구독자 이메일 전역 유일',
    subscribed_newsletters boolean not null default true comment '뉴스레터 수신 여부',
    subscribed_issues boolean not null default true comment '웹진/이슈 수신 여부',
    consented_at timestamp not null comment '수신 동의 시각',
    subscribed_at timestamp not null comment '구독 등록 시각',
    subscription_source varchar(50) not null comment '구독 유입 채널',
    active boolean not null default true comment '활성 구독 상태',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_email_subscriptions_email unique (email)
) comment='구독자 마스터 테이블 수신 동의/활성 상태 포함';

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

-- 사이트 도메인, 배너, 팝업, 메인이미지
-- ---
-- tb_domain_list: 멀티 사이트구분용 도메인 마스터
-- ---
create table if not exists tb_domain_list (
    domain_id varchar(20) primary key comment '사이트 도메인 식별자. kr, en처럼 짧은 코드',
    domain_nm varchar(100) comment '표시명',
    domain_path varchar(200) comment 'URL 경로 프리픽스 등',
    domain_dc varchar(500) comment '설명',
    use_at char(1) default 'Y' comment '사용 여부 Y/N',
    actvty_at char(1) default 'Y' comment '활성 여부 Y/N',
    sort_seq int default 0 comment '정렬 순서'
) engine=InnoDB default charset=utf8mb4 comment='사이트·언어 도메인 목록 배너/팝업/메인이미지 공통 참조';

-- ---
-- tb_banner: 상단·슬롯 등 배너 콘텐츠
-- ---
create table if not exists tb_banner (
    banner_id varchar(20) primary key comment '배너 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    banner_nm varchar(200) comment '배너명',
    link_url varchar(500) comment '클릭 시 이동 URL',
    banner_image varchar(500) comment '이미지 경로/URL',
    banner_image_file varchar(500) comment '원본 파일명 등',
    banner_dc varchar(1000) comment '설명',
    sort_ordr int default 0 comment '노출 정렬 순서',
    reflct_at char(1) default 'Y' comment '게시 반영 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_banner_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='배너 관리';

-- ---
-- tb_popup_manage: 기간·위치·크기 속성을 가진 팝업
-- ---
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
    popup_vrticl_size int default 400 comment '세로 크기 px',
    popup_width_size int default 500 comment '가로 크기 px',
    ntce_bgnde varchar(20) comment '게시 시작 시각. 문자열로 저장한다',
    ntce_endde varchar(20) comment '게시 종료',
    stopvew_setup_at char(1) default 'Y' comment '다시보지 않기 설정 Y/N',
    ntce_at char(1) default 'Y' comment '게시 알림 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_popup_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='팝업 관리';

-- ---
-- tb_main_image: 메인 영역 슬라이드/히어로 이미지
-- ---
create table if not exists tb_main_image (
    image_id varchar(20) primary key comment '이미지 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    image_nm varchar(200) comment '이미지 제목',
    image varchar(500) comment '이미지 경로나 URL. 기존 운영 스키마와 맞추기 위해 컬럼 이름을 image 로 둔다',
    image_file varchar(500) comment '파일명',
    image_dc varchar(1000) comment '설명',
    link_url varchar(500) comment '슬라이드 클릭 시 이동 경로',
    category_label varchar(20) default '메인' comment '슬라이드 분류. 메인, 이벤트, 할인, 공지',
    reflct_at char(1) default 'Y' comment '게시 반영 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_main_image_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='메인 이미지 관리';

-- MySQL
-- ---
-- editor_documents
-- ---
create table if not exists editor_documents (
    id char(36) not null primary key,
    title varchar(120) not null,
    author varchar(60) not null,
    markdown longtext not null,
    tags_json longtext not null default ('[]'),
    updated_at timestamp(6) not null,
    embedding text not null,
    publication_status varchar(20) not null default 'PUBLISHED',
    url_slug varchar(180),
    introduction text null,
    thumbnail_image_url text,
    view_count bigint not null default 0
) engine=InnoDB default charset=utf8mb4;

-- ---
-- editor_assets
-- ---
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

CREATE TABLE user_password_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 행 PK' PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'users.id FK',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 등으로 인코딩된 비밀번호',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이력 저장 시각',
    CONSTRAINT fk_user_password_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) COMMENT = '과거 비밀번호 해시 재사용 방지 조회용';

-- 구독 콘텐츠 자동 발송 스케줄
-- 콘텐츠 유형 뉴스레터·웹진 등 별로 발송 주기·요일·시각을 정의한다.
-- content_type 당 스케줄은 한 행만 허용 UK. 스케줄 비활성 시 active=false.
-- 애플리케이션의 배치/스케줄러가 이 테이블을 읽어 발송 시점을 결정한다.
-- updated_at은 행 변경 시 DB가 자동 갱신한다 구독·발송 관련 테이블과 동일 패턴.
create table if not exists subscription_dispatch_schedules (
    id bigint not null auto_increment comment '스케줄 행 PK',
    content_type varchar(40) not null comment '콘텐츠 유형 코드. 유형당 스케줄은 한 행만 둔다',
    frequency_type varchar(20) not null comment '반복 주기 유형. DAILY, WEEKLY, MONTHLY 등',
    day_of_week varchar(20) null comment '주간 등 주기에 따른 요일 애플리케이션 규약 문자열. 해당 없으면 NULL',
    dispatch_hour int not null comment '발송 시각 시, 0–23',
    dispatch_minute int not null comment '발송 시각 분, 0–59',
    active boolean not null default true comment '스케줄 사용 여부 배치에서 false 행은 건너뜀',
    created_at timestamp not null default current_timestamp comment '행 생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    primary key (id),
    constraint uk_subscription_dispatch_schedule_content_type unique (content_type)
) comment='구독 콘텐츠 자동 발송 스케줄 유형별 주기·시각';

-- 인증 검증 감사 로그
CREATE TABLE IF NOT EXISTS auth_verification_audit_logs (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '로그 PK',
    verification_type  VARCHAR(64)   NOT NULL COMMENT '검증 유형 코드. 서비스에서 정한 문자열',
    email              VARCHAR(190)  NULL COMMENT '시도에 사용된 이메일 주소',
    username           VARCHAR(120)  NULL COMMENT '시도에 사용된 사용자명 또는 닉네임',
    ip_address         VARCHAR(64)   NOT NULL COMMENT '요청 클라이언트 IP 주소',
    success            BOOLEAN       NOT NULL COMMENT '검증 성공 여부 1 성공, 0 실패',
    detail             VARCHAR(500)  NULL COMMENT '실패 사유·메모 등 부가 설명',
    created_at         DATETIME(6)   NOT NULL COMMENT '로그가 기록된 시각'
) COMMENT='인증·검증 이메일 코드 등 시도 감사 로그';

-- 계정 검증 토큰
CREATE TABLE IF NOT EXISTS signup_verification_tokens (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '행 PK',
    token          VARCHAR(120)  NOT NULL COMMENT '일회성 검증 토큰 전역 유일',
    type           VARCHAR(40)   NOT NULL COMMENT '검증 유형 코드',
    email          VARCHAR(190)  NOT NULL COMMENT '검증 대상 이메일',
    username       VARCHAR(120)  NULL COMMENT '닉네임 등 선택',
    user_id        BIGINT        NULL COMMENT '확인 후 연결된 users.id 선택',
    status         VARCHAR(30)   NOT NULL COMMENT '토큰 상태 코드',
    requested_ip   VARCHAR(64)   NOT NULL COMMENT '발급 요청 IP',
    confirmed_ip   VARCHAR(64)   NULL COMMENT '검증 완료 시 IP 선택',
    expires_at     DATETIME(6)   NOT NULL COMMENT '만료 시각',
    created_at     DATETIME(6)   NOT NULL COMMENT '행 생성 시각',
    confirmed_at   DATETIME(6)   NULL COMMENT '검증·사용 완료 시각 선택',
    detail         VARCHAR(500)  NULL COMMENT '거절 사유 등 선택',
    CONSTRAINT uk_signup_verification_tokens_token UNIQUE (token)
) COMMENT='가입·계정 확인용 검증 토큰 저장소';

CREATE TABLE IF NOT EXISTS product_latest_korea_recommendation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'PK',
    display_order INT NOT NULL COMMENT '화면 노출 순서 1부터 증가',
    product_id BIGINT NOT NULL COMMENT '추천 대상 상품 ID',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_latest_korea_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_latest_korea_display_order UNIQUE (display_order),
    CONSTRAINT uk_latest_korea_product_id UNIQUE (product_id)
) COMMENT='latest-in-korea 추천 상품 수동 큐레이션 순서';

-- ----------------------------------------------------------------------------
-- [B] structured survey — 테이블 DDL 의존 순서
-- ----------------------------------------------------------------------------

-- survey_form: 설문 템플릿 제목·설명·활성. active 로 버전 전환 운영 가능.
CREATE TABLE IF NOT EXISTS survey_form (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- survey_question: 문항 정의 순서·타입·필수. form 삭제 시 CASCADE.
CREATE TABLE IF NOT EXISTS survey_question (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT NULL,
    question_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_question_form
        FOREIGN KEY (survey_form_id)
        REFERENCES survey_form (id)
        ON DELETE CASCADE
);

-- survey_question_option: 객관식 선택지. display_order 로 UI 순서 고정.
CREATE TABLE IF NOT EXISTS survey_question_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_question_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    option_text VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_question_option_question
        FOREIGN KEY (survey_question_id)
        REFERENCES survey_question (id)
        ON DELETE CASCADE
);

-- survey_submission: 사용자 1회 제출 헤더 form + user FK.
CREATE TABLE IF NOT EXISTS survey_submission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_submission_form
        FOREIGN KEY (survey_form_id)
        REFERENCES survey_form (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_survey_submission_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

-- survey_answer: 문항별 응답 본문 TEXT는 answer_text, 객관식은 answer_option 확장.
CREATE TABLE IF NOT EXISTS survey_answer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_submission_id BIGINT NOT NULL,
    survey_question_id BIGINT NOT NULL,
    answer_text TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_answer_submission
        FOREIGN KEY (survey_submission_id)
        REFERENCES survey_submission (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_survey_answer_question
        FOREIGN KEY (survey_question_id)
        REFERENCES survey_question (id)
        ON DELETE CASCADE
);

-- survey_answer_option: 객관식·복수선택 스냅샷 option_text.
CREATE TABLE IF NOT EXISTS survey_answer_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_answer_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    option_text VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_answer_option_answer
        FOREIGN KEY (survey_answer_id)
        REFERENCES survey_answer (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exchange_branch (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    rate DECIMAL(20, 8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    airport_hub BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exchange_alert (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    from_currency VARCHAR(8) NOT NULL,
    to_currency VARCHAR(8) NOT NULL,
    target_rate DECIMAL(20, 8) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_exchange_alert_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS survey_form_description (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_id BIGINT NOT NULL COMMENT '소속 설문지 ID FK',
    markdown TEXT NOT NULL COMMENT '설문 설명 마크다운',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_form_description_form
        FOREIGN KEY (survey_form_id)
        REFERENCES survey_form (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_survey_form_description_form UNIQUE (survey_form_id)
) COMMENT='설문 설명 마크다운 본문';

CREATE TABLE IF NOT EXISTS survey_form_description_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    survey_form_description_id BIGINT NOT NULL COMMENT '소속 설문 설명 ID FK',
    display_order INT NOT NULL COMMENT '태그 노출 순서',
    tag_name VARCHAR(100) NOT NULL COMMENT '태그명',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_survey_form_description_tag_description
        FOREIGN KEY (survey_form_description_id)
        REFERENCES survey_form_description (id)
        ON DELETE CASCADE
) COMMENT='설문 설명 태그 목록';

CREATE TABLE IF NOT EXISTS search_keyword_stats (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 1,
    last_searched_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_search_keyword_stats_keyword UNIQUE (keyword)
);

-- =============================================================================
-- 토스 빌링 글로우패스 핵심 테이블
-- -----
-- 목적
-- - 카드 자동결제용 결제수단·구독·회차 결제 이력 저장
-- - 일반결제와 분리된 정기결제 원장
-- 대상
-- - 마이에스큐엘 / toss_billing_methods, toss_billing_subscriptions, toss_billing_charges
-- 연관
-- - TossBillingService, BillingChargeExecutor, /payments/toss/billing
-- =============================================================================

-- 등록된 카드 빌링키. 카드번호 원문과 씨브이씨는 저장하지 않음
CREATE TABLE IF NOT EXISTS toss_billing_methods (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '결제수단 기본키',
    user_id BIGINT NOT NULL COMMENT '회원 아이디. 한 사용자에 활성 수단 1개 권장',
    customer_key VARCHAR(100) NOT NULL COMMENT '토스 구매자 식별키. idolglow-user-회원아이디 형식',
    billing_key VARCHAR(300) NOT NULL COMMENT '토스 빌링키. 카드 대신 자동결제에 쓰는 값. 이후 암호화하여 저장',
    card_company VARCHAR(50) NULL COMMENT '카드사명 표시용. 예: 현대, 국민',
    card_number VARCHAR(40) NULL COMMENT '마스킹된 카드번호. 예: 43301234****123*',
    card_type VARCHAR(30) NULL COMMENT '카드 종류. 예: 신용, 체크',
    owner_type VARCHAR(30) NULL COMMENT '소유 구분. 예: 개인, 법인',
    authenticated_at DATETIME(6) NULL COMMENT '토스 카드 인증 등록 완료 시각',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '현재 사용 중 여부. 재등록 시 기존 행은 비활성',
    created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각',
    CONSTRAINT uk_toss_billing_methods_billing_key UNIQUE (billing_key)
) COMMENT='글로우패스 카드 자동결제 수단';

-- 월간 구독 상태. 회원과 요금제 조합당 월간 1건
CREATE TABLE IF NOT EXISTS toss_billing_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '구독 기본키',
    user_id BIGINT NOT NULL COMMENT '회원 아이디',
    billing_method_id BIGINT NOT NULL COMMENT '결제에 쓸 결제수단 아이디 외래키',
    plan_code VARCHAR(50) NOT NULL DEFAULT 'MONTHLY' COMMENT '요금제 코드. 현재 월간 고정',
    order_name VARCHAR(200) NOT NULL COMMENT '토스와 영수증에 노출되는 주문명. 예: 아이돌글로우 월간 구독',
    amount BIGINT NOT NULL COMMENT '매월 청구 금액 원 단위. 기본 4900',
    status VARCHAR(30) NOT NULL COMMENT '상태. 활성, 일시중지, 해지예약, 해지완료',
    next_billing_at DATETIME(6) NULL COMMENT '다음 자동결제 예정 시각. 스케줄러 도래 판단 기준',
    last_charged_at DATETIME(6) NULL COMMENT '최근 결제 성공 시각',
    created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각',
    CONSTRAINT fk_toss_billing_subscriptions_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id),
    CONSTRAINT uk_toss_billing_subscriptions_user_plan UNIQUE (user_id, plan_code)
) COMMENT='글로우패스 월간 구독';

-- 결제 회차. 주문번호 유니크로 같은 달 이중 청구 방지
CREATE TABLE IF NOT EXISTS toss_billing_charges (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회차 결제 기본키',
    user_id BIGINT NOT NULL COMMENT '회원 아이디',
    subscription_id BIGINT NULL COMMENT '소속 구독 아이디. 단건 테스트 결제는 비울 수 있음',
    billing_method_id BIGINT NOT NULL COMMENT '실제 청구에 사용한 결제수단 아이디 외래키',
    order_id VARCHAR(200) NOT NULL COMMENT '주문번호. 구독회차는 구독아이디와 연월, 단건은 임의값',
    order_name VARCHAR(200) NOT NULL COMMENT '해당 회차 주문명',
    amount BIGINT NOT NULL COMMENT '이 회차 청구 금액 원 단위',
    payment_key VARCHAR(200) NULL COMMENT '토스 결제키. 승인 성공 시 저장',
    status VARCHAR(30) NOT NULL COMMENT '상태. 대기, 성공, 실패',
    raw_response TEXT NULL COMMENT '토스 에이피아이 원문 응답. 디버깅과 대사에 사용',
    fail_message VARCHAR(500) NULL COMMENT '실패 시 사용자와 운영 안내 메시지',
    approved_at DATETIME(6) NULL COMMENT '토스 승인 완료 시각',
    created_at DATETIME(6) NOT NULL COMMENT '회차 행 생성 시각. 대기 상태로 먼저 저장하는 시점',
    updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각',
    CONSTRAINT uk_toss_billing_charges_order_id UNIQUE (order_id),
    CONSTRAINT fk_toss_billing_charges_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id)
) COMMENT='빌링 자동결제 회차별 승인 이력';

-- =============================================================================
-- 스케줄 잠금 테이블과 빌링키 해시·암호문 컬럼 마이에스큐엘
-- -----
-- 목적
-- - 스케줄러 다중 인스턴스 잠금
-- - 빌링키 암호문 길이 확장과 평문 유니크용 해시 컬럼
-- =============================================================================

-- 스케줄 잡 분산 락. 같은 이름 잡은은 한 인스턴스만 실행
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL COMMENT '잠금 이름. 예: 토스빌링자동결제',
    lock_until TIMESTAMP(6) NOT NULL COMMENT '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능',
    locked_at TIMESTAMP(6) NOT NULL COMMENT '락을 잡은 시각',
    locked_by VARCHAR(255) NOT NULL COMMENT '락을 잡은 호스트 또는 인스턴스 식별자',
    PRIMARY KEY (name)
) COMMENT='스케줄러 분산 락';

CREATE TABLE IF NOT EXISTS toss_billing_charge_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회차 결제 시도 기본키',
    charge_id BIGINT NOT NULL COMMENT '소속 청구 회차 아이디 외래키',
    billing_method_id BIGINT NOT NULL COMMENT '이번 시도에 사용한 결제수단 아이디 외래키',
    attempt_no INT NOT NULL COMMENT '회차 내 시도 순번. 1부터 증가',
    toss_order_id VARCHAR(220) NOT NULL COMMENT '토스에 보낸 주문번호. 회차 orderId에 시도 번호를 붙인 값',
    payment_key VARCHAR(200) NULL COMMENT '토스 결제키. 승인 성공 시 저장',
    status VARCHAR(30) NOT NULL COMMENT '상태. 대기, 성공, 실패',
    fail_message VARCHAR(500) NULL COMMENT '실패 시 안내 메시지',
    raw_response TEXT NULL COMMENT '토스 에이피아이 원문 응답',
    approved_at DATETIME(6) NULL COMMENT '토스 승인 완료 시각',
    created_at DATETIME(6) NOT NULL COMMENT '시도 행 생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각',
    CONSTRAINT uk_toss_billing_charge_attempts_charge_no UNIQUE (charge_id, attempt_no),
    CONSTRAINT uk_toss_billing_charge_attempts_toss_order UNIQUE (toss_order_id),
    CONSTRAINT fk_toss_billing_charge_attempts_charge
        FOREIGN KEY (charge_id) REFERENCES toss_billing_charges (id),
    CONSTRAINT fk_toss_billing_charge_attempts_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id)
) COMMENT='빌링 회차 내 카드별 결제 시도 이력';

-- =============================================================================
-- 토스 빌링 구매자 키
-- -----
-- 목적
-- - 사용자별 토스 customerKey를 무작위 UUID로 저장
-- - 새 카드 등록은 추측 가능한 idolglow-user-회원아이디 형식을 쓰지 않음
-- 대상
-- - 마이에스큐엘 / toss_billing_customers
-- 주의
-- - users 테이블은 변경하지 않음
-- - 기존 toss_billing_methods.customer_key는 옮기거나 바꾸지 않음
-- 연관
-- - TossBillingService.customerKeyOf, /payments/toss/billing
-- =============================================================================

-- 사용자별 토스 구매자 키. 기존 카드에 저장된 키는 이 테이블로 복사하지 않음
CREATE TABLE IF NOT EXISTS toss_billing_customers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '구매자 키 기본키',
    user_id BIGINT NOT NULL COMMENT '회원 아이디. 사용자당 1건',
    customer_key VARCHAR(36) NOT NULL COMMENT '토스 구매자 식별키. UUID 형식. 기존 카드 키와 별도',
    created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    CONSTRAINT uk_toss_billing_customers_user_id UNIQUE (user_id),
    CONSTRAINT uk_toss_billing_customers_customer_key UNIQUE (customer_key)
) COMMENT='글로우패스 토스 구매자 키. 사용자당 UUID 1개';

-- 수신자 한 명당 한 행. 성공한 주소는 재개 때 다시 보내지 않는다.
CREATE TABLE IF NOT EXISTS subscription_dispatch_recipients (
    id bigint auto_increment primary key,
    dispatch_id bigint not null comment '구독 발송 이력 ID',
    email varchar(255) not null comment '수신 이메일',
    status varchar(20) not null comment '수신자 상태. SENT, FAILED',
    sent_at datetime(6) null comment '발송에 성공한 시각',
    error varchar(1000) null comment '실패 원인. 1000자 이내',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_subscription_dispatch_recipient unique (dispatch_id, email),
    constraint fk_subscription_dispatch_recipient_history
        foreign key (dispatch_id) references subscription_dispatch_histories (id)
) comment='구독 발송 수신자별 결과';

-- =============================================================================
-- 공연 메타 글로벌 식별자
-- -----
-- 목적
-- - 공급처 공연을 배치로 모아 같은 공연은 global_id 하나로 묶는다
-- 대상
-- - 마이에스큐엘 / event_master, event_source_ref, event_collect_log
-- 주의
-- - 제목만 같으면 확정하지 않는다. 기간이 겹치고 장소까지 같을 때만 CONFIRMED
-- - source, source_id 는 공급처 원본 한 건이다
-- 연관
-- - EventMetaCollectService, EventMetaCollectScheduler
-- =============================================================================

-- 대표 공연. 공급처 표기가 달라도 같은 공연이면 이 행을 같이 가리킨다.
CREATE TABLE IF NOT EXISTS event_master (
    id bigint auto_increment primary key,
    global_id varchar(36) not null comment '글로벌 공연 ID. UUID',
    title varchar(255) not null comment '대표 제목',
    normalized_title varchar(255) not null comment '매칭용 제목. 괄호·공백·기호를 뺀 값',
    start_date date null comment '대표 시작일',
    end_date date null comment '대표 종료일',
    venue varchar(255) null comment '대표 장소',
    normalized_venue varchar(255) null comment '매칭용 장소',
    status varchar(20) not null comment '대표 상태. ACTIVE',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_event_master_global_id unique (global_id)
) comment='공연 대표 식별자. 공급처가 달라도 같은 공연은 한 행';

-- 공급처 원본 한 건. 확정이 아니면 global_id를 비워 자동 병합하지 않는다.
CREATE TABLE IF NOT EXISTS event_source_ref (
    id bigint auto_increment primary key,
    source varchar(80) not null comment '공급처 코드. KOPIS_PBLPRFR, SEOUL_SJW_PERFORM 등',
    source_id varchar(80) not null comment '공급처 공연 ID',
    global_id varchar(36) null comment '매칭된 글로벌 공연 ID. CONFLICT면 비움',
    match_status varchar(20) not null comment '매칭 상태. CONFIRMED, CANDIDATE, CONFLICT',
    match_score int not null comment '매칭 점수. 100이면 제목·기간·장소 일치',
    matched_at datetime(6) null comment '마지막으로 매칭한 시각',
    raw_hash varchar(64) not null comment '공급처 원본 해시. 바뀌면 다시 매칭',
    raw_payload text null comment '공급처에서 받은 원본 스냅샷',
    title varchar(255) not null comment '공급처 제목',
    start_date date null comment '공급처 시작일',
    end_date date null comment '공급처 종료일',
    venue varchar(255) null comment '공급처 장소',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_event_source_ref_source unique (source, source_id),
    constraint fk_event_source_ref_master
        foreign key (global_id) references event_master (global_id)
) comment='공급처 공연 원본과 글로벌 식별자 매칭';

-- 공급처별 수집 결과. 한 곳이 실패해도 다른 곳 저장은 남긴다.
CREATE TABLE IF NOT EXISTS event_collect_log (
    id bigint auto_increment primary key,
    source varchar(80) not null comment '공급처 코드',
    status varchar(20) not null comment '수집 상태. SUCCESS, FAILED',
    item_count int not null comment '이번 수집에서 저장을 시도한 건수',
    error_message varchar(1000) null comment '실패 원인. 1000자 이내',
    collected_at datetime(6) not null comment '수집 시각',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각'
) comment='공연 공급처 수집 결과';

-- =============================================================================
-- 공연 언어별 제목·소개
-- -----
-- 목적
-- - 글로벌 공연에 공급처·운영자 번역을 언어별로 붙인다
-- 대상
-- - 마이에스큐엘 / event_translation
-- 주의
-- - MACHINE 행은 저장할 수 있으나 조회에서는 노출하지 않는다
-- - global_id, locale 은 언어당 한 행
-- 연관
-- - EventLocalizedTextService, TourEventTranslationCollector
-- =============================================================================

-- 공연 번역. 요청 언어가 없으면 영어, 그다음 한국어로 대체한다.
CREATE TABLE IF NOT EXISTS event_translation (
    id bigint auto_increment primary key,
    global_id varchar(36) not null comment '글로벌 공연 ID',
    locale varchar(8) not null comment '언어 코드. ko, en, ja, zh',
    title varchar(255) not null comment '해당 언어 제목',
    synopsis text null comment '해당 언어 소개. 없으면 비움',
    origin varchar(20) not null comment '출처. PROVIDER, MANUAL, MACHINE. MACHINE은 노출하지 않음',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막으로 번역을 바꾼 시각',
    constraint uk_event_translation_global_locale unique (global_id, locale),
    constraint fk_event_translation_master
        foreign key (global_id) references event_master (global_id)
) comment='공연 언어별 제목·소개';

-- 운영 변경 한 건. 적용 시점에 이전 상태를 남기고 되돌리기에서 복구한다.
CREATE TABLE IF NOT EXISTS ops_change_request (
    id bigint auto_increment primary key,
    type varchar(80) not null comment '변경 종류. EVENT_MATCH_MERGE 등',
    target varchar(255) not null comment '변경 대상. 공연 병합은 공급처|공급처ID',
    payload text not null comment '요청 내용. JSON',
    requested_by varchar(80) not null comment '요청자. 사용자 ID 또는 에이전트 이름',
    requested_by_kind varchar(20) not null comment '요청자 종류. USER, AGENT',
    status varchar(20) not null comment '상태. REQUESTED, APPROVED, REJECTED, APPLIED, REVERTED',
    before_snapshot text null comment '적용 직전 상태. 되돌리기에서 복구',
    approved_by varchar(80) null comment '승인자 사용자 ID',
    applied_at datetime(6) null comment '적용 시각',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각'
) comment='운영 변경 요청. 승인 뒤에만 적용하고 되돌릴 수 있다';
