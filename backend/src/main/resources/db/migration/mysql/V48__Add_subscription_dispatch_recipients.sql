-- =============================================================================
-- V48: 구독 발송 선점과 수신자별 결과
-- -----
-- 목적
-- - 발송 전에 SENDING으로 이력을 선점하고, 수신자별 성공·실패를 남긴다
-- 대상
-- - 마이에스큐엘 / subscription_dispatch_histories, subscription_dispatch_recipients
-- 주의
-- - 기존 이력의 성공·실패 수는 0이다. 발송 상태 값 길이는 그대로다
-- - PARTIAL_FAILED(14), SENDING(7)은 dispatch_status varchar(20)에 들어간다
-- 연관
-- - SubscriptionDispatchMailService, SubscriptionDispatchTxService
-- =============================================================================

-- 발송 중 선점 시각과 성공·실패 수를 이력에 남긴다. 기존 행은 0이다.
ALTER TABLE subscription_dispatch_histories
    ADD COLUMN success_count BIGINT NOT NULL DEFAULT 0 COMMENT '수신자별 발송 성공 수',
    ADD COLUMN failure_count BIGINT NOT NULL DEFAULT 0 COMMENT '수신자별 발송 실패 수',
    ADD COLUMN last_attempt_at DATETIME(6) NULL COMMENT '마지막으로 발송을 시도한 시각. 오래되면 안 보낸 수신자만 이어서 보낸다';

ALTER TABLE subscription_dispatch_histories
    MODIFY COLUMN dispatch_status VARCHAR(20) NOT NULL COMMENT '발송 상태. RECORDED, SENDING, SENT, PARTIAL_FAILED, FAILED';

CREATE INDEX idx_subscription_dispatch_histories_sending
    ON subscription_dispatch_histories (dispatch_status, last_attempt_at);

-- 수신자 한 명당 한 행. 성공한 주소는 재개 때 다시 보내지 않는다.
CREATE TABLE IF NOT EXISTS subscription_dispatch_recipients (
    id bigint auto_increment primary key,
    dispatch_id bigint not null comment '구독 발송 이력 ID',
    email varchar(255) not null comment '수신 이메일',
    status varchar(20) not null comment '수신자 상태. SENT, FAILED',
    sent_at datetime(6) null comment '발송에 성공한 시각',
    error varchar(1000) null comment '실패 원인. 1000자 이내',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_subscription_dispatch_recipient unique (dispatch_id, email),
    constraint fk_subscription_dispatch_recipient_history
        foreign key (dispatch_id) references subscription_dispatch_histories (id)
) comment='구독 발송 수신자별 결과';

CREATE INDEX idx_subscription_dispatch_recipients_dispatch
    ON subscription_dispatch_recipients (dispatch_id);
