-- =============================================================================
-- comment: 테이블·컬럼 설명
-- -----
-- 목적
-- - 테이블과 컬럼 설명을 한 파일에 모은다.
-- 대상
-- - ps
-- 주의
-- - 새 데이터베이스는 setting, create, column, comment, insert, select 순서로 실행한다
-- - 이미 반영된 데이터베이스에는 이 묶음을 다시 실행하지 않는다
-- =============================================================================

COMMENT ON TABLE users IS '서비스 회원. OAuth 연동은 user_oauths 참조';

COMMENT ON COLUMN users.id IS '회원 PK';

COMMENT ON COLUMN users.email IS '로그인·식별용 이메일 유니크 인덱스';

COMMENT ON COLUMN users.nickname IS '표시 닉네임';

COMMENT ON COLUMN users.role IS '권한 역할 현재 USER만 허용';

COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 시각';

COMMENT ON TABLE user_oauths IS '외부 OAuth 계정과 users 매핑';

COMMENT ON COLUMN user_oauths.id IS '연동 행 PK';

COMMENT ON COLUMN user_oauths.user_id IS 'users.id FK';

COMMENT ON COLUMN user_oauths.provider IS 'OAuth 제공자 GOOGLE, TEST';

COMMENT ON COLUMN user_oauths.provider_id IS '제공자 측 사용자 식별자';

COMMENT ON COLUMN user_oauths.email IS '제공자가 넘긴 이메일';

COMMENT ON TABLE options IS '상품 옵션 마스터 가격·설명·위치 문구';

COMMENT ON COLUMN options.id IS '옵션 PK';

COMMENT ON COLUMN options.name IS '옵션명';

COMMENT ON COLUMN options.description IS '옵션 상세 설명';

COMMENT ON COLUMN options.price IS '옵션 가격';

COMMENT ON COLUMN options.location IS '옵션 관련 위치 표시 문자열';

COMMENT ON COLUMN options.created_at IS '생성 시각';

COMMENT ON COLUMN options.updated_at IS '수정 시각';

COMMENT ON TABLE products IS '상품 기본 정보';

COMMENT ON COLUMN products.id IS '상품 PK';

COMMENT ON COLUMN products.name IS '상품명';

COMMENT ON COLUMN products.description IS '상품 설명';

COMMENT ON COLUMN products.created_at IS '생성 시각';

COMMENT ON COLUMN products.updated_at IS '수정 시각';

COMMENT ON COLUMN products.tour_attraction_picks_json IS 'Tour API 관광지 다중 선택 JSON 배열 문자열';

COMMENT ON COLUMN products.base_price IS '상품 기본가 옵션 합산과 별도';

COMMENT ON TABLE product_option IS '상품에 포함된 옵션 연결';

COMMENT ON COLUMN product_option.id IS '연결 행 PK';

COMMENT ON COLUMN product_option.product_id IS 'products.id FK';

COMMENT ON COLUMN product_option.option_id IS 'options.id FK';

COMMENT ON COLUMN product_option.created_at IS '생성 시각';

COMMENT ON COLUMN product_option.updated_at IS '수정 시각';

COMMENT ON TABLE product_tag IS '상품별 검색·분류용 태그';

COMMENT ON COLUMN product_tag.id IS '태그 행 PK';

COMMENT ON COLUMN product_tag.product_id IS 'products.id FK';

COMMENT ON COLUMN product_tag.tag_name IS '태그 문자열';

COMMENT ON COLUMN product_tag.created_at IS '생성 시각';

COMMENT ON COLUMN product_tag.updated_at IS '수정 시각';

COMMENT ON TABLE product_locations IS '상품당 하나의 지도/주소 정보';

COMMENT ON COLUMN product_locations.id IS '위치 행 PK';

COMMENT ON COLUMN product_locations.product_id IS 'products.id FK';

COMMENT ON COLUMN product_locations.name IS '장소 표시명';

COMMENT ON COLUMN product_locations.latitude IS '위도';

COMMENT ON COLUMN product_locations.longitude IS '경도';

COMMENT ON COLUMN product_locations.road_address_name IS '도로명 주소';

COMMENT ON COLUMN product_locations.address_name IS '지번/일반 주소';

COMMENT ON COLUMN product_locations.kakao_place_id IS '카카오 장소 ID';

COMMENT ON COLUMN product_locations.created_at IS '생성 시각';

COMMENT ON COLUMN product_locations.updated_at IS '수정 시각';

COMMENT ON TABLE reservation_slots IS '예약 가능 시간 슬롯';

COMMENT ON COLUMN reservation_slots.id IS '슬롯 PK';

COMMENT ON COLUMN reservation_slots.product_id IS 'products.id FK';

COMMENT ON COLUMN reservation_slots.reservation_date IS '슬롯 날짜';

COMMENT ON COLUMN reservation_slots.start_time IS '시작 시각';

COMMENT ON COLUMN reservation_slots.end_time IS '종료 시각 시작보다 커야 함';

COMMENT ON COLUMN reservation_slots.admin_note IS '운영자 메모';

COMMENT ON COLUMN reservation_slots.is_booked IS '예약 확정 등으로 점유 여부';

COMMENT ON COLUMN reservation_slots.created_at IS '생성 시각';

COMMENT ON COLUMN reservation_slots.updated_at IS '수정 시각';

COMMENT ON TABLE reservations IS '회원의 예약 건';

COMMENT ON COLUMN reservations.id IS '예약 PK';

COMMENT ON COLUMN reservations.reservation_slot_id IS 'reservation_slots.id FK';

COMMENT ON COLUMN reservations.user_id IS '예약자 users.id';

COMMENT ON COLUMN reservations.visit_date IS '방문일';

COMMENT ON COLUMN reservations.visit_start_time IS '방문 시작 시각';

COMMENT ON COLUMN reservations.visit_end_time IS '방문 종료 시각';

COMMENT ON COLUMN reservations.total_price IS '예약 총액';

COMMENT ON COLUMN reservations.status IS '예약 상태 PREBOOK/PENDING/BOOKED/COMPLETED/CANCELED';

COMMENT ON COLUMN reservations.admin_memo IS '관리자 메모';

COMMENT ON COLUMN reservations.created_at IS '생성 시각';

COMMENT ON COLUMN reservations.updated_at IS '수정 시각';

COMMENT ON TABLE product_reviews IS '상품별 사용자 리뷰 상품·사용자 유니크';

COMMENT ON COLUMN product_reviews.id IS '리뷰 PK';

COMMENT ON COLUMN product_reviews.product_id IS 'products.id FK';

COMMENT ON COLUMN product_reviews.user_id IS '작성자 users.id';

COMMENT ON COLUMN product_reviews.rating IS '별점 1~5';

COMMENT ON COLUMN product_reviews.content IS '리뷰 본문';

COMMENT ON COLUMN product_reviews.created_at IS '생성 시각';

COMMENT ON COLUMN product_reviews.updated_at IS '수정 시각';

COMMENT ON TABLE images IS '도메인 엔티티별 저장 이미지 메타';

COMMENT ON COLUMN images.id IS '이미지 PK';

COMMENT ON COLUMN images.aggregate_type IS '소속 엔티티 종류';

COMMENT ON COLUMN images.aggregate_id IS '소속 엔티티 PK';

COMMENT ON COLUMN images.original_filename IS '업로드 원본 파일명';

COMMENT ON COLUMN images.unique_filename IS '저장용 유니크 파일명';

COMMENT ON COLUMN images.extension IS '확장자';

COMMENT ON COLUMN images.file_size IS '파일 크기 바이트, 양수';

COMMENT ON COLUMN images.url IS '접근 URL';

COMMENT ON COLUMN images.sort_order IS '같은 aggregate 내 정렬 순서 0 이상';

COMMENT ON COLUMN images.created_at IS '생성 시각';

COMMENT ON COLUMN images.updated_at IS '수정 시각';

COMMENT ON TABLE user_survey IS '사용자 설문 콘셉트·아이돌명·방문 기간';

COMMENT ON COLUMN user_survey.id IS '설문 PK';

COMMENT ON COLUMN user_survey.user_id IS 'users.id FK';

COMMENT ON COLUMN user_survey.concept IS '선호 콘셉트 코드';

COMMENT ON COLUMN user_survey.idol_name IS '관심 아이돌/그룹명';

COMMENT ON COLUMN user_survey.visit_start_date IS '방문 희망 시작일';

COMMENT ON COLUMN user_survey.visit_end_date IS '방문 희망 종료일';

COMMENT ON COLUMN user_survey.created_at IS '생성 시각';

COMMENT ON COLUMN user_survey.updated_at IS '수정 시각';

COMMENT ON TABLE user_survey_places IS '설문에 기입한 방문 희망 장소 복수 행';

COMMENT ON COLUMN user_survey_places.user_survey_id IS 'user_survey.id FK';

COMMENT ON COLUMN user_survey_places.place IS '장소명';

COMMENT ON TABLE wishes IS '사용자의 상품 찜';

COMMENT ON COLUMN wishes.id IS '찜 PK';

COMMENT ON COLUMN wishes.user_id IS 'users.id FK';

COMMENT ON COLUMN wishes.product_id IS 'products.id FK';

