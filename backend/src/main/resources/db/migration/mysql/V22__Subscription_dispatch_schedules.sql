-- =============================================================================
-- Flyway V22 (MySQL) - 구독 콘텐츠 자동 발송 스케줄
-- =============================================================================
-- 콘텐츠 유형(뉴스레터·웹진 등)별로 발송 주기·요일·시각을 정의한다.
-- content_type 당 스케줄은 한 행만 허용(UK). 스케줄 비활성 시 active=false.
-- 애플리케이션의 배치/스케줄러가 이 테이블을 읽어 발송 시점을 결정한다.
-- updated_at은 행 변경 시 DB가 자동 갱신한다(V16 구독 테이블과 동일 패턴).
-- =============================================================================

create table if not exists subscription_dispatch_schedules (
    id bigint not null auto_increment comment '스케줄 행 PK',
    content_type varchar(40) not null comment '콘텐츠 유형(예: NEWSLETTER, WEBZINE). 전역 유일',
    frequency_type varchar(20) not null comment '반복 주기 유형(예: DAILY, WEEKLY, MONTHLY)',
    day_of_week varchar(20) null comment '주간 등 주기에 따른 요일(애플리케이션 규약 문자열). 해당 없으면 NULL',
    dispatch_hour int not null comment '발송 시각(시, 0–23)',
    dispatch_minute int not null comment '발송 시각(분, 0–59)',
    active boolean not null default true comment '스케줄 사용 여부(배치에서 false 행은 건너뜀)',
    created_at timestamp not null default current_timestamp comment '행 생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    primary key (id),
    constraint uk_subscription_dispatch_schedule_content_type unique (content_type)
) comment='구독 콘텐츠 자동 발송 스케줄(유형별 주기·시각)';

create index idx_subscription_dispatch_schedules_active on subscription_dispatch_schedules (active);
