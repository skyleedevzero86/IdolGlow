# Changelog

이 문서는 `IdolGlow` 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 참고했고, 날짜 기준으로 정리했습니다.

## [0.12.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.11.2...v0.12.0) (2026-10-25)


### Features

* **#65:** 공연 매칭, 운영 승인, 근거 검색, 결제·구독 알림을 추가 ([8c9e548](https://github.com/skyleedevzero86/IdolGlow/commit/8c9e548ccb751a49d8c05f1850d3dbf042d1ed5b))
* 공연 공급처 데이터를 배치로 모아 같은 공연은 하나의 식별자로 묶는다 ([ad2d19c](https://github.com/skyleedevzero86/IdolGlow/commit/ad2d19cfa40c1b0a04add52bd7fe3aa35f86512e))
* 공연 매칭 충돌을 조회·비교·병합 제안하는 MCP Tool을 추가 ([829d7d5](https://github.com/skyleedevzero86/IdolGlow/commit/829d7d510e552428412fea20cebbb4872ba1c658))
* 공연 메타에 언어별 제목·소개를 두고 없는 언어는 영어·한국어로 대체하기 ([de1da53](https://github.com/skyleedevzero86/IdolGlow/commit/de1da5386bfcdc3e096706ea40e05a7b18d817e7))
* 멈춘 승인·환불·빌링 건을 하루 한 번만 관리자에게 알림처리 ([d248f17](https://github.com/skyleedevzero86/IdolGlow/commit/d248f1764cd6c6c81c03c4c0ec9f60d6a1d3bede))
* 반복된 관리자 작업 상위 5개를 집계하고 자동화 후보를 선택 ([bcaa7fb](https://github.com/skyleedevzero86/IdolGlow/commit/bcaa7fb6c1d12433dfe810152de18a3dcd933943))
* 운영 변경은 다른 사람이 승인한 뒤에만 적용하고 되돌릴 수 있게 하기 ([1892c86](https://github.com/skyleedevzero86/IdolGlow/commit/1892c86e07088ef106df5c8478bdc82329781799))
* 웹진·상품 문단을 키워드와 벡터로 같이 검색 ([f441fba](https://github.com/skyleedevzero86/IdolGlow/commit/f441fbaeb548d3e7cc4a028a562b8a8fdfd62305))


### Bug Fixes

* 1시간 넘은 빌링 승인 확인 테스트가 실행 시각에 따라 깨지지 않게 처리 ([c17cce8](https://github.com/skyleedevzero86/IdolGlow/commit/c17cce8b19176944a6e0cc62c29737077849445d))
* 구독 메일은 발송 이력을 먼저 선점하고 트랜잭션 밖에서 보내기 ([d452788](https://github.com/skyleedevzero86/IdolGlow/commit/d4527883a53f45adf21d5a0b5eacce494308325c))
* 근거 검색 문단 설명을 테이블 생성 SQL에 두기 ([b54a82a](https://github.com/skyleedevzero86/IdolGlow/commit/b54a82a37de921e4791c230d0cc41d30660a5282))

## [0.11.2](https://github.com/skyleedevzero86/IdolGlow/compare/v0.11.1...v0.11.2) (2026-10-23)


### Bug Fixes

* **#54:** reconcile unknown billing charges and delete Toss billing keys outside locks ([823f723](https://github.com/skyleedevzero86/IdolGlow/commit/823f7235fa7d4a5892e5d06eb4369c077514b533))
* 구독이 없어도 기본 카드를 지우면 다음 카드를 기본으로 업로드 ([f583e31](https://github.com/skyleedevzero86/IdolGlow/commit/f583e31d888bf3547c69a51b83a628266842e888))
* 단건 결제 실패 알림은 정기구독 문구와 나누어 보낸다 ([6a3b8f5](https://github.com/skyleedevzero86/IdolGlow/commit/6a3b8f5944386488612e344a2731e7c7ed81e5cf))
* 대기 중인 정기결제 승인을 토스 조회로만 다시 맞추고 새 승인은 보내지 않게 처리 ([4bcd658](https://github.com/skyleedevzero86/IdolGlow/commit/4bcd6580753b5639226bc611e5d06121e07d86b6))
* 자동결제 승인만 토스 권장 60초 읽기 타임아웃을 쓰게 처리 ([47e01a6](https://github.com/skyleedevzero86/IdolGlow/commit/47e01a69f168d494f277643866706cb6cee7a55b))
* 정기결제 토스 조회와 승인을 트랜잭션 밖에서 호출 ([e23c751](https://github.com/skyleedevzero86/IdolGlow/commit/e23c751c378fa92c8561e5b3e2a50c5a34304e93))
* 정기결제는 결과를 모르면 실패로 기록하지 않고 대기 중 시도가 있으면 다시 승인하지 않게 처리 ([c369a39](https://github.com/skyleedevzero86/IdolGlow/commit/c369a39ebd9e29fc0aae52a5b3a64839ff37a9c8))
* 카드 비활성화 때 토스 빌링키도 지우고 결과를 모르면 삭제 대기로 남긴다 ([e6caa1d](https://github.com/skyleedevzero86/IdolGlow/commit/e6caa1df0d34de630a2cbb9f8093936dd85919e6))

## [0.11.1](https://github.com/skyleedevzero86/IdolGlow/compare/v0.11.0...v0.11.1) (2026-10-23)


### Bug Fixes

* **#47:** 승인 처리 중 결제와 웹훅을 토스 조회 결과로만 정리한다 ([4ecfbc9](https://github.com/skyleedevzero86/IdolGlow/commit/4ecfbc9b34be0acfaf8f62b86c4831e1614d878a))
* 승인 처리 중 결제를 시간만으로 실패 처리하지 않고 조회 결과로만 정리 ([1094dca](https://github.com/skyleedevzero86/IdolGlow/commit/1094dcaa0b30304ac9682289985e6b565dd4166a))
* 실패로 정리된 결제에 늦게 확인된 토스 승인은 관리자 확인 로그만 작성 ([14fe043](https://github.com/skyleedevzero86/IdolGlow/commit/14fe0437fcb51a75bac0e761ffacbc3a9fc2566f))
* 토스 웹훅은 결제 조회를 끝낸 뒤 짧게 잠가 반영 ([7bce97b](https://github.com/skyleedevzero86/IdolGlow/commit/7bce97beccb9e900d11946c615cad522a31bcd8b))

## [0.11.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.10.2...v0.11.0) (2026-10-23)


### Features

* 환불 처리 중 상태를 두고 토스 결과 모름 판단을 공통으로 뺀다 ([6eb8015](https://github.com/skyleedevzero86/IdolGlow/commit/6eb801500648fdedcaf5743d85d40957ad9a6b2b))


### Bug Fixes

* 관리자 환불 재시도도 토스 조회 뒤 트랜잭션 밖에서 보내고 거절은 기록만 처리 ([a59ce3e](https://github.com/skyleedevzero86/IdolGlow/commit/a59ce3e75f8f21cd3c299c860b9c5962b0f4ee86))
* 토스 환불을 락 밖에서 처리하고 결과를 모르면 조회해 맞춘다 [#46](https://github.com/skyleedevzero86/IdolGlow/issues/46) ([01c0c8f](https://github.com/skyleedevzero86/IdolGlow/commit/01c0c8fb98e087d224101e20b690a3854514325b))
* 토스 환불을 트랜잭션 밖에서 호출하고 결과를 모르면 환불 처리 중으로 남긴다 ([c8ee4fe](https://github.com/skyleedevzero86/IdolGlow/commit/c8ee4feb8bc1136da2ae845133bcf1f012db886c))
* 환불 결과를 모르면 토스를 먼저 조회해 맞추고 환불 실패가 취소를 깨지 않게 처리 ([ac1f73f](https://github.com/skyleedevzero86/IdolGlow/commit/ac1f73f01e0a4a755e989ddfbc4b5e199586cac6))
* 환불 응답이 전액 취소일 때만 끝내고 정산 실패가 승인 응답을 깨지 않게 처리 ([ff348b0](https://github.com/skyleedevzero86/IdolGlow/commit/ff348b07227999625c58c45413d4b15f7250b305))

## [0.10.2](https://github.com/skyleedevzero86/IdolGlow/compare/v0.10.1...v0.10.2) (2026-10-22)


### Bug Fixes

* **#43:** 토스 결제 승인과 정기결제 구매자 키를 서버 기준으로 검증한다 ([353777d](https://github.com/skyleedevzero86/IdolGlow/commit/353777d1e5969b1ed81c4a7515c6d04fe96706d2))
* customerKey 충돌 후 재조회를 별도 트랜잭션에서 한다 ([f7b6cd7](https://github.com/skyleedevzero86/IdolGlow/commit/f7b6cd7a10680cb23b8349a728aefff2f9b0bdc2))
* 마이페이지 결제 취소는 예약 다음 결제 순서로 잠그고 잠금 후 상태로 확인처리 ([ef46b79](https://github.com/skyleedevzero86/IdolGlow/commit/ef46b7988e3263e731a2b981584b5d23cbeb7f8c))
* 모의 결제 웹훅을 로컬·테스트 프로필의 MOCK 결제로만 제한한다 ([cf0c978](https://github.com/skyleedevzero86/IdolGlow/commit/cf0c978f05a620a3b0cced789a33142056c8faad))
* 빈 모의 웹훅 시크릿과 ShedLock 클래스 누락으로 서버 다시재가동 ([333b9e3](https://github.com/skyleedevzero86/IdolGlow/commit/333b9e3abc5f9d80c5ca25dd75daa6e86aa61223))
* 정기결제 customerKey를 사용자별 무작위 UUID로 저장 ([955df36](https://github.com/skyleedevzero86/IdolGlow/commit/955df36ba45c7ad7e3cc177f7e184d545b3ed309))
* 정기결제 구독 금액을 서버 플랜 가격으로만 정한다 ([a22e7a7](https://github.com/skyleedevzero86/IdolGlow/commit/a22e7a718254ebd2667207c837894b1486add812))
* 토스 결제 상태 웹훅은 서명 대신 결제 조회로 검증한다 ([e4509f7](https://github.com/skyleedevzero86/IdolGlow/commit/e4509f729c0eb2a209d4f788b349585d634b2a59))
* 토스 승인 응답이 DONE이고 금액이 일치할 때만 결제를 성공 처리 ([8caf9db](https://github.com/skyleedevzero86/IdolGlow/commit/8caf9dbcc302435365031c0c01e64c2e6bb5f79a))


### Tests

* 백엔드 테스트에 given when then 주석과 한글 표시 이름을 정리 ([6e69cac](https://github.com/skyleedevzero86/IdolGlow/commit/6e69cac0af2a278518e4d8a2584512a93b217813))

## [0.10.1](https://github.com/skyleedevzero86/IdolGlow/compare/v0.10.0...v0.10.1) (2026-10-20)


### Bug Fixes

* **#40:** 토스 승인을 트랜잭션 밖에서 처리하고 만료·재시도 경로를 정리한다 ([6d1f253](https://github.com/skyleedevzero86/IdolGlow/commit/6d1f25369410acaff0eb97628e0956783ba0c376))
* 결제 결과 메일은 트랜잭션이 커밋된 뒤에만 보낸다 ([14e005a](https://github.com/skyleedevzero86/IdolGlow/commit/14e005a87bf0dd67bf2a7528d1a1e5dda2a454b2))
* 만료 예약 정리 시 다른 예약이 잡은 슬롯은 건드리지 않기 ([3521aaa](https://github.com/skyleedevzero86/IdolGlow/commit/3521aaa23d8c3b6236fa124fbaef7e60c0fbba56))
* 만료 예약은 한 건이 실패해도 나머지를 처리한다 ([1710979](https://github.com/skyleedevzero86/IdolGlow/commit/1710979ca214685e747865e1955070ac476b9d5d))
* 멱등 키가 없으면 결제마다 고정된 키로 토스 승인을 요청처리 ([7091631](https://github.com/skyleedevzero86/IdolGlow/commit/7091631f4349cf57e52aa9e48bb2637d20fa7802))
* 토스 결제의 참조값을 pay_toss_ 로 시작해 목업 결제와 구분처리 ([4fc1f3a](https://github.com/skyleedevzero86/IdolGlow/commit/4fc1f3aae178a4871c4b4e3efc18adf07ec25fc7))
* 토스 승인을 트랜잭션 밖에서 호출하고 처리 중 결제를 조회로 정리 ([fcdaf33](https://github.com/skyleedevzero86/IdolGlow/commit/fcdaf33d665171636bd1fdf3a605a1928ab21544))


### Documentation

* README 한계에 승인 처리 중 결제의 늦은 승인 웹훅 경로를 추가 ([c34696d](https://github.com/skyleedevzero86/IdolGlow/commit/c34696d2ca3b222781e62af5588ce1f11353b747))

## [0.10.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.9.0...v0.10.0) (2026-10-20)


### Features

* 주문번호로도 예약을 먼저 잠그는 경로를 추가 ([4a5bf10](https://github.com/skyleedevzero86/IdolGlow/commit/4a5bf109dae2fe20c9ea8fd2596e86f12d35be1e))


### Bug Fixes

* **#35:** 결제 락 순서와 만료 자동 환불을 맞춘다 ([7e59707](https://github.com/skyleedevzero86/IdolGlow/commit/7e59707e65720dccc60e338a3770f3fa32224e49))
* docker-compose 비밀번호와 기본 시크릿을 infra/.env 필수값으로 변경 ([8a6a441](https://github.com/skyleedevzero86/IdolGlow/commit/8a6a4411b7aee929f306cd22ecac6528924f6339))
* 결제 웹훅 처리의 잠금 순서를 예약 먼저로 맞춘다 ([1031f8c](https://github.com/skyleedevzero86/IdolGlow/commit/1031f8c1885e2f0ae96ae02def1e8dcd8cf3ce11))
* 날짜 파싱 순환 호출을 끊고 전역 JSON 시각 포맷을 뺀다 ([114c8e2](https://github.com/skyleedevzero86/IdolGlow/commit/114c8e2d6dec84c819bf322d1c422aef865b9125))
* 내려간 MinIO 공식 이미지 대신 pgsty 이미지를 작성 ([70a47cd](https://github.com/skyleedevzero86/IdolGlow/commit/70a47cd4ed77737616d0e78a26d28f7ceee80822))
* 만료 예약 자동 환불이 실패해도 실패 기록을 처리 ([df9bb62](https://github.com/skyleedevzero86/IdolGlow/commit/df9bb62059b9c6ceae4a33fcff2c68ac727ba9aa))
* 만료·취소된 예약은 토스 승인을 요청하지 않게 처리 ([ae11a94](https://github.com/skyleedevzero86/IdolGlow/commit/ae11a946adc9178b7af71b6d59b6905fc80b7303))
* 빌링 결제 기록을 별도 빈으로 분리해 REQUIRES_NEW가 적용되게 한다 ([3b5bcf6](https://github.com/skyleedevzero86/IdolGlow/commit/3b5bcf6ce4b66d139d13fcc5edc9320e5641495c))
* 빌링 실패 알림을 커밋 뒤로 빼고 날짜는 표시만 포맷한다 ([bec6be4](https://github.com/skyleedevzero86/IdolGlow/commit/bec6be485b0013166ba5171dbbe5128790da3721))
* 빌링 암호화 키 기본값을 코드에서 제거 ([9019d59](https://github.com/skyleedevzero86/IdolGlow/commit/9019d5980b778f1ff259869f15dee7bde4d8f179))
* 토스 결제 승인도 예약에서 결제 순서로  잠금 ([61ba567](https://github.com/skyleedevzero86/IdolGlow/commit/61ba567b810c4a33b8d764af63a788034d7e025d))
* 토스 승인 뒤 예약이 만료되면 시스템 자동 환불처리 ([4a8c00a](https://github.com/skyleedevzero86/IdolGlow/commit/4a8c00aa7c9f5b5d7197feb9c4a9b94d7fe51b3a))
* 토스 웹훅도 예약에서 결제 순서로 정리 ([47efbfb](https://github.com/skyleedevzero86/IdolGlow/commit/47efbfb94c5b543a53fdd524df6c8e397a4890a2))


### Documentation

* README에 결제·예약 한계와 다음 단계 절을 추가 ([6fa64be](https://github.com/skyleedevzero86/IdolGlow/commit/6fa64be8650dfe5c14d99d068f831ced01fea411))


### Tests

* 알림 실패가 결제 FAILED를 롤백하지 않는지 테스트로 고정한다 ([9304412](https://github.com/skyleedevzero86/IdolGlow/commit/93044128dc76a9b515298a9f946c90940512b0d5))

## [0.9.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.8.0...v0.9.0) (2026-10-16)


### Features

* Discord 활동 알림과 릴리즈·CI·MTTR 운영 알림 연동 [#29](https://github.com/skyleedevzero86/IdolGlow/issues/29) ([615bb25](https://github.com/skyleedevzero86/IdolGlow/commit/615bb255d186b869c6631b4a253459a75f8e05be))
* GitHub PR·이슈·CI·릴리즈 다른버전 추가 ([726e56d](https://github.com/skyleedevzero86/IdolGlow/commit/726e56dab6f9542a7e7708c5544e04787495d5fb))
* 릴리즈 배포 버전 검증·CI 상태·MTTR 알림을 Telegram/Discord에 추가 ([3563a44](https://github.com/skyleedevzero86/IdolGlow/commit/3563a443ca3d55383425cc5163666334ff6b45a3))


### Refactoring

* 빌링 기본값을 BillingDefaults로 맞추고 결제 테스트 검증을 강화 ([d71e0fb](https://github.com/skyleedevzero86/IdolGlow/commit/d71e0fb70698b07b5b706900a9f54c44392f5a85))

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