COMMENT ON COLUMN wishes.created_at IS '생성 시각';

COMMENT ON COLUMN wishes.updated_at IS '수정 시각';

COMMENT ON TABLE schedules IS '사용자 일정 상품 참조';

COMMENT ON COLUMN schedules.id IS '일정 PK';

COMMENT ON COLUMN schedules.user_id IS 'users.id';

COMMENT ON COLUMN schedules.product_id IS 'products.id';

COMMENT ON COLUMN schedules.title IS '일정 제목';

COMMENT ON COLUMN schedules.start_at IS '시작 시각';

COMMENT ON COLUMN schedules.end_at IS '종료 시각 시작 이상';

COMMENT ON COLUMN schedules.created_at IS '생성 시각';

COMMENT ON COLUMN schedules.updated_at IS '수정 시각';

COMMENT ON COLUMN users.role IS '권한 역할 USER, ADMIN';

COMMENT ON COLUMN reservation_slots.hold_reservation_id IS '현재 슬롯을 홀드 중인 reservations.id';

COMMENT ON COLUMN reservation_slots.hold_expires_at IS '홀드 만료 시각 만료 시 재사용 가능';

COMMENT ON COLUMN reservations.expires_at IS '예약 만료 시각 PENDING 타임아웃 등';

COMMENT ON COLUMN reservations.confirmed_at IS '예약 확정 시각';

COMMENT ON COLUMN reservations.canceled_at IS '예약 취소 시각';

COMMENT ON COLUMN reservations.cancel_reason IS '취소 사유 USER_REQUESTED, PAYMENT_FAILED, PAYMENT_EXPIRED, ADMIN_CANCELED';

COMMENT ON TABLE payments IS '예약별 결제 정보 및 상태 이력 타임스탬프';

COMMENT ON COLUMN payments.id IS '결제 PK';

COMMENT ON COLUMN payments.reservation_id IS 'reservations.id FK 예약당 1건';

COMMENT ON COLUMN payments.provider IS '결제 제공자 현재 MOCK';

COMMENT ON COLUMN payments.payment_reference IS '결제 건을 가리키는 외부 참조 번호. 유일해야 한다.';

COMMENT ON COLUMN payments.amount IS '결제 금액';

COMMENT ON COLUMN payments.status IS '결제 상태 PENDING/SUCCEEDED/FAILED/CANCELED/EXPIRED';

COMMENT ON COLUMN payments.approved_at IS '승인 시각';

COMMENT ON COLUMN payments.failed_at IS '실패 시각';

COMMENT ON COLUMN payments.expired_at IS '만료 시각';

COMMENT ON COLUMN payments.failure_reason IS '실패 사유 메시지';

COMMENT ON COLUMN payments.created_at IS '생성 시각';

COMMENT ON COLUMN payments.updated_at IS '수정 시각';

COMMENT ON TABLE notifications IS '사용자 대상 시스템 알림';

COMMENT ON COLUMN notifications.id IS '알림 PK';

COMMENT ON COLUMN notifications.user_id IS '수신자 users.id';

COMMENT ON COLUMN notifications.type IS '알림 타입 예약/결제 이벤트';

COMMENT ON COLUMN notifications.title IS '알림 제목';

COMMENT ON COLUMN notifications.message IS '알림 본문';

COMMENT ON COLUMN notifications.link IS '클릭 이동 링크 옵션';

COMMENT ON COLUMN notifications.read_at IS '읽음 처리 시각 NULL이면 미읽음';

COMMENT ON COLUMN notifications.created_at IS '생성 시각';

COMMENT ON COLUMN notifications.updated_at IS '수정 시각';

-- -
-- 컬럼 설명 주석
-- -
COMMENT ON TABLE images IS '도메인 엔티티별 저장 이미지 메타';

COMMENT ON COLUMN images.id IS '이미지 PK 자동 증가';

COMMENT ON COLUMN images.aggregate_type IS '이미지가 속한 도메인 타입 PRODUCT/OPTION/USER/PRODUCT_REVIEW';

COMMENT ON COLUMN images.aggregate_id IS 'aggregate_type에 해당하는 실제 엔티티 PK';

COMMENT ON COLUMN images.original_filename IS '사용자가 업로드한 원본 파일명';

COMMENT ON COLUMN images.unique_filename IS '스토리지 저장용 유니크 파일명';

COMMENT ON COLUMN images.extension IS '파일 확장자. jpg, png 등 소문자로 둔다.';

COMMENT ON COLUMN images.file_size IS '파일 크기 byte';

COMMENT ON COLUMN images.url IS '클라이언트가 접근 가능한 이미지 URL';

COMMENT ON COLUMN images.sort_order IS '동일 엔티티 내 이미지 정렬 순서 0부터';

COMMENT ON COLUMN images.created_at IS '생성 시각';

COMMENT ON COLUMN images.updated_at IS '수정 시각';

COMMENT ON TABLE images IS '도메인 엔티티별 저장 이미지 메타';

COMMENT ON COLUMN images.id IS '이미지 PK 자동 증가';

COMMENT ON COLUMN images.aggregate_type IS '이미지가 속한 도메인 타입 PRODUCT/OPTION/USER/PRODUCT_REVIEW';

COMMENT ON COLUMN images.aggregate_id IS 'aggregate_type에 해당하는 실제 엔티티 PK';

COMMENT ON COLUMN images.original_filename IS '사용자가 업로드한 원본 파일명';

COMMENT ON COLUMN images.unique_filename IS '스토리지 저장용 유니크 파일명';

COMMENT ON COLUMN images.extension IS '파일 확장자. jpg, png 등 소문자로 둔다.';

COMMENT ON COLUMN images.file_size IS '파일 크기 byte';

COMMENT ON COLUMN images.url IS '클라이언트가 접근 가능한 이미지 URL';

COMMENT ON COLUMN images.sort_order IS '동일 엔티티 내 이미지 정렬 순서 0부터';

COMMENT ON COLUMN images.created_at IS '생성 시각';

COMMENT ON COLUMN images.updated_at IS '수정 시각';

COMMENT ON COLUMN users.password_hash IS '로컬 가입 비밀번호 BCrypt. OAuth 전용 계정은 NULL';

COMMENT ON COLUMN product_reviews.reservation_id IS '방문 완료 예약 FK 인증 리뷰';

COMMENT ON COLUMN product_reviews.helpful_count IS '도움돼요 수 비정규화';

COMMENT ON COLUMN product_reviews.hidden_at IS '신고 등으로 비공개 처리 시각';

COMMENT ON COLUMN product_reviews.hidden_reason IS '숨김 사유';

COMMENT ON TABLE product_review_helpful_votes IS '리뷰 도움돼요 사용자당 1회 토글';

COMMENT ON TABLE product_review_reports IS '리뷰 신고';

COMMENT ON TABLE reservation_slot_waitlist IS '매진/선점 중인 슬롯 빈자리 알림 대기';

COMMENT ON COLUMN reservation_slot_waitlist.user_id IS 'users.id';

COMMENT ON COLUMN reservation_slot_waitlist.reservation_slot_id IS 'reservation_slots.id';

COMMENT ON TABLE payments IS '예약 단위 결제 정보와 상태 이력. 결제대행사 승인·환불·응답 본문·중복 방지 키를 담는다.';

COMMENT ON COLUMN payments.payment_no IS '내부 결제 번호 유니크';

COMMENT ON COLUMN payments.order_id IS '가맹점 주문 식별자. 대행사 orderId와 맞추며 유일해야 한다.';

COMMENT ON COLUMN payments.payment_key IS '결제대행사가 내려준 결제 건 고유 키. 승인이 끝난 뒤 채운다.';

COMMENT ON COLUMN payments.order_name IS '주문명. 대행사 orderName에 대응한다.';

COMMENT ON COLUMN payments.supplied_amount IS '공급가액.';

COMMENT ON COLUMN payments.vat IS '부가세.';

COMMENT ON COLUMN payments.tax_free_amount IS '비과세 금액.';

COMMENT ON COLUMN payments.currency IS '통화 코드. ISO 4217 규칙을 따른다.';

COMMENT ON COLUMN payments.gateway_method IS '결제 수단 요약. 대행사 method에 대응한다.';

COMMENT ON COLUMN payments.gateway_type IS '결제 유형 코드. NORMAL 같은 값을 쓴다.';

COMMENT ON COLUMN payments.external_status IS '대행사가 내려준 결제 상태 문자열.';

COMMENT ON COLUMN payments.requested_at IS '결제 요청 시각.';

COMMENT ON COLUMN payments.last_transaction_key IS '마지막 거래 식별 키.';

COMMENT ON COLUMN payments.fail_code IS '실패 코드.';

COMMENT ON COLUMN payments.canceled_at IS '전액 취소 완료 시각 내부 마킹';

COMMENT ON COLUMN payments.cancel_amount IS '누적 취소 환불 금액';

COMMENT ON COLUMN payments.card_company IS '카드사 이름.';

COMMENT ON COLUMN payments.card_number IS '마스킹된 카드번호.';

COMMENT ON COLUMN payments.installment_plan_months IS '할부 개월 수';

COMMENT ON COLUMN payments.is_interest_free IS '무이자 여부';

