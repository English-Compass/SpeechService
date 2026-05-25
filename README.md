# SpeechService

OpenAI API를 활용한 음성 기반 영어 학습 서비스입니다. TTS/STT 변환과 실제 상황 시뮬레이션 롤플레잉 대화 연습을 제공합니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 17 |
| 빌드 도구 | Gradle |
| 프레임워크 | Spring Boot 3.2.0 |
| 포트 | 8085 |
| AI | OpenAI API (gpt-4o, Whisper, TTS) |
| 데이터베이스 | H2 인메모리 (개발), MySQL (운영) |
| 캐시 | Redis |
| 웹 서버 | Undertow |

## 주요 기능

- **TTS (Text-to-Speech)**: 텍스트를 자연스러운 영어 음성으로 변환
- **STT (Speech-to-Text)**: 사용자 음성을 텍스트로 변환 (OpenAI Whisper)
- **롤플레잉 대화 연습**: 다양한 실생활 시나리오에서 AI와 대화 연습
- **발음 평가**: 사용자 발음을 분석해 피드백 제공
- **세션 관리**: 대화 히스토리 및 학습 이력 저장

## API 엔드포인트

| Method | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| GET | `/api/speech/health` | 헬스 체크 | 불필요 |
| POST | `/api/speech/text-to-speech` | 텍스트 → 음성 변환 | 필요 |
| POST | `/api/speech/speech-to-text` | 음성 → 텍스트 변환 | 필요 |
| POST | `/api/speech/role-playing` | 롤플레잉 세션 시작 | 필요 |
| GET | `/api/speech/sessions/{sessionId}` | 세션 조회 | 필요 |

### POST /api/speech/text-to-speech

```
Content-Type: text/plain

Hello, how can I help you today?
```

응답: `audio/mpeg` 바이너리 스트림

### POST /api/speech/speech-to-text

```
Content-Type: multipart/form-data

audio: [오디오 파일 (mp3, wav, m4a 등)]
```

응답:

```json
{
  "text": "Hello, how can I help you today?",
  "confidence": 0.95
}
```

### POST /api/speech/role-playing

```json
{
  "userId": "user123",
  "scenario": "job_interview",
  "difficulty": "B",
  "userRole": "applicant"
}
```

응답:

```json
{
  "sessionId": "session-uuid",
  "scenario": "job_interview",
  "firstMessage": "Good morning! Please tell me about yourself.",
  "audioUrl": "/api/speech/sessions/session-uuid/audio"
}
```

## 롤플레잉 시나리오

| 시나리오 | 설명 |
|----------|------|
| `job_interview` | 영어 면접 연습 |
| `hotel_checkin` | 호텔 체크인 |
| `restaurant_order` | 레스토랑 주문 |
| `airport_customs` | 공항 출입국 심사 |
| `business_meeting` | 비즈니스 미팅 |
| `shopping` | 쇼핑 상황 |

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `OPENAI_API_KEY` | OpenAI API 키 | 필수 |
| `SPRING_DATASOURCE_URL` | MySQL 접속 URL (운영 환경) | 선택 |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 이름 | 선택 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | 선택 |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | 선택 (기본값: `localhost`) |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | 선택 (기본값: `6379`) |

## 실행 방법

### 로컬 실행

```bash
# 인프라 먼저 실행 (api-gateway 디렉토리에서)
cd ../api-gateway && docker-compose up -d

cd SpeechService
export OPENAI_API_KEY="your-openai-api-key"
./gradlew bootRun
```

### Docker

```bash
cd SpeechService
docker-compose up --build -d
docker-compose logs -f app
```

## 프로젝트 구조

```
src/main/java/com/example/speechservice/
├── SpeechServiceApplication.java
├── controller/
│   ├── SpeechSessionController.java   # TTS/STT 엔드포인트
│   └── RolePlayingController.java     # 롤플레잉 엔드포인트
├── service/
│   ├── SpeechSessionService.java      # 세션 관리
│   ├── EvaluationService.java         # 발음 평가
│   ├── OpenAIService.java             # OpenAI API 통신
│   └── RolePlayingScenarioService.java # 시나리오 관리
├── entity/
│   └── SpeechSession.java
├── dto/
│   ├── SessionStartRequest.java
│   ├── SessionStartResponse.java
│   ├── SpeechEvaluationResult.java
│   ├── RolePlayingSessionRequest.java
│   └── RolePlayingScenario.java
└── config/
    └── RedisConfig.java
```

## 데이터베이스

개발 환경에서는 H2 인메모리 DB를 사용합니다. 운영 환경에서는 `SPRING_DATASOURCE_URL`로 MySQL을 설정합니다.

H2 콘솔 (개발 환경): `http://localhost:8085/h2-console`

## 빌드 및 테스트

```bash
# 빌드 (테스트 제외)
./gradlew clean build -x test

# 테스트 실행
./gradlew test

# 헬스 체크
curl http://localhost:8085/api/speech/health
```
