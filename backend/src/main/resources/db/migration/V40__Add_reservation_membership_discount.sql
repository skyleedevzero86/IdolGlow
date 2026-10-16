-- =============================================================================
-- V40: 예약 글로우패스 멤버십 할인 금액
-- -----
-- 목적
-- - 정기결제 글로우패스 활성 회원의 예약 10퍼센트 할인액을 기록
-- - 환불 시 할인 반영 금액 추적
-- 대상
-- - reservations.membership_discount
-- 연관
-- - ReservationCommandService, MembershipQuery
-- =============================================================================

ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS membership_discount NUMERIC(15, 2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN reservations.membership_discount IS '글로우패스 멤버 할인액 원 단위. 비멤버는 0, 멤버는 정상가에서 실결제액을 뺀 값';
