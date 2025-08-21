-- 테스트용 샘플 데이터
-- 공유 user 테이블에 테스트 사용자 추가

-- H2에서는 MERGE를 사용하여 upsert 수행
MERGE INTO "user" (id, kakao_id, name, level, prefer_category, profile, created_at, updated_at) 
KEY(id) VALUES (1, 12345, '테스트 사용자', 1, 'travel', '테스트용 프로필', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
