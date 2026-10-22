-- visit_start_time, visit_end_time 은 V1 통합 스키마의 user_survey 에 이미 있다.
-- MySQL 은 ADD COLUMN IF NOT EXISTS 를 지원하지 않아 다시 추가하지 않는다. 버전 번호를 유지하려고 빈 작업만 남긴다.
SELECT 1;
