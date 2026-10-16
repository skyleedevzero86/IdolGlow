-- =============================================================================
-- V41: 스케줄 잠금 테이블과 빌링키 해시·암호문 컬럼 마이에스큐엘
-- -----
-- 목적
-- - 스케줄러 다중 인스턴스 잠금
-- - 빌링키 암호문 길이 확장과 평문 유니크용 해시 컬럼
-- =============================================================================

-- 스케줄 잡 분산 락. 같은 이름 잡은은 한 인스턴스만 실행
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL COMMENT '잠금 이름. 예: 토스빌링자동결제',
    lock_until TIMESTAMP(6) NOT NULL COMMENT '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능',
    locked_at TIMESTAMP(6) NOT NULL COMMENT '락을 잡은 시각',
    locked_by VARCHAR(255) NOT NULL COMMENT '락을 잡은 호스트 또는 인스턴스 식별자',
    PRIMARY KEY (name)
) COMMENT='스케줄러 분산 락';

SET @drop_uk := (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE toss_billing_methods DROP INDEX uk_toss_billing_methods_billing_key',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND index_name = 'uk_toss_billing_methods_billing_key'
);
PREPARE stmt FROM @drop_uk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 암호문은 평문보다 길어서 길이를 1000으로 확장
ALTER TABLE toss_billing_methods
    MODIFY COLUMN billing_key VARCHAR(1000) NOT NULL
        COMMENT '토스 빌링키 암호화 저장값. 평문 카드정보 대체값';

SET @add_hash := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE toss_billing_methods ADD COLUMN billing_key_hash VARCHAR(64) NULL COMMENT ''빌링키 평문 해시. 암호문이 매번 달라도 동일 키 중복 등록 방지''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND column_name = 'billing_key_hash'
);
PREPARE stmt2 FROM @add_hash;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

UPDATE toss_billing_methods
SET billing_key_hash = CONCAT('legacy-', id)
WHERE billing_key_hash IS NULL;

ALTER TABLE toss_billing_methods
    MODIFY COLUMN billing_key_hash VARCHAR(64) NOT NULL
        COMMENT '빌링키 평문 해시. 암호문이 매번 달라도 동일 키 중복 등록 방지';

SET @add_uk := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE toss_billing_methods ADD CONSTRAINT uk_toss_billing_methods_billing_key_hash UNIQUE (billing_key_hash)',
        'SELECT 1'
    )
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'toss_billing_methods'
      AND constraint_name = 'uk_toss_billing_methods_billing_key_hash'
);
PREPARE stmt3 FROM @add_uk;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
