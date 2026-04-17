-- =============================================================================
-- V11 (PostgreSQL): 알림 설정 테이블 + 만료 예정 알림 타입
-- =============================================================================
-- 목적
--   1) notifications.type 에 'RESERVATION_EXPIRING_SOON' 을 허용한다.
--      (예약/결제 마감 등 만료 임박 시 사용자에게 보내는 알림)
--   2) 사용자별로 알림 종류(type)마다 수신 여부를 저장한다.
--      앱/배치에서 발송 전에 이 테이블을 조회해 OFF 인 타입은 보내지 않도록 한다.
--
-- 타입 코드 (notifications / notification_preferences 공통)
--   RESERVATION_CONFIRMED      예약 확정
--   RESERVATION_CANCELED       예약 취소
--   PAYMENT_FAILED             결제 실패
--   PAYMENT_EXPIRED            결제 기한 만료
--   RESERVATION_SLOT_AVAILABLE 예약 가능 슬롯 알림(대기 등)
--   RESERVATION_EXPIRING_SOON  예약/결제 등 만료 임박
-- =============================================================================

-- -----------------------------------------------------------------------------
-- notifications: type CHECK 제약 갱신
-- 기존 허용 목록에 RESERVATION_EXPIRING_SOON 추가 (애플리케이션 Enum 과 일치)
-- -----------------------------------------------------------------------------
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_type;
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

-- -----------------------------------------------------------------------------
-- notification_preferences: 사용자별 알림 채널(타입) ON/OFF
-- (user_id, type) 유일 — 동일 사용자·동일 타입 행은 하나만 존재
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_preferences (
    -- surrogate PK
    id           BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- users.id FK
    user_id      BIGINT        NOT NULL,
    -- 알림 비즈니스 타입 (notifications.type 과 동일 집합)
    type         VARCHAR(40)   NOT NULL,
    -- TRUE: 해당 타입 알림 수신, FALSE: 차단
    enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP(6)  NOT NULL,
    updated_at   TIMESTAMP(6)  NOT NULL,
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
);

-- 사용자 기준 설정 조회·배치 스캔용
CREATE INDEX IF NOT EXISTS idx_np_user_id ON notification_preferences (user_id);
