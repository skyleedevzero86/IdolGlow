-- =============================================================================
-- V11 (MySQL): 알림 설정 테이블 + 만료 예정 알림 타입
-- =============================================================================
-- 목적
--   1) notifications.type CHECK 에 'RESERVATION_EXPIRING_SOON' 추가
--      (만료 임박 알림 행 저장 시 DB 제약과 애플리케이션 Enum 일치)
--   2) notification_preferences: 사용자·알림종류별 수신 여부(기본 ON 가정, 행이 없으면 앱 정책에 따름)
--
-- 타입 코드 (notifications / notification_preferences 공통)
--   RESERVATION_CONFIRMED, RESERVATION_CANCELED, PAYMENT_FAILED, PAYMENT_EXPIRED,
--   RESERVATION_SLOT_AVAILABLE, RESERVATION_EXPIRING_SOON
-- =============================================================================

-- notifications.type 허용 값 확장
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

-- 사용자별 알림 타입 수신 설정 (user_id + type 유일)
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
