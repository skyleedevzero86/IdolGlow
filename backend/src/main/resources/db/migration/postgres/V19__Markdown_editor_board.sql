-- =============================================================================
-- Flyway V19 PostgreSQL
-- 마크다운 에디터 게시판 도메인 테이블 editor_documents 와 에디터 업로드 자산 editor_assets
-- docs board 참고 스키마이며 pgvector 확장 없이 embedding 은 TEXT 로 저장한다
-- 토큰 해시 기반 8차원 벡터 문자열을 그대로 저장하고 검색 정렬은 주로 updated_at 을 사용한다
-- =============================================================================

-- -----------------------------------------------------------------------------
-- editor_documents
-- 문서 본문은 markdown TEXT
-- tags_json 은 문자열 배열을 JSON 배열로 저장한다
-- publication_status 는 DRAFT 또는 PUBLISHED
-- url_slug 는 공개 글의 경로 식별자로 사용할 수 있다
-- introduction 과 thumbnail_image_url 은 목록 카드용 요약 필드다
-- embedding 은 검색 보조용 수치 벡터를 대괄호 리터럴 문자열로 저장한다
-- -----------------------------------------------------------------------------
create table if not exists editor_documents (
    id uuid primary key,
    title varchar(120) not null,
    author varchar(60) not null,
    markdown text not null,
    tags_json jsonb not null default '[]'::jsonb,
    updated_at timestamptz not null,
    embedding text not null,
    publication_status varchar(20) not null default 'PUBLISHED',
    url_slug varchar(180),
    introduction varchar(150),
    thumbnail_image_url text
);

comment on table editor_documents is '마크다운 게시글 마스터';
comment on column editor_documents.id is '문서 UUID 기본키';
comment on column editor_documents.title is '제목 최대 120자';
comment on column editor_documents.author is '작성자 표시명';
comment on column editor_documents.markdown is '마크다운 본문';
comment on column editor_documents.tags_json is '태그 문자열 배열 JSON';
comment on column editor_documents.updated_at is '최종 수정 시각';
comment on column editor_documents.embedding is '8차원 근사 벡터 문자열';
comment on column editor_documents.publication_status is 'DRAFT 또는 PUBLISHED';
comment on column editor_documents.url_slug is '공개용 슬러그';
comment on column editor_documents.introduction is '짧은 소개';
comment on column editor_documents.thumbnail_image_url is '썸네일 이미지 URL';

create index if not exists editor_documents_updated_at_idx
    on editor_documents (updated_at desc);

create index if not exists editor_documents_publication_status_idx
    on editor_documents (publication_status, updated_at desc);

create index if not exists editor_documents_url_slug_idx
    on editor_documents (url_slug);

-- -----------------------------------------------------------------------------
-- editor_assets
-- MinIO 등 객체 스토리지의 object_key 와 원본 파일명을 기록한다
-- -----------------------------------------------------------------------------
create table if not exists editor_assets (
    id uuid primary key,
    object_key varchar(255) not null unique,
    bucket_name varchar(80) not null,
    original_file_name varchar(255) not null,
    content_type varchar(160) not null,
    file_size bigint not null,
    uploaded_at timestamptz not null
);

comment on table editor_assets is '에디터 이미지 업로드 메타';
comment on column editor_assets.id is '자산 UUID';
comment on column editor_assets.object_key is '스토리지 객체 키';
comment on column editor_assets.bucket_name is '버킷 이름';
comment on column editor_assets.original_file_name is '원본 파일명';
comment on column editor_assets.content_type is 'MIME 타입';
comment on column editor_assets.file_size is '바이트 크기';
comment on column editor_assets.uploaded_at is '업로드 시각';

create index if not exists editor_assets_uploaded_at_idx
    on editor_assets (uploaded_at desc);

-- -----------------------------------------------------------------------------
-- pg_search 가 설치된 경우에만 BM25 검색 인덱스를 시도한다
-- 확장이 없으면 애플리케이션은 ILIKE 기반 검색으로 동작한다
-- -----------------------------------------------------------------------------
do $pgsearch$
begin
    if exists (select 1 from pg_extension where extname = 'pg_search') then
        execute '
            create index if not exists editor_documents_search_idx
            on editor_documents
            using bm25 (id, title, author, markdown)
            with (key_field = ''id'')
        ';
    end if;
end
$pgsearch$;
