-- =============================================================================
-- V50: 공연 언어별 제목·소개
-- -----
-- 목적
-- - 글로벌 공연에 공급처·운영자 번역을 언어별로 붙인다
-- 대상
-- - 마이에스큐엘 / event_translation
-- 주의
-- - 가이드의 V49는 공연 식별자 테이블이 이미 사용한다. 이 변경은 V50
-- - MACHINE 행은 저장할 수 있으나 조회에서는 노출하지 않는다
-- - (global_id, locale)은 언어당 한 행
-- 연관
-- - EventLocalizedTextService, TourEventTranslationCollector
-- =============================================================================

-- 공연 번역. 요청 언어가 없으면 영어, 그다음 한국어로 대체한다.
CREATE TABLE IF NOT EXISTS event_translation (
    id bigint auto_increment primary key,
    global_id varchar(36) not null comment '글로벌 공연 ID',
    locale varchar(8) not null comment '언어 코드. ko, en, ja, zh',
    title varchar(255) not null comment '해당 언어 제목',
    synopsis text null comment '해당 언어 소개. 없으면 비움',
    origin varchar(20) not null comment '출처. PROVIDER, MANUAL, MACHINE. MACHINE은 노출하지 않음',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막으로 번역을 바꾼 시각',
    constraint uk_event_translation_global_locale unique (global_id, locale),
    constraint fk_event_translation_master
        foreign key (global_id) references event_master (global_id)
) comment='공연 언어별 제목·소개';
