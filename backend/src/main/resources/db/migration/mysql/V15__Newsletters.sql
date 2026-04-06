-- Newsletter admin domain tables for MySQL
-- 목록/상세/수정 화면에서 같은 데이터를 재사용할 수 있도록 태그와 문단을 별도 테이블로 분리한다.

create table if not exists newsletters (
    id bigint not null auto_increment primary key comment '뉴스레터 PK',
    slug varchar(160) not null comment '공개 URL용 슬러그',
    title varchar(200) not null comment '뉴스레터 제목',
    category_label varchar(80) not null comment '관리 화면 카테고리 라벨',
    published_at date not null comment '게시일',
    image_url varchar(500) not null comment '대표 이미지 URL',
    summary text not null comment '목록/공유용 요약',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_newsletter_slug unique (slug)
) comment='뉴스레터 마스터';

create table if not exists newsletter_tags (
    id bigint not null auto_increment primary key comment '태그 PK',
    newsletter_id bigint not null comment 'FK → newsletters.id',
    display_order integer not null comment '표시 순서',
    tag_name varchar(80) not null comment '태그 문자열',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_newsletter_tag_name unique (newsletter_id, tag_name),
    constraint fk_newsletter_tags_newsletter foreign key (newsletter_id) references newsletters(id) on delete cascade
) comment='뉴스레터 태그';

create table if not exists newsletter_paragraphs (
    id bigint not null auto_increment primary key comment '문단 PK',
    newsletter_id bigint not null comment 'FK → newsletters.id',
    display_order integer not null comment '문단 순서',
    body text not null comment '본문 문단',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각',
    constraint uk_newsletter_paragraph_order unique (newsletter_id, display_order),
    constraint fk_newsletter_paragraphs_newsletter foreign key (newsletter_id) references newsletters(id) on delete cascade
) comment='뉴스레터 본문 문단';

create index idx_newsletters_published_at on newsletters(published_at);
