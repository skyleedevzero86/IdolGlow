-- =============================================================================
-- Flyway V18 MySQL
-- 마크다운 에디터 게시판 테이블 editor_documents 와 editor_assets
-- PostgreSQL V19 와 동일한 필드 의미이며 embedding 은 TEXT 로 저장한다
-- =============================================================================

-- -----------------------------------------------------------------------------
-- editor_documents
-- -----------------------------------------------------------------------------
create table if not exists editor_documents (
    id char(36) not null primary key,
    title varchar(120) not null,
    author varchar(60) not null,
    markdown longtext not null,
    tags_json longtext not null default '[]',
    updated_at timestamp(6) not null,
    embedding text not null,
    publication_status varchar(20) not null default 'PUBLISHED',
    url_slug varchar(180),
    introduction varchar(150),
    thumbnail_image_url text
) engine=InnoDB default charset=utf8mb4;

create index if not exists editor_documents_updated_at_idx
    on editor_documents (updated_at desc);

create index if not exists editor_documents_publication_status_idx
    on editor_documents (publication_status, updated_at desc);

create index if not exists editor_documents_url_slug_idx
    on editor_documents (url_slug);

-- -----------------------------------------------------------------------------
-- editor_assets
-- -----------------------------------------------------------------------------
create table if not exists editor_assets (
    id char(36) not null primary key,
    object_key varchar(255) not null,
    bucket_name varchar(80) not null,
    original_file_name varchar(255) not null,
    content_type varchar(160) not null,
    file_size bigint not null,
    uploaded_at timestamp(6) not null,
    constraint uk_editor_assets_object_key unique (object_key)
) engine=InnoDB default charset=utf8mb4;

create index if not exists editor_assets_uploaded_at_idx
    on editor_assets (uploaded_at desc);
