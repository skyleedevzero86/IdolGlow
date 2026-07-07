# Changelog

이 문서는 `IdolGlow` 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 참고했고, 날짜 기준으로 정리했습니다.

## [Unreleased]

### Added

- `/admin/issues`, `/admin/issues/{issueSlug}`, `/admin/issues/{issueSlug}/articles/{articleSlug}` 기준의 관리자 프론트 화면을 구현했습니다.
- 호 등록/수정/삭제, 기사 등록/수정/삭제, 이미지 업로드를 위한 웹진 관리자 백엔드 API를 추가했습니다.
- 이미지 업로드를 위한 MinIO/local 스토리지 경로와 공개 접근 설정을 추가했습니다.
- `AuthenticatedPrincipal`, `JwtClaimNames`, `@LoginPrincipal` 및 인증 문서(`docs/auth/*`)를 추가했습니다.

### Changed

- `global.MutationGraphQlController`를 도메인별 GraphQL mutation controller로 분리했습니다.
- package 선언과 폴더 경로를 일치시켰습니다 (`*.ui` → `*.adapter.web`, `*.graphql` → `*.adapter.graphql`, 208건).
- `global.infrastructure.config` 패키지를 `global.config`로 이전했습니다 (36파일).
- application layer의 UI DTO 직접 참조를 제거하고 `application.dto` + adapter mapper 패턴으로 정리했습니다.
- `@LoginUser` / GraphQL `AuthenticatedUserIdResolver`가 `AuthenticatedPrincipalResolver`를 통해 userId를 해석하도록 변경했습니다 (API 호환).
- 관리자 프론트가 더미 상태 대신 실제 백엔드 API를 호출하도록 변경했습니다.
- 백엔드를 기존 프로젝트 스타일은 유지하면서 `ui / application / domain / infrastructure` 구조 위에 포트/어댑터 방식으로 정리했습니다.
- 컨트롤러가 요청 DTO를 바로 서비스에 넘기지 않고, 애플리케이션 커맨드로 변환한 뒤 유스케이스 인터페이스를 호출하도록 변경했습니다.
- 리포지토리 접근을 JPA 직접 의존 방식에서 도메인 리포지토리 인터페이스 + 인프라 구현체 방식으로 변경했습니다.
- 이미지 업로드 로직을 애플리케이션 서비스와 스토리지 어댑터로 분리해 MinIO/S3 확장에 대비하도록 정리했습니다.

### Fixed

- Flyway `V13__Add_updated_at_to_review_vote_report.sql` 체크섬 불일치 문제를 해결하기 위해 기존 적용 기준 내용으로 복구했습니다.
- MinIO 버킷 공개 정책 JSON 반환 시 문자열이 깨지던 문제를 수정했습니다.
- 웹진 모듈 구조 변경 후 백엔드 `classes` 컴파일이 통과하도록 정리했습니다.

## [2026-03-28]

### Fixed

- 전반적인 에러 처리와 안정성 문제를 개선했습니다.

## [2026-03-26]

### Added

- 프론트 프로젝트를 생성하고 기본 작업 구조를 추가했습니다.

### Changed

- 프론트와 백엔드 작업 경계를 더 분명하게 구분하도록 프로젝트 방향을 정리했습니다.

## [2026-03-23]

### Changed

- 문서 섹션 제목과 포맷을 개선했습니다.

## [2026-03-21]

### Added

- 프로젝트 소개, 실행 방법, 아키텍처, 주요 기능이 포함된 README 내용을 확장 및 프로젝트 생성하였습니다.

## [2025-11-27]

### Added

- 프로젝트 레파지토리를 생성했습니다.
