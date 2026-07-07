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

- Jenkins 백엔드 빌드를 `JAR 빌드` / `테스트` / `Detekt` / `Ktlint` stage로 분리하고, stage별 timeout·`--profile`·`--stacktrace`·`--no-parallel`로 실패 지점을 바로 확인할 수 있게 했습니다.
- dev 환경 Poll SCM 빌드는 `bootJar`를 건너뛰고 품질 검사만 실행합니다 (staging/prod·릴리즈 태그는 `clean bootJar` 포함).
- Gradle kapt/worker 캐시 튜닝(`gradle.properties`)과 Jenkins Docker `gradle_cache` named volume으로 Windows I/O 병목을 줄였습니다.
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

- 테스트 프로필에서 Flyway가 `mysql/V1`·`postgres/V1` 마이그레이션을 동시에 읽어 `:test`가 실패하던 문제를 수정했습니다 (`application-test.yml`을 저장소에 포함, Flyway 비활성화 + H2 `create-drop`).
- MinIO 버킷 공개 정책 JSON 반환 시 문자열이 깨지던 문제를 수정했습니다.
- 웹진 모듈 구조 변경 후 백엔드 `classes` 컴파일이 통과하도록 정리했습니다.

## [0.6.1] - 2026-07-08

### Added

- Next.js 기반 `frontend` 프로젝트를 저장소에 포함했습니다.

### Changed

- Jenkins Telegram 알림에 릴리즈 버전·프론트 경로를 표시하도록 개선했습니다.
- release-please manifest·`version.txt`·Git 태그 버전을 `v0.6.1` 기준으로 동기화했습니다.
- 프론트엔드 CI 빌드를 위해 `pnpm-lock.yaml` 추적 및 TypeScript 설정을 정리했습니다.

## [0.6.0] - 2026-06-20

### Changed

- Jenkins 파이프라인 및 Gradle 캐시 경로를 정리했습니다.

## [0.5.0] - 2026-06-20

### Changed

- 백엔드 빌드·배포 파이프라인 안정화 작업을 반영했습니다.

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
