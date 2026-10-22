-- =============================================================================
-- V45: 토스 빌링 구매자 키
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
