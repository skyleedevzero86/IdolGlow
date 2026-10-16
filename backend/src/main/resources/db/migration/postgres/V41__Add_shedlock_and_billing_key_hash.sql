-- =============================================================================
-- V41: 스케줄 잠금 테이블과 빌링키 해시·암호문 컬럼 포스트그레스
-- -----
-- 목적
-- - 스케줄러 다중 인스턴스 잠금
-- - 빌링키 암호문 길이 확장과 평문 유니크용 해시 컬럼
-- =============================================================================

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(6) NOT NULL,
    locked_at TIMESTAMP(6) NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

COMMENT ON TABLE shedlock IS '스케줄러 분산 락';
COMMENT ON COLUMN shedlock.name IS '잠금 이름. 예: 토스빌링자동결제';
COMMENT ON COLUMN shedlock.lock_until IS '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능';
COMMENT ON COLUMN shedlock.locked_at IS '락을 잡은 시각';
COMMENT ON COLUMN shedlock.locked_by IS '락을 잡은 호스트 또는 인스턴스 식별자';

ALTER TABLE toss_billing_methods
    DROP CONSTRAINT IF EXISTS uk_toss_billing_methods_billing_key;

ALTER TABLE toss_billing_methods
    ALTER COLUMN billing_key TYPE VARCHAR(1000);

ALTER TABLE toss_billing_methods
    ADD COLUMN IF NOT EXISTS billing_key_hash VARCHAR(64);

UPDATE toss_billing_methods
SET billing_key_hash = 'legacy-' || id::text
WHERE billing_key_hash IS NULL;

ALTER TABLE toss_billing_methods
    ALTER COLUMN billing_key_hash SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_toss_billing_methods_billing_key_hash'
    ) THEN
        ALTER TABLE toss_billing_methods
            ADD CONSTRAINT uk_toss_billing_methods_billing_key_hash UNIQUE (billing_key_hash);
    END IF;
END $$;

COMMENT ON COLUMN toss_billing_methods.billing_key IS '토스 빌링키 암호화 저장값. 평문 카드정보 대체값';
COMMENT ON COLUMN toss_billing_methods.billing_key_hash IS '빌링키 평문 해시. 암호문이 매번 달라도 동일 키 중복 등록 방지';
