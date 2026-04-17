-- =============================================================================
-- Flyway V17 
-- =============================================================================
-- 이전 버전: V16__Subscriptions.sql
-- 내용: 사이트 도메인(tb_domain_list), 배너(tb_banner), 팝업(tb_popup_manage),
--       메인이미지(tb_main_image). 논리 스키마는 postgres/V18, mysql/V17 과 동일.
--
-- 사용처: Flyway locations 가 db/migration 만 가리키는 경우(예: 통합 테스트,
--         H2 + MODE=PostgreSQL 로 루트 스크립트만 적용하는 구성).
-- application-local.yml 이 classpath:db/migration/postgres 를 쓰면 이 파일은
-- 적용되지 않으며, 그때는 postgres/V18__Site_domain_banner_popup_main_image.sql
-- 가 실행된다.
--
-- 시드: H2 호환을 위해 ON CONFLICT 대신 INSERT ... SELECT ... WHERE NOT EXISTS
-- 로 동일 PK 가 있으면 건너뜀.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- tb_domain_list: 멀티 사이트 구분용 도메인 마스터
-- -----------------------------------------------------------------------------
create table if not exists tb_domain_list (
    domain_id varchar(20) primary key,
    domain_nm varchar(100),
    domain_path varchar(200),
    domain_dc varchar(500),
    use_at char(1) default 'Y',
    actvty_at char(1) default 'Y',
    sort_seq integer default 0
);

comment on table tb_domain_list is '사이트·언어 도메인 목록(배너/팝업/메인이미지 공통 참조)';
comment on column tb_domain_list.domain_id is '도메인 PK (예: kr, en)';
comment on column tb_domain_list.domain_nm is '표시명';
comment on column tb_domain_list.domain_path is 'URL 경로 프리픽스 등';
comment on column tb_domain_list.domain_dc is '설명';
comment on column tb_domain_list.use_at is '사용 여부 Y/N';
comment on column tb_domain_list.actvty_at is '활성 여부 Y/N';
comment on column tb_domain_list.sort_seq is '정렬 순서';

-- -----------------------------------------------------------------------------
-- tb_banner: 상단·슬롯 등 배너 콘텐츠
-- -----------------------------------------------------------------------------
create table if not exists tb_banner (
    banner_id varchar(20) primary key,
    domain_id varchar(20) default 'kr',
    banner_nm varchar(200),
    link_url varchar(500),
    banner_image varchar(500),
    banner_image_file varchar(500),
    banner_dc varchar(1000),
    sort_ordr integer default 0,
    reflct_at char(1) default 'Y',
    frst_register_id varchar(50),
    frst_regist_pnttm timestamp default current_timestamp,
    last_updusr_id varchar(50),
    last_updt_pnttm timestamp,
    constraint fk_tb_banner_domain foreign key (domain_id) references tb_domain_list (domain_id)
);

comment on table tb_banner is '배너 관리';
comment on column tb_banner.banner_id is '배너 PK (애플리케이션에서 BNR_ 접두 + 타임스탬프 등)';
comment on column tb_banner.domain_id is 'tb_domain_list 참조';
comment on column tb_banner.banner_nm is '배너명';
comment on column tb_banner.link_url is '클릭 시 이동 URL';
comment on column tb_banner.banner_image is '이미지 경로/URL';
comment on column tb_banner.banner_image_file is '원본 파일명 등';
comment on column tb_banner.banner_dc is '설명';
comment on column tb_banner.sort_ordr is '노출 정렬 순서';
comment on column tb_banner.reflct_at is '게시(반영) 여부 Y/N';
comment on column tb_banner.frst_register_id is '최초 등록자';
comment on column tb_banner.frst_regist_pnttm is '최초 등록 일시';
comment on column tb_banner.last_updusr_id is '최종 수정자';
comment on column tb_banner.last_updt_pnttm is '최종 수정 일시';

