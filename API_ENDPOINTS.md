# SpeechService API 엔드포인트 가이드

---

## 1. 세션 시작 (Session Start)

### POST `/api/v1/speech-sessions/role-playing`

**설명**: 새로운 영어 회화 세션을 시작합니다.

**Request Body**:
```json
{
  "userId": "string",
  "topic": "string",
  "difficultyLevel": "BEGINNER" | "INTERMEDIATE" | "ADVANCED"
}
```

**난이도 설명:**
- `BEGINNER`: 초급 - 일상생활 단어, 짧은 문장, 현재시제
- `INTERMEDIATE`: 중급 - 일반 어휘, 복잡한 문장 구조, 과거/미래시제  
- `ADVANCED`: 고급 - 고급 어휘, 복잡한 문장, 관용구 사용

**Response** (200 OK):
```json
{
  "sessionId": "string",
  "topic": "string",
  "difficultyLevel": "string",
  "aiFirstGreeting": "string",
  "createdAt": "string (ISO 8601)"
}
```

**예시**:
```bash
curl -X POST http://localhost:8082/api/v1/speech-sessions/role-playing \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "topic": "travel",
    "difficultyLevel": "BEGINNER"
  }'
```

---

## 2. 대화 진행 (Talk)

### POST `/api/v1/speech-sessions/role-playing/{sessionId}/talk`

**설명**: 사용자의 음성 발화를 처리하고 AI의 응답을 받습니다.

**Path Parameters**:
- `sessionId`: 세션 ID

**Request Body**: `multipart/form-data`
- `audio`: 음성 파일 (WAV, MP3, M4A 등)

**Response** (200 OK):
```json
{
  "sessionId": "string",
  "aiResponse": "string",
  "audioUrl": "string (AI 음성 파일 URL)"
}
```

**예시**:
```bash
curl -X POST http://localhost:8082/api/v1/speech-sessions/role-playing/session_123/talk \
  -F "audio=@user_speech.wav"
```

---

## 3. 세션 종료 (Session End)

### POST `/api/v1/speech-sessions/role-playing/{sessionId}/end`

**설명**: 현재 진행 중인 세션을 종료하고 평가 결과를 받습니다.

**Path Parameters**:
- `sessionId`: 세션 ID

**Request Body**: 없음

**Response** (200 OK):
```json
{
  "sessionId": "string",
  "feedback": "string",
  "recommendedDifficulty": "string (optional)"
}
```

**예시**:
```bash
curl -X POST http://localhost:8082/api/v1/speech-sessions/role-playing/session_123/end
```

---

## 4. 피드백 히스토리 조회 (Feedback History)

### GET `/api/v1/speech-sessions/evaluations`

**설명**: 사용자의 이전 세션 피드백 히스토리를 페이지네이션으로 조회합니다.

**Query Parameters**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 10)

**Response** (200 OK):
```json
{
  "content": [
    {
      "sessionId": "string",
      "topic": "string",
      "difficultyLevel": "string",
      "feedback": "string",
      "recommendedDifficulty": "string (optional)",
      "createdAt": "string (ISO 8601)"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "first": true,
  "numberOfElements": 10
}
```

**예시**:
```bash
curl "http://localhost:8082/api/v1/speech-sessions/evaluations?page=0&size=5"
```

---

## 5. 세션 정보 조회 (Session Info)

### GET `/api/v1/speech-sessions/{sessionId}`

**설명**: 특정 세션의 기본 정보를 조회합니다.

**Path Parameters**:
- `sessionId`: 세션 ID

**Response** (200 OK):
```json
{
  "sessionId": "string",
  "userId": "string",
  "topic": "string",
  "difficultyLevel": "string",
  "status": "ACTIVE" | "ENDED",
  "createdAt": "string (ISO 8601)",
  "endedAt": "string (ISO 8601, optional)"
}
```

**예시**:
```bash
curl http://localhost:8082/api/v1/speech-sessions/session_123
```

---

## 에러 응답 (Error Responses)

### 400 Bad Request
```json
{
  "error": "string",
  "message": "string",
  "timestamp": "string"
}
```

### 404 Not Found
```json
{
  "error": "Session not found",
  "message": "Session not found with id: session_123",
  "timestamp": "string"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "string",
  "timestamp": "string"
}
```

---

## 사용 시 주의사항

### 1. 세션 관리
- 세션은 최대 15분 동안 활성 상태를 유지합니다
- 20번의 대화 후 자동으로 종료됩니다
- 세션 ID는 고유하며 재사용할 수 없습니다

### 2. 음성 파일
- 지원 형식: WAV, MP3, M4A, OGG
- 권장 샘플링 레이트: 16kHz 이상
- 파일 크기: 10MB 이하

### 3. 난이도 레벨
- **BEGINNER (초급)**: 기본 어휘, 단순 문장, 현재시제
- **INTERMEDIATE (중급)**: 일반 어휘, 다양한 문장 구조, 과거/미래시제
- **ADVANCED (고급)**: 고급 어휘, 복잡한 문장, 관용구 사용

**참고**: AI 피드백에서는 난이도를 한글로 표시합니다 (초급, 중급, 고급)

### 4. AI 응답 특성
- AI는 사용자의 영어 실력에 맞는 난이도로 응답합니다
- 중요한 문법 오류나 어휘 오용 시에만 교정을 제안합니다
- 대화의 자연스러운 흐름을 유지합니다




---



