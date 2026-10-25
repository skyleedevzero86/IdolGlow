-- =============================================================================
-- V49: 공연 메타 글로벌 식별자
-- -----
-- 목적
-- - 공급처 공연을 배치로 모아 같은 공연은 global_id 하나로 묶는다
-- 대상
-- - 마이에스큐엘 / event_master, event_source_ref, event_collect_log
-- 주의
-- - 제목만 같으면 확정하지 않는다. 기간이 겹치고 장소까지 같을 때만 CONFIRMED
-- - (source, source_id)는 공급처 원본 한 건이다
-- 연관
-- - EventMetaCollectService, EventMetaCollectScheduler
-- =============================================================================

-- 대표 공연. 공급처 표기가 달라도 같은 공연이면 이 행을 같이 가리킨다.
CREATE TABLE IF NOT EXISTS event_master (
    id bigint auto_increment primary key,
    global_id varchar(36) not null comment '글로벌 공연 ID. UUID',
    title varchar(255) not null comment '대표 제목',
    normalized_title varchar(255) not null comment '매칭용 제목. 괄호·공백·기호를 뺀 값',
    start_date date null comment '대표 시작일',
    end_date date null comment '대표 종료일',
    venue varchar(255) null comment '대표 장소',
    normalized_venue varchar(255) null comment '매칭용 장소',
    status varchar(20) not null comment '대표 상태. ACTIVE',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_event_master_global_id unique (global_id)
) comment='공연 대표 식별자. 공급처가 달라도 같은 공연은 한 행';

CREATE INDEX idx_event_master_normalized_title
    ON event_master (normalized_title);

-- 공급처 원본 한 건. 확정이 아니면 global_id를 비워 자동 병합하지 않는다.
CREATE TABLE IF NOT EXISTS event_source_ref (
    id bigint auto_increment primary key,
    source varchar(80) not null comment '공급처 코드. KOPIS_PBLPRFR, SEOUL_SJW_PERFORM 등',
    source_id varchar(80) not null comment '공급처 공연 ID',
    global_id varchar(36) null comment '매칭된 글로벌 공연 ID. CONFLICT면 비움',
    match_status varchar(20) not null comment '매칭 상태. CONFIRMED, CANDIDATE, CONFLICT',
    match_score int not null comment '매칭 점수. 100이면 제목·기간·장소 일치',
    matched_at datetime(6) null comment '마지막으로 매칭한 시각',
    raw_hash varchar(64) not null comment '공급처 원본 해시. 바뀌면 다시 매칭',
    raw_payload text null comment '공급처에서 받은 원본 스냅샷',
    title varchar(255) not null comment '공급처 제목',
    start_date date null comment '공급처 시작일',
    end_date date null comment '공급처 종료일',
    venue varchar(255) null comment '공급처 장소',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_event_source_ref_source unique (source, source_id),
    constraint fk_event_source_ref_master
        foreign key (global_id) references event_master (global_id)
) comment='공급처 공연 원본과 글로벌 식별자 매칭';

CREATE INDEX idx_event_source_ref_global_id
    ON event_source_ref (global_id);

-- 공급처별 수집 결과. 한 곳이 실패해도 다른 곳 저장은 남긴다.
CREATE TABLE IF NOT EXISTS event_collect_log (
    id bigint auto_increment primary key,
    source varchar(80) not null comment '공급처 코드',
    status varchar(20) not null comment '수집 상태. SUCCESS, FAILED',
    item_count int not null comment '이번 수집에서 저장을 시도한 건수',
    error_message varchar(1000) null comment '실패 원인. 1000자 이내',
    collected_at datetime(6) not null comment '수집 시각',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각'
) comment='공연 공급처 수집 결과';
