-- =============================================================================
-- V2 (MySQL): 예약·결제·알림·보안 확장
-- - users: ADMIN 역할 허용
-- - reservation_slots: 결제 대기 시 슬롯 잠금(hold) 컬럼 및 FK
-- - reservations: 만료/확정/취소 시각·취소 사유
-- - payments: 예약당 1건 결제(Mock)
-- - notifications: 사용자 알림
-- =============================================================================

-- -----------------------------------------------------------------------------
-- users: ADMIN 역할 추가 (운영·관리 API용)
-- -----------------------------------------------------------------------------
ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- -----------------------------------------------------------------------------
-- reservation_slots: PENDING 예약이 슬롯을 점유할 때 hold 정보
-- hold_reservation_id: 잠금을 건 예약 PK (reservations 참조, V2에서 컬럼 추가 후 FK)
-- hold_expires_at: hold 만료 시각(스케줄러가 해제)
-- -----------------------------------------------------------------------------
ALTER TABLE reservation_slots ADD COLUMN hold_reservation_id BIGINT NULL COMMENT 'hold를 건 reservations.id';
ALTER TABLE reservation_slots ADD COLUMN hold_expires_at DATETIME(6) NULL COMMENT 'hold 만료 시각';
ALTER TABLE reservation_slots
    ADD CONSTRAINT fk_reservation_slots_hold_reservation FOREIGN KEY (hold_reservation_id) REFERENCES reservations (id);

-- hold 만료 스캔·조회용
CREATE INDEX idx_reservation_slots_hold ON reservation_slots (hold_reservation_id, hold_expires_at);

-- -----------------------------------------------------------------------------
-- reservations: 결제·만료·취소 도메인 필드
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- payments: 예약 1건당 1건 결제(Mock PG)
-- -----------------------------------------------------------------------------
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '결제 PK',
    reservation_id BIGINT NOT NULL COMMENT 'reservations.id FK(유니크)',
    provider VARCHAR(20) NOT NULL COMMENT '결제 제공자(현재 MOCK)',
    payment_reference VARCHAR(80) NOT NULL COMMENT '외부·웹훅 식별용 참조 번호',
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

-- -----------------------------------------------------------------------------
-- notifications: 사용자 알림(읽음 여부·딥링크)
-- -----------------------------------------------------------------------------
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
