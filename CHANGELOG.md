# Changelog

이 문서는 `IdolGlow` 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 참고했고, 날짜 기준으로 정리했습니다.

## [0.19.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.18.0...v0.19.0) (2026-11-14)


### Features

* GLOW 지역사회 게시글·좋아요·신고 도메인과 DTO를 추가 ([39b7057](https://github.com/skyleedevzero86/IdolGlow/commit/39b70573c1481cf68a5cc5774f453529b5a8f028))
* GLOW 지역사회 게시판·공통 댓글·알림·메일 발송 기록과 관리자 구독관리 검색 추가 ([#118](https://github.com/skyleedevzero86/IdolGlow/issues/118)) ([3f8762c](https://github.com/skyleedevzero86/IdolGlow/commit/3f8762c224d0d3053b4fbbe16ee2d74d1731bef3)), closes [#119](https://github.com/skyleedevzero86/IdolGlow/issues/119)
* GLOW 지역사회 게시판과 관리자 신고 처리 API를 추가 ([aa3a864](https://github.com/skyleedevzero86/IdolGlow/commit/aa3a864d8f89303b72d6f411683b19a4eac38cd0))
* GLOW 지역사회 글쓰기·조회·신고·블라인드와 추천 검색어 서비스를 추 ([499998a](https://github.com/skyleedevzero86/IdolGlow/commit/499998a1c20712d7b882b8e7b41fa2e032f7f2b0))
* GLOW 지역사회 유스케이스와 저장소·반응·작성자·검색어 생성 포트를 추가 ([fa49eb2](https://github.com/skyleedevzero86/IdolGlow/commit/fa49eb2f0b011010101ecc6919a01a82757e5886))
* GLOW 지역사회 저장소와 추천 검색어 LLM 어댑터를 추가 ([db64f40](https://github.com/skyleedevzero86/IdolGlow/commit/db64f40986d01506353c740b99fc812ffba3dc92))
* 결제 완료와 확인이 필요한 결제를 관리자 앱 알림 발송 ([5fd4702](https://github.com/skyleedevzero86/IdolGlow/commit/5fd470296a3a0d59244cf781e213f35d47177d67))
* 공용 콘텐츠 대상 종류와 페이지 응답을 추가하고 댓글·관리자 알림 종류를 등록 ([60ed444](https://github.com/skyleedevzero86/IdolGlow/commit/60ed444d49b708e10c8a2da97d9ce0502fc1a030))
* 공통 댓글 API와 나의 리뷰 API, 댓글 저장소를 추가 ([0939688](https://github.com/skyleedevzero86/IdolGlow/commit/09396887667b5bed7c7123c45e6f5010906413b1))
* 관리자 구독 관리에서 구독자·발송 이력·메일 발송 기록을 검색 기능 추가 ([df534cf](https://github.com/skyleedevzero86/IdolGlow/commit/df534cf6546647359ae92c012d2f9832687a7e81))
* 관리자 전원에게 앱 알림을 보내는 발행·수신 구조를 추가 ([be0725c](https://github.com/skyleedevzero86/IdolGlow/commit/be0725c1bf28eeafd2c3daef52e8613a6adf8535))
* 댓글·답글 작성과 나의 리뷰, 댓글 이미지 업로드, 댓글 알림 서비스를 추가 ([f7c134e](https://github.com/skyleedevzero86/IdolGlow/commit/f7c134e88249c268d9c2324bbfdb6a7169a687b0))
* 메일 발송 기록 도메인과 메일 종류, 기록 포트를 추가 ([8fe99a1](https://github.com/skyleedevzero86/IdolGlow/commit/8fe99a1bd73075bb53ce1a230c00618118ab1c7d))
* 모든 발송 메일을 기록하는 어댑터와 기록 보관 기간 정리를 추가 ([db6d502](https://github.com/skyleedevzero86/IdolGlow/commit/db6d502673ebc8341aba339e1748f7d064215972))
* 모든 콘텐츠에 다는 공통 댓글 도메인과 포트를 추가 ([e1d10bd](https://github.com/skyleedevzero86/IdolGlow/commit/e1d10bdcf8b4f37b9de1208227feac4632c30877))
* 지역사회·댓글 공개 조회와 관리자 경로 권한을 정하고 권한 거부 응답을 401/403으로 분리 ([ed538e4](https://github.com/skyleedevzero86/IdolGlow/commit/ed538e40a0eb4a52590dec92971d3ee2d37fc820))
* 지역사회·댓글·최근 본 콘텐츠·메일 발송 기록 테이블과 새 알림 종류를 스키마에 추가 ([44ae07e](https://github.com/skyleedevzero86/IdolGlow/commit/44ae07e57cd7cd619fec93ad7924171f92d95cdd))
* 최근 본 콘텐츠를 기록·조회·삭제하는 API를 추가 ([e12c64f](https://github.com/skyleedevzero86/IdolGlow/commit/e12c64f0bbb956c0f4342f3eb6c0d32cacfe2a29))
* 추천·소식지·인증·결제 메일에 종류와 발송 이력 ID를 달아 발송 ([3c59331](https://github.com/skyleedevzero86/IdolGlow/commit/3c59331f790f4156bc46a428b22ab1994b5875f9))


### Bug Fixes

* 문화시설정보 API 초당 한도 초과 시 잠시 호출을 멈춰 요청이 쏟아지지 않게 한다 ([bca24db](https://github.com/skyleedevzero86/IdolGlow/commit/bca24db81b5bc7b8f191b698fa60741f390b4052))


### Tests

* GLOW 지역사회와 공통 댓글의 포스트그레스·보안 통합 테스트를 추가 ([8be99b2](https://github.com/skyleedevzero86/IdolGlow/commit/8be99b2c4fcb3252f1e814bd2e4ac9b50ca68e60))

## [0.18.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.17.0...v0.18.0) (2026-11-14)


### Features

* **#115:** add personalized recommendation mail workflow ([64b3120](https://github.com/skyleedevzero86/IdolGlow/commit/64b3120bd018013496408a09fe06cbd02bcbe8bb))
* add GLOW community data APIs ([9e164dd](https://github.com/skyleedevzero86/IdolGlow/commit/9e164ddaf27778370c76da816336ba242cb217b1))
* add GLOW content sections ([af1d436](https://github.com/skyleedevzero86/IdolGlow/commit/af1d436a505e389566adf22f6bef880b9021990f))
* **frontend:** restore GLOW data APIs and content sections ([#114](https://github.com/skyleedevzero86/IdolGlow/issues/114)) ([e2d011c](https://github.com/skyleedevzero86/IdolGlow/commit/e2d011c68eb705693ec5ebbe7d1365337bf0a62b))
* GraphQL 추천 응답에 추천 이유와 문항 추천 신호를 추가한다 ([3ccf7bc](https://github.com/skyleedevzero86/IdolGlow/commit/3ccf7bca109f530f399ca2685b869670cff7efdf))
* restore GLOW data APIs and content sections ([#114](https://github.com/skyleedevzero86/IdolGlow/issues/114)) ([ad92ba1](https://github.com/skyleedevzero86/IdolGlow/commit/ad92ba187a7883edadca808678a6c60682811b82))
* 관리자 발송 기록에 발송·실패·건너뜀 건수를 함께 보여 준다 ([dae1d01](https://github.com/skyleedevzero86/IdolGlow/commit/dae1d01b8054eec64215da6b672867c630beac0f))
* 동의한 회원에게 설문 취향 또는 인기순 추천 상품 메일을 주기마다 한 번씩 보낸다 ([1634587](https://github.com/skyleedevzero86/IdolGlow/commit/1634587466f927f47ac2e5d882bebcf04e5d5419))
* 상품 추천 메일 구독 테이블과 도메인, 수신 거부 토큰을 추가한다 ([23fb9d0](https://github.com/skyleedevzero86/IdolGlow/commit/23fb9d00116864ea39f57022e137d6b329a146d4))
* 설문 문항마다 추천에 쓸 신호 종류를 관리자가 지정할 수 있게 한다 ([38a0304](https://github.com/skyleedevzero86/IdolGlow/commit/38a03040d2e44614c32d59068a4a8a8b9bd7b689))
* 설문지 폼 응답을 추천 신호로 바꿔 회원 추천과 같은 점수로 상품을 고른다 ([31529d0](https://github.com/skyleedevzero86/IdolGlow/commit/31529d05734fdfce03d00e837eafa0c139248271))
* 추천 메일 수신 동의 2년이 지난 회원에게 동의 상태 안내 메일을 보낸다 ([7e6be0c](https://github.com/skyleedevzero86/IdolGlow/commit/7e6be0cba79b38ea0f31cdfbf4abb60ab7786071))
* 추천 점수에 컨셉 한글 태그를 맞추고 추천 이유를 함께 돌려준다 ([baad8b2](https://github.com/skyleedevzero86/IdolGlow/commit/baad8b2088bb9763c62911379ac8138248d8a80f))
* 행사별 언어 번역 누락 현황을 계산해 관리 도구로 보여 준다 ([3134f83](https://github.com/skyleedevzero86/IdolGlow/commit/3134f83d441ec5c113c33e24d018e2ff941f757c))
* 회원이 상품 추천 메일 수신에 동의하고 로그인 없이 링크로 거부할 수 있게 한다 ([003a2a0](https://github.com/skyleedevzero86/IdolGlow/commit/003a2a0d741b0347442443894d788e0535d0eb54))


### Bug Fixes

* mysql 빌링키 해시 컬럼 타입 문법 오류로 마이그레이션이 실패하던 문제수정 ([25f35b8](https://github.com/skyleedevzero86/IdolGlow/commit/25f35b8126f5e4df347c3ff8bd8e23dcd868da87))
* 설문 추천 신호 커밋에서 빠진 응답 매핑·detekt 기준·점수 테스트를 추가한다 ([fda3437](https://github.com/skyleedevzero86/IdolGlow/commit/fda3437269f70e8cc97ec73677f7396fcf99f46a))
* 포트번호변경.. ([be52cc2](https://github.com/skyleedevzero86/IdolGlow/commit/be52cc2e95e10ef2930ca22cb3bedc02da52b2fe))

## [0.17.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.16.0...v0.17.0) (2026-11-12)


### Features

* **#100:** 관리자 공통 틀과 트렌드·뉴스레터·설문 결과 화면 추가 ([5fc5b12](https://github.com/skyleedevzero86/IdolGlow/commit/5fc5b125b3447acb7444caeeea98640de84dc1cc))
* **#103:** 관리자 콘텐츠·운영·결제 화면 추가 ([9347763](https://github.com/skyleedevzero86/IdolGlow/commit/93477637b2a2d36a65ed898801ccb4a395d81a34)), closes [#102](https://github.com/skyleedevzero86/IdolGlow/issues/102)
* **#105:** 관리자 상품·예약·회원 화면과 사용자 기사·정기결제 화면 추가 ([3e7109f](https://github.com/skyleedevzero86/IdolGlow/commit/3e7109f675f511e0cc33a09db1f8172c91a7564a)), closes [#104](https://github.com/skyleedevzero86/IdolGlow/issues/104)
* **#108:** add core service pages and styles ([8275de6](https://github.com/skyleedevzero86/IdolGlow/commit/8275de6b138d90e77b942a28a29bcdd06ffc8805))
* **#109:** add application routing and user service pages ([f598bc8](https://github.com/skyleedevzero86/IdolGlow/commit/f598bc8b89b3e263229aa73cd925188105580945))
* **#110:** integrate frontend services and EKS deployment ([cb70541](https://github.com/skyleedevzero86/IdolGlow/commit/cb70541d471b6a598e21cece1ec85ce934985a8b))
* **#98:** 웹진 공통 컴포넌트와 관리자 이슈 기사 화면 추가 ([010231b](https://github.com/skyleedevzero86/IdolGlow/commit/010231b847aa9e694e868117f9e9b4d15564efe2))
* GLOW 날씨 화면 추가 ([7b402eb](https://github.com/skyleedevzero86/IdolGlow/commit/7b402ebcd0c4db4de357ffc95558a9b64cd5392f))
* GLOW 알림 화면 추가 ([71e9fec](https://github.com/skyleedevzero86/IdolGlow/commit/71e9fec1e2020429bd24d293dc22bda9001508ce))
* GLOW 지도 화면 추가 ([447f34e](https://github.com/skyleedevzero86/IdolGlow/commit/447f34edf8a1cfb388b44da9cbe4b52e7f3fae33))
* GLOW 화면 추가 ([8610940](https://github.com/skyleedevzero86/IdolGlow/commit/8610940294f353d91f4899063b643662fe4d65be))
* 검색 모달 공통 컴포넌트 추가 ([3013582](https://github.com/skyleedevzero86/IdolGlow/commit/30135826f2673a31034d9bff0ad95fd58f5480b1))
* 공지 공개 상세 화면 추가 ([f23d4d5](https://github.com/skyleedevzero86/IdolGlow/commit/f23d4d5493f84b43a2e3891cc35150861ba31bdb))
* 공지 화면 추가 ([d757ee0](https://github.com/skyleedevzero86/IdolGlow/commit/d757ee0f397b321393a3cf82b1b37f307d613099))
* 공항 혼잡도 화면 추가 ([726e509](https://github.com/skyleedevzero86/IdolGlow/commit/726e509dac839ac32a0b525e68c49c6034616089))
* 관리자 GLOW 추천 화면 추가 ([6886a0b](https://github.com/skyleedevzero86/IdolGlow/commit/6886a0b82aacc779c5dda491391d3d7356c145b3))
* 관리자 결제 화면 추가 ([dab19a2](https://github.com/skyleedevzero86/IdolGlow/commit/dab19a20bbf9c80203c117800ff177a8f5da8638))
* 관리자 공지 상세 화면 추가 ([d87e277](https://github.com/skyleedevzero86/IdolGlow/commit/d87e2779c4f4fa8207cb34e878cfadc5e590545c))
* 관리자 공지 상세 화면 추가 ([109758d](https://github.com/skyleedevzero86/IdolGlow/commit/109758d9da744b99ec0d1bd91d70a2c7ddc8ac72))
* 관리자 공지 편집 화면 추가 ([8b90584](https://github.com/skyleedevzero86/IdolGlow/commit/8b905844cff6d06ba31bec258e6849237561bcf7))
* 관리자 광고 화면 추가 ([19ad628](https://github.com/skyleedevzero86/IdolGlow/commit/19ad6288d9b6abb9c9372cc056d9e09eccabc490))
* 관리자 구독 화면 추가 ([76490c7](https://github.com/skyleedevzero86/IdolGlow/commit/76490c755e6c2e039c65647f5520bf5833996538))
* 관리자 뉴스레터 상세 화면 추가 ([888b5b4](https://github.com/skyleedevzero86/IdolGlow/commit/888b5b4f6118facef00c3da73b068701c4766695))
* 관리자 뉴스레터 편집 화면 추가 ([81c9717](https://github.com/skyleedevzero86/IdolGlow/commit/81c971710e803b61e433fcc489f956f481648990))
* 관리자 뉴스레터 화면 추가 ([3a9a0d4](https://github.com/skyleedevzero86/IdolGlow/commit/3a9a0d4c08de9efac3ed72b30018948869b5469c))
* 관리자 리뷰 화면 추가 ([bcf2f04](https://github.com/skyleedevzero86/IdolGlow/commit/bcf2f04d5d1e617ed944b91d0479f5c7e0e2dc83))
* 관리자 마케팅 레이아웃 컴포넌트 추가 ([54db0ef](https://github.com/skyleedevzero86/IdolGlow/commit/54db0ef5403ff0c15b5b8ff2953b617f6022ef69))
* 관리자 메뉴 컴포넌트 추가 ([87fed38](https://github.com/skyleedevzero86/IdolGlow/commit/87fed386281da8f3153ea553db3a96791e7cf199))
* 관리자 배너 화면 추가 ([9673216](https://github.com/skyleedevzero86/IdolGlow/commit/967321659fad32246e99a0d2ec747e04dbd2a890))
* 관리자 상품 추천 화면 추가 ([b9fc2a9](https://github.com/skyleedevzero86/IdolGlow/commit/b9fc2a9da3e8964c3ab206081d80e7bb4ea5d171))
* 관리자 상품 편집 화면 추가 ([a51a9d8](https://github.com/skyleedevzero86/IdolGlow/commit/a51a9d87bd29003453c7e1aba6dccdc6022c5371))
* 관리자 상품 화면 추가 ([cc9cf4a](https://github.com/skyleedevzero86/IdolGlow/commit/cc9cf4af088dce247fcaaa31b215f340fc3731d4))
* 관리자 서버 상태 스타일 추가 ([e8f3cc0](https://github.com/skyleedevzero86/IdolGlow/commit/e8f3cc062c8612bb99971735ff657edee7a9db03))
* 관리자 서버 상태 화면 추가 ([813d5af](https://github.com/skyleedevzero86/IdolGlow/commit/813d5af7691974d53c715b514c3f59570bb4723a))
* 관리자 설문 스타일 추가 ([468f552](https://github.com/skyleedevzero86/IdolGlow/commit/468f552cd73d9c288628a47031a7d0755ce07bc8))
* 관리자 설문 화면 추가 ([cf82ae1](https://github.com/skyleedevzero86/IdolGlow/commit/cf82ae113210e42e538987317cbb0196453df27a))
* 관리자 예약 슬롯 화면 추가 ([6be6534](https://github.com/skyleedevzero86/IdolGlow/commit/6be653454f02fc80487856b5d692aa356094ce3e))
* 관리자 예약 화면 추가 ([86f1c9d](https://github.com/skyleedevzero86/IdolGlow/commit/86f1c9dcf3cdcbc4645b8732f4e5ae6693849dfe))
* 관리자 옵션 편집 화면 추가 ([aab0706](https://github.com/skyleedevzero86/IdolGlow/commit/aab070677c087a6ebe7b20b73bc2a65506d1e33c))
* 관리자 옵션 화면 추가 ([c41adec](https://github.com/skyleedevzero86/IdolGlow/commit/c41adec14fb5fae6fda92a9d7418c182a239ac45))
* 관리자 운영 변경 이력 스타일 추가 ([d3d7501](https://github.com/skyleedevzero86/IdolGlow/commit/d3d7501ca87730b7baa423faa6f751873ec6d49a))
* 관리자 운영 변경 이력 화면 추가 ([cbb0ae8](https://github.com/skyleedevzero86/IdolGlow/commit/cbb0ae83b50aa35916a92d11511a68ab01d94405))
* 관리자 이벤트 상세 화면 추가 ([525eba8](https://github.com/skyleedevzero86/IdolGlow/commit/525eba87f2ccedba83d5393a3b3a7f4fe2fa34aa))
* 관리자 이벤트 편집 화면 추가 ([2e5ed65](https://github.com/skyleedevzero86/IdolGlow/commit/2e5ed65127d9f4d15347771fe5ab937cc288d1a2))
* 관리자 이벤트 화면 추가 ([2e04337](https://github.com/skyleedevzero86/IdolGlow/commit/2e04337f6e03bfbfcf2bc84daa4c813ab0235abc))
* 관리자 이슈 기사 편집 화면 추가 ([4670ae4](https://github.com/skyleedevzero86/IdolGlow/commit/4670ae44717ea172112490accb89882c856d882d))
* 관리자 이슈 기사 화면 추가 ([b4143ea](https://github.com/skyleedevzero86/IdolGlow/commit/b4143ea17598996f4a8a2d200ce0dcbbe7a50833))
* 관리자 이슈 기사화면 추가 ([9c54f85](https://github.com/skyleedevzero86/IdolGlow/commit/9c54f852bd5b62ed59048b0c36b1f2e06e4f51ee))
* 관리자 이슈 목록 화면 추가 ([ea836e5](https://github.com/skyleedevzero86/IdolGlow/commit/ea836e57be240433a3916f7248970cba309bb634))
* 관리자 이슈 호 편집 화면 추가 ([5d1309c](https://github.com/skyleedevzero86/IdolGlow/commit/5d1309c58870f9b8f288535065ec62d17189e8c2))
* 관리자 인증 본인 확인 화면 추가 ([18f6dbc](https://github.com/skyleedevzero86/IdolGlow/commit/18f6dbce6660af67c77416ba76c6d5b6d2ac0c50))
* 관리자 일정 화면 추가 ([f6f951a](https://github.com/skyleedevzero86/IdolGlow/commit/f6f951a775e99153a6003e43751e4fcd89b85008))
* 관리자 팝업 화면 추가 ([9e6baf1](https://github.com/skyleedevzero86/IdolGlow/commit/9e6baf19bf59c06ccac5e6df514f62eee5a7bd21))
* 관리자 회원 화면 추가 ([be7a3aa](https://github.com/skyleedevzero86/IdolGlow/commit/be7a3aa14eb37609ccd861d4d965436ea18eec4a))
* 기사 목록 화면 추가 ([81384e8](https://github.com/skyleedevzero86/IdolGlow/commit/81384e8edbb068eda7ffae4f4712c0f7b4c70c25))
* 기사 상세 화면 추가 ([c35ec32](https://github.com/skyleedevzero86/IdolGlow/commit/c35ec32cc34749e805ba3ac49c7c46abb1c964b4))
* 내 결제 화면 추가 ([e81552c](https://github.com/skyleedevzero86/IdolGlow/commit/e81552c5991fe4dec5a01078f20ecc5787a5e32e))
* 내 리뷰 화면 추가 ([e9abc4d](https://github.com/skyleedevzero86/IdolGlow/commit/e9abc4da782f81c0eea8be1cbebf8b33801a39a9))
* 내 설문 보드 컴포넌트 추가 ([1e449dd](https://github.com/skyleedevzero86/IdolGlow/commit/1e449ddf66bb89dda1294ee59402b3ff02f405f7))
* 내 설문 화면 추가 ([176408c](https://github.com/skyleedevzero86/IdolGlow/commit/176408c4953904119a710c76d359914ffef3b220))
* 내 아카이브 화면 추가 ([db556c0](https://github.com/skyleedevzero86/IdolGlow/commit/db556c0ede6466b866df50d7010f6bd646b88554))
* 내 회원 정보 화면 추가 ([264fb0f](https://github.com/skyleedevzero86/IdolGlow/commit/264fb0fff947566fa8dab32342554eb78c7cd9d5))
* 뉴스레터 구독 모달 공통 컴포넌트 추가 ([7de82fc](https://github.com/skyleedevzero86/IdolGlow/commit/7de82fc2b4eab3397c9b302eebb18592da7952ed))
* 라우트 가드 공통 컴포넌트 추가 ([f431784](https://github.com/skyleedevzero86/IdolGlow/commit/f431784c0e36fd80945e5ebbc702741f71f6301d))
* 레이아웃 공통 컴포넌트 추가 ([eadbdf6](https://github.com/skyleedevzero86/IdolGlow/commit/eadbdf602abd681ba3d15aeebacf9b6166570154))
* 로딩 스피너 공통 컴포넌트 추가 ([6b10f65](https://github.com/skyleedevzero86/IdolGlow/commit/6b10f654190907d9858f5883cc9c16e19b34aca8))
* 마이페이지 화면 추가 ([5259852](https://github.com/skyleedevzero86/IdolGlow/commit/52598526fb5cef8be15a20ba7dd9940ddd05ee93))
* 마크다운 편집 입력 공통 컴포넌트 추가 ([ab76dd3](https://github.com/skyleedevzero86/IdolGlow/commit/ab76dd37e41bbc4e4230511867e4681f7ac9bee7))
* 배포본에 LLM 설정 파일 처리 ([a0cf8d5](https://github.com/skyleedevzero86/IdolGlow/commit/a0cf8d58ae6a2a7288ec3a1faa48a4a14d6834c5))
* 상품 공개 상세 화면 추가 ([da3dda8](https://github.com/skyleedevzero86/IdolGlow/commit/da3dda865834ec5245976c5330a8e9f1935068ed))
* 설문 결과 보드 컴포넌트 추가 ([a93a1f6](https://github.com/skyleedevzero86/IdolGlow/commit/a93a1f6fa9aa6b3feea97fb04df4887157ad555f))
* 아카이브 화면 추가 ([eb0d86b](https://github.com/skyleedevzero86/IdolGlow/commit/eb0d86b3cc2d4e23f7531721db1ff06529a7328a))
* 앱 진입점과 렌더링 추가 ([6aff1c5](https://github.com/skyleedevzero86/IdolGlow/commit/6aff1c5e395b3cc22f3ed4a6655437743a630e21))
* 웹진 Glow 메인 페이지 추가 ([c97ddfb](https://github.com/skyleedevzero86/IdolGlow/commit/c97ddfb6119547ccfde70d8f2088ea1124e2a6c5))
* 이벤트 공개 상세 화면 추가 ([1416e83](https://github.com/skyleedevzero86/IdolGlow/commit/1416e83a36004da993de6d24898d1edec22a1b3a))
* 이벤트 정보 상세 화면 추가 ([61f7695](https://github.com/skyleedevzero86/IdolGlow/commit/61f769548b5675910291ba0ef6974c83a45eb3c8))
* 이벤트 정보 화면 추가 ([c87aa8e](https://github.com/skyleedevzero86/IdolGlow/commit/c87aa8ea62370de6d00977c1e93da8d015df04e9))
* 이벤트 화면 추가 ([10b6e15](https://github.com/skyleedevzero86/IdolGlow/commit/10b6e15dbe0c802351b732563ca78105505bfcca))
* 이슈 배지 공통 컴포넌트 추가 ([c998714](https://github.com/skyleedevzero86/IdolGlow/commit/c998714389a529e53a9819cd6c491e16d3af17db))
* 인증 OAuth 콜백 화면 추가 ([026e756](https://github.com/skyleedevzero86/IdolGlow/commit/026e75653c53c7bacf71f771bc7df0c198ef58b9))
* 전체 라우트와 권한 가드 연결 ([0d2e063](https://github.com/skyleedevzero86/IdolGlow/commit/0d2e0630c7ec0de405107d4e7ed052390cb1bb8d))
* 정기결제 결제 수단 카드 컴포넌트 추가 ([a7af305](https://github.com/skyleedevzero86/IdolGlow/commit/a7af305732410d5ac89f8af62417dca30a91ad92))
* 정기결제 구독 카드 컴포넌트 추가 ([186e2f7](https://github.com/skyleedevzero86/IdolGlow/commit/186e2f78836b3f1db322a3f1a591d71dcac2e209))
* 정기결제 성공 화면 추가 ([7b19bdc](https://github.com/skyleedevzero86/IdolGlow/commit/7b19bdce75abdc717822ff4de7f2002ed807981c))
* 정기결제 실패 화면 추가 ([0cd813a](https://github.com/skyleedevzero86/IdolGlow/commit/0cd813a2fa60715d093ede2de1fedbf8ca2a0dc6))
* 정기결제 청구 목록 컴포넌트 추가 ([012a1af](https://github.com/skyleedevzero86/IdolGlow/commit/012a1af1f5d12a844532c08310fdcac407fafb83))
* 정기결제 화면 추가 ([a8eb74f](https://github.com/skyleedevzero86/IdolGlow/commit/a8eb74fcc02d7370105c4133f6cfdd4b1eb50fb1))
* 정렬 필터 공통 컴포넌트 추가 ([4c1f3a2](https://github.com/skyleedevzero86/IdolGlow/commit/4c1f3a252d1b3cc522a1c6030077fbd5781936ce))
* 지하철 영역 컴포넌트 추가 ([467a369](https://github.com/skyleedevzero86/IdolGlow/commit/467a369e8f0fd5817ffd7090bdb18676e5a1d3a7))
* 지하철 화면 추가 ([cf45908](https://github.com/skyleedevzero86/IdolGlow/commit/cf4590881d5033f3b5a9476a5deded48cda45620))
* 찜 화면 추가 ([50c7a63](https://github.com/skyleedevzero86/IdolGlow/commit/50c7a638658a9023ba0cf7817f9c50e318e76873))
* 퀵 메뉴 공통 컴포넌트 추가 ([bfc75f4](https://github.com/skyleedevzero86/IdolGlow/commit/bfc75f448f9ad55d8d535565633466ea2df90a5a))
* 통합 검색 화면 추가 ([b714dd1](https://github.com/skyleedevzero86/IdolGlow/commit/b714dd1f038a49dd98b356e98e4736ce0dc3b645))
* 통화 선택 컴포넌트 추가 ([b6ad925](https://github.com/skyleedevzero86/IdolGlow/commit/b6ad925d5ebee6313b83f97e257570b6672db2dd))
* 프로필 수정 모달 컴포넌트 추가 ([dceed7a](https://github.com/skyleedevzero86/IdolGlow/commit/dceed7a9c941a3b351b342b1c32720721c8b32b7))
* 호 영역 공통 컴포넌트 추가 ([a1e0905](https://github.com/skyleedevzero86/IdolGlow/commit/a1e0905d13cdd5aacf19c309500a2d943f70ed4c))
* 홈 추천 탐색 공통 컴포넌트 추가 ([8b13125](https://github.com/skyleedevzero86/IdolGlow/commit/8b131253600efdab195cc13418778bd37dfca7a3))
* 홈 팝업 모달 공통 컴포넌트 추가 ([bdccab5](https://github.com/skyleedevzero86/IdolGlow/commit/bdccab54a769fe09178e539ed03e68735befca20))
* 홈 화면 추가 ([9f6c7ef](https://github.com/skyleedevzero86/IdolGlow/commit/9f6c7ef9a1ca931ad8cf62272b3e6530c65ed881))
* 환율 영역 컴포넌트 추가 ([034d732](https://github.com/skyleedevzero86/IdolGlow/commit/034d732c9197ff92393b8ac97fbe70b99349ff26))
* 환율 화면 추가 ([8cec8f3](https://github.com/skyleedevzero86/IdolGlow/commit/8cec8f360083e17f45dd182a75c4dd2ede992ab1))


### Bug Fixes

* 이미지 수정 ([30538dd](https://github.com/skyleedevzero86/IdolGlow/commit/30538dd65d7aff307fb7786a55f47eeedc1cd39c))


### Documentation

* 로컬 실행과 OAuth 포트 안내 문서 추가 ([24e5cc5](https://github.com/skyleedevzero86/IdolGlow/commit/24e5cc5244e62f2f7cce652b8676978d2981065f))


### CI

* 알림 중복을 줄이고 수정 알림 추가 ([4b87485](https://github.com/skyleedevzero86/IdolGlow/commit/4b874852842ddcc2315a61d7671ffa872834abac))
* 쿠버네티스·EKS·ArgoCD 배포 설정 추가 ([f5983a5](https://github.com/skyleedevzero86/IdolGlow/commit/f5983a587577ee44c7cae4bb95b2bab89f538522))

## [0.16.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.15.4...v0.16.0) (2026-11-06)


### Features

* **llm:** 로컬 우선 LLM 게이트웨이와 기능 이전, 설문 추천 품질 평가 ([#96](https://github.com/skyleedevzero86/IdolGlow/issues/96)) ([6e41b30](https://github.com/skyleedevzero86/IdolGlow/commit/6e41b303e0079e309ebbc8fca4ac4667aa7179bd))
* 로컬 우선 LLM 게이트웨이 추가 ([e06570b](https://github.com/skyleedevzero86/IdolGlow/commit/e06570b82e1b36550d1b71b9be26dfa87b135e87))
* 로컬 우선 LLM 게이트웨이 추가 ([6483211](https://github.com/skyleedevzero86/IdolGlow/commit/6483211d8afa4e86bb5334abaf470907d1f7e337))
* 서울시 역사마스터 CSV로 지하철역 좌표를 찾기 ([fe57a30](https://github.com/skyleedevzero86/IdolGlow/commit/fe57a305f3cc01b0d830ce4fa9d4d1b3f01c4a22))
* 설문 추천은 기본 문구로 바로 응답하고 LLM 문구는 뒤에서 만듬 ([f1c530d](https://github.com/skyleedevzero86/IdolGlow/commit/f1c530d8df412f32bcee3a8e466fe1b6d99d949c))
* 지하철역 요약에 실제 노선·주변 장소 데이터를 넣고 사실과 맞는지 검증 ([292df7c](https://github.com/skyleedevzero86/IdolGlow/commit/292df7ca181088236cb08afe333482ae228c853f))


### Bug Fixes

* ArticleViewModel에 likeCount 추가 ([76c0fd9](https://github.com/skyleedevzero86/IdolGlow/commit/76c0fd9723f4aebdd5c406a4ea818d260535e7dd))
* 로컬 LLM 주소와 모델은 설정에 있을 때만 사용 ([08c0881](https://github.com/skyleedevzero86/IdolGlow/commit/08c0881c63c8ed5c9c2580d3fd59a9373fd58f2b))
* 지하철역 요약은 영어로 만든 뒤 한국어로 번역하고 섞인 문자는 제거 ([309858e](https://github.com/skyleedevzero86/IdolGlow/commit/309858eb55b0730da160e49a11bbf45717fb00ef))


### Refactoring

* 공연 매칭 근거를 LLM 게이트웨이로 옮기고 규칙 판정과 맞는지 검증 ([33225ae](https://github.com/skyleedevzero86/IdolGlow/commit/33225ae1f7e696e6f9b6f83cbbcae7450d3aacb6))
* 지하철역 요약을 LLM 게이트웨이로 리팩토링 ([de24daf](https://github.com/skyleedevzero86/IdolGlow/commit/de24daf8c6afce509b2de8903db5950707505ca9))


### Tests

* 설문 추천 LLM 품질 평가 추가 ([020a101](https://github.com/skyleedevzero86/IdolGlow/commit/020a101ebd837479ab76e63a2b9900bb51236589))

## [0.15.4](https://github.com/skyleedevzero86/IdolGlow/compare/v0.15.3...v0.15.4) (2026-11-06)


### CI

* Discord 빌드 알림 추가와 실패 단계 표시 버그 수정 ([afb276b](https://github.com/skyleedevzero86/IdolGlow/commit/afb276bc8ec6c6d482096b180db57a06838c5f4c))
* send build summary to Discord and fix failed stage name ([#94](https://github.com/skyleedevzero86/IdolGlow/issues/94)) ([1d942a5](https://github.com/skyleedevzero86/IdolGlow/commit/1d942a5f182da5c6dc0d13b694b3c21169739762))

## [0.15.3](https://github.com/skyleedevzero86/IdolGlow/compare/v0.15.2...v0.15.3) (2026-11-06)


### CI

* analyze failed builds with local LLM and attach summary to Telegram ([#93](https://github.com/skyleedevzero86/IdolGlow/issues/93)) ([6882cef](https://github.com/skyleedevzero86/IdolGlow/commit/6882ceff5bc7517c7c69860221d852fa021c5023))
* Jenkins에서 로컬 Ollama를 부르고 첫 모델 로딩 시간을 반영하기 ([f2fd545](https://github.com/skyleedevzero86/IdolGlow/commit/f2fd545810da931f9be3240176e180a7dacceec8))
* Telegram 실패 알림에 로그 분석 블록 추가 ([9dbb836](https://github.com/skyleedevzero86/IdolGlow/commit/9dbb836e05ce453e91ae509908ca94c85b5669ec))
* 빌드 실패 로그 분석기 추가 ([e06c058](https://github.com/skyleedevzero86/IdolGlow/commit/e06c058ba5bf53781b64e50646d1c33192c13755))
* 빌드 실패 분석 실행 함수 추가 ([b40ffc8](https://github.com/skyleedevzero86/IdolGlow/commit/b40ffc8b479a73eabf71589c4870f08630734134))
* 빌드 실패 시 Telegram 알림 전에 로그 분석 실행 ([d16c60e](https://github.com/skyleedevzero86/IdolGlow/commit/d16c60e84617d37084a73e7d4c27c1054316e732))

## [0.15.2](https://github.com/skyleedevzero86/IdolGlow/compare/v0.15.1...v0.15.2) (2026-11-06)


### Bug Fixes

* save frontend build log to repository root ci-logs ([#89](https://github.com/skyleedevzero86/IdolGlow/issues/89)) ([265bef7](https://github.com/skyleedevzero86/IdolGlow/commit/265bef7eabd769baca1451f29e17f38488e1c3e9))
* 프론트 빌드 로그를 frontend 폴더가 아닌 저장소 루트 ci-logs에 저장하기 ([3ca3e4e](https://github.com/skyleedevzero86/IdolGlow/commit/3ca3e4ef815b3e69087ec20c386dee5dd53ced0e))

## [0.15.1](https://github.com/skyleedevzero86/IdolGlow/compare/v0.15.0...v0.15.1) (2026-11-06)


### Bug Fixes

* **#87:** 공통 컴포넌트를 Next.js 라우팅으로 바꾸고 프론트 빌드 로그 경로를 고친다 ([eda7365](https://github.com/skyleedevzero86/IdolGlow/commit/eda7365ea7d2c23f2424820fe9ab6aef873087dd))
* 공통 컴포넌트의 react-router-dom을 Next.js 라우팅으로 바꾸기 ([6149f7d](https://github.com/skyleedevzero86/IdolGlow/commit/6149f7dc59e7d29ad38b7b3703787684ab8b3fd8))
* 잘못된 마크다운수정 ([ff51ee1](https://github.com/skyleedevzero86/IdolGlow/commit/ff51ee1a9e35defad39d14c029d68c7651eb3536))
* 프론트 빌드 로그를 저장소 루트 ci-logs에 저장하기 ([9b63511](https://github.com/skyleedevzero86/IdolGlow/commit/9b635113bb6d9ab44775dbe9ac546f5128a24ce0))


### CI

* **#87:** 빌드 실패 분석 파라미터를 추가한다 ([ab6530f](https://github.com/skyleedevzero86/IdolGlow/commit/ab6530f16856106231e967ba08dce0e1c39fdd5d))
* 빌드 실패 분석 파라미터 추가 ([e661353](https://github.com/skyleedevzero86/IdolGlow/commit/e66135379e37c9280a913766e932054b4e7e38dc))

## [0.15.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.14.0...v0.15.0) (2026-11-06)


### Features

* **#75:** Vite 기반 프론트엔드 설정과 공통 유틸 추가 (#PR번호) ([9d7467d](https://github.com/skyleedevzero86/IdolGlow/commit/9d7467df92b5bff757252cb57beab5b2fa652ed8))
* **#77:** 도메인 모델과 서비스 API 클라이언트 추가 ([d32a0cb](https://github.com/skyleedevzero86/IdolGlow/commit/d32a0cba35bd2641cdfea28bb137a024d6a2e661))
* **#80:** 콘텐츠 관리와 검색·구독·설문 API 연동 추가 ([0484a28](https://github.com/skyleedevzero86/IdolGlow/commit/0484a28300b4943417258da0fda060a220e8016c))
* **#81:** add shared UI components and billing integration foundation ([3209fc3](https://github.com/skyleedevzero86/IdolGlow/commit/3209fc375e77b2dfd4aba7082ff6c0d1d46d6fb2))
* **#82:** webzine 프론트 기반과 공통 UI를 합치고 Jenkins 빌드 로그를 남긴다 ([72f18e9](https://github.com/skyleedevzero86/IdolGlow/commit/72f18e9516de8f1576485744d3b038c786b81a34))
* adminAuthVerificationApi API 클라이언트 추가 ([569da23](https://github.com/skyleedevzero86/IdolGlow/commit/569da236648d0ad373e563857ca245ee8b5881d0))
* adminEventsApi API 클라이언트 추가 ([cb7e7f7](https://github.com/skyleedevzero86/IdolGlow/commit/cb7e7f705a96734320f17a50abc38aae9f752b53))
* adminNoticesApi API 클라이언트 추가 ([6b28ff6](https://github.com/skyleedevzero86/IdolGlow/commit/6b28ff69218f294c2f618b4ac4d6353be426edd8))
* adminUserApi API 클라이언트 추가 ([8b8f65c](https://github.com/skyleedevzero86/IdolGlow/commit/8b8f65c0909d59f1b2267b83a0b82534cbd0d850))
* article 도메인 domain 계층 article.types 추가 ([833f9dc](https://github.com/skyleedevzero86/IdolGlow/commit/833f9dc8f09d5075849a52153e5aae4082eb4404))
* article 도메인 infrastructure 계층 articleMapper 추가 ([ff10511](https://github.com/skyleedevzero86/IdolGlow/commit/ff10511d6315e38fe0ee640cffe320a9bac945c8))
* article 도메인 infrastructure 계층 mockArticleRepository 추가 ([161ca8f](https://github.com/skyleedevzero86/IdolGlow/commit/161ca8f8dcb09de93d9d0eba15dd952b4168119b))
* banner 도메인 domain 계층 banner.types 추가 ([93261af](https://github.com/skyleedevzero86/IdolGlow/commit/93261af49fe5792ee55ee8843050a25e746fc3c6))
* Biome 린트와 포맷 규칙 추가 ([acfaba2](https://github.com/skyleedevzero86/IdolGlow/commit/acfaba2bb967c0c7459fd58cfd68ed5769609a0c))
* catalog 도메인 domain 계층 productSummary 추가 ([2bab9f5](https://github.com/skyleedevzero86/IdolGlow/commit/2bab9f52679bef163f0599c5a86ac01d0cac490e))
* category 도메인 domain 계층 category.types 추가 ([2ed4ede](https://github.com/skyleedevzero86/IdolGlow/commit/2ed4ede545f29c1bc8e2571ffbc53c0aff6a1561))
* currencyToFlagCountry 유틸 추가 ([f475587](https://github.com/skyleedevzero86/IdolGlow/commit/f4755872d16df9928fc37e3a0c38221bc3a182a5))
* exchangeCurrencies 유틸 추가 ([e1f8119](https://github.com/skyleedevzero86/IdolGlow/commit/e1f8119ad2c908c2c8a8b6a0acfb5180275bf158))
* exchangeRateApi API 클라이언트 추가 ([81564fd](https://github.com/skyleedevzero86/IdolGlow/commit/81564fd223e25828e30e2ef10969e4d4336f9014))
* festivalInfoApi API 클라이언트 추가 ([0726d09](https://github.com/skyleedevzero86/IdolGlow/commit/0726d09233865a65ecfe3aa8befebdd550626a89))
* glow 화면 업데이트 ([e89de9e](https://github.com/skyleedevzero86/IdolGlow/commit/e89de9e1dad07a02e10c73ba03deb0b32355d902))
* glowAlertsApi API 클라이언트 추가 ([3627150](https://github.com/skyleedevzero86/IdolGlow/commit/3627150d6e06037017b797947a8e33b1654f6dbc))
* glowWeatherApi API 클라이언트 추가 ([340c762](https://github.com/skyleedevzero86/IdolGlow/commit/340c762e0b583e12e10142ae891c65392fbf42d5))
* gqlRequest 유틸 추가 ([070c9d6](https://github.com/skyleedevzero86/IdolGlow/commit/070c9d6a2f3f82a849ec7849995f24a9e9b9ee3e))
* HTML 진입점 추가 ([d408845](https://github.com/skyleedevzero86/IdolGlow/commit/d408845b507d3eb4ad8405e81c2b95bc079d4369))
* idolGlowDateTime 유틸 추가 ([76a5ec7](https://github.com/skyleedevzero86/IdolGlow/commit/76a5ec7b65cc2fe470a4a89c19e75f29b9ae4d13))
* idolGlowMarkdown 유틸 추가 ([3f70e6a](https://github.com/skyleedevzero86/IdolGlow/commit/3f70e6aded9f7375345bfcd29b037bd93b4bd16f))
* integratedSearchApi API 클라이언트 추가 ([9cc1c05](https://github.com/skyleedevzero86/IdolGlow/commit/9cc1c055ad55c169380d77b0a216cb3b4570effc))
* IssueAdminContext 상태 컨텍스트 추가 ([e8ab534](https://github.com/skyleedevzero86/IdolGlow/commit/e8ab5349d31cbeed947785ab579381fa9486bd7d))
* mbrdBoardMarkers API 클라이언트 추가 ([ae00957](https://github.com/skyleedevzero86/IdolGlow/commit/ae0095739b9bbf1da62c65ed4303fd0fa8c1d1a2))
* newsletterAdminApi API 클라이언트 추가 ([e1e2497](https://github.com/skyleedevzero86/IdolGlow/commit/e1e2497e64d39b3ea7bc4829403d499bb4101216))
* NewsletterAdminContext 상태 컨텍스트 추가 ([a593bab](https://github.com/skyleedevzero86/IdolGlow/commit/a593bab202808986a36602d13763cecc6b051795))
* nicknameValidation 유틸 추가 ([c78868c](https://github.com/skyleedevzero86/IdolGlow/commit/c78868c39033ffe3403cff7d9e7833e4d04a938b))
* noticeFiles API 클라이언트 추가 ([704efac](https://github.com/skyleedevzero86/IdolGlow/commit/704efacbf976eb1cd1525f7ddb9f4d1f561e38de))
* opsChangeApi API 클라이언트 추가 ([739f97f](https://github.com/skyleedevzero86/IdolGlow/commit/739f97fbb5dcd7467acd25d8f23ac753897f63a7))
* platform 도메인 health 계층 health 추가 ([a415445](https://github.com/skyleedevzero86/IdolGlow/commit/a4154451d52950d62362f75f5fec51195d5dd27a))
* popularSearchApi API 클라이언트 추가 ([0524cae](https://github.com/skyleedevzero86/IdolGlow/commit/0524cae58ab3238a4fc94093c6a70e52d55ade8f))
* productRecommendationApi API 클라이언트 추가 ([1c59d81](https://github.com/skyleedevzero86/IdolGlow/commit/1c59d81a64a20200b9a45b48052ff89008dd2309))
* publicEventsApi API 클라이언트 추가 ([0f6b4e0](https://github.com/skyleedevzero86/IdolGlow/commit/0f6b4e01f03a71a2b67a9bf48dd9c9a6770baca2))
* publicNoticesApi API 클라이언트 추가 ([33016ef](https://github.com/skyleedevzero86/IdolGlow/commit/33016ef30958c0242a21fc73a37894a13da04074))
* seoulTourDistrictOptions API 클라이언트 추가 ([a7f409b](https://github.com/skyleedevzero86/IdolGlow/commit/a7f409b78482ca793ba40bf9da5191ae5178e8aa))
* siteContentAdminApi API 클라이언트 추가 ([42ddcca](https://github.com/skyleedevzero86/IdolGlow/commit/42ddcca232d3d6193a32e66b50128017c1c0e13e))
* siteContentApi API 클라이언트 추가 ([f9741b7](https://github.com/skyleedevzero86/IdolGlow/commit/f9741b7d5ecd7445538a4d9e94cb9ac21ab9d9eb))
* subscriptionApi API 클라이언트 추가 ([0fcebf9](https://github.com/skyleedevzero86/IdolGlow/commit/0fcebf9309a7ae7d2331cc022ae94f911494bd43))
* subwayApi API 클라이언트 추가 ([0bd9aaf](https://github.com/skyleedevzero86/IdolGlow/commit/0bd9aaf97074c614ac3e4126479bdec46ed816e4))
* surveyAdminApi API 클라이언트 추가 ([d21254f](https://github.com/skyleedevzero86/IdolGlow/commit/d21254f7d4fa3644e370569bd3b75606332c8d3e))
* surveyFormAdminApi API 클라이언트 추가 ([a79d313](https://github.com/skyleedevzero86/IdolGlow/commit/a79d313cded6ccea95763db5e358e30949130910))
* surveyFormUserApi API 클라이언트 추가 ([c896d02](https://github.com/skyleedevzero86/IdolGlow/commit/c896d02051582e9bdc4d4d24d7bca673fb2dfac3))
* uiLangStorage 유틸 추가 ([869e763](https://github.com/skyleedevzero86/IdolGlow/commit/869e763c7d9984539e63e533c80e932370aa71e6))
* useArticles 훅 추가 ([e3c0cb6](https://github.com/skyleedevzero86/IdolGlow/commit/e3c0cb6e2d000824d73742e5ecc81fea9da2d88d))
* useInfiniteScroll 훅 추가 ([4c14193](https://github.com/skyleedevzero86/IdolGlow/commit/4c1419360be8dc3d94785846af93c033afd0a1d7))
* userSurveyApi API 클라이언트 추가 ([5bb53d7](https://github.com/skyleedevzero86/IdolGlow/commit/5bb53d7d23cfdc1140e0ad51b5e9b4f10daa75d7))
* useTheme 훅 추가 ([645a74f](https://github.com/skyleedevzero86/IdolGlow/commit/645a74f6aaf1de6ea586e2639b2db2c093ee2e16))
* Vite 개발 서버와 API 프록시 설정 추가 ([d7c9da9](https://github.com/skyleedevzero86/IdolGlow/commit/d7c9da9129b82b015660242eb6fc3c4c584dc500))
* Vite 환경 변수 타입 선언 추가 ([f268086](https://github.com/skyleedevzero86/IdolGlow/commit/f268086a35f781d496e9b10f9ee303e92cf36193))
* 결제기능 캡처 페이지 추가 ([53f2cad](https://github.com/skyleedevzero86/IdolGlow/commit/53f2cade6771ba2e593461cf2d373ec28e7573ff))
* 공항 교통, GLOW 지도, 라우팅 정적 파일 추가 ([027205f](https://github.com/skyleedevzero86/IdolGlow/commit/027205f09b4490d6237215f8916899b233e42984))
* 기사 카드 공통 컴포넌트 추가 ([6154b56](https://github.com/skyleedevzero86/IdolGlow/commit/6154b567ac2cfac8cdf49d9d0bc26f47939cea41))
* 뉴스레터 구독 이미지 추가 ([81a10f1](https://github.com/skyleedevzero86/IdolGlow/commit/81a10f1973dafa2c266a65d2385ed84131fde805))
* 메뉴 이벤트 화면 추가 ([7aaf70c](https://github.com/skyleedevzero86/IdolGlow/commit/7aaf70ccdbc42ed554c0104347cca5bce8339114))
* 인증 adminSessionWall 추가 ([c230f87](https://github.com/skyleedevzero86/IdolGlow/commit/c230f87d473fc4fa3b051b9f7d156e4f2fa2912e))
* 인증 cookieUtil 추가 ([f78ce1a](https://github.com/skyleedevzero86/IdolGlow/commit/f78ce1a8295669f0416b88008e75862f91963f26))
* 토스 정기결제 billingDefaults 추가 ([9541ce3](https://github.com/skyleedevzero86/IdolGlow/commit/9541ce3e6f312f4133d9f2496231d32a0d768f1e))
* 토스 정기결제 loadTossPaymentsSdk 추가 ([2143163](https://github.com/skyleedevzero86/IdolGlow/commit/2143163f9288fcde4b7e6b4d6e1a30bfbb6186ef))
* 토스 정기결제 requestBillingAuth 추가 ([3ff57af](https://github.com/skyleedevzero86/IdolGlow/commit/3ff57af709639cbb98d921c89aeaeb02117191ec))
* 토스 정기결제 tossBillingApi 추가 ([9adcc5e](https://github.com/skyleedevzero86/IdolGlow/commit/9adcc5e38274291c864873bb8e185b9a7edb1a85))
* 토스 정기결제 TossBillingCharge 타입 추가 ([f6dd369](https://github.com/skyleedevzero86/IdolGlow/commit/f6dd3697af62f6bca74111e6c708effaf391d436))
* 토스 정기결제 TossBillingClientConfig 타입 추가 ([76066b3](https://github.com/skyleedevzero86/IdolGlow/commit/76066b3d54cfd950765a91573f4701e25590c60a))
* 토스 정기결제 TossBillingMethod 타입 추가 ([d3db85e](https://github.com/skyleedevzero86/IdolGlow/commit/d3db85ea9f9df4c2c92d73aac4252e109a40e500))
* 토스 정기결제 TossBillingOverview 타입 추가 ([723fd94](https://github.com/skyleedevzero86/IdolGlow/commit/723fd94866f682ca1b0543b8504419638ea8e4c6))
* 토스 정기결제 TossBillingSubscription 타입 추가 ([ae4c992](https://github.com/skyleedevzero86/IdolGlow/commit/ae4c992ef0080aff318c84a9a287306c75d126a5))
* 푸터 공통 컴포넌트 추가 ([d6d0df9](https://github.com/skyleedevzero86/IdolGlow/commit/d6d0df9a14f3dcaca405eaf8adff29bcc33c188b))
* 헤더 검색 패널 공통 컴포넌트 추가 ([ec09697](https://github.com/skyleedevzero86/IdolGlow/commit/ec09697c69611a1efcce92afa34dfd7c6bb835ce))
* 헤더 공통 컴포넌트 추가 ([1fc9b20](https://github.com/skyleedevzero86/IdolGlow/commit/1fc9b204d1ebea572f3efeff31d3bb5937113d7a))
* 홈 카테고리 메뉴 아이콘 이미지 추가 ([168eccf](https://github.com/skyleedevzero86/IdolGlow/commit/168eccf5e14a9bc9352102bcd07bb25674a306e8))
* 홈 화면 배너 이미지 추가 ([d8c7492](https://github.com/skyleedevzero86/IdolGlow/commit/d8c74929e26bbd72f1e88fac476421defdbabb82))
* 환율 기사 썸네일과 쿠폰 배너 이미지 추가 ([3d725e0](https://github.com/skyleedevzero86/IdolGlow/commit/3d725e024ed2a84ae2057d82b184e1ad8cadecb6))
* 히어로 배너 공통 컴포넌트 추가 ([fa7eba4](https://github.com/skyleedevzero86/IdolGlow/commit/fa7eba4f855d69b24abd8033b4b112024327b5c1))


### Bug Fixes

* Telegram 알림에 Push 비교 링크 유지 ([6b496de](https://github.com/skyleedevzero86/IdolGlow/commit/6b496de5a24c070475be85241245a1fde473b177))
* Telegram 커밋 알림의 HTML 잘림 오류 수정 ([36c7ae7](https://github.com/skyleedevzero86/IdolGlow/commit/36c7ae7b2e426c511c76745c712a4920d426a19d))
* 누락된 타이틀 추가 ([6643654](https://github.com/skyleedevzero86/IdolGlow/commit/6643654d175b151d1fb1ddd69218f6666ddbafd2))


### Build

* 정적 배포용 Nginx, Docker, Netlify 설정 추가 ([3b19032](https://github.com/skyleedevzero86/IdolGlow/commit/3b19032cf97765e4a049885fa71517184f8ec887))


### CI

* 빌드·테스트 출력을 ci-logs에 남기고 pipefail로 실패를 유지하기 ([746c83a](https://github.com/skyleedevzero86/IdolGlow/commit/746c83a429c39ca0a3ba910d77d0f850b086d4fd))


### Tests

* mockArticles 개발용 목 데이터 추가 ([8782c74](https://github.com/skyleedevzero86/IdolGlow/commit/8782c74512836e095ad5f511c2584d5ac74033b0))
* mockBanners 개발용 목 데이터 추가 ([b0579a7](https://github.com/skyleedevzero86/IdolGlow/commit/b0579a7777d7dcbf2eac0d1b7d08979635b7ab35))
* mockIssues 개발용 목 데이터 추가 ([3fd707c](https://github.com/skyleedevzero86/IdolGlow/commit/3fd707c0c440bdb00a08508c4e488ea54f39721c))
* mockNewsletters 개발용 목 데이터 추가 ([43bdd62](https://github.com/skyleedevzero86/IdolGlow/commit/43bdd62d9650c0f63dac256013f0599cb4f1ef4d))

## [0.14.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.13.0...v0.14.0) (2026-11-01)


### Features

* App Main 이미지 변경 ([9eda33f](https://github.com/skyleedevzero86/IdolGlow/commit/9eda33f7ffe1e9125cd9c4987b388536ca45bc43))


### Bug Fixes

* 끊긴 응답과 만료된 리프레시 재발급의 로그를 줄이기 ([f9e4fe1](https://github.com/skyleedevzero86/IdolGlow/commit/f9e4fe1b1de22806907d852ce13dc2946e3cc11b))
* 서버를 다시 켜면 이전 로그인 토큰을 거절하기 ([7517111](https://github.com/skyleedevzero86/IdolGlow/commit/75171110bed21d511dc6c525cc30944a5dc50f19))
* 주관식 엑셀의 숫자 아이디를 사용자명과 이메일로 변경 ([9a733f7](https://github.com/skyleedevzero86/IdolGlow/commit/9a733f7dfb3ddeb24aee12f1fe21ce0ef0a255f9))
* 품질 검사가 막은 업로드 예외와 슬라이드 경로 길이를 정리 ([3998e5e](https://github.com/skyleedevzero86/IdolGlow/commit/3998e5e1c0821ffa6118027347937029ebfc7184))

## [0.13.0](https://github.com/skyleedevzero86/IdolGlow/compare/v0.12.0...v0.13.0) (2026-10-26)


### Features

* **#70:** add survey sheets, hero slide links, and grounded-search eval ([5df8085](https://github.com/skyleedevzero86/IdolGlow/commit/5df8085c857fcbeaa072077a90d4eac6095eeda6))
* 근거 검색 골든셋 평가를 일반 테스트와 분리하고 CI 테스트 힙을 고정 ([c0da107](https://github.com/skyleedevzero86/IdolGlow/commit/c0da107ad81a54fcda35cd6558eeb6cb354f44ac))
* 메인 슬라이드에 이동 경로와 분류를 두고 이미지 업로드 한도를 맞추기 ([33b4462](https://github.com/skyleedevzero86/IdolGlow/commit/33b4462e3538d33c97025649677378fc9ed5e05e))
* 설문 문항에 단문형과 장문형을 나누기 ([b6909be](https://github.com/skyleedevzero86/IdolGlow/commit/b6909be3fe7657fadac39dd490ce25e65630fbb3))
* 설문 통계를 객관식·주관식 시트로 나누고 참여는 로그인으로 제한 ([e12e8c3](https://github.com/skyleedevzero86/IdolGlow/commit/e12e8c31c4612b07818cd9bc4165541b50cb253f))


### Bug Fixes

* 설문 목록 검색의 lower 오류를 없애고 문항 상한을 10개로 올리기 ([9aec2fd](https://github.com/skyleedevzero86/IdolGlow/commit/9aec2fd4f0e5ea34e8e19efeaba388daf7e9852c))


### Refactoring

* 슬라이드 컬럼과 근거 검색 SQL을 1번부터 7번 파일로 정리 ([cec0a5c](https://github.com/skyleedevzero86/IdolGlow/commit/cec0a5cff304066572944912b4839fa391196291))

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