COMMENT ON COLUMN payments.virtual_account_bank IS '가상계좌 은행';

COMMENT ON COLUMN payments.virtual_account_number IS '가상계좌 번호';

COMMENT ON COLUMN payments.virtual_account_due_date IS '가상계좌 입금 만료 시각';

COMMENT ON COLUMN payments.easy_pay_provider IS '간편결제 제공자.';

COMMENT ON COLUMN payments.raw_response_json IS '승인 또는 오류 응답 본문 JSON.';

COMMENT ON COLUMN payments.idempotency_key IS '승인 요청마다 다른 중복 방지 키. 유일해야 한다.';

COMMENT ON TABLE payment_logs IS '결제 승인·취소·서버 알림 요청과 응답, 오류를 남긴다.';

COMMENT ON COLUMN payment_logs.id IS '로그 PK';

COMMENT ON COLUMN payment_logs.payment_id IS 'payments.id FK 삭제 시 NULL';

COMMENT ON COLUMN payment_logs.order_id IS '주문 식별자 검색용.';

COMMENT ON COLUMN payment_logs.payment_key IS '결제대행사가 내려준 결제 건 고유 키';

COMMENT ON COLUMN payment_logs.log_type IS 'CONFIRM_*/CANCEL_*/WEBHOOK_*/MOCK_WEBHOOK/SYSTEM';

COMMENT ON COLUMN payment_logs.step IS 'CLIENT / SERVER / TOSS_API';

COMMENT ON COLUMN payment_logs.request_url IS '호출 URL 경로';

COMMENT ON COLUMN payment_logs.http_method IS 'HTTP 메서드';

COMMENT ON COLUMN payment_logs.http_status IS 'HTTP 응답 상태 코드';

COMMENT ON COLUMN payment_logs.request_body IS '요청 본문 JSON 등';

COMMENT ON COLUMN payment_logs.response_body IS '응답 본문 JSON 등';

COMMENT ON COLUMN payment_logs.error_code IS '에러 코드';

COMMENT ON COLUMN payment_logs.error_message IS '에러 메시지';

COMMENT ON COLUMN payment_logs.stack_trace IS '예외 스택 내부 오류 시';

COMMENT ON COLUMN payment_logs.client_ip IS '클라이언트 IP';

COMMENT ON COLUMN payment_logs.user_agent IS 'User-Agent';

COMMENT ON COLUMN payment_logs.trace_id IS '요청 추적 ID X-Trace-Id 등';

COMMENT ON COLUMN payment_logs.created_at IS '생성 시각';

COMMENT ON COLUMN payment_logs.updated_at IS '수정 시각';

COMMENT ON TABLE payment_refunds IS '결제대행사 환불·취소 요청 이력. 고객 지원과 재시도 추적에 쓴다.';

COMMENT ON COLUMN payment_refunds.id IS '환불 이력 PK';

COMMENT ON COLUMN payment_refunds.payment_id IS 'payments.id FK';

COMMENT ON COLUMN payment_refunds.reservation_id IS 'reservations.id FK 조회 편의';

COMMENT ON COLUMN payment_refunds.cancel_amount IS '해당 요청 취소 환불 금액';

COMMENT ON COLUMN payment_refunds.cancel_reason IS '취소 사유. 사용자·운영·시스템이 남긴 문구.';

COMMENT ON COLUMN payment_refunds.status IS 'PENDING / SUCCEEDED / FAILED';

COMMENT ON COLUMN payment_refunds.requested_by IS 'USER / ADMIN / SYSTEM / WEBHOOK';

COMMENT ON COLUMN payment_refunds.external_transaction_key IS '대행사 거래 식별 키.';

COMMENT ON COLUMN payment_refunds.fail_code IS '실패 코드.';

COMMENT ON COLUMN payment_refunds.fail_message IS '실패 메시지.';

COMMENT ON COLUMN payment_refunds.idempotency_key IS '취소 요청 중복 방지 키. 유일해야 한다.';

COMMENT ON COLUMN payment_refunds.raw_response_json IS '취소 응답 본문 JSON.';

COMMENT ON COLUMN payment_refunds.created_at IS '생성 시각';

COMMENT ON COLUMN payment_refunds.updated_at IS '수정 시각';

COMMENT ON TABLE admin_audit_logs IS '관리자 운영 액션 감사 로그';

COMMENT ON COLUMN admin_audit_logs.id IS '로그 PK 자동 증가';

COMMENT ON COLUMN admin_audit_logs.admin_user_id IS '행동한 관리자 users.id FK';

COMMENT ON COLUMN admin_audit_logs.action_code IS '비즈니스 액션 코드 예 RESERVATION_CANCEL SLOTS_BULK_CREATE';

COMMENT ON COLUMN admin_audit_logs.target_type IS '대상 도메인 유형 예 RESERVATION PRODUCT RESERVATION_SLOT OPTION';

COMMENT ON COLUMN admin_audit_logs.target_id IS '대상 엔티티 PK 없으면 null';

COMMENT ON COLUMN admin_audit_logs.detail IS '추가 맥락 짧은 JSON 또는 문장 최대 2000자';

COMMENT ON COLUMN admin_audit_logs.created_at IS '기록 시각';

COMMENT ON INDEX idx_admin_audit_logs_created_at IS '최근 로그 목록 조회용';

COMMENT ON INDEX idx_admin_audit_logs_admin_user IS '관리자별 로그 조회용';

comment on table newsletters is '뉴스레터 마스터';

comment on column newsletters.slug is '공개 URL용 슬러그';

comment on column newsletters.title is '뉴스레터 제목';

comment on column newsletters.category_label is '관리 화면 카테고리 라벨';

comment on column newsletters.published_at is '게시일';

comment on column newsletters.image_url is '대표 이미지 URL';

comment on column newsletters.summary is '목록/공유용 요약';

comment on column newsletters.created_at is '생성 시각';

comment on column newsletters.updated_at is '마지막 수정 시각';

comment on table newsletter_tags is '뉴스레터 태그';

comment on column newsletter_tags.newsletter_id is 'FK → newsletters.id';

comment on column newsletter_tags.display_order is '표시 순서';

comment on column newsletter_tags.tag_name is '태그 문자열';

comment on column newsletter_tags.created_at is '생성 시각';

comment on column newsletter_tags.updated_at is '마지막 수정 시각';

comment on table newsletter_paragraphs is '뉴스레터 본문 문단';

comment on column newsletter_paragraphs.newsletter_id is 'FK → newsletters.id';

comment on column newsletter_paragraphs.display_order is '문단 순서';

comment on column newsletter_paragraphs.body is '본문 문단';

comment on column newsletter_paragraphs.created_at is '생성 시각';

comment on column newsletter_paragraphs.updated_at is '마지막 수정 시각';

comment on table email_subscriptions is '구독자 마스터 테이블 수신 동의/활성 상태 포함';

comment on column email_subscriptions.email is '구독자 이메일 전역 유일';

comment on column email_subscriptions.subscribed_newsletters is '뉴스레터 수신 여부';

comment on column email_subscriptions.subscribed_issues is '웹진/이슈 수신 여부';

comment on column email_subscriptions.consented_at is '수신 동의 시각';

comment on column email_subscriptions.subscribed_at is '구독 등록 시각';

comment on column email_subscriptions.subscription_source is '구독 유입 채널';

comment on column email_subscriptions.active is '활성 구독 상태';

comment on column email_subscriptions.created_at is '생성 시각';

comment on column email_subscriptions.updated_at is '마지막 수정 시각';

comment on table subscription_dispatch_histories is '콘텐츠 발송 이력 테이블';

comment on column subscription_dispatch_histories.content_type is '콘텐츠 유형 코드. NEWSLETTER, WEBZINE 등 서비스에서 정의한 값.';

comment on column subscription_dispatch_histories.content_slug is '콘텐츠 고유 slug/키';

comment on column subscription_dispatch_histories.content_title is '발송 당시 제목 스냅샷';

comment on column subscription_dispatch_histories.content_summary is '발송 당시 요약 스냅샷';

comment on column subscription_dispatch_histories.dispatch_channel is '발송 채널 코드. EMAIL 등.';

comment on column subscription_dispatch_histories.dispatch_status is '발송 상태 코드. SENT, FAILED 등.';

comment on column subscription_dispatch_histories.recipient_count is '발송 대상자 수';

comment on column subscription_dispatch_histories.content_created_at is '콘텐츠 원본 생성 시각';

comment on column subscription_dispatch_histories.dispatched_at is '실제 발송 시각';

comment on column subscription_dispatch_histories.created_at is '생성 시각';

comment on column subscription_dispatch_histories.updated_at is '마지막 수정 시각';

comment on table admin_audit_logs is '관리자 주요 행위에 대한 감사 로그';

comment on column admin_audit_logs.id is '감사 로그 PK';

comment on column admin_audit_logs.admin_user_id is '행위를 수행한 관리자 식별자. 운영 복구 단계에서는 FK를 지연 적용한다';

comment on column admin_audit_logs.action_code is '행위 코드. 예: RESERVATION_CANCEL, ISSUE_CREATE';

comment on column admin_audit_logs.target_type is '대상 도메인 유형. 예: RESERVATION, WEBZINE_ISSUE';

