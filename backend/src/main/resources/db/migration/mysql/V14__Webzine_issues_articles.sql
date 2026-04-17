-- =============================================================================
-- Flyway V14 (MySQL) — 웹진(Webzine) 이슈(호)·기사·본문 섹션·갤러리·태그
-- =============================================================================
-- PK: BIGINT AUTO_INCREMENT. FK는 별도 CONSTRAINT로 선언(순서: 자식 테이블 정의 후).
-- updated_at: ON UPDATE CURRENT_TIMESTAMP — DB에서 갱신 시각 자동 반영(앱과 이중 갱신 시 정책 통일 권장).
-- =============================================================================

-- 웹진 호(이슈)
create table if not exists webzine_issues (
    id bigint not null auto_increment primary key comment '이슈 PK',
    slug varchar(80) not null comment '공개 URL용 슬러그',
    volume int not null comment '권호(정수, 전역 유일)',
    issue_date date not null comment '발행일',
    cover_image_url varchar(500) not null comment '표지 이미지 URL',
    teaser varchar(1000) not null comment '목록/공유용 티저',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_issue_slug unique (slug),
    constraint uk_webzine_issue_volume unique (volume)
) comment='웹진 이슈(호) 메타';

-- 기사(이슈 소속, 이슈당 slug 유일)
create table if not exists webzine_articles (
    id bigint not null auto_increment primary key comment '기사 PK',
    issue_id bigint not null comment 'FK → webzine_issues.id',
    slug varchar(150) not null comment '이슈 내 기사 URL 슬러그',
    title varchar(200) not null comment '제목',
    kicker varchar(200) not null comment '상단 키커/톤',
    summary text not null comment '요약/리드',
    hero_image_url varchar(500) not null comment '상세 상단 히어로',
    card_image_url varchar(500) not null comment '목록 카드 이미지',
    category varchar(30) not null comment '분류 키',
    format_label varchar(60) not null comment '포맷 라벨(인터뷰 등)',
    author_name varchar(120) not null comment '표시용 저자명',
    author_email varchar(255) not null comment '연락/메타용 이메일',
    credit_line varchar(255) not null comment '사진·출처 크레딧',
    highlight_quote varchar(500) comment '강조 인용(옵션)',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_issue_slug unique (issue_id, slug),
    constraint fk_webzine_articles_issue foreign key (issue_id) references webzine_issues(id) on delete cascade
) comment='웹진 기사 본문 메타';

-- 이슈별 기사 목록 조회
create index idx_webzine_articles_issue_id on webzine_articles(issue_id);

-- 본문 섹션(순서, 소제목, 본문, 편집 노트)
create table if not exists webzine_article_sections (
    id bigint not null auto_increment primary key comment '섹션 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '렌더 순서',
    heading varchar(200) comment '소제목(옵션)',
    body text not null comment '본문(표현 형식은 앱 규약)',
    note varchar(1000) comment '각주·편집자 주(옵션)',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_section_order unique (article_id, display_order),
    constraint fk_webzine_article_sections_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 본문 블록(섹션)';

-- 갤러리 이미지
create table if not exists webzine_article_gallery_images (
    id bigint not null auto_increment primary key comment '갤러리 행 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '갤러리 내 순서',
    image_url varchar(500) not null comment '이미지 URL',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_gallery_order unique (article_id, display_order),
    constraint fk_webzine_article_gallery_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 갤러리 이미지(순서)';

-- 태그(기사당 tag_name 유일)
create table if not exists webzine_article_tags (
    id bigint not null auto_increment primary key comment '태그 행 PK',
    article_id bigint not null comment 'FK → webzine_articles.id',
    display_order int not null comment '표시 순서',
    tag_name varchar(80) not null comment '태그 문자열',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_webzine_article_tag_name unique (article_id, tag_name),
    constraint fk_webzine_article_tags_article foreign key (article_id) references webzine_articles(id) on delete cascade
) comment='기사 태그';