-- -----------------------------------------------------------------------------
-- tb_popup_manage: 기간·위치 속성을 가진 팝업
-- -----------------------------------------------------------------------------
create table if not exists tb_popup_manage (
    popup_id varchar(20) primary key,
    domain_id varchar(20) default 'kr',
    popup_sj_nm varchar(200),
    file_url varchar(500),
    link_target varchar(20) default '_blank',
    popup_img_path varchar(500),
    popup_file_nm varchar(200),
    popup_vrticl_lc integer default 0,
    popup_width_lc integer default 0,
    popup_vrticl_size integer default 400,
    popup_width_size integer default 500,
    ntce_bgnde varchar(20),
    ntce_endde varchar(20),
    stopvew_setup_at char(1) default 'Y',
    ntce_at char(1) default 'Y',
    frst_register_id varchar(50),
    frst_regist_pnttm timestamp default current_timestamp,
    last_updusr_id varchar(50),
    last_updt_pnttm timestamp,
    constraint fk_tb_popup_domain foreign key (domain_id) references tb_domain_list (domain_id)
);

comment on table tb_popup_manage is '팝업 관리';
comment on column tb_popup_manage.popup_id is '팝업 PK';
comment on column tb_popup_manage.domain_id is 'tb_domain_list 참조';
comment on column tb_popup_manage.popup_sj_nm is '팝업 제목';
comment on column tb_popup_manage.file_url is '링크 URL';
comment on column tb_popup_manage.link_target is 'a 태그 target (_blank, _self 등)';
comment on column tb_popup_manage.popup_img_path is '팝업 이미지 경로';
comment on column tb_popup_manage.popup_file_nm is '이미지 파일명';
comment on column tb_popup_manage.popup_vrticl_lc is '세로 위치';
comment on column tb_popup_manage.popup_width_lc is '가로 위치';
comment on column tb_popup_manage.popup_vrticl_size is '세로 크기(px)';
comment on column tb_popup_manage.popup_width_size is '가로 크기(px)';
comment on column tb_popup_manage.ntce_bgnde is '게시 시작일시(문자열 형식, 레거시 호환)';
comment on column tb_popup_manage.ntce_endde is '게시 종료일시';
comment on column tb_popup_manage.stopvew_setup_at is '다시보지 않기 설정 사용 여부 Y/N';
comment on column tb_popup_manage.ntce_at is '게시(알림) 여부 Y/N';
comment on column tb_popup_manage.frst_register_id is '최초 등록자';
comment on column tb_popup_manage.frst_regist_pnttm is '최초 등록 일시';
comment on column tb_popup_manage.last_updusr_id is '최종 수정자';
comment on column tb_popup_manage.last_updt_pnttm is '최종 수정 일시';

-- -----------------------------------------------------------------------------
-- tb_main_image: 메인 영역 슬라이드/히어로 이미지
-- -----------------------------------------------------------------------------
create table if not exists tb_main_image (
    image_id varchar(20) primary key,
    domain_id varchar(20) default 'kr',
    image_nm varchar(200),
    image varchar(500),
    image_file varchar(500),
    image_dc varchar(1000),
    reflct_at char(1) default 'Y',
    frst_register_id varchar(50),
    frst_regist_pnttm timestamp default current_timestamp,
    last_updusr_id varchar(50),
    last_updt_pnttm timestamp,
    constraint fk_tb_main_image_domain foreign key (domain_id) references tb_domain_list (domain_id)
);

comment on table tb_main_image is '메인 이미지 관리';
comment on column tb_main_image.image_id is '이미지 PK (애플리케이션에서 IMG_ 접두 등)';
comment on column tb_main_image.domain_id is 'tb_domain_list 참조';
comment on column tb_main_image.image_nm is '이미지 제목';
comment on column tb_main_image.image is '이미지 경로/URL (컬럼명 image 는 레거시 스키마 유지)';
comment on column tb_main_image.image_file is '파일명';
comment on column tb_main_image.image_dc is '설명';
comment on column tb_main_image.reflct_at is '게시(반영) 여부 Y/N';
comment on column tb_main_image.frst_register_id is '최초 등록자';
comment on column tb_main_image.frst_regist_pnttm is '최초 등록 일시';
comment on column tb_main_image.last_updusr_id is '최종 수정자';
comment on column tb_main_image.last_updt_pnttm is '최종 수정 일시';