comment on column admin_audit_logs.target_id is '대상 엔티티 식별자';

comment on column admin_audit_logs.detail is '추가 설명 또는 JSON 스냅샷';

comment on column admin_audit_logs.created_at is '감사 로그 생성 시각';

comment on index idx_admin_audit_logs_created_at is '최신 감사 로그 목록 조회 최적화';

comment on index idx_admin_audit_logs_admin_user is '관리자별 감사 로그 조회 최적화';

comment on table webzine_issues is '웹진 호 메타데이터';

comment on column webzine_issues.slug is '호 공개 slug';

comment on column webzine_issues.volume is '발행 호수';

comment on column webzine_issues.issue_date is '발행일';

comment on column webzine_issues.cover_image_url is '호 표지 이미지 URL';

comment on column webzine_issues.teaser is '호 소개 문구';

comment on table webzine_articles is '웹진 기사 메타데이터';

comment on column webzine_articles.issue_id is '소속 웹진 호 FK';

comment on column webzine_articles.slug is '기사 공개 slug';

comment on column webzine_articles.title is '기사 제목';

comment on column webzine_articles.kicker is '카테고리성 보조 제목';

comment on column webzine_articles.summary is '목록/공유용 요약';

comment on column webzine_articles.hero_image_url is '상세 상단 대표 이미지 URL';

comment on column webzine_articles.card_image_url is '카드 목록용 이미지 URL';

comment on column webzine_articles.category is '기사 분류';

comment on column webzine_articles.format_label is '아티클 형식 라벨';

comment on column webzine_articles.author_name is '작성자명';

comment on column webzine_articles.author_email is '작성자 연락 이메일';

comment on column webzine_articles.credit_line is '크레딧 라인';

comment on column webzine_articles.highlight_quote is '강조 인용문';

comment on index idx_webzine_articles_issue_id is '호별 기사 목록 조회 최적화';

comment on table webzine_article_sections is '웹진 기사 본문 섹션';

comment on column webzine_article_sections.article_id is '소속 기사 FK';

comment on column webzine_article_sections.display_order is '본문 표시 순서';

comment on column webzine_article_sections.heading is '섹션 제목';

comment on column webzine_article_sections.body is '본문 내용';

comment on column webzine_article_sections.note is '보충 메모 또는 각주';

comment on table webzine_article_gallery_images is '웹진 기사 상세 갤러리 이미지';

comment on column webzine_article_gallery_images.article_id is '소속 기사 FK';

comment on column webzine_article_gallery_images.display_order is '갤러리 이미지 표시 순서';

comment on column webzine_article_gallery_images.image_url is '갤러리 이미지 URL';

comment on table webzine_article_tags is '웹진 기사 태그';

comment on column webzine_article_tags.article_id is '소속 기사 FK';

comment on column webzine_article_tags.display_order is '태그 표시 순서';

comment on column webzine_article_tags.tag_name is '태그 문자열';

comment on table newsletters is '소식지 마스터';

comment on column newsletters.slug is '소식지 공개 slug';

comment on column newsletters.title is '소식지 제목';

comment on column newsletters.category_label is '관리 화면 분류 라벨';

comment on column newsletters.published_at is '게시일';

comment on column newsletters.image_url is '대표 이미지 URL';

comment on column newsletters.summary is '목록/공유용 요약';

comment on table newsletter_tags is '소식지 태그';

comment on column newsletter_tags.newsletter_id is '소속 소식지 FK';

comment on column newsletter_tags.display_order is '태그 표시 순서';

comment on column newsletter_tags.tag_name is '태그 문자열';

comment on table newsletter_paragraphs is '소식지 본문 문단';

comment on column newsletter_paragraphs.newsletter_id is '소속 소식지 FK';

comment on column newsletter_paragraphs.display_order is '문단 표시 순서';

comment on column newsletter_paragraphs.body is '본문 문단 내용';

comment on index idx_newsletters_published_at is '게시일 기준 소식지 정렬 최적화';

comment on table email_subscriptions is '이메일 구독 마스터';

comment on column email_subscriptions.email is '구독 이메일';

comment on column email_subscriptions.subscribed_newsletters is '소식지 수신 여부';

comment on column email_subscriptions.subscribed_issues is '호별보기 수신 여부';

comment on column email_subscriptions.consented_at is '개인정보 수집 및 활용 동의 시각';

comment on column email_subscriptions.subscribed_at is '구독 등록 시각';

comment on column email_subscriptions.subscription_source is '구독 유입 채널';

comment on column email_subscriptions.active is '활성 구독 여부';

comment on table subscription_dispatch_histories is '구독 콘텐츠 발송 이력';

comment on column subscription_dispatch_histories.content_type is '콘텐츠 유형. 예: NEWSLETTER, WEBZINE_ISSUE';

comment on column subscription_dispatch_histories.content_slug is '발송된 콘텐츠 slug';

comment on column subscription_dispatch_histories.content_title is '발송 시점의 콘텐츠 제목 스냅샷';

comment on column subscription_dispatch_histories.content_summary is '발송 시점의 요약 스냅샷';

comment on column subscription_dispatch_histories.dispatch_channel is '발송 채널. 현재 EMAIL';

comment on column subscription_dispatch_histories.dispatch_status is '발송 상태 또는 기록 상태';

comment on column subscription_dispatch_histories.recipient_count is '수신자 수';

comment on column subscription_dispatch_histories.content_created_at is '원본 콘텐츠 생성 시각';

comment on column subscription_dispatch_histories.dispatched_at is '발송 또는 기록 시각';

comment on index idx_email_subscriptions_active is '활성 구독자 조회 최적화';

comment on index idx_subscription_dispatch_histories_dispatched_at is '최신 발송 이력 조회 최적화';

comment on table tb_domain_list is '사이트·언어 도메인 목록. 배너·팝업·메인 이미지가 함께 참조한다.';

comment on column tb_domain_list.domain_id is '사이트 도메인 식별자. kr, en처럼 짧은 코드로 구분한다.';

comment on column tb_domain_list.domain_nm is '표시명';

comment on column tb_domain_list.domain_path is 'URL 경로 프리픽스 등';

comment on column tb_domain_list.domain_dc is '설명';

comment on column tb_domain_list.use_at is '사용 여부 Y/N';

comment on column tb_domain_list.actvty_at is '활성 여부 Y/N';

comment on column tb_domain_list.sort_seq is '정렬 순서';

comment on table tb_banner is '배너 관리';

comment on column tb_banner.banner_id is '배너 PK 애플리케이션에서 BNR_ 접두 + 타임스탬프 등';

comment on column tb_banner.domain_id is 'tb_domain_list 참조';

comment on column tb_banner.banner_nm is '배너명';

comment on column tb_banner.link_url is '클릭 시 이동 URL';

comment on column tb_banner.banner_image is '이미지 경로/URL';

comment on column tb_banner.banner_image_file is '원본 파일명 등';

comment on column tb_banner.banner_dc is '설명';

comment on column tb_banner.sort_ordr is '노출 정렬 순서';

comment on column tb_banner.reflct_at is '게시 반영 여부 Y/N';

comment on column tb_banner.frst_register_id is '최초 등록자';

comment on column tb_banner.frst_regist_pnttm is '최초 등록 일시';

comment on column tb_banner.last_updusr_id is '최종 수정자';

comment on column tb_banner.last_updt_pnttm is '최종 수정 일시';

comment on table tb_popup_manage is '팝업 관리';

comment on column tb_popup_manage.popup_id is '팝업 PK';

comment on column tb_popup_manage.domain_id is 'tb_domain_list 참조';

comment on column tb_popup_manage.popup_sj_nm is '팝업 제목';

comment on column tb_popup_manage.file_url is '링크 URL';

comment on column tb_popup_manage.link_target is 'a 태그 target _blank, _self 등';

comment on column tb_popup_manage.popup_img_path is '팝업 이미지 경로';

comment on column tb_popup_manage.popup_file_nm is '이미지 파일명';

comment on column tb_popup_manage.popup_vrticl_lc is '세로 위치';

comment on column tb_popup_manage.popup_width_lc is '가로 위치';

comment on column tb_popup_manage.popup_vrticl_size is '세로 크기 px';

comment on column tb_popup_manage.popup_width_size is '가로 크기 px';

comment on column tb_popup_manage.ntce_bgnde is '게시 시작 일시. 문자열로 저장하는 기존 운영 규격과 맞춘다';

comment on column tb_popup_manage.ntce_endde is '게시 종료일시';

comment on column tb_popup_manage.stopvew_setup_at is '다시보지 않기 설정 사용 여부 Y/N';

comment on column tb_popup_manage.ntce_at is '게시 알림 여부 Y/N';

comment on column tb_popup_manage.frst_register_id is '최초 등록자';

comment on column tb_popup_manage.frst_regist_pnttm is '최초 등록 일시';

comment on column tb_popup_manage.last_updusr_id is '최종 수정자';

comment on column tb_popup_manage.last_updt_pnttm is '최종 수정 일시';

comment on table tb_main_image is '메인 이미지 관리';

comment on column tb_main_image.image_id is '이미지 PK 애플리케이션에서 IMG_ 접두 등';

