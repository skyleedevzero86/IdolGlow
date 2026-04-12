-- =============================================================================
-- V17: 사이트 도메인, 배너, 팝업, 메인이미지
-- - PostgreSQL V18 과 동일한 논리 스키마 
-- - 시드는 INSERT IGNORE 로 기존 PK 가 있으면 건너뜀
-- =============================================================================

-- -----------------------------------------------------------------------------
-- tb_domain_list: 멀티 사이트구분용 도메인 마스터
-- -----------------------------------------------------------------------------
create table if not exists tb_domain_list (
    domain_id varchar(20) primary key comment '도메인 PK (예: kr, en)',
    domain_nm varchar(100) comment '표시명',
    domain_path varchar(200) comment 'URL 경로 프리픽스 등',
    domain_dc varchar(500) comment '설명',
    use_at char(1) default 'Y' comment '사용 여부 Y/N',
    actvty_at char(1) default 'Y' comment '활성 여부 Y/N',
    sort_seq int default 0 comment '정렬 순서'
) engine=InnoDB default charset=utf8mb4 comment='사이트·언어 도메인 목록(배너/팝업/메인이미지 공통 참조)';

-- -----------------------------------------------------------------------------
-- tb_banner: 상단·슬롯 등 배너 콘텐츠
-- -----------------------------------------------------------------------------
create table if not exists tb_banner (
    banner_id varchar(20) primary key comment '배너 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    banner_nm varchar(200) comment '배너명',
    link_url varchar(500) comment '클릭 시 이동 URL',
    banner_image varchar(500) comment '이미지 경로/URL',
    banner_image_file varchar(500) comment '원본 파일명 등',
    banner_dc varchar(1000) comment '설명',
    sort_ordr int default 0 comment '노출 정렬 순서',
    reflct_at char(1) default 'Y' comment '게시(반영) 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_banner_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='배너 관리';

-- -----------------------------------------------------------------------------
-- tb_popup_manage: 기간·위치·크기 속성을 가진 팝업
-- -----------------------------------------------------------------------------
create table if not exists tb_popup_manage (
    popup_id varchar(20) primary key comment '팝업 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    popup_sj_nm varchar(200) comment '팝업 제목',
    file_url varchar(500) comment '링크 URL',
    link_target varchar(20) default '_blank' comment 'a 태그 target',
    popup_img_path varchar(500) comment '팝업 이미지 경로',
    popup_file_nm varchar(200) comment '이미지 파일명',
    popup_vrticl_lc int default 0 comment '세로 위치',
    popup_width_lc int default 0 comment '가로 위치',
    popup_vrticl_size int default 400 comment '세로 크기(px)',
    popup_width_size int default 500 comment '가로 크기(px)',
    ntce_bgnde varchar(20) comment '게시 시작(레거시 문자열)',
    ntce_endde varchar(20) comment '게시 종료',
    stopvew_setup_at char(1) default 'Y' comment '다시보지 않기 설정 Y/N',
    ntce_at char(1) default 'Y' comment '게시(알림) 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_popup_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='팝업 관리';

-- -----------------------------------------------------------------------------
-- tb_main_image: 메인 영역 슬라이드/히어로 이미지
-- -----------------------------------------------------------------------------
create table if not exists tb_main_image (
    image_id varchar(20) primary key comment '이미지 PK',
    domain_id varchar(20) default 'kr' comment 'tb_domain_list FK',
    image_nm varchar(200) comment '이미지 제목',
    image varchar(500) comment '이미지 경로/URL (컬럼명은 레거시)',
    image_file varchar(500) comment '파일명',
    image_dc varchar(1000) comment '설명',
    reflct_at char(1) default 'Y' comment '게시(반영) 여부 Y/N',
    frst_register_id varchar(50) comment '최초 등록자',
    frst_regist_pnttm timestamp default current_timestamp comment '최초 등록 일시',
    last_updusr_id varchar(50) comment '최종 수정자',
    last_updt_pnttm timestamp null comment '최종 수정 일시',
    constraint fk_tb_main_image_domain foreign key (domain_id) references tb_domain_list (domain_id)
) engine=InnoDB default charset=utf8mb4 comment='메인 이미지 관리';

-- -----------------------------------------------------------------------------
-- 시드: 도메인 + 샘플 데이터 (PK 충돌 시 무시)
-- -----------------------------------------------------------------------------
insert ignore into tb_domain_list (domain_id, domain_nm, domain_path, domain_dc, use_at, sort_seq) values
('kr', '한국어', '/kr', '한국어 메인사이트', 'Y', 1),
('en', 'English', '/en', 'English site', 'Y', 2),
('tour', '관광', '/tour', '관광 사이트', 'Y', 3);

insert ignore into tb_banner (banner_id, domain_id, banner_nm, link_url, banner_dc, sort_ordr, reflct_at, frst_register_id) values
('BNR_001', 'kr', '샘플 배너 1', 'https://example.com', '샘플 배너입니다', 1, 'Y', 'admin'),
('BNR_002', 'kr', '샘플 배너 2', 'https://example.com/2', '두번째 배너', 2, 'Y', 'admin'),
('BNR_003', 'en', 'English Banner', 'https://example.com/en', 'English site banner', 1, 'Y', 'admin');

insert ignore into tb_popup_manage (popup_id, domain_id, popup_sj_nm, file_url, link_target, ntce_bgnde, ntce_endde, ntce_at, stopvew_setup_at, frst_register_id) values
('POP_001', 'kr', '안내 팝업', 'https://example.com/popup', '_blank', '202603010000', '202612312359', 'Y', 'Y', 'admin'),
('POP_002', 'kr', '이벤트 팝업', 'https://example.com/event-popup', '_self', '202606010000', '202608312359', 'Y', 'N', 'admin');

insert ignore into tb_main_image (image_id, domain_id, image_nm, image_dc, reflct_at, frst_register_id) values
('IMG_001', 'kr', '메인 이미지 1', '메인 슬라이드 이미지', 'Y', 'admin'),
('IMG_002', 'kr', '메인 이미지 2', '서브 슬라이드 이미지', 'Y', 'admin');
