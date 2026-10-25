-- =============================================================================
-- V51: 운영 변경 요청·승인·되돌리기
-- -----
-- 목적
-- - 운영 변경을 요청, 승인, 적용, 되돌리기 상태로 남긴다
-- 대상
-- - 마이에스큐엘 / ops_change_request, users.role
-- 주의
-- - 가이드의 V50은 공연 번역 테이블이 이미 사용한다. 이 변경은 V51
-- - 요청자와 승인자는 같을 수 없다. 적용 전 상태는 before_snapshot에 남긴다
-- - OPERATOR는 요청·적용, APPROVER는 승인. ADMIN은 기존 관리 권한을 유지한다
-- 연관
-- - OpsChangeCommandService, EventMatchMergeHandler, AdminAuditService
-- =============================================================================

-- 제안자와 승인자를 역할로 나눈다. 기존 USER, ADMIN은 그대로 둔다.
ALTER TABLE users DROP CHECK chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'OPERATOR', 'APPROVER'));
ALTER TABLE users MODIFY COLUMN role VARCHAR(255) NOT NULL COMMENT '권한 역할. USER 일반, ADMIN 관리자, OPERATOR 운영 변경 요청, APPROVER 승인';

-- 운영 변경 한 건. 적용 시점에 이전 상태를 남기고 되돌리기에서 복구한다.
CREATE TABLE IF NOT EXISTS ops_change_request (
    id bigint auto_increment primary key,
    type varchar(80) not null comment '변경 종류. EVENT_MATCH_MERGE 등',
    target varchar(255) not null comment '변경 대상. 공연 병합은 공급처|공급처ID',
    payload text not null comment '요청 내용. JSON',
    requested_by varchar(80) not null comment '요청자. 사용자 ID 또는 에이전트 이름',
    requested_by_kind varchar(20) not null comment '요청자 종류. USER, AGENT',
    status varchar(20) not null comment '상태. REQUESTED, APPROVED, REJECTED, APPLIED, REVERTED',
    before_snapshot text null comment '적용 직전 상태. 되돌리기에서 복구',
    approved_by varchar(80) null comment '승인자 사용자 ID',
    applied_at datetime(6) null comment '적용 시각',
    created_at timestamp not null default current_timestamp comment '생성 시각',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '마지막 수정 시각'
) comment='운영 변경 요청. 승인 뒤에만 적용하고 되돌릴 수 있다';

CREATE INDEX idx_ops_change_request_status
    ON ops_change_request (status);
