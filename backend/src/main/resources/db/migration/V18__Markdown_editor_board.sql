-- =============================================================================
-- Flyway V18 루트 마이그레이션 트랙
-- 마크다운 에디터 게시판 테이블 editor_documents 와 editor_assets
-- PostgreSQL V19 와 동일한 논리 구조이나 H2 호환을 위해 BM25 인덱스 생성 블록은 생략한다
-- embedding 은 TEXT 로 저장한다
-- =============================================================================

create table if not exists editor_documents (
    id uuid primary key,
    title varchar(120) not null,
    author varchar(60) not null,
    markdown clob not null,
    tags_json varchar(8000) not null default '[]',
    updated_at timestamp with time zone not null,
    embedding varchar(512) not null,
    publication_status varchar(20) not null default 'PUBLISHED',
    url_slug varchar(180),
    introduction varchar(150),
    thumbnail_image_url clob
);

create index if not exists editor_documents_updated_at_idx
    on editor_documents (updated_at desc);

create index if not exists editor_documents_publication_status_idx
    on editor_documents (publication_status, updated_at desc);

create index if not exists editor_documents_url_slug_idx
    on editor_documents (url_slug);

create table if not exists editor_assets (
    id uuid primary key,
    object_key varchar(255) not null,
    bucket_name varchar(80) not null,
    original_file_name varchar(255) not null,
    content_type varchar(160) not null,
    file_size bigint not null,
    uploaded_at timestamp with time zone not null,
    constraint uk_editor_assets_object_key unique (object_key)
);

create index if not exists editor_assets_uploaded_at_idx
    on editor_assets (uploaded_at desc);
