# Changelog

이 문서는 `IdolGlow` 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 참고했고, 날짜 기준으로 정리했습니다.
작성 규칙은 [docs/CHANGELOG_POLICY.md](IdolGlow/CHANGELOG_POLICY.md)를 참고합니다.
자동 갱신은 [release-please.yml](IdolGlow/.github/workflows/release-please.yml)에서 처리합니다.

## [0.2.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.1.0...v0.2.0) (2026-04-17)


### Features

* add admin issue APIs, uploads, and frontend integration ([2682fe3](https://github.com/skyleedevzero86/IdolGlow/commit/2682fe333c9de93998d742bc39aaad65fcd5e812))
* add changelog policy and github release automation ([07cf897](https://github.com/skyleedevzero86/IdolGlow/commit/07cf897f3edc4527b20085ddd4094eba907c983a))
* add Gemma admin chat integration ([b9fcaa7](https://github.com/skyleedevzero86/IdolGlow/commit/b9fcaa738f8fa73812f6be50646793867389b847))
* add initial schema for product/reservation/wish/review/schedule ([e2b4156](https://github.com/skyleedevzero86/IdolGlow/commit/e2b415670d137339ec0d509a01f223b6a5636ebd))
* add payment mock webhook and integrate with reservation confirmation/cancel ([9354efb](https://github.com/skyleedevzero86/IdolGlow/commit/9354efb603bcf66a367515e813b07536fcfd002f))
* add product discovery and admin catalog/reservation management ([f76a7d8](https://github.com/skyleedevzero86/IdolGlow/commit/f76a7d8d85c14b58c7b8b54ec87f41c82d365e8e))
* add product/option modules with image event pipeline ([c430c35](https://github.com/skyleedevzero86/IdolGlow/commit/c430c3550a4666c5ce7835802177f98180bcd4ac))
* add schedule management APIs and cursor pagination ([252f6db](https://github.com/skyleedevzero86/IdolGlow/commit/252f6dbb106930fb41623362f38353a49c1c9b59))
* add structured exception handling and concurrency controls ([e89b016](https://github.com/skyleedevzero86/IdolGlow/commit/e89b016d32e53f690dea3dbbf4c7a14fa2dd5bd4))
* admin analytics, payment([#3](https://github.com/skyleedevzero86/IdolGlow/issues/3)), notification([#3](https://github.com/skyleedevzero86/IdolGlow/issues/3)), calendar export, review system 구현  ([c2cc2a6](https://github.com/skyleedevzero86/IdolGlow/commit/c2cc2a616c2d803b5b8636d3635bdea24d473fc1))
* configure SecurityConfig and auth endpoints ([f9d4560](https://github.com/skyleedevzero86/IdolGlow/commit/f9d456047659017f1eb2f84fa295cf608f08314c))
* GraphQL 값 변환 유틸 ([0515f18](https://github.com/skyleedevzero86/IdolGlow/commit/0515f18a5d68f03a58505deb36a6560bb4e5fa33))
* Idol Glow 소식지 관리자 백엔드 ([0d32cec](https://github.com/skyleedevzero86/IdolGlow/commit/0d32cec14cbb2c2b90bd4a84fb438b330fba51b5))
* IdolGlow 백엔드 MVP 전 도메인 계층 및 예약/결제 라이프사이클 구현 ([ba52f25](https://github.com/skyleedevzero86/IdolGlow/commit/ba52f2525fa9bb96a3891670f6c5f776a6e38bde))
* images 테이블 추가 ([cf0e197](https://github.com/skyleedevzero86/IdolGlow/commit/cf0e1970070c7cbe092d00e0fcffa356de3d4575))
* implement product review CRUD with image pipeline and graphql queries ([9555b1c](https://github.com/skyleedevzero86/IdolGlow/commit/9555b1c503793435970bcb8441cc0edbe4626b52))
* implement reservation lifecycle (hold, confirm, expire) with notifications ([07d94ac](https://github.com/skyleedevzero86/IdolGlow/commit/07d94ac7fca02f3ef31acba25160a17fd4a090b9))
* implement wish toggle/query with event-driven cleanup ([9bc28c3](https://github.com/skyleedevzero86/IdolGlow/commit/9bc28c38b757ffd4e669b7c88f53d938eb82df20))
* introduce payments/notifications and reservation lifecycle fields ([ff33b6b](https://github.com/skyleedevzero86/IdolGlow/commit/ff33b6b392bdddcf8d52e8afc685ab08c4eb25e9))
* ObjectMapper 의존 방식 보정으로 애플리케이션 기동 실패 처리 기능 추가 ([8efe709](https://github.com/skyleedevzero86/IdolGlow/commit/8efe709f9102b0f9c9bf0c46f95bf45ff5cfaf33))
* persist notifications and stream via SSE with after-commit delivery ([5f6ec79](https://github.com/skyleedevzero86/IdolGlow/commit/5f6ec79e8cd89d226a7691b66f88ea2105b2e9ef))
* seed local/dev sample data via LocalProfileMySqlDataWarmer ([57ec133](https://github.com/skyleedevzero86/IdolGlow/commit/57ec133bd540c6a1b6674b72eebe3b08df624119))
* 개발서버 mysql 추가 ([d7a0bb1](https://github.com/skyleedevzero86/IdolGlow/commit/d7a0bb14da7022c3248a04f08b877e80522203d7))
* 구독 저장 및 관리자 구독관리 기능 추가 ([6e055f6](https://github.com/skyleedevzero86/IdolGlow/commit/6e055f646b155c71ce1646692e0d14a1e3af092c))
* 구독 저장 및 관리자 구독관리 기능 추가 ([b7ce4f3](https://github.com/skyleedevzero86/IdolGlow/commit/b7ce4f3f66644fbdd5994c7c315cff5e09065663))
* 마크다운 에디터 게시판 API 및 WebSocket 실시간 동기 추가 ([bd211b6](https://github.com/skyleedevzero86/IdolGlow/commit/bd211b65444407a12c103c06f54ad0d2bc947580))
* 사용자 정보 조회/닉네임 수정 + MyPage 집계 API ([4fc2dda](https://github.com/skyleedevzero86/IdolGlow/commit/4fc2ddabfd665478a407916c7a02a0e90663327d))
* 사이트 도메인·배너·팝업·메인이미지 관리 API 추가 ([073e778](https://github.com/skyleedevzero86/IdolGlow/commit/073e7780462c803bb17b394456636df44ad8d358))
* 사이트 콘텐츠 관리자와 홈 화면 연동 ([d33d078](https://github.com/skyleedevzero86/IdolGlow/commit/d33d0785f1ac7bea9463f8f44748a315033c0c6d))
* 상품·옵션·찜·랭킹 조회에 이미지 URL·리뷰 미리보기 필드 추가 ([8c57c4e](https://github.com/skyleedevzero86/IdolGlow/commit/8c57c4e4c863449fd91147d0956f6c0b361c9dd9))
* 신뢰 리뷰, 매진 웨이팅, 토스 PG·환불 이력 ([a063a49](https://github.com/skyleedevzero86/IdolGlow/commit/a063a49301b5884e2e0d4ef65948f1f2e4df6688))
* 알림  설정/필터/전체읽음/만료예정 알림 추가 고도화 ([0c5d866](https://github.com/skyleedevzero86/IdolGlow/commit/0c5d866619ec917c6c81542a4faf3d1356550e11))
* 역활 분리 모델 분리1 (dto분리) ([21d083f](https://github.com/skyleedevzero86/IdolGlow/commit/21d083fa50d5d0ac6b0a67e881adab1a0c0958a9))
* 역활 분리 모델분리2 (dto 분리) ([c0935ad](https://github.com/skyleedevzero86/IdolGlow/commit/c0935add2c845c254f288551dd3f59511ced156f))
* 역활 분리 모델분리3 (dto 분리) ([a956c88](https://github.com/skyleedevzero86/IdolGlow/commit/a956c8844a1d4613662057513334731a74399e2b))
* 외부 캘린더 연동 및 방문 리마인더 VALARM ([a9947ef](https://github.com/skyleedevzero86/IdolGlow/commit/a9947ef268e255e665e636a7398ce9222dd1e208))
* 운영 분석 API·CSV·감사 로그·주말 제외 슬롯 생성 ([feb8f04](https://github.com/skyleedevzero86/IdolGlow/commit/feb8f04497e002515884857c779eb19845ef5bc7))
* 운영 분석/결제/알림/SSE/캘린더 연동/리뷰 시스템 통합 구현 [#2](https://github.com/skyleedevzero86/IdolGlow/issues/2) ([5812435](https://github.com/skyleedevzero86/IdolGlow/commit/58124354388aa6739e6b2861100d1b5effce9f42))
* 젠킨스 dev 설치용 구독 설계테이블 ([2014ffa](https://github.com/skyleedevzero86/IdolGlow/commit/2014ffa71214f2af5c61fe5dd21c8f3921c30d03))
* 젠킨스파일 셋팅 ([354454c](https://github.com/skyleedevzero86/IdolGlow/commit/354454cfe5e37ba387fe8d14408fff383787d18c))
* 젠킨스파일 수정 ([e08e456](https://github.com/skyleedevzero86/IdolGlow/commit/e08e456374002da8c5ed911296bff8ed0adf19ac))
* 추천패키지 추가 ([cefff27](https://github.com/skyleedevzero86/IdolGlow/commit/cefff272bdf76d22e9ed587eaad4be2f444e935b))
* 토스 웹훅에서 결제 조회·상태 반영 및 가상계좌 등 비동기 완료 처리 ([2d408c2](https://github.com/skyleedevzero86/IdolGlow/commit/2d408c2cfded70c8608c15e1b178d3ea7161f667))
* 파일 삭제처리 ([a8f6cb7](https://github.com/skyleedevzero86/IdolGlow/commit/a8f6cb7358b5a4b4d7e3a0822d0f8b2b350063cc))
* 파일 업로드 로컬 스토리지 서버 셋팅 처리기능 추가 ([028cf74](https://github.com/skyleedevzero86/IdolGlow/commit/028cf7407828edeb1041671ea946c01e25e2e857))
* 프로젝트생성 ([55f53cc](https://github.com/skyleedevzero86/IdolGlow/commit/55f53cce3d35a87c8c3f52aa491aa92be394a62f))
* 플랫폼 인증 이식·역할 정리·가입 즉시 사용 ([cbe676d](https://github.com/skyleedevzero86/IdolGlow/commit/cbe676db18a61e77c4cec2b352eef07d00c3e5fb))
* 플랫폼 인증·콘텐츠 운영·웹진/에디터·결제 처리 통합 및 아키텍처 안정화 [#4](https://github.com/skyleedevzero86/IdolGlow/issues/4) ([4a0482b](https://github.com/skyleedevzero86/IdolGlow/commit/4a0482b7005f5b3a1341ef4ab036ecaac6356f22))
* 플랫폼 인증·콘텐츠 운영·웹진/에디터·결제 처리 통합 및 아키텍처 안정화 [#5](https://github.com/skyleedevzero86/IdolGlow/issues/5) ([5e67e85](https://github.com/skyleedevzero86/IdolGlow/commit/5e67e859fdcb2f5817d65605df2d4886adb44b63))


### Bug Fixes

* @Lob → @JdbcTypeCode(LONGVARCHAR)로 Hibernate 7 스키마 검증 오류 수정 ([55f5319](https://github.com/skyleedevzero86/IdolGlow/commit/55f5319f5dfb9bd6ad2024e6bd338f87d0a5f4af))
* CHAR 컬럼 스키마 검증 오류 수정 및 마크다운 에디터 기능 반영 ([572c1c0](https://github.com/skyleedevzero86/IdolGlow/commit/572c1c05c9347295b2b4da83f61db766e02388ef))
* GraphQL기능 확장 겸 미리보기 기능 개선 ([18df609](https://github.com/skyleedevzero86/IdolGlow/commit/18df6099b4f72c6c4011e2fc247e4a81ea04df01))
* Jackson 3.x 패키지 마이그레이션 ([83bef4a](https://github.com/skyleedevzero86/IdolGlow/commit/83bef4a257baddb56d2763fd013c00043c134343))
* Kotlin 컴파일 오류 수정 및 backend build 복구 ([6229ef7](https://github.com/skyleedevzero86/IdolGlow/commit/6229ef7dcc1827a04945aa8041efa456548bda2e))
* local 프로필에서 images 테이블 누락으로 부팅 실패하지 않도록 조정 ([0dad13d](https://github.com/skyleedevzero86/IdolGlow/commit/0dad13d2e41ed6295481100806463e87b2977043))
* MinIO 버킷 보장·503 응답·업로드 API 오류 메시지 노출 ([8abd25c](https://github.com/skyleedevzero86/IdolGlow/commit/8abd25cb33463e489e2fd2e52a03013857ba5f42))
* MinIO 프로필 이미지 공개 읽기 정책 및 프로필 미리보기 오류 처리  또는 영문 선호 ([12c7cf9](https://github.com/skyleedevzero86/IdolGlow/commit/12c7cf994ff4f3ee96692dccdbf47617bccaff55))
* product_review_helpful_votes/reports 테이블 updated_at 누락 보정 ([0260986](https://github.com/skyleedevzero86/IdolGlow/commit/026098684efba3308d8571e0484129dc1ac79119))
* restore v13 migration checksums and stabilize webzine boot ([dfec1c9](https://github.com/skyleedevzero86/IdolGlow/commit/dfec1c982bb64635c1d0a29328c85075f723d6a5))
* sns로그인 프로필 사진,닉네임 변경 기능 추가 ([955c750](https://github.com/skyleedevzero86/IdolGlow/commit/955c7504a4571c2451bacee5e46989661bd9b0b8))
* Spring Boot 4 Jackson 타입 정합성 및 예외 처리 개선 및 운영에 맞게 Postgresql 버전으로 마이그레이션 셋팅 ([d9fd226](https://github.com/skyleedevzero86/IdolGlow/commit/d9fd2268be800347627f6c5894cf6ca09b9b9fbc))
* 관리 목록 페이지 카운트와 JOIN FETCH 충돌 수정 및 Windows kapt 빌드 안정화 ([d692261](https://github.com/skyleedevzero86/IdolGlow/commit/d692261d5193e7a3288842509b0b4e5bda11c6e0))
* 구글로그인 및 에러방지 수정 ([5662a5c](https://github.com/skyleedevzero86/IdolGlow/commit/5662a5caf166a7ad89e19158094f23d8ce3efb2e))
* 누락된 import코드처리 ([2932689](https://github.com/skyleedevzero86/IdolGlow/commit/29326891f1579725614edfe31bea1f7826a26f0d))
* 닉네임 한글기능 추가 ([1bfb966](https://github.com/skyleedevzero86/IdolGlow/commit/1bfb96674def4cdb557570b1d7e1023f81eeb6ae))
* 로그인 에러처리 ([a2277f2](https://github.com/skyleedevzero86/IdolGlow/commit/a2277f2894b6149521e7d3d7f9de6f22cf4a3c0a))
* 로컬 부팅 시 JPA 임베디드 매핑과 데이터 워밍 오류 수정 ([2eed004](https://github.com/skyleedevzero86/IdolGlow/commit/2eed00408325ff05537ce17e6b7b3c49397e292e))
* 로컬 부팅과 시크릿 설정 정리 ([c719dc4](https://github.com/skyleedevzero86/IdolGlow/commit/c719dc431b7fe99ccb0cbaf0601035e1f9161838))
* 버전변경 jdk25 버전 업그레이드 작업 ([d87a8e6](https://github.com/skyleedevzero86/IdolGlow/commit/d87a8e62a83d1c9566047cd8a25770986e2418f0))
* 사용자 이름 과  프로필 이미지 가져오기 처리하기 ([34381c1](https://github.com/skyleedevzero86/IdolGlow/commit/34381c1716b11517b010a983692a62ad84c48bf0))
* 컬럼변경 ([d7134e0](https://github.com/skyleedevzero86/IdolGlow/commit/d7134e09c669156b60609f030e6342647bb22acd))
* 코드개선 ([29b200f](https://github.com/skyleedevzero86/IdolGlow/commit/29b200f1e8934dd28d5e5cd5076f78d3f2b7ebb3))
* 테스트 코드정리 ([754b816](https://github.com/skyleedevzero86/IdolGlow/commit/754b816c534baf3dba1de77af7300af6ed1a1a10))
* 패키지 정리 ([bc37707](https://github.com/skyleedevzero86/IdolGlow/commit/bc37707a9f8b1c30b08cf6616f46ba6a2e37a3e6))
* 필요없는 이그노어 추가 ([b5bb5e7](https://github.com/skyleedevzero86/IdolGlow/commit/b5bb5e763af1362e4172944760645f92f4f11e3a))


### Refactoring

* introduce ports and adapters in admin issue module ([c709d69](https://github.com/skyleedevzero86/IdolGlow/commit/c709d693e3392bffe015aa192d52944e24c781b4))
* 구독 모듈 포트/어댑터 구조 정리 ([8aad799](https://github.com/skyleedevzero86/IdolGlow/commit/8aad799368539c8387c53c68cd7019324692d30d))


### Documentation

* Swagger API 설명 보강 및 DTO 스키마 문서화 ([fb574db](https://github.com/skyleedevzero86/IdolGlow/commit/fb574db6d37063f8125210a57a65657dbfc7f557))
* V11 마이그레이션 주석 보강 ([6f5b3c7](https://github.com/skyleedevzero86/IdolGlow/commit/6f5b3c7307f43b1dbb591414597641f531e35aca))

## [Unreleased]

### Added
- `frontend`에 ACC 웹진 관리자 화면 시안과 실제 동작 흐름을 추가했습니다.
- `/admin/issues`, `/admin/issues/{issueSlug}`, `/admin/issues/{issueSlug}/articles/{articleSlug}` 기준의 웹진 관리자 프론트 화면을 구현했습니다.
- 호 등록/수정/삭제, 기사 등록/수정/삭제, 이미지 업로드를 위한 웹진 관리자 백엔드 API를 추가했습니다.
- 웹진 이미지 업로드를 위한 MinIO/local 스토리지 경로와 공개 접근 설정을 추가했습니다.

### Changed
- 웹진 관리자 프론트가 더미 상태 대신 실제 백엔드 API를 호출하도록 변경했습니다.
- 웹진 백엔드를 기존 프로젝트 스타일은 유지하면서 `ui / application / domain / infrastructure` 구조 위에 포트/어댑터 방식으로 정리했습니다.
- 컨트롤러가 요청 DTO를 바로 서비스에 넘기지 않고, 애플리케이션 커맨드로 변환한 뒤 유스케이스 인터페이스를 호출하도록 변경했습니다.
- 웹진 리포지토리 접근을 JPA 직접 의존 방식에서 도메인 리포지토리 인터페이스 + 인프라 구현체 방식으로 변경했습니다.
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
