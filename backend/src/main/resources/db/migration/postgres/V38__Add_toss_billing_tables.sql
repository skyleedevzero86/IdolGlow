-- =============================================================================
-- V38: 토스 빌링 글로우패스 핵심 테이블
-- -----
-- 목적
-- - 카드 자동결제용 결제수단·구독·회차 결제 이력 저장
-- - 일반결제와 분리된 정기결제 원장
-- 대상
-- - 포스트그레스 / toss_billing_methods, toss_billing_subscriptions, toss_billing_charges
-- =============================================================================

CREATE TABLE IF NOT EXISTS toss_billing_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    customer_key VARCHAR(100) NOT NULL,
    billing_key VARCHAR(300) NOT NULL,
    card_company VARCHAR(50),
    card_number VARCHAR(40),
    card_type VARCHAR(30),
    owner_type VARCHAR(30),
    authenticated_at TIMESTAMP(6),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_toss_billing_methods_billing_key UNIQUE (billing_key)
);

CREATE INDEX IF NOT EXISTS idx_toss_billing_methods_user_id
    ON toss_billing_methods (user_id);

COMMENT ON TABLE toss_billing_methods IS '글로우패스 카드 자동결제 수단. 카드번호 원문과 씨브이씨는 미저장';
COMMENT ON COLUMN toss_billing_methods.id IS '결제수단 기본키';
COMMENT ON COLUMN toss_billing_methods.user_id IS '회원 아이디. 한 사용자에 활성 수단 1개 권장';
COMMENT ON COLUMN toss_billing_methods.customer_key IS '토스 구매자 식별키. idolglow-user-회원아이디 형식';
COMMENT ON COLUMN toss_billing_methods.billing_key IS '토스 빌링키. 카드 대신 자동결제에 쓰는 값. 이후 암호화하여 저장';
COMMENT ON COLUMN toss_billing_methods.card_company IS '카드사명 표시용. 예: 현대, 국민';
COMMENT ON COLUMN toss_billing_methods.card_number IS '마스킹된 카드번호. 예: 43301234****123*';
COMMENT ON COLUMN toss_billing_methods.card_type IS '카드 종류. 예: 신용, 체크';
COMMENT ON COLUMN toss_billing_methods.owner_type IS '소유 구분. 예: 개인, 법인';
COMMENT ON COLUMN toss_billing_methods.authenticated_at IS '토스 카드 인증 등록 완료 시각';
COMMENT ON COLUMN toss_billing_methods.active IS '현재 사용 중 여부. 재등록 시 기존 행은 비활성';
COMMENT ON COLUMN toss_billing_methods.created_at IS '행 생성 시각';
COMMENT ON COLUMN toss_billing_methods.updated_at IS '행 수정 시각';

CREATE TABLE IF NOT EXISTS toss_billing_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    billing_method_id BIGINT NOT NULL,
    plan_code VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    order_name VARCHAR(200) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    next_billing_at TIMESTAMP(6),
    last_charged_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_toss_billing_subscriptions_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id),
    CONSTRAINT uk_toss_billing_subscriptions_user_plan UNIQUE (user_id, plan_code)
);

CREATE INDEX IF NOT EXISTS idx_toss_billing_subscriptions_next
    ON toss_billing_subscriptions (status, next_billing_at);

COMMENT ON TABLE toss_billing_subscriptions IS '글로우패스 월간 구독. 회원과 요금제 조합당 1건';
COMMENT ON COLUMN toss_billing_subscriptions.id IS '구독 기본키';
COMMENT ON COLUMN toss_billing_subscriptions.user_id IS '회원 아이디';
COMMENT ON COLUMN toss_billing_subscriptions.billing_method_id IS '결제에 쓸 결제수단 아이디 외래키';
COMMENT ON COLUMN toss_billing_subscriptions.plan_code IS '요금제 코드. 현재 월간 고정';
COMMENT ON COLUMN toss_billing_subscriptions.order_name IS '토스와 영수증 주문명. 예: 아이돌글로우 월간 구독';
COMMENT ON COLUMN toss_billing_subscriptions.amount IS '매월 청구 금액 원 단위. 기본 4900';
COMMENT ON COLUMN toss_billing_subscriptions.status IS '상태. 활성, 일시중지, 해지예약, 해지완료';
COMMENT ON COLUMN toss_billing_subscriptions.next_billing_at IS '다음 자동결제 예정 시각. 스케줄러 도래 판단 기준';
COMMENT ON COLUMN toss_billing_subscriptions.last_charged_at IS '최근 결제 성공 시각';
COMMENT ON COLUMN toss_billing_subscriptions.created_at IS '행 생성 시각';
COMMENT ON COLUMN toss_billing_subscriptions.updated_at IS '행 수정 시각';

CREATE TABLE IF NOT EXISTS toss_billing_charges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT,
    billing_method_id BIGINT NOT NULL,
    order_id VARCHAR(200) NOT NULL,
    order_name VARCHAR(200) NOT NULL,
    amount BIGINT NOT NULL,
    payment_key VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    raw_response TEXT,
    fail_message VARCHAR(500),
    approved_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_toss_billing_charges_order_id UNIQUE (order_id),
    CONSTRAINT fk_toss_billing_charges_method
        FOREIGN KEY (billing_method_id) REFERENCES toss_billing_methods (id)
);

CREATE INDEX IF NOT EXISTS idx_toss_billing_charges_user_id
    ON toss_billing_charges (user_id);

COMMENT ON TABLE toss_billing_charges IS '빌링 자동결제 회차별 승인 이력. 주문번호로 이중청구 방지';
COMMENT ON COLUMN toss_billing_charges.id IS '회차 결제 기본키';
COMMENT ON COLUMN toss_billing_charges.user_id IS '회원 아이디';
COMMENT ON COLUMN toss_billing_charges.subscription_id IS '소속 구독 아이디. 단건 테스트 결제는 비울 수 있음';
COMMENT ON COLUMN toss_billing_charges.billing_method_id IS '실제 청구에 사용한 결제수단 아이디 외래키';
COMMENT ON COLUMN toss_billing_charges.order_id IS '주문번호. 구독회차는 구독아이디와 연월, 단건은 임의값';
COMMENT ON COLUMN toss_billing_charges.order_name IS '해당 회차 주문명';
COMMENT ON COLUMN toss_billing_charges.amount IS '이 회차 청구 금액 원 단위';
COMMENT ON COLUMN toss_billing_charges.payment_key IS '토스 결제키. 승인 성공 시 저장';
COMMENT ON COLUMN toss_billing_charges.status IS '상태. 대기, 성공, 실패';
COMMENT ON COLUMN toss_billing_charges.raw_response IS '토스 에이피아이 원문 응답. 디버깅과 대사에 사용';
COMMENT ON COLUMN toss_billing_charges.fail_message IS '실패 시 사용자와 운영 안내 메시지';
COMMENT ON COLUMN toss_billing_charges.approved_at IS '토스 승인 완료 시각';
COMMENT ON COLUMN toss_billing_charges.created_at IS '회차 행 생성 시각. 대기 상태로 먼저 저장하는 시점';
COMMENT ON COLUMN toss_billing_charges.updated_at IS '행 수정 시각';
