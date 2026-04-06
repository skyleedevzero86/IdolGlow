-- =============================================================================
-- Flyway V16 (MySQL) - 구독자 및 콘텐츠 발송 이력 테이블
-- =============================================================================
-- 이메일 구독 상태(동의/활성 여부)와 뉴스레터·웹진 발송 결과를 추적한다.
-- updated_at은 ON UPDATE CURRENT_TIMESTAMP로 DB에서 자동 갱신한다.
-- =============================================================================

create table if not exists email_subscriptions (
    id bigint auto_increment primary key,
    email varchar(255) not null comment '구독자 이메일(전역 유일)',
    subscribed_newsletters boolean not null default true comment '뉴스레터 수신 여부',
    subscribed_issues boolean not null default true comment '웹진/이슈 수신 여부',
    consented_at timestamp not null comment '수신 동의 시각',
    subscribed_at timestamp not null comment '구독 등록 시각',
    subscription_source varchar(50) not null comment '구독 유입 채널',
    active boolean not null default true comment '활성 구독 상태',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_email_subscriptions_email unique (email)
) comment='구독자 마스터 테이블(수신 동의/활성 상태 포함)';

create table if not exists subscription_dispatch_histories (
    id bigint auto_increment primary key,
    content_type varchar(40) not null comment '콘텐츠 유형(예: NEWSLETTER, WEBZINE)',
    content_slug varchar(160) not null comment '콘텐츠 고유 slug/키',
    content_title varchar(255) not null comment '발송 당시 제목 스냅샷',
    content_summary varchar(1000) comment '발송 당시 요약 스냅샷',
    dispatch_channel varchar(20) not null comment '발송 채널(예: EMAIL)',
    dispatch_status varchar(20) not null comment '발송 상태(예: SENT, FAILED)',
    recipient_count bigint not null comment '발송 대상자 수',
    content_created_at timestamp null comment '콘텐츠 원본 생성 시각',
    dispatched_at timestamp not null comment '실제 발송 시각',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_subscription_dispatch_content unique (content_type, content_slug, dispatch_channel)
) comment='콘텐츠 발송 이력 테이블';

create index idx_email_subscriptions_active on email_subscriptions(active);
create index idx_subscription_dispatch_histories_dispatched_at on subscription_dispatch_histories(dispatched_at);

