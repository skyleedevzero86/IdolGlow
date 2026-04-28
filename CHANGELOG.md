# Changelog

이 문서는 `IdolGlow` 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 참고했고, 날짜 기준으로 정리했습니다.
작성 규칙은 [docs/CHANGELOG_POLICY.md](IdolGlow/CHANGELOG_POLICY.md)를 참고합니다.
자동 갱신은 [release-please.yml](IdolGlow/.github/workflows/release-please.yml)에서 처리합니다.

## [0.4.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.3.0...v0.4.0) (2026-04-28)


### Features

* **#9:** expand travel services and recommendation APIs ([29e14e2](https://github.com/skyleedevzero86/IdolGlow/commit/29e14e25886ac6d642c8a6bd8b047e1759692d11))
* complete event detail modal and source-specific integrations ([74a4459](https://github.com/skyleedevzero86/IdolGlow/commit/74a4459baa5b50beb4c59756ecebaad7e756e2a8))
* Glow 지하철 페이지 추가 ([6607edc](https://github.com/skyleedevzero86/IdolGlow/commit/6607edce9cb794521419cb86d7dc810f87374dbf))
* improve survey UX and add visit time slots ([fa409fe](https://github.com/skyleedevzero86/IdolGlow/commit/fa409fefcd4fe438a774c2fcb0ac33960ac32425))
* LLM 라우팅 및 공항철도·수인분당 노선 데이터 추가 ([a5e6009](https://github.com/skyleedevzero86/IdolGlow/commit/a5e60098e47141fe9e1619436a92c2b4944c2199))
* persist tour attraction picks and wire Tour API ([3f43c83](https://github.com/skyleedevzero86/IdolGlow/commit/3f43c8332471fe69e42632eeede82157eff5712f))
* Tour 추천 기능 구현 및 아키텍처 정렬 ([4b600f0](https://github.com/skyleedevzero86/IdolGlow/commit/4b600f02007073fd49bfb1b57ae6d222ea7bf4de))
* 문화캘린더 API·고급검색·요청 파라미터 바인딩 정리 ([5dda84c](https://github.com/skyleedevzero86/IdolGlow/commit/5dda84c1cc8498d98cf41126b2795b90d02aa0aa))
* 상품 관광지 추천 API 문서화 및 퍼블릭 상품 상세 연결 ([8c74327](https://github.com/skyleedevzero86/IdolGlow/commit/8c74327f05353c2e7a6787db9dbb52ba17dc3004))
* 상품 주변 관광지 추천 GraphQL 연동 및 Tour 설정 정리 ([403d998](https://github.com/skyleedevzero86/IdolGlow/commit/403d998383e8fa3815abd3c79c8fe05650f681b9))
* 상품 추천 API·DB 및 설문 기반 추천·OpenAI 연동 추가 ([cc0cd56](https://github.com/skyleedevzero86/IdolGlow/commit/cc0cd56bcf26dc1233318410532f96d78052b9ab))
* 상품 테이블·등록폼·테마 동작 정리 ([de4b5ea](https://github.com/skyleedevzero86/IdolGlow/commit/de4b5ea75cf5b64fbf7e4db0ec711b72f521e29c))
* 설문 도메인 구조화 및 추천/AI 연계 기반 확장 ([6ed0b06](https://github.com/skyleedevzero86/IdolGlow/commit/6ed0b064e191064b2ddee87280c75fc2b01ef5ed))
* 옵션 검색·페이징 API 및 상품 편집에 추가 아이템 팝업 ([f6b5050](https://github.com/skyleedevzero86/IdolGlow/commit/f6b5050d948ffc95ee93ea29630d9f98203df0d5))
* 지역·분류 기반 Tour 축제 조회 및 홈페이지/HTML 정제 ([934c43d](https://github.com/skyleedevzero86/IdolGlow/commit/934c43dc22abf878341c1cbe180e62179e3d886d))
* 환율 조회 API 추가 ([933f0ac](https://github.com/skyleedevzero86/IdolGlow/commit/933f0ac5649fd43f99cc191c1e9e0b6eefdc1ac6))
* 환전소 목록·NAVER 차량 이동시간·환율 알림 API 및 환율 페이지 UI ([0b1fb7f](https://github.com/skyleedevzero86/IdolGlow/commit/0b1fb7f045656bf922364003cdbf1f6613d0a024))


### Bug Fixes

* Glow추천 장소 비우기/삭제 저장 안정화 ([98f0863](https://github.com/skyleedevzero86/IdolGlow/commit/98f086386ae844b212e434d5451ec8c8c525d4d4))
* ObjectMapper 패키지 변경 ([a5e8bf7](https://github.com/skyleedevzero86/IdolGlow/commit/a5e8bf7632d9e5057d69cc1a2e01e116d29431b4))
* Spring Boot 메인 클래스 실행 설정 명시 ([c9b1dc5](https://github.com/skyleedevzero86/IdolGlow/commit/c9b1dc5fa96edae7d9ddd454ac64bb695515b2eb))
* Tour 역직렬화에 Jackson 2(fasterxml) 타입 정렬 ([1ba28c4](https://github.com/skyleedevzero86/IdolGlow/commit/1ba28c44e32344b764796eca752f0d3e9c7307b9))
* 공식 환율 조회 시 이전 영업일 fallback 추가 ([bb5aeab](https://github.com/skyleedevzero86/IdolGlow/commit/bb5aeab464909558e873e9adee55d9c1f870fd07))
* 목록 조회 시 대표 위치 로드 및 Tour 연계 필드 정리 ([5f67f58](https://github.com/skyleedevzero86/IdolGlow/commit/5f67f5821e2acb6bff9a81c583090a7ef37eb2b8))
* 상품 UI·테마·DB 마이그레이션 정리 ([af333d4](https://github.com/skyleedevzero86/IdolGlow/commit/af333d4bd632340ababfe631c9c7706c113e0b7f))
* 서울 SJW XML 응답 처리 및 Tour API 빈 items 역직렬화 ([6e0bcf9](https://github.com/skyleedevzero86/IdolGlow/commit/6e0bcf91c7014c4651d7abf16ea137c618ef4de1))
* 코드정리 ([13bcd01](https://github.com/skyleedevzero86/IdolGlow/commit/13bcd011672ef10d5fec69e758fb79877cc912de))
* 쿼리 수정 ([e6bcfd9](https://github.com/skyleedevzero86/IdolGlow/commit/e6bcfd9f42a90184b357f7eaff968b4d9b10066c))
* 패키지 정리 ([4a372e2](https://github.com/skyleedevzero86/IdolGlow/commit/4a372e216ba5e970f179ad7e2969139f3d792b53))
* 환율 계산기 및 환전소 통화 표시 보정 ([789584f](https://github.com/skyleedevzero86/IdolGlow/commit/789584ff80a85bc248dcd877e16c2add7b0e25a5))


### Refactoring

* decouple tour attraction lookup from product location persistence ([7cb2813](https://github.com/skyleedevzero86/IdolGlow/commit/7cb28137b39e30db60a3741cba40f487c54e54e4))

## [0.3.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.2.0...v0.3.0) (2026-04-22)


### Features

* **#7:** unify auth and mail flows, add subscription scheduling and admin extensions ([0588b14](https://github.com/skyleedevzero86/IdolGlow/commit/0588b14ae2c5ca7d825013b4d67f618a8b41a880))
* add admin issue APIs, uploads, and frontend integration ([2682fe3](https://github.com/skyleedevzero86/IdolGlow/commit/2682fe333c9de93998d742bc39aaad65fcd5e812))
* add auth verification audit logs and management endpoint ([ead402e](https://github.com/skyleedevzero86/IdolGlow/commit/ead402ec8fe9c0885e23ca201bd0c261233d50fd))
* add changelog policy and github release automation ([07cf897](https://github.com/skyleedevzero86/IdolGlow/commit/07cf897f3edc4527b20085ddd4094eba907c983a))
* add event admin API  작성 ([075e768](https://github.com/skyleedevzero86/IdolGlow/commit/075e768aaf9fe258adbd700055da8665a2b655c1))
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
* SMTP 메일 발송이나 외부 이메일 서비스 기능 추가 ([bb14b65](https://github.com/skyleedevzero86/IdolGlow/commit/bb14b65942c2202be35f1134a80d3c7a9f33a704))
* 개발서버 mysql 추가 ([d7a0bb1](https://github.com/skyleedevzero86/IdolGlow/commit/d7a0bb14da7022c3248a04f08b877e80522203d7))
* 결제·운영 GraphQL/XLSX 확장 및 관리자 화면 페이징 정비 ([b3b2a7d](https://github.com/skyleedevzero86/IdolGlow/commit/b3b2a7d07ebec4e9ec270cc1ad3d29fbdaf5330d))
* 공지게시판 view count 추가.. ([894812b](https://github.com/skyleedevzero86/IdolGlow/commit/894812be8a4b68ee4220943acd180320440526d2))
* 관리자 이벤트 API 경로 확장 및 전체 상태 페이지네이션 추가 ([745dd65](https://github.com/skyleedevzero86/IdolGlow/commit/745dd65f1bf008e60ae61b17b2aaf5aa59df4b81))
* 구독 예약 발송 관리 추가 및 OAuth2/JWT 인증 충돌 수정 ([d0080e1](https://github.com/skyleedevzero86/IdolGlow/commit/d0080e145871d878caae6bfe3011bce88afb98d7))
* 구독 저장 및 관리자 구독관리 기능 추가 ([6e055f6](https://github.com/skyleedevzero86/IdolGlow/commit/6e055f646b155c71ce1646692e0d14a1e3af092c))
* 구독 저장 및 관리자 구독관리 기능 추가 ([b7ce4f3](https://github.com/skyleedevzero86/IdolGlow/commit/b7ce4f3f66644fbdd5994c7c315cff5e09065663))
* 마크다운 에디터 게시판 API 및 WebSocket 실시간 동기 추가 ([bd211b6](https://github.com/skyleedevzero86/IdolGlow/commit/bd211b65444407a12c103c06f54ad0d2bc947580))
* 사용자  리스트 추가 ([e7ad9de](https://github.com/skyleedevzero86/IdolGlow/commit/e7ad9de6868c617f2aedc1edbedb3503acc37db0))
* 사용자 정보 조회/닉네임 수정 + MyPage 집계 API ([4fc2dda](https://github.com/skyleedevzero86/IdolGlow/commit/4fc2ddabfd665478a407916c7a02a0e90663327d))
* 사이트 도메인·배너·팝업·메인이미지 관리 API 추가 ([073e778](https://github.com/skyleedevzero86/IdolGlow/commit/073e7780462c803bb17b394456636df44ad8d358))
* 사이트 콘텐츠 관리자와 홈 화면 연동 ([d33d078](https://github.com/skyleedevzero86/IdolGlow/commit/d33d0785f1ac7bea9463f8f44748a315033c0c6d))
* 상품·옵션·찜·랭킹 조회에 이미지 URL·리뷰 미리보기 필드 추가 ([8c57c4e](https://github.com/skyleedevzero86/IdolGlow/commit/8c57c4e4c863449fd91147d0956f6c0b361c9dd9))
* 신뢰 리뷰, 매진 웨이팅, 토스 PG·환불 이력 ([a063a49](https://github.com/skyleedevzero86/IdolGlow/commit/a063a49301b5884e2e0d4ef65948f1f2e4df6688))
* 실시간 서버상태 기능 추가 ([9b3c763](https://github.com/skyleedevzero86/IdolGlow/commit/9b3c763dbc8233c263e14c3e113d1041ae75e1a8))
* 알림  설정/필터/전체읽음/만료예정 알림 추가 고도화 ([0c5d866](https://github.com/skyleedevzero86/IdolGlow/commit/0c5d866619ec917c6c81542a4faf3d1356550e11))
* 역활 분리 모델 분리1 (dto분리) ([21d083f](https://github.com/skyleedevzero86/IdolGlow/commit/21d083fa50d5d0ac6b0a67e881adab1a0c0958a9))
* 역활 분리 모델분리2 (dto 분리) ([c0935ad](https://github.com/skyleedevzero86/IdolGlow/commit/c0935add2c845c254f288551dd3f59511ced156f))
* 역활 분리 모델분리3 (dto 분리) ([a956c88](https://github.com/skyleedevzero86/IdolGlow/commit/a956c8844a1d4613662057513334731a74399e2b))
* 외부 캘린더 연동 및 방문 리마인더 VALARM ([a9947ef](https://github.com/skyleedevzero86/IdolGlow/commit/a9947ef268e255e665e636a7398ce9222dd1e208))
* 운영 분석 API·CSV·감사 로그·주말 제외 슬롯 생성 ([feb8f04](https://github.com/skyleedevzero86/IdolGlow/commit/feb8f04497e002515884857c779eb19845ef5bc7))
* 운영 분석/결제/알림/SSE/캘린더 연동/리뷰 시스템 통합 구현 [#2](https://github.com/skyleedevzero86/IdolGlow/issues/2) ([5812435](https://github.com/skyleedevzero86/IdolGlow/commit/58124354388aa6739e6b2861100d1b5effce9f42))
* 이메일 기반 아이디 찾기와 임시 비밀번호 복구 흐름 강화 ([07950e6](https://github.com/skyleedevzero86/IdolGlow/commit/07950e60877be795f3056afcac5e938c019c6cff))
* 젠킨스 dev 설치용 구독 설계테이블 ([2014ffa](https://github.com/skyleedevzero86/IdolGlow/commit/2014ffa71214f2af5c61fe5dd21c8f3921c30d03))
* 젠킨스파일 셋팅 ([354454c](https://github.com/skyleedevzero86/IdolGlow/commit/354454cfe5e37ba387fe8d14408fff383787d18c))
* 젠킨스파일 수정 ([e08e456](https://github.com/skyleedevzero86/IdolGlow/commit/e08e456374002da8c5ed911296bff8ed0adf19ac))
* 추천패키지 추가 ([cefff27](https://github.com/skyleedevzero86/IdolGlow/commit/cefff272bdf76d22e9ed587eaad4be2f444e935b))
* 카운트 기능 추가 ([b99dc71](https://github.com/skyleedevzero86/IdolGlow/commit/b99dc7163b1f73ed8908daac7d01eef3b1c9ab4f))
* 토스 웹훅에서 결제 조회·상태 반영 및 가상계좌 등 비동기 완료 처리 ([2d408c2](https://github.com/skyleedevzero86/IdolGlow/commit/2d408c2cfded70c8608c15e1b178d3ea7161f667))
* 파일 삭제처리 ([a8f6cb7](https://github.com/skyleedevzero86/IdolGlow/commit/a8f6cb7358b5a4b4d7e3a0822d0f8b2b350063cc))
* 파일 업로드 로컬 스토리지 서버 셋팅 처리기능 추가 ([028cf74](https://github.com/skyleedevzero86/IdolGlow/commit/028cf7407828edeb1041671ea946c01e25e2e857))
* 프로젝트생성 ([55f53cc](https://github.com/skyleedevzero86/IdolGlow/commit/55f53cce3d35a87c8c3f52aa491aa92be394a62f))
* 플랫폼 인증 이식·역할 정리·가입 즉시 사용 ([cbe676d](https://github.com/skyleedevzero86/IdolGlow/commit/cbe676db18a61e77c4cec2b352eef07d00c3e5fb))
* 플랫폼 인증·콘텐츠 운영·웹진/에디터·결제 처리 통합 및 아키텍처 안정화 [#4](https://github.com/skyleedevzero86/IdolGlow/issues/4) ([4a0482b](https://github.com/skyleedevzero86/IdolGlow/commit/4a0482b7005f5b3a1341ef4ab036ecaac6356f22))
* 플랫폼 인증·콘텐츠 운영·웹진/에디터·결제 처리 통합 및 아키텍처 안정화 [#5](https://github.com/skyleedevzero86/IdolGlow/issues/5) ([5e67e85](https://github.com/skyleedevzero86/IdolGlow/commit/5e67e859fdcb2f5817d65605df2d4886adb44b63))
* 회원가입 페이징 추가.. ([a62f2b2](https://github.com/skyleedevzero86/IdolGlow/commit/a62f2b29b445c21909fd1e8d80777d37b01df5ab))


### Bug Fixes

* @Lob → @JdbcTypeCode(LONGVARCHAR)로 Hibernate 7 스키마 검증 오류 수정 ([55f5319](https://github.com/skyleedevzero86/IdolGlow/commit/55f5319f5dfb9bd6ad2024e6bd338f87d0a5f4af))
* 5173 포트추가 ([dfe467d](https://github.com/skyleedevzero86/IdolGlow/commit/dfe467d619bb8be1d13a43c17859cce266eedd9b))
* CHAR 컬럼 스키마 검증 오류 수정 및 마크다운 에디터 기능 반영 ([572c1c0](https://github.com/skyleedevzero86/IdolGlow/commit/572c1c05c9347295b2b4da83f61db766e02388ef))
* DB V23 감사 로그 스키마 및 에디터 이미지 업로드 폴백 ([7a86c3f](https://github.com/skyleedevzero86/IdolGlow/commit/7a86c3f0d2260e22ce4a36a823634cff6ddfa20e))
* DB에서 Flyway 체크섬 불일치 시에도 기동되도록 처리 ([429b97e](https://github.com/skyleedevzero86/IdolGlow/commit/429b97e857169ff877f51fecae6929bc54a978f7))
* GraphQL기능 확장 겸 미리보기 기능 개선 ([18df609](https://github.com/skyleedevzero86/IdolGlow/commit/18df6099b4f72c6c4011e2fc247e4a81ea04df01))
* introduction 컬럼 확장을 Flyway  무결성 위반 시 400 응답 ([2700019](https://github.com/skyleedevzero86/IdolGlow/commit/270001995b735d19e6a55809a2564d0b7bf0c5e2))
* Jackson 3.x 패키지 마이그레이션 ([83bef4a](https://github.com/skyleedevzero86/IdolGlow/commit/83bef4a257baddb56d2763fd013c00043c134343))
* Kotlin 컴파일 오류 수정 및 backend build 복구 ([6229ef7](https://github.com/skyleedevzero86/IdolGlow/commit/6229ef7dcc1827a04945aa8041efa456548bda2e))
* local 프로필에서 images 테이블 누락으로 부팅 실패하지 않도록 조정 ([0dad13d](https://github.com/skyleedevzero86/IdolGlow/commit/0dad13d2e41ed6295481100806463e87b2977043))
* MinIO 버킷 보장·503 응답·업로드 API 오류 메시지 노출 ([8abd25c](https://github.com/skyleedevzero86/IdolGlow/commit/8abd25cb33463e489e2fd2e52a03013857ba5f42))
* MinIO 프로필 이미지 공개 읽기 정책 및 프로필 미리보기 오류 처리  또는 영문 선호 ([12c7cf9](https://github.com/skyleedevzero86/IdolGlow/commit/12c7cf994ff4f3ee96692dccdbf47617bccaff55))
* normalize survey read response to no-content ([233ab76](https://github.com/skyleedevzero86/IdolGlow/commit/233ab76b8f6eb8bfb0a97ffd18eaf6ac8d8dc500))
* product_review_helpful_votes/reports 테이블 updated_at 누락 보정 ([0260986](https://github.com/skyleedevzero86/IdolGlow/commit/026098684efba3308d8571e0484129dc1ac79119))
* restore v13 migration checksums and stabilize webzine boot ([dfec1c9](https://github.com/skyleedevzero86/IdolGlow/commit/dfec1c982bb64635c1d0a29328c85075f723d6a5))
* sns로그인 프로필 사진,닉네임 변경 기능 추가 ([955c750](https://github.com/skyleedevzero86/IdolGlow/commit/955c7504a4571c2451bacee5e46989661bd9b0b8))
* Spring Boot 4 Jackson 타입 정합성 및 예외 처리 개선 및 운영에 맞게 Postgresql 버전으로 마이그레이션 셋팅 ([d9fd226](https://github.com/skyleedevzero86/IdolGlow/commit/d9fd2268be800347627f6c5894cf6ca09b9b9fbc))
* 공지/에디터 저장 시 introduction 길이 제한으로 400 나던 문제 수정 ([3a87cb1](https://github.com/skyleedevzero86/IdolGlow/commit/3a87cb15a26a7e5b0d3a77668431f58158de0c7e))
* 관리 목록 페이지 카운트와 JOIN FETCH 충돌 수정 및 Windows kapt 빌드 안정화 ([d692261](https://github.com/skyleedevzero86/IdolGlow/commit/d692261d5193e7a3288842509b0b4e5bda11c6e0))
* 관리자 회원관리 화면 가시성 개선 ([3fe184a](https://github.com/skyleedevzero86/IdolGlow/commit/3fe184aae0607efb1f938647db9efe432971cd24))
* 구글 OAuth 로그인 무한 리다이렉트 수정 ([6cc168e](https://github.com/skyleedevzero86/IdolGlow/commit/6cc168e1e845fa738be981222c34e57065502238))
* 구글로그인 및 에러방지 수정 ([5662a5c](https://github.com/skyleedevzero86/IdolGlow/commit/5662a5caf166a7ad89e19158094f23d8ce3efb2e))
* 누락된 import코드처리 ([2932689](https://github.com/skyleedevzero86/IdolGlow/commit/29326891f1579725614edfe31bea1f7826a26f0d))
* 닉네임 한글기능 추가 ([1bfb966](https://github.com/skyleedevzero86/IdolGlow/commit/1bfb96674def4cdb557570b1d7e1023f81eeb6ae))
* 로그인 에러처리 ([a2277f2](https://github.com/skyleedevzero86/IdolGlow/commit/a2277f2894b6149521e7d3d7f9de6f22cf4a3c0a))
* 로컬 부팅 시 JPA 임베디드 매핑과 데이터 워밍 오류 수정 ([2eed004](https://github.com/skyleedevzero86/IdolGlow/commit/2eed00408325ff05537ce17e6b7b3c49397e292e))
* 로컬 부팅과 시크릿 설정 정리 ([c719dc4](https://github.com/skyleedevzero86/IdolGlow/commit/c719dc431b7fe99ccb0cbaf0601035e1f9161838))
* 버전변경 jdk25 버전 업그레이드 작업 ([d87a8e6](https://github.com/skyleedevzero86/IdolGlow/commit/d87a8e62a83d1c9566047cd8a25770986e2418f0))
* 사용자 이름 과  프로필 이미지 가져오기 처리하기 ([34381c1](https://github.com/skyleedevzero86/IdolGlow/commit/34381c1716b11517b010a983692a62ad84c48bf0))
* 수정 v2버전정리 ([022fd0e](https://github.com/skyleedevzero86/IdolGlow/commit/022fd0e62f4769c95a6bfc728cb96727ae58717d))
* 에디터 이미지 업로드 MinIO 실패 시 폴백 ([5e792d8](https://github.com/skyleedevzero86/IdolGlow/commit/5e792d8f8d0e1586b75c3b4509312a3ceba055b7))
* 이미지 업로드·HTTP 상태 예외 처리 개선 ([33ad0ef](https://github.com/skyleedevzero86/IdolGlow/commit/33ad0ef0e2e7eafc4f4bee11b9d3647e22ac00b6))
* 컬럼변경 ([d7134e0](https://github.com/skyleedevzero86/IdolGlow/commit/d7134e09c669156b60609f030e6342647bb22acd))
* 코드개선 ([29b200f](https://github.com/skyleedevzero86/IdolGlow/commit/29b200f1e8934dd28d5e5cd5076f78d3f2b7ebb3))
* 테스트 코드정리 ([754b816](https://github.com/skyleedevzero86/IdolGlow/commit/754b816c534baf3dba1de77af7300af6ed1a1a10))
* 패키지 정리 ([bc37707](https://github.com/skyleedevzero86/IdolGlow/commit/bc37707a9f8b1c30b08cf6616f46ba6a2e37a3e6))
* 포트번호 수정 ([aa2978a](https://github.com/skyleedevzero86/IdolGlow/commit/aa2978a055d88195b932430858e8051e7cefc64a))
* 플랫폼 인증용 AuthenticationManager 빈 등록 & SecurityFilterChain 순서로 플랫폼 체인 도달 가능하게 수정 ([482c4de](https://github.com/skyleedevzero86/IdolGlow/commit/482c4debbc266135b491e198f0768ca442555aba))
* 필요없는 이그노어 추가 ([b5bb5e7](https://github.com/skyleedevzero86/IdolGlow/commit/b5bb5e763af1362e4172944760645f92f4f11e3a))


### Refactoring

* introduce ports and adapters in admin issue module ([c709d69](https://github.com/skyleedevzero86/IdolGlow/commit/c709d693e3392bffe015aa192d52944e24c781b4))
* JWT 및 메일 설정 단일화 ([734e5d8](https://github.com/skyleedevzero86/IdolGlow/commit/734e5d8f59d8ddced13bcc10bc4fea2bcfac205d))
* 구독 모듈 포트/어댑터 구조 정리 ([8aad799](https://github.com/skyleedevzero86/IdolGlow/commit/8aad799368539c8387c53c68cd7019324692d30d))
* 메일 발송 설정 단일화 ([b3912de](https://github.com/skyleedevzero86/IdolGlow/commit/b3912de2e8f8373b789ba73e2d99f224a2eb77d2))


### Documentation

* Swagger API 설명 보강 및 DTO 스키마 문서화 ([fb574db](https://github.com/skyleedevzero86/IdolGlow/commit/fb574db6d37063f8125210a57a65657dbfc7f557))
* V11 마이그레이션 주석 보강 ([6f5b3c7](https://github.com/skyleedevzero86/IdolGlow/commit/6f5b3c7307f43b1dbb591414597641f531e35aca))

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