comment on column tb_main_image.domain_id is 'tb_domain_list 참조';

comment on column tb_main_image.image_nm is '이미지 제목';

comment on column tb_main_image.image is '이미지 경로나 URL. 기존 운영 스키마와 맞추기 위해 컬럼 이름을 image 로 둔다';

comment on column tb_main_image.image_file is '파일명';

comment on column tb_main_image.image_dc is '설명';

comment on column tb_main_image.reflct_at is '게시 반영 여부 Y/N';

comment on column tb_main_image.frst_register_id is '최초 등록자';

comment on column tb_main_image.frst_regist_pnttm is '최초 등록 일시';

comment on column tb_main_image.last_updusr_id is '최종 수정자';

comment on column tb_main_image.last_updt_pnttm is '최종 수정 일시';

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

comment on column editor_documents.introduction is '목록 카드용 요약 또는 공지 첨부 메타데이터';

comment on column editor_documents.thumbnail_image_url is '썸네일 이미지 URL';

comment on column editor_documents.view_count is '게시 공개 문서 조회수 누적';

comment on table editor_assets is '에디터 이미지 업로드 메타';

comment on column editor_assets.id is '자산 UUID';

comment on column editor_assets.object_key is '스토리지 객체 키';

comment on column editor_assets.bucket_name is '버킷 이름';

comment on column editor_assets.original_file_name is '원본 파일명';

comment on column editor_assets.content_type is 'MIME 타입';

comment on column editor_assets.file_size is '바이트 크기';

comment on column editor_assets.uploaded_at is '업로드 시각';

COMMENT ON COLUMN users.role IS '권한 역할 USER: 일반, ADMIN: 관리자';

COMMENT ON COLUMN users.account_status IS '계정 승인·정지 상태 PENDING, APPROVED, REJECTED, SUSPENDED, WITHDRAWN';

COMMENT ON COLUMN users.platform_username IS '플랫폼 가입 시 로그인명 계정 복구 시 이메일과 함께 사용';

COMMENT ON COLUMN users.login_fail_count IS '연속 로그인 실패 횟수';

COMMENT ON COLUMN users.account_locked_at IS '계정 잠금 처리 시각';

COMMENT ON COLUMN users.password_changed_at IS '마지막 비밀번호 변경 시각';

COMMENT ON COLUMN users.last_password_change_date IS '당일 비밀번호 변경 횟수 집계 기준일';

COMMENT ON COLUMN users.password_change_daily_count IS '기준일 당일 비밀번호 변경 횟수';

COMMENT ON TABLE user_password_history IS '과거 비밀번호 해시 재사용 방지 조회용';

COMMENT ON COLUMN user_password_history.id IS '이력 행 PK';

COMMENT ON COLUMN user_password_history.user_id IS 'users.id FK';

COMMENT ON COLUMN user_password_history.password_hash IS 'BCrypt 등으로 인코딩된 비밀번호';

COMMENT ON COLUMN user_password_history.created_at IS '이력 저장 시각';

COMMENT ON COLUMN users.role IS '권한 역할 USER: 일반, ADMIN: 관리자';

COMMENT ON TABLE subscription_dispatch_schedules IS '구독 콘텐츠 자동 발송 스케줄 유형별 주기·시각';

COMMENT ON COLUMN subscription_dispatch_schedules.id IS '스케줄 행 PK';

COMMENT ON COLUMN subscription_dispatch_schedules.content_type IS '콘텐츠 유형 코드. 유형당 스케줄 행은 하나만 둔다.';

COMMENT ON COLUMN subscription_dispatch_schedules.frequency_type IS '반복 주기 유형. DAILY, WEEKLY, MONTHLY 등.';

COMMENT ON COLUMN subscription_dispatch_schedules.day_of_week IS '주간 등 주기에 따른 요일 애플리케이션 규약 문자열. 해당 없으면 NULL';

COMMENT ON COLUMN subscription_dispatch_schedules.dispatch_hour IS '발송 시각 시, 0–23';

COMMENT ON COLUMN subscription_dispatch_schedules.dispatch_minute IS '발송 시각 분, 0–59';

COMMENT ON COLUMN subscription_dispatch_schedules.active IS '스케줄 사용 여부 배치에서 false 행은 건너뜀';

COMMENT ON COLUMN subscription_dispatch_schedules.created_at IS '행 생성 시각';

COMMENT ON COLUMN subscription_dispatch_schedules.updated_at IS '마지막 수정 시각';

COMMENT ON INDEX idx_subscription_dispatch_schedules_active IS '활성 스케줄만 조회할 때 사용';

COMMENT ON TABLE auth_verification_audit_logs IS '인증·검증 이메일 코드 등 시도 감사 로그';

COMMENT ON COLUMN auth_verification_audit_logs.id IS '로그 PK 자동 증가';

COMMENT ON COLUMN auth_verification_audit_logs.verification_type IS '검증 유형 코드 서비스에서 정의한 문자열, 예: SIGNUP_EMAIL';

COMMENT ON COLUMN auth_verification_audit_logs.email IS '시도에 사용된 이메일 주소';

COMMENT ON COLUMN auth_verification_audit_logs.username IS '시도에 사용된 사용자명 또는 닉네임';

COMMENT ON COLUMN auth_verification_audit_logs.ip_address IS '요청 클라이언트 IP 주소';

COMMENT ON COLUMN auth_verification_audit_logs.success IS '검증 성공 여부 true 성공, false 실패';

COMMENT ON COLUMN auth_verification_audit_logs.detail IS '실패 사유·메모 등 부가 설명 최대 500자';

COMMENT ON COLUMN auth_verification_audit_logs.created_at IS '로그가 기록된 시각';

COMMENT ON INDEX idx_auth_verification_audit_logs_created_at IS '최근 검증 시도 목록 조회용';

COMMENT ON INDEX idx_auth_verification_audit_logs_type_created IS '유형별 최근 시도 조회용';

COMMENT ON TABLE signup_verification_tokens IS '가입·계정 확인용 검증 토큰 저장소';

COMMENT ON COLUMN signup_verification_tokens.id IS '행 PK';

COMMENT ON COLUMN signup_verification_tokens.token IS '클라이언트에 전달하는 불투명 토큰 유일';

COMMENT ON COLUMN signup_verification_tokens.type IS '검증 유형 코드. EMAIL_SIGNUP_VERIFY, ACCOUNT_CONFIRM 등 서비스에서 정의';

COMMENT ON COLUMN signup_verification_tokens.email IS '검증 대상 이메일';

COMMENT ON COLUMN signup_verification_tokens.username IS '표시용 사용자명·닉네임 있을 때만';

COMMENT ON COLUMN signup_verification_tokens.user_id IS '검증 완료 후 매핑된 사용자 PK';

COMMENT ON COLUMN signup_verification_tokens.status IS 'PENDING, VERIFIED, USED, REJECTED, EXPIRED';

COMMENT ON COLUMN signup_verification_tokens.requested_ip IS '토큰 발급 요청 IP';

COMMENT ON COLUMN signup_verification_tokens.confirmed_ip IS '토큰 검증 요청 IP';

COMMENT ON COLUMN signup_verification_tokens.expires_at IS '토큰 만료 시각';

COMMENT ON COLUMN signup_verification_tokens.created_at IS '레코드 생성 시각';

COMMENT ON COLUMN signup_verification_tokens.confirmed_at IS '검증 또는 사용 처리 시각';

COMMENT ON COLUMN signup_verification_tokens.detail IS '실패·거절 시 부가 설명';

COMMENT ON INDEX idx_signup_verification_tokens_email_type_status_created IS '이메일·유형·상태별 최신 토큰 조회용';

COMMENT ON COLUMN users.temporary_password_issued_at IS '플랫폼 임시 비밀번호를 마지막으로 발급한 시각. 미발급이면 NULL';

COMMENT ON COLUMN users.temporary_password_required IS '임시 비밀번호 발급 등으로 비밀번호 변경이 필요하면 true';

COMMENT ON COLUMN reservation_slots.admin_note IS '운영자 메모';

COMMENT ON COLUMN reservations.admin_memo IS '관리자 메모';

COMMENT ON COLUMN products.base_price IS '상품 기본가 옵션 합산과 별도';

COMMENT ON COLUMN products.is_recommended IS '관리자 추천 노출 여부';

COMMENT ON COLUMN products.recommendation_score IS '관리자 수동 추천 점수 높을수록 우선';

COMMENT ON TABLE product_latest_korea_recommendation IS 'latest-in-korea 추천 상품 수동 큐레이션 순서';

COMMENT ON COLUMN product_latest_korea_recommendation.display_order IS '화면 노출 순서 1부터 증가';

COMMENT ON COLUMN product_latest_korea_recommendation.product_id IS '추천 대상 상품 ID';

-- ----------------------------------------------------------------------------
-- [C] structured survey — 테이블·컬럼 코멘트
-- ----------------------------------------------------------------------------

COMMENT ON TABLE survey_form IS '설문지 메타 정보 제목/설명/활성 상태';

COMMENT ON COLUMN survey_form.id IS '설문지 PK';

COMMENT ON COLUMN survey_form.title IS '설문지 제목';

COMMENT ON COLUMN survey_form.description IS '설문지 설명';

