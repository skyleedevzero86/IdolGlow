-- =============================================================================
-- V42: 토스 빌링 테이블·컬럼 주석 보강 마이에스큐엘
-- -----
-- 목적
-- - 이미 적용된 브이38부터 브이41 스키마에 컬럼 역할 주석을 부여
-- - 신규 설치는 이전 버전에 주석이 포함되어 있으나, 기존 디비 호환을 위해 재적용
-- 대상
-- - toss_billing_methods, subscriptions, charges, shedlock, reservations.membership_discount
-- =============================================================================

ALTER TABLE toss_billing_methods
    COMMENT = '글로우패스 카드 자동결제 수단. 카드번호 원문과 씨브이씨는 미저장';

ALTER TABLE toss_billing_methods
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제수단 기본키',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디. 한 사용자에 활성 수단 1개 권장',
    MODIFY COLUMN customer_key VARCHAR(100) NOT NULL COMMENT '토스 구매자 식별키. idolglow-user-회원아이디 형식',
    MODIFY COLUMN billing_key VARCHAR(1000) NOT NULL COMMENT '토스 빌링키 암호화 저장값. 평문 카드정보 대체값',
    MODIFY COLUMN billing_key_hash VARCHAR(64) NOT NULL COMMENT '빌링키 평문 해시. 동일 키 중복 등록 방지',
    MODIFY COLUMN card_company VARCHAR(50) NULL COMMENT '카드사명 표시용. 예: 현대, 국민',
    MODIFY COLUMN card_number VARCHAR(40) NULL COMMENT '마스킹된 카드번호. 예: 43301234****123*',
    MODIFY COLUMN card_type VARCHAR(30) NULL COMMENT '카드 종류. 예: 신용, 체크',
    MODIFY COLUMN owner_type VARCHAR(30) NULL COMMENT '소유 구분. 예: 개인, 법인',
    MODIFY COLUMN authenticated_at DATETIME(6) NULL COMMENT '토스 카드 인증 등록 완료 시각',
    MODIFY COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '현재 사용 중 여부. 재등록 시 기존 행은 비활성',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각';

ALTER TABLE toss_billing_subscriptions
    COMMENT = '글로우패스 월간 구독. 회원과 요금제 조합당 1건';

ALTER TABLE toss_billing_subscriptions
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '구독 기본키',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디',
    MODIFY COLUMN billing_method_id BIGINT NOT NULL COMMENT '결제에 쓸 결제수단 아이디 외래키',
    MODIFY COLUMN plan_code VARCHAR(50) NOT NULL DEFAULT 'MONTHLY' COMMENT '요금제 코드. 현재 월간 고정',
    MODIFY COLUMN order_name VARCHAR(200) NOT NULL COMMENT '토스와 영수증 주문명. 예: 아이돌글로우 월간 구독',
    MODIFY COLUMN amount BIGINT NOT NULL COMMENT '매월 청구 금액 원 단위. 기본 4900',
    MODIFY COLUMN status VARCHAR(30) NOT NULL COMMENT '상태. 활성, 일시중지, 해지예약, 해지완료',
    MODIFY COLUMN next_billing_at DATETIME(6) NULL COMMENT '다음 자동결제 예정 시각. 스케줄러 도래 판단 기준',
    MODIFY COLUMN last_charged_at DATETIME(6) NULL COMMENT '최근 결제 성공 시각',
    MODIFY COLUMN fail_count INT NOT NULL DEFAULT 0 COMMENT '연속 결제 실패 횟수. 1회는 하루 뒤, 2회는 이틀 뒤 재시도, 3회는 일시중지',
    MODIFY COLUMN retry_at DATETIME(6) NULL COMMENT '다음 재시도 시각. 비어 있으면 다음결제일 기준 도래',
    MODIFY COLUMN ends_at DATETIME(6) NULL COMMENT '해지 예약 시 혜택 유지 종료 시각. 이후 해지완료',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '행 생성 시각',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각';

ALTER TABLE toss_billing_charges
    COMMENT = '빌링 자동결제 회차별 승인 이력. 주문번호로 이중청구 방지';

ALTER TABLE toss_billing_charges
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '회차 결제 기본키',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '회원 아이디',
    MODIFY COLUMN subscription_id BIGINT NULL COMMENT '소속 구독 아이디. 단건 테스트 결제는 비울 수 있음',
    MODIFY COLUMN billing_method_id BIGINT NOT NULL COMMENT '실제 청구에 사용한 결제수단 아이디 외래키',
    MODIFY COLUMN order_id VARCHAR(200) NOT NULL COMMENT '주문번호. 구독회차는 구독아이디와 연월, 단건은 임의값',
    MODIFY COLUMN order_name VARCHAR(200) NOT NULL COMMENT '해당 회차 주문명',
    MODIFY COLUMN amount BIGINT NOT NULL COMMENT '이 회차 청구 금액 원 단위',
    MODIFY COLUMN payment_key VARCHAR(200) NULL COMMENT '토스 결제키. 승인 성공 시 저장',
    MODIFY COLUMN status VARCHAR(30) NOT NULL COMMENT '상태. 대기, 성공, 실패',
    MODIFY COLUMN raw_response TEXT NULL COMMENT '토스 에이피아이 원문 응답. 디버깅과 대사에 사용',
    MODIFY COLUMN fail_message VARCHAR(500) NULL COMMENT '실패 시 사용자와 운영 안내 메시지',
    MODIFY COLUMN approved_at DATETIME(6) NULL COMMENT '토스 승인 완료 시각',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '회차 행 생성 시각. 대기 상태로 먼저 저장하는 시점',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '행 수정 시각';

ALTER TABLE shedlock
    COMMENT = '스케줄러 분산 락';

ALTER TABLE shedlock
    MODIFY COLUMN name VARCHAR(64) NOT NULL COMMENT '잠금 이름. 예: 토스빌링자동결제',
    MODIFY COLUMN lock_until TIMESTAMP(6) NOT NULL COMMENT '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능',
    MODIFY COLUMN locked_at TIMESTAMP(6) NOT NULL COMMENT '락을 잡은 시각',
    MODIFY COLUMN locked_by VARCHAR(255) NOT NULL COMMENT '락을 잡은 호스트 또는 인스턴스 식별자';

ALTER TABLE reservations
    MODIFY COLUMN membership_discount DECIMAL(15, 2) NOT NULL DEFAULT 0
        COMMENT '글로우패스 멤버 할인액 원 단위. 비멤버는 0, 멤버는 정상가에서 실결제액을 뺀 값';
