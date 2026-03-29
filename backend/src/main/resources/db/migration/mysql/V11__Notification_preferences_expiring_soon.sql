-- =============================================================================
-- V11 (MySQL): 알림 설정 + 만료 예정 알림 타입 추가
-- =============================================================================

-- notifications.type CHECK 제약 업데이트 (RESERVATION_SLOT_AVAILABLE, RESERVATION_EXPIRING_SOON 추가)
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

-- 사용자별 알림 수신 설정
CREATE TABLE notification_preferences (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '설정 PK',
    user_id      BIGINT        NOT NULL COMMENT '사용자 users.id',
    type         VARCHAR(40)   NOT NULL COMMENT '알림 타입',
    enabled      TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '수신 여부(0=OFF, 1=ON)',
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
) COMMENT='사용자별 알림 수신 설정';

CREATE INDEX idx_np_user_id ON notification_preferences (user_id);