COMMENT ON COLUMN survey_form.active IS '활성화 여부 true=현재 운영 설문';

COMMENT ON COLUMN survey_form.created_at IS '생성 시각';

COMMENT ON COLUMN survey_form.updated_at IS '수정 시각';

COMMENT ON TABLE survey_question IS '설문지에 속한 문항 정의';

COMMENT ON COLUMN survey_question.id IS '문항 PK';

COMMENT ON COLUMN survey_question.survey_form_id IS '소속 설문지 ID FK';

COMMENT ON COLUMN survey_question.display_order IS '설문 화면 노출 순서';

COMMENT ON COLUMN survey_question.title IS '문항 제목';

COMMENT ON COLUMN survey_question.description IS '문항 부가 설명';

COMMENT ON COLUMN survey_question.question_type IS '문항 타입 TEXT/SINGLE_CHOICE/MULTIPLE_CHOICE';

COMMENT ON COLUMN survey_question.required IS '필수 응답 여부';

COMMENT ON COLUMN survey_question.created_at IS '생성 시각';

COMMENT ON COLUMN survey_question.updated_at IS '수정 시각';

COMMENT ON TABLE survey_question_option IS '객관식 문항의 선택지 집합';

COMMENT ON COLUMN survey_question_option.id IS '선택지 PK';

COMMENT ON COLUMN survey_question_option.survey_question_id IS '소속 문항 ID FK';

COMMENT ON COLUMN survey_question_option.display_order IS '선택지 노출 순서';

COMMENT ON COLUMN survey_question_option.option_text IS '선택지 문구';

COMMENT ON COLUMN survey_question_option.created_at IS '생성 시각';

COMMENT ON COLUMN survey_question_option.updated_at IS '수정 시각';

COMMENT ON TABLE survey_submission IS '사용자 설문 제출 헤더';

COMMENT ON COLUMN survey_submission.id IS '제출 PK';

COMMENT ON COLUMN survey_submission.survey_form_id IS '응답한 설문지 ID FK';

COMMENT ON COLUMN survey_submission.user_id IS '응답 사용자 ID FK';

COMMENT ON COLUMN survey_submission.created_at IS '제출 생성 시각';

COMMENT ON COLUMN survey_submission.updated_at IS '제출 수정 시각';

COMMENT ON TABLE survey_answer IS '제출 내 문항별 응답 본문';

COMMENT ON COLUMN survey_answer.id IS '응답 PK';

COMMENT ON COLUMN survey_answer.survey_submission_id IS '소속 제출 ID FK';

COMMENT ON COLUMN survey_answer.survey_question_id IS '응답 대상 문항 ID FK';

COMMENT ON COLUMN survey_answer.answer_text IS '주관식 응답 텍스트';

COMMENT ON COLUMN survey_answer.created_at IS '생성 시각';

COMMENT ON COLUMN survey_answer.updated_at IS '수정 시각';

COMMENT ON TABLE survey_answer_option IS '객관식/복수선택 응답의 선택값';

COMMENT ON COLUMN survey_answer_option.id IS '선택 응답 PK';

COMMENT ON COLUMN survey_answer_option.survey_answer_id IS '소속 응답 ID FK';

COMMENT ON COLUMN survey_answer_option.display_order IS '선택 순서 복수선택 지원';

COMMENT ON COLUMN survey_answer_option.option_text IS '선택된 문구 스냅샷';

COMMENT ON COLUMN survey_answer_option.created_at IS '생성 시각';

COMMENT ON COLUMN survey_answer_option.updated_at IS '수정 시각';

COMMENT ON COLUMN products.is_recommended IS '관리자 추천 노출 여부';

COMMENT ON COLUMN products.recommendation_score IS '관리자 수동 추천 점수 높을수록 우선';

COMMENT ON COLUMN user_survey.visit_start_time IS '방문 희망 시작 시각 HH:mm, 예: 09:00';

COMMENT ON COLUMN user_survey.visit_end_time IS '방문 희망 종료 시각 HH:mm, 예: 24:00';

COMMENT ON COLUMN survey_form.status IS '설문 진행 상태 PLANNED=계획, SCHEDULED=예정, IN_PROGRESS=진행';

COMMENT ON COLUMN survey_form.primary_category IS '설문 1뎁스 카테고리';

COMMENT ON COLUMN survey_form.secondary_category IS '설문 2뎁스 카테고리';

COMMENT ON TABLE survey_form_description IS '설문 설명 마크다운 본문';

COMMENT ON COLUMN survey_form_description.survey_form_id IS '소속 설문지 ID FK';

COMMENT ON COLUMN survey_form_description.markdown IS '설문 설명 마크다운';

COMMENT ON TABLE survey_form_description_tag IS '설문 설명 태그 목록';

COMMENT ON COLUMN survey_form_description_tag.survey_form_description_id IS '소속 설문 설명 ID FK';

COMMENT ON COLUMN survey_form_description_tag.display_order IS '태그 노출 순서';

COMMENT ON COLUMN survey_form_description_tag.tag_name IS '태그명';

COMMENT ON COLUMN survey_form.status IS '설문 진행 상태 SCHEDULED=예정, IN_PROGRESS=진행, COMPLETED=완료';

COMMENT ON TABLE toss_billing_methods IS '글로우패스 카드 자동결제 수단. 카드번호 원문과 씨브이씨는 미저장';

COMMENT ON COLUMN toss_billing_methods.id IS '결제수단 기본키';

COMMENT ON COLUMN toss_billing_methods.user_id IS '회원 아이디. 한 사용자에 활성 수단 1개 권장';

COMMENT ON COLUMN toss_billing_methods.customer_key IS '토스 구매자 식별키. idolglow-user-회원아이디 형식';

COMMENT ON COLUMN toss_billing_methods.billing_key IS '토스 빌링키. 카드 대신 자동결제에 쓰는 값. 이후 암호화하여 저장';

COMMENT ON COLUMN toss_billing_methods.card_company IS '카드사명 표시용. 예: 현대, 국민';

COMMENT ON COLUMN toss_billing_methods.card_number IS '마스킹된 카드번호. 예: 43301234****123*';

COMMENT ON COLUMN toss_billing_methods.card_type IS '카드 종류. 예: 신용, 체크';

COMMENT ON COLUMN toss_billing_methods.owner_type IS '소유 구분. 예: 개인, 법인';

COMMENT ON COLUMN toss_billing_methods.authenticated_at IS '토스 카드 인증 등록 완료 시각';

COMMENT ON COLUMN toss_billing_methods.active IS '현재 사용 중 여부. 재등록 시 기존 행은 비활성';

COMMENT ON COLUMN toss_billing_methods.created_at IS '행 생성 시각';

COMMENT ON COLUMN toss_billing_methods.updated_at IS '행 수정 시각';

COMMENT ON TABLE toss_billing_subscriptions IS '글로우패스 월간 구독. 회원과 요금제 조합당 1건';

COMMENT ON COLUMN toss_billing_subscriptions.id IS '구독 기본키';

COMMENT ON COLUMN toss_billing_subscriptions.user_id IS '회원 아이디';

COMMENT ON COLUMN toss_billing_subscriptions.billing_method_id IS '결제에 쓸 결제수단 아이디 외래키';

COMMENT ON COLUMN toss_billing_subscriptions.plan_code IS '요금제 코드. 현재 월간 고정';

COMMENT ON COLUMN toss_billing_subscriptions.order_name IS '토스와 영수증 주문명. 예: 아이돌글로우 월간 구독';

COMMENT ON COLUMN toss_billing_subscriptions.amount IS '매월 청구 금액 원 단위. 기본 4900';

COMMENT ON COLUMN toss_billing_subscriptions.status IS '상태. 활성, 일시중지, 해지예약, 해지완료';

COMMENT ON COLUMN toss_billing_subscriptions.next_billing_at IS '다음 자동결제 예정 시각. 스케줄러 도래 판단 기준';

COMMENT ON COLUMN toss_billing_subscriptions.last_charged_at IS '최근 결제 성공 시각';

COMMENT ON COLUMN toss_billing_subscriptions.created_at IS '행 생성 시각';

COMMENT ON COLUMN toss_billing_subscriptions.updated_at IS '행 수정 시각';

COMMENT ON TABLE toss_billing_charges IS '빌링 자동결제 회차별 승인 이력. 주문번호로 이중청구 방지';

COMMENT ON COLUMN toss_billing_charges.id IS '회차 결제 기본키';

COMMENT ON COLUMN toss_billing_charges.user_id IS '회원 아이디';

COMMENT ON COLUMN toss_billing_charges.subscription_id IS '소속 구독 아이디. 단건 테스트 결제는 비울 수 있음';

COMMENT ON COLUMN toss_billing_charges.billing_method_id IS '실제 청구에 사용한 결제수단 아이디 외래키';

COMMENT ON COLUMN toss_billing_charges.order_id IS '주문번호. 구독회차는 구독아이디와 연월, 단건은 임의값';

COMMENT ON COLUMN toss_billing_charges.order_name IS '해당 회차 주문명';

COMMENT ON COLUMN toss_billing_charges.amount IS '이 회차 청구 금액 원 단위';

