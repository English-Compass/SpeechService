-- Speech Service 데이터베이스 스키마
-- 이 파일은 H2 인메모리 데이터베이스에서 사용됩니다.

-- user 테이블 (공유 데이터베이스와 동일한 구조)
CREATE TABLE IF NOT EXISTS "user" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kakao_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    prefer_category VARCHAR(100),
    profile TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- speech_sessions 테이블 (롤플레잉 세션 정보)
CREATE TABLE IF NOT EXISTS speech_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,                    -- 사용자 ID (공유 user 테이블 참조)
    topic VARCHAR(255) NOT NULL,                -- 대화 주제 (기록용)
    difficulty_level VARCHAR(50) NOT NULL,      -- 난이도 (기록용)
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,                         -- 세션 종료 시간 (null이면 활성)
    
    -- 통합된 피드백 정보
    feedback TEXT,                              -- AI 분석 결과를 사용자 친화적으로 표현
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 외래키 제약조건
    FOREIGN KEY (user_id) REFERENCES "user"(id)
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_speech_sessions_user_id ON speech_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_speech_sessions_session_id ON speech_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_speech_sessions_end_time ON speech_sessions(end_time);
CREATE INDEX IF NOT EXISTS idx_speech_sessions_created_at ON speech_sessions(created_at);

-- user 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_user_kakao_id ON "user"(kakao_id);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON "user"(created_at);

-- speech_evaluation 테이블은 제거됨 (speech_sessions에 통합)
