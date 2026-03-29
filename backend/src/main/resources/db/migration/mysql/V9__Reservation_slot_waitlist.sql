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