COMMENT ON COLUMN toss_billing_charges.payment_key IS '토스 결제키. 승인 성공 시 저장';

COMMENT ON COLUMN toss_billing_charges.status IS '상태. 대기, 성공, 실패';

COMMENT ON COLUMN toss_billing_charges.raw_response IS '토스 에이피아이 원문 응답. 디버깅과 대사에 사용';

COMMENT ON COLUMN toss_billing_charges.fail_message IS '실패 시 사용자와 운영 안내 메시지';

COMMENT ON COLUMN toss_billing_charges.approved_at IS '토스 승인 완료 시각';

COMMENT ON COLUMN toss_billing_charges.created_at IS '회차 행 생성 시각. 대기 상태로 먼저 저장하는 시점';

COMMENT ON COLUMN toss_billing_charges.updated_at IS '행 수정 시각';

COMMENT ON COLUMN toss_billing_subscriptions.fail_count IS '연속 결제 실패 횟수. 1회는 하루 뒤, 2회는 이틀 뒤 재시도, 3회는 일시중지';

COMMENT ON COLUMN toss_billing_subscriptions.retry_at IS '다음 재시도 시각. 비어 있으면 다음결제일 기준으로 도래 판단';

COMMENT ON COLUMN toss_billing_subscriptions.ends_at IS '해지 예약 시 혜택 유지 종료 시각. 보통 다음 결제일. 이후 해지완료';

COMMENT ON COLUMN reservations.membership_discount IS '글로우패스 멤버 할인액 원 단위. 비멤버는 0, 멤버는 정상가에서 실결제액을 뺀 값';

COMMENT ON TABLE shedlock IS '스케줄러 분산 락';

COMMENT ON COLUMN shedlock.name IS '잠금 이름. 예: 토스빌링자동결제';

COMMENT ON COLUMN shedlock.lock_until IS '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능';

COMMENT ON COLUMN shedlock.locked_at IS '락을 잡은 시각';

COMMENT ON COLUMN shedlock.locked_by IS '락을 잡은 호스트 또는 인스턴스 식별자';

COMMENT ON COLUMN toss_billing_methods.billing_key IS '토스 빌링키 암호화 저장값. 평문 카드정보 대체값';

COMMENT ON COLUMN toss_billing_methods.billing_key_hash IS '빌링키 평문 해시. 암호문이 매번 달라도 동일 키 중복 등록 방지';

-- =============================================================================
-- 토스 빌링 테이블·컬럼 주석 보강 포스트그레스
-- -----
-- 목적
-- - 이미 적용된 스키마에 컬럼 역할 주석을 재부여
-- =============================================================================

COMMENT ON TABLE toss_billing_methods IS '글로우패스 카드 자동결제 수단. 카드번호 원문과 씨브이씨는 미저장';

COMMENT ON COLUMN toss_billing_methods.id IS '결제수단 기본키';

COMMENT ON COLUMN toss_billing_methods.user_id IS '회원 아이디. 한 사용자에 활성 수단 1개 권장';

COMMENT ON COLUMN toss_billing_methods.customer_key IS '토스 구매자 식별키. idolglow-user-회원아이디 형식';

COMMENT ON COLUMN toss_billing_methods.billing_key IS '토스 빌링키 암호화 저장값. 평문 카드정보 대체값';

COMMENT ON COLUMN toss_billing_methods.billing_key_hash IS '빌링키 평문 해시. 동일 키 중복 등록 방지';

COMMENT ON COLUMN toss_billing_methods.card_company IS '카드사명 표시용. 예: 현대, 국민';

COMMENT ON COLUMN toss_billing_methods.card_number IS '마스킹된 카드번호. 예: 43301234****123*';

COMMENT ON COLUMN toss_billing_methods.card_type IS '카드 종류. 예: 신용, 체크';

COMMENT ON COLUMN toss_billing_methods.owner_type IS '소유 구분. 예: 개인, 법인';

COMMENT ON COLUMN toss_billing_methods.authenticated_at IS '토스 카드 인증 등록 완료 시각';

COMMENT ON COLUMN toss_billing_methods.active IS '현재 사용 중 여부. 재등록 시 기존 행은 비활성';

COMMENT ON COLUMN toss_billing_methods.created_at IS '행 생성 시각';

COMMENT ON COLUMN toss_billing_methods.updated_at IS '행 수정 시각';

COMMENT ON TABLE toss_billing_subscriptions IS '글로우패스 월간 구독. 회원과 요금제 조합당 1건';

COMMENT ON COLUMN toss_billing_subscriptions.id IS '구독 기본키';

COMMENT ON COLUMN toss_billing_subscriptions.user_id IS '회원 아이디';

COMMENT ON COLUMN toss_billing_subscriptions.billing_method_id IS '결제에 쓸 결제수단 아이디 외래키';

COMMENT ON COLUMN toss_billing_subscriptions.plan_code IS '요금제 코드. 현재 월간 고정';

COMMENT ON COLUMN toss_billing_subscriptions.order_name IS '토스와 영수증 주문명. 예: 아이돌글로우 월간 구독';

COMMENT ON COLUMN toss_billing_subscriptions.amount IS '매월 청구 금액 원 단위. 기본 4900';

COMMENT ON COLUMN toss_billing_subscriptions.status IS '상태. 활성, 일시중지, 해지예약, 해지완료';

COMMENT ON COLUMN toss_billing_subscriptions.next_billing_at IS '다음 자동결제 예정 시각. 스케줄러 도래 판단 기준';

COMMENT ON COLUMN toss_billing_subscriptions.last_charged_at IS '최근 결제 성공 시각';

COMMENT ON COLUMN toss_billing_subscriptions.fail_count IS '연속 결제 실패 횟수. 1회는 하루 뒤, 2회는 이틀 뒤 재시도, 3회는 일시중지';

COMMENT ON COLUMN toss_billing_subscriptions.retry_at IS '다음 재시도 시각. 비어 있으면 다음결제일 기준 도래';

COMMENT ON COLUMN toss_billing_subscriptions.ends_at IS '해지 예약 시 혜택 유지 종료 시각. 이후 해지완료';

COMMENT ON COLUMN toss_billing_subscriptions.created_at IS '행 생성 시각';

COMMENT ON COLUMN toss_billing_subscriptions.updated_at IS '행 수정 시각';

COMMENT ON TABLE toss_billing_charges IS '빌링 자동결제 회차별 승인 이력. 주문번호로 이중청구 방지';

COMMENT ON COLUMN toss_billing_charges.id IS '회차 결제 기본키';

COMMENT ON COLUMN toss_billing_charges.user_id IS '회원 아이디';

COMMENT ON COLUMN toss_billing_charges.subscription_id IS '소속 구독 아이디. 단건 테스트 결제는 비울 수 있음';

COMMENT ON COLUMN toss_billing_charges.billing_method_id IS '실제 청구에 사용한 결제수단 아이디 외래키';

COMMENT ON COLUMN toss_billing_charges.order_id IS '주문번호. 구독회차는 구독아이디와 연월, 단건은 임의값';

COMMENT ON COLUMN toss_billing_charges.order_name IS '해당 회차 주문명';

COMMENT ON COLUMN toss_billing_charges.amount IS '이 회차 청구 금액 원 단위';

COMMENT ON COLUMN toss_billing_charges.payment_key IS '토스 결제키. 승인 성공 시 저장';

COMMENT ON COLUMN toss_billing_charges.status IS '상태. 대기, 성공, 실패';

COMMENT ON COLUMN toss_billing_charges.raw_response IS '토스 에이피아이 원문 응답. 디버깅과 대사에 사용';

COMMENT ON COLUMN toss_billing_charges.fail_message IS '실패 시 사용자와 운영 안내 메시지';

COMMENT ON COLUMN toss_billing_charges.approved_at IS '토스 승인 완료 시각';

COMMENT ON COLUMN toss_billing_charges.created_at IS '회차 행 생성 시각. 대기 상태로 먼저 저장하는 시점';

COMMENT ON COLUMN toss_billing_charges.updated_at IS '행 수정 시각';

COMMENT ON TABLE shedlock IS '스케줄러 분산 락';

COMMENT ON COLUMN shedlock.name IS '잠금 이름. 예: 토스빌링자동결제';

COMMENT ON COLUMN shedlock.lock_until IS '이 시각까지 락 유효. 만료 후 다른 노드가 획득 가능';

COMMENT ON COLUMN shedlock.locked_at IS '락을 잡은 시각';

COMMENT ON COLUMN shedlock.locked_by IS '락을 잡은 호스트 또는 인스턴스 식별자';

COMMENT ON COLUMN reservations.membership_discount IS '글로우패스 멤버 할인액 원 단위. 비멤버는 0, 멤버는 정상가에서 실결제액을 뺀 값';

COMMENT ON COLUMN toss_billing_methods.user_id IS '회원 아이디. 활성 카드는 사용자당 여러 장 가능';

COMMENT ON COLUMN toss_billing_methods.active IS '삭제하지 않은 사용 가능 카드 여부. 여러 장 동시 활성 가능';

COMMENT ON COLUMN toss_billing_methods.is_default IS '구독 자동결제에 쓸 기본 수단 여부';

COMMENT ON TABLE toss_billing_charge_attempts IS '빌링 회차 내 카드별 결제 시도 이력';

