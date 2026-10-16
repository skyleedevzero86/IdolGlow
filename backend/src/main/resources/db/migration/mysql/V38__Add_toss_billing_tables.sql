-- =============================================================================
-- V38: 토스 빌링 글로우패스 핵심 테이블
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

CREATE INDEX idx_toss_billing_methods_user_id
    ON toss_billing_methods (user_id);

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

CREATE INDEX idx_toss_billing_subscriptions_next
    ON toss_billing_subscriptions (status, next_billing_at);

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

CREATE INDEX idx_toss_billing_charges_user_id
    ON toss_billing_charges (user_id);