-- -----------------------------------------------------------------------------
-- 시드 (멱등: 동일 PK 가 이미 있으면 삽입 생략)
-- -----------------------------------------------------------------------------
insert into tb_domain_list (domain_id, domain_nm, domain_path, domain_dc, use_at, sort_seq)
select 'kr', '한국어', '/kr', '한국어 메인사이트', 'Y', 1
where not exists (select 1 from tb_domain_list where domain_id = 'kr');

insert into tb_domain_list (domain_id, domain_nm, domain_path, domain_dc, use_at, sort_seq)
select 'en', 'English', '/en', 'English site', 'Y', 2
where not exists (select 1 from tb_domain_list where domain_id = 'en');

insert into tb_domain_list (domain_id, domain_nm, domain_path, domain_dc, use_at, sort_seq)
select 'tour', '관광', '/tour', '관광 사이트', 'Y', 3
where not exists (select 1 from tb_domain_list where domain_id = 'tour');

insert into tb_banner (banner_id, domain_id, banner_nm, link_url, banner_dc, sort_ordr, reflct_at, frst_register_id)
select 'BNR_001', 'kr', '샘플 배너 1', 'https://example.com', '샘플 배너입니다', 1, 'Y', 'admin'
where not exists (select 1 from tb_banner where banner_id = 'BNR_001');

insert into tb_banner (banner_id, domain_id, banner_nm, link_url, banner_dc, sort_ordr, reflct_at, frst_register_id)
select 'BNR_002', 'kr', '샘플 배너 2', 'https://example.com/2', '두번째 배너', 2, 'Y', 'admin'
where not exists (select 1 from tb_banner where banner_id = 'BNR_002');

insert into tb_banner (banner_id, domain_id, banner_nm, link_url, banner_dc, sort_ordr, reflct_at, frst_register_id)
select 'BNR_003', 'en', 'English Banner', 'https://example.com/en', 'English site banner', 1, 'Y', 'admin'
where not exists (select 1 from tb_banner where banner_id = 'BNR_003');

insert into tb_popup_manage (popup_id, domain_id, popup_sj_nm, file_url, link_target, ntce_bgnde, ntce_endde, ntce_at, stopvew_setup_at, frst_register_id)
select 'POP_001', 'kr', '안내 팝업', 'https://example.com/popup', '_blank', '202603010000', '202612312359', 'Y', 'Y', 'admin'
where not exists (select 1 from tb_popup_manage where popup_id = 'POP_001');

insert into tb_popup_manage (popup_id, domain_id, popup_sj_nm, file_url, link_target, ntce_bgnde, ntce_endde, ntce_at, stopvew_setup_at, frst_register_id)
select 'POP_002', 'kr', '이벤트 팝업', 'https://example.com/event-popup', '_self', '202606010000', '202608312359', 'Y', 'N', 'admin'
where not exists (select 1 from tb_popup_manage where popup_id = 'POP_002');

insert into tb_main_image (image_id, domain_id, image_nm, image_dc, reflct_at, frst_register_id)
select 'IMG_001', 'kr', '메인 이미지 1', '메인 슬라이드 이미지', 'Y', 'admin'
where not exists (select 1 from tb_main_image where image_id = 'IMG_001');

insert into tb_main_image (image_id, domain_id, image_nm, image_dc, reflct_at, frst_register_id)
select 'IMG_002', 'kr', '메인 이미지 2', '서브 슬라이드 이미지', 'Y', 'admin'
where not exists (select 1 from tb_main_image where image_id = 'IMG_002');