COMMENT ON COLUMN toss_billing_charge_attempts.id IS '회차 결제 시도 기본키';

COMMENT ON COLUMN toss_billing_charge_attempts.charge_id IS '소속 청구 회차 아이디 외래키';

COMMENT ON COLUMN toss_billing_charge_attempts.billing_method_id IS '이번 시도에 사용한 결제수단 아이디 외래키';

COMMENT ON COLUMN toss_billing_charge_attempts.attempt_no IS '회차 내 시도 순번. 1부터 증가';

COMMENT ON COLUMN toss_billing_charge_attempts.toss_order_id IS '토스에 보낸 주문번호. 회차 orderId에 시도 번호를 붙인 값';

COMMENT ON COLUMN toss_billing_charge_attempts.payment_key IS '토스 결제키. 승인 성공 시 저장';

COMMENT ON COLUMN toss_billing_charge_attempts.status IS '상태. 대기, 성공, 실패';

COMMENT ON COLUMN toss_billing_charge_attempts.fail_message IS '실패 시 안내 메시지';

COMMENT ON COLUMN toss_billing_charge_attempts.raw_response IS '토스 에이피아이 원문 응답';

COMMENT ON COLUMN toss_billing_charge_attempts.approved_at IS '토스 승인 완료 시각';

COMMENT ON COLUMN toss_billing_charge_attempts.created_at IS '시도 행 생성 시각';

COMMENT ON COLUMN toss_billing_charge_attempts.updated_at IS '행 수정 시각';

COMMENT ON COLUMN payments.status IS '결제 상태. CONFIRMING은 토스 승인 호출 중·결과 미확인';

COMMENT ON TABLE toss_billing_customers IS '글로우패스 토스 구매자 키. 사용자당 UUID 1개';

COMMENT ON COLUMN toss_billing_customers.id IS '구매자 키 기본키';

COMMENT ON COLUMN toss_billing_customers.user_id IS '회원 아이디. 사용자당 1건';

COMMENT ON COLUMN toss_billing_customers.customer_key IS '토스 구매자 식별키. UUID 형식. 기존 카드 키와 별도';

COMMENT ON COLUMN toss_billing_customers.created_at IS '행 생성 시각';

COMMENT ON COLUMN payments.status IS 'PENDING 대기, CONFIRMING 승인 처리 중, SUCCEEDED 성공, FAILED 실패, CANCELED 취소, EXPIRED 만료, REFUNDED 전액환불, PARTIAL_CANCELED 부분취소, REFUNDING 환불 처리 중';

COMMENT ON COLUMN payment_refunds.first_requested_at IS '환불 첫 요청 시각. 기존 행은 created_at으로 채운다. NULL 허용.';

COMMENT ON COLUMN toss_billing_methods.deletion_status IS '빌링키 삭제 상태. ACTIVE, DELETE_PENDING, DELETED, DELETE_FAILED';

COMMENT ON COLUMN toss_billing_methods.deletion_requested_at IS '사용자가 카드 삭제를 요청한 시각';

COMMENT ON COLUMN toss_billing_methods.deletion_completed_at IS '토스 삭제 성공 또는 이미 없음으로 확정한 시각';

COMMENT ON COLUMN toss_billing_methods.deletion_last_attempt_at IS '토스 삭제 API를 마지막으로 호출한 시각';

COMMENT ON COLUMN toss_billing_methods.deletion_attempt_count IS '토스 삭제 API 호출 횟수';

COMMENT ON COLUMN toss_billing_methods.deletion_failure_message IS '확정 거절 시 안내 메시지. 500자 이내';

COMMENT ON COLUMN subscription_dispatch_histories.dispatch_status IS '발송 상태. RECORDED, SENDING, SENT, PARTIAL_FAILED, FAILED';

COMMENT ON COLUMN subscription_dispatch_histories.success_count IS '수신자별 발송 성공 수';

COMMENT ON COLUMN subscription_dispatch_histories.failure_count IS '수신자별 발송 실패 수';

COMMENT ON COLUMN subscription_dispatch_histories.last_attempt_at IS '마지막으로 발송을 시도한 시각. 오래되면 안 보낸 수신자만 이어서 보낸다';

COMMENT ON TABLE subscription_dispatch_recipients IS '구독 발송 수신자별 결과';

COMMENT ON COLUMN subscription_dispatch_recipients.dispatch_id IS '구독 발송 이력 ID';

COMMENT ON COLUMN subscription_dispatch_recipients.email IS '수신 이메일';

COMMENT ON COLUMN subscription_dispatch_recipients.status IS '수신자 상태. SENT, FAILED';

COMMENT ON COLUMN subscription_dispatch_recipients.sent_at IS '발송에 성공한 시각';

COMMENT ON COLUMN subscription_dispatch_recipients.error IS '실패 원인. 1000자 이내';

COMMENT ON TABLE event_master IS '공연 대표 식별자. 공급처가 달라도 같은 공연은 한 행';

COMMENT ON COLUMN event_master.global_id IS '글로벌 공연 ID. UUID';

COMMENT ON COLUMN event_master.title IS '대표 제목';

COMMENT ON COLUMN event_master.normalized_title IS '매칭용 제목. 괄호·공백·기호를 뺀 값';

COMMENT ON COLUMN event_master.start_date IS '대표 시작일';

COMMENT ON COLUMN event_master.end_date IS '대표 종료일';

COMMENT ON COLUMN event_master.venue IS '대표 장소';

COMMENT ON COLUMN event_master.normalized_venue IS '매칭용 장소';

COMMENT ON COLUMN event_master.status IS '대표 상태. ACTIVE';

COMMENT ON TABLE event_source_ref IS '공급처 공연 원본과 글로벌 식별자 매칭';

COMMENT ON COLUMN event_source_ref.source IS '공급처 코드. KOPIS_PBLPRFR, SEOUL_SJW_PERFORM 등';

COMMENT ON COLUMN event_source_ref.source_id IS '공급처 공연 ID';

COMMENT ON COLUMN event_source_ref.global_id IS '매칭된 글로벌 공연 ID. CONFLICT면 비움';

COMMENT ON COLUMN event_source_ref.match_status IS '매칭 상태. CONFIRMED, CANDIDATE, CONFLICT';

COMMENT ON COLUMN event_source_ref.match_score IS '매칭 점수. 100이면 제목·기간·장소 일치';

COMMENT ON COLUMN event_source_ref.matched_at IS '마지막으로 매칭한 시각';

COMMENT ON COLUMN event_source_ref.raw_hash IS '공급처 원본 해시. 바뀌면 다시 매칭';

COMMENT ON COLUMN event_source_ref.raw_payload IS '공급처에서 받은 원본 스냅샷';

COMMENT ON TABLE event_collect_log IS '공연 공급처 수집 결과';

COMMENT ON COLUMN event_collect_log.status IS '수집 상태. SUCCESS, FAILED';

COMMENT ON COLUMN event_collect_log.item_count IS '이번 수집에서 저장을 시도한 건수';

COMMENT ON COLUMN event_collect_log.error_message IS '실패 원인. 1000자 이내';

COMMENT ON COLUMN event_collect_log.collected_at IS '수집 시각';

COMMENT ON TABLE event_translation IS '공연 언어별 제목·소개';

COMMENT ON COLUMN event_translation.global_id IS '글로벌 공연 ID';

COMMENT ON COLUMN event_translation.locale IS '언어 코드. ko, en, ja, zh';

COMMENT ON COLUMN event_translation.title IS '해당 언어 제목';

COMMENT ON COLUMN event_translation.synopsis IS '해당 언어 소개. 없으면 비움';

COMMENT ON COLUMN event_translation.origin IS '출처. PROVIDER, MANUAL, MACHINE. MACHINE은 노출하지 않음';

COMMENT ON COLUMN event_translation.updated_at IS '마지막으로 번역을 바꾼 시각';

COMMENT ON COLUMN users.role IS '권한 역할. USER 일반, ADMIN 관리자, OPERATOR 운영 변경 요청, APPROVER 승인';

COMMENT ON TABLE ops_change_request IS '운영 변경 요청. 승인 뒤에만 적용하고 되돌릴 수 있다';

COMMENT ON COLUMN ops_change_request.type IS '변경 종류. EVENT_MATCH_MERGE 등';

COMMENT ON COLUMN ops_change_request.target IS '변경 대상. 공연 병합은 공급처|공급처ID';

COMMENT ON COLUMN ops_change_request.payload IS '요청 내용. JSON';

COMMENT ON COLUMN ops_change_request.requested_by IS '요청자. 사용자 ID 또는 에이전트 이름';

COMMENT ON COLUMN ops_change_request.requested_by_kind IS '요청자 종류. USER, AGENT';

COMMENT ON COLUMN ops_change_request.status IS '상태. REQUESTED, APPROVED, REJECTED, APPLIED, REVERTED';

COMMENT ON COLUMN ops_change_request.before_snapshot IS '적용 직전 상태. 되돌리기에서 복구';

COMMENT ON COLUMN ops_change_request.approved_by IS '승인자 사용자 ID';

COMMENT ON COLUMN ops_change_request.applied_at IS '적용 시각';
