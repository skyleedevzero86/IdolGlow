# Changelog

이 문서는 `IdolGlow` 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 참고했고, 날짜 기준으로 정리했습니다.

## [0.8.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.7.1...v0.8.0) (2026-10-16)


### Features

* GitHub PR·이슈·CI·릴리즈 알림 추가 ([6b8c46d](https://github.com/skyleedevzero86/IdolGlow/commit/6b8c46d5b5b9708dec07049dc97730a7b0e50344))
* Glow Pass 다중 카드 등록·임의 변경·미납 재결제 추가 ([aeeeb0d](https://github.com/skyleedevzero86/IdolGlow/commit/aeeeb0d274ca3b4d13092d7b353edb4a25d65aa9))
* Glow Pass 토스 빌링·다중카드와 시스템 배포시 확인 알림 추가 [#26](https://github.com/skyleedevzero86/IdolGlow/issues/26) ([deaf8c2](https://github.com/skyleedevzero86/IdolGlow/commit/deaf8c2ebdce9c59215ebe89c4f6d2e9647f7db9))
* 알람웹후크 추가 ([41d84a0](https://github.com/skyleedevzero86/IdolGlow/commit/41d84a0bb7e061a2e7442923fe785f2197b8b88b))
* 이동 경로 링크 추가 ([705d220](https://github.com/skyleedevzero86/IdolGlow/commit/705d22048569748e839c1152eef1c2a7f2b15f32))
* 전체 이용자 기준 자주 찾는 검색어 집계 추가 ([232808a](https://github.com/skyleedevzero86/IdolGlow/commit/232808acc1d91104fac33ef61856b15a1751fb56))
* 토스 빌링키 발급·구독·회차 결제 추가 ([7e18c3b](https://github.com/skyleedevzero86/IdolGlow/commit/7e18c3bb893753e1373f93db1f46271bb4087f7e))


### Bug Fixes

* **#21:** Jenkins 로컬 배포 경로와 헬스체크 흐름 안정화 ([b7915fc](https://github.com/skyleedevzero86/IdolGlow/commit/b7915fcb6753bf119d4f91daa60d7193d8680a25))
* Backend CI detekt 위반 정리 및 빌링 품질 게이트 통과 ([3647543](https://github.com/skyleedevzero86/IdolGlow/commit/3647543a76f97dfa370f316fb04fac3c4a55e47a))
* Backend CI detekt 위반 정리 및 빌링 품질 게이트 통과 [#27](https://github.com/skyleedevzero86/IdolGlow/issues/27) ([cd4409c](https://github.com/skyleedevzero86/IdolGlow/commit/cd4409c5566d49b5ca73e7741c40ec67e90e9ad1))
* Jenkins 로컬 배포 경로 및 헬스체크 흐름 안정화 ([f40d30b](https://github.com/skyleedevzero86/IdolGlow/commit/f40d30b580871552551318ac88c5c24fe3ca2d46))
* push기능추가 ([6704500](https://github.com/skyleedevzero86/IdolGlow/commit/6704500cb46b12a16fa12307d44646b0883cfb02))
* readme.md파일 테스트로 수정중 ([ee51721](https://github.com/skyleedevzero86/IdolGlow/commit/ee517216bc9ab64f4adbd9cf05ccc967a45349f6))
* webhook User-Agent 추가 ([eafa9ba](https://github.com/skyleedevzero86/IdolGlow/commit/eafa9baffc131c16ea2848d1a99f78d58a34aee3))
* workerflow테스트 적용 ([1f1810e](https://github.com/skyleedevzero86/IdolGlow/commit/1f1810e5ff05b0352296c6dac4aee8d8d4f71123))
* 결제 GraphQL 보안 강화 및 예약·결제 도메인 테스트 추가 ([19827be](https://github.com/skyleedevzero86/IdolGlow/commit/19827be6e4486cabf32e80f91d43cd17c85d25ee))
* 메세지발송 수정 ([855128e](https://github.com/skyleedevzero86/IdolGlow/commit/855128e75d34dcbce6d9ef1f0a96c3ed9e12788c))
* 알림 전송 수정 ([f197695](https://github.com/skyleedevzero86/IdolGlow/commit/f197695dc396dec7ddbc3a08f472c79d21e2928b))
* 웹후크알람 설정 확인 테스트 ([e7e4c98](https://github.com/skyleedevzero86/IdolGlow/commit/e7e4c983ed9dd6299590c1cd4d3fbe9302b1bd82))


### CI

* Telegram 커밋 알림 워크플로 추가 ([d38a7ac](https://github.com/skyleedevzero86/IdolGlow/commit/d38a7ac698e41600a71973ad18f57161aec27f03))

## [0.7.1](https://github.com/skyleedevzero86/IdolGlow/compare/v0.7.0...v0.7.1) (2026-07-07)


### Bug Fixes

* Jenkins 프론트엔드 빌드 및 배포 파이프라인 안정화 ([0bb6a5e](https://github.com/skyleedevzero86/IdolGlow/commit/0bb6a5e5304cacc56739e05723f9cdc5e3c4755a))
* lock파일 커밋테스트 ([024dddb](https://github.com/skyleedevzero86/IdolGlow/commit/024dddbe0a70d701ef83ec74535129ef18ef5cfe))
* 내용변경 ([b0c6760](https://github.com/skyleedevzero86/IdolGlow/commit/b0c676016e77a18ba722e0ef12df349106395e32))
* 프론트 빌드처리하기위해 추가 ([bbfb856](https://github.com/skyleedevzero86/IdolGlow/commit/bbfb856e9a461e99e3dec46827bd89c580437483))

## [0.7.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.6.1...v0.7.0) (2026-07-07)


### Features

* 프론트 프로젝트 추가 ([4d3f830](https://github.com/skyleedevzero86/IdolGlow/commit/4d3f83009fe54635c95929c05fcf1d7c069f1c3f))
* 프론트프로젝트 셋팅 ([e70d796](https://github.com/skyleedevzero86/IdolGlow/commit/e70d79656435b3e5f1b804cb01621733a5684233))


### Bug Fixes

* enable Jenkins frontend build with pnpm lockfile ([b6cee29](https://github.com/skyleedevzero86/IdolGlow/commit/b6cee298a065b6c55a60bb12470bf13d87175120))
* Jenkins 알림 개선 및 민감 infra 파일 untrack ([6a6ef3b](https://github.com/skyleedevzero86/IdolGlow/commit/6a6ef3b346b45ad892d64cf9dde7848f32a33d78))
* lock파일 커밋 ([c186e47](https://github.com/skyleedevzero86/IdolGlow/commit/c186e476a941cacf91adc776d8dc997b59fdfe43))
* sync release version and improve Telegram context ([2ea2075](https://github.com/skyleedevzero86/IdolGlow/commit/2ea2075d5653f5f3e733215de9008c1b3b45163b))
* 민감파일 커밋금지 ([c1654ad](https://github.com/skyleedevzero86/IdolGlow/commit/c1654adeb59ea203ec875619f80203b0d5f7bbcd))

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
