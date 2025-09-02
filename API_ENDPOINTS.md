# SpeechService API 엔드포인트 가이드

---

## 1. 롤 플레잉 시나리오 목록 조회

### GET `/api/v1/role-playing/scenarios`

**설명**: 미리 정의된 롤 플레잉 시나리오 목록을 조회합니다.

**Response** (200 OK):
```json
[
  {
    "id": "cafe",
    "name": "카페",
    "aiRole": "바리스타",
    "userRole": "고객",
    "situation": "당신은 카페에서 음료를 주문하려고 합니다.",
    "description": "카페에서 음료 주문하기"
  },
  {
    "id": "restaurant",
    "name": "식당",
    "aiRole": "웨이터",
    "userRole": "손님",
    "situation": "당신은 식당에서 주문한 음식과 다른 음식을 받았습니다.",
    "description": "식당에서 음식 문제 해결하기"
  }
]
```

**예시**:
```bash
curl http://localhost:8082/api/v1/role-playing/scenarios
```

---

## 2. 롤 플레잉 세션 시작

### POST `/api/v1/role-playing/start`

**설명**: 새로운 롤 플레잉 세션을 시작합니다.

**Request Body**:
```json
{
  "userId": "string",
  "difficultyLevel": "BEGINNER" | "INTERMEDIATE" | "ADVANCED",
  "scenarioId": "string (optional, for predefined scenarios)",
  "customAiRole": "string (optional, for custom scenarios)",
  "customUserRole": "string (optional, for custom scenarios)",
  "customSituation": "string (optional, for custom scenarios)"
}
```

**난이도 설명:**
- `BEGINNER`: 초급 - 일상생활 단어, 짧은 문장, 현재시제
- `INTERMEDIATE`: 중급 - 일반 어휘, 복잡한 문장 구조, 과거/미래시제  
- `ADVANCED`: 고급 - 고급 어휘, 복잡한 문장, 관용구 사용

**시나리오 타입:**
- **정해진 시나리오**: `scenarioId`를 제공 (cafe, restaurant, hotel, shop, doctor, airport, bank)
- **사용자 정의 시나리오**: `customAiRole`, `customUserRole`, `customSituation`을 모두 제공

**Response** (200 OK):
```json
{
  "sessionId": "string",
  "aiRole": "string",
  "userRole": "string",
  "situation": "string",
  "aiFirstGreeting": "string (null for custom scenarios)",
  "createdAt": "string (ISO 8601)"
}
```

**예시 (정해진 시나리오)**:
```bash
curl -X POST http://localhost:8082/api/v1/role-playing/start \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "difficultyLevel": "BEGINNER",
    "scenarioId": "cafe"
  }'
```

**예시 (사용자 정의 시나리오)**:
```bash
curl -X POST http://localhost:8082/api/v1/role-playing/start \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "difficultyLevel": "INTERMEDIATE",
    "customAiRole": "영어 선생님",
    "customUserRole": "학생",
    "customSituation": "학교에서 영어 수업을 듣고 있습니다."
  }'
```

---

## 3. 일반 토픽 세션 시작

### POST `/api/v1/speech-sessions/role-playing`

**설명**: 일반적인 주제로 새로운 영어 회화 세션을 시작합니다.

**Request Body**:
```json
{
  "userId": "string",
  "topic": "string",
  "difficultyLevel": "BEGINNER" | "INTERMEDIATE" | "ADVANCED"
}
```

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

## 4. 대화 진행 (Talk)

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

## 5. 세션 종료 (Session End)

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

## 6. 세션 평가 결과 조회

### GET `/api/v1/speech-sessions/role-playing/{sessionId}/evaluations`

**설명**: 특정 세션의 음성 평가 결과를 조회합니다.

**Path Parameters**:
- `sessionId`: 세션 ID

**Response** (200 OK):
```json
{
  "sessionId": "string",
  "evaluations": [
    {
      "feedback": "string",
      "recommendedDifficulty": "string (optional)",
      "createdAt": "string (ISO 8601)"
    }
  ]
}
```

**예시**:
```bash
curl http://localhost:8082/api/v1/speech-sessions/role-playing/session_123/evaluations
```

---

## 7. 피드백 히스토리 조회 (Feedback History)

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

## 8. 헬스 체크

### GET `/api/v1/speech-sessions/health`

**설명**: 서비스의 상태를 확인합니다.

**Response** (200 OK):
```json
"Session Service is running!"
```

**예시**:
```bash
curl http://localhost:8082/api/v1/speech-sessions/health
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

### 4. 롤 플레잉 시나리오
- **정해진 시나리오**: AI가 "Hello! How can I help you?"로 대화를 시작합니다
- **사용자 정의 시나리오**: AI 첫 인사가 null이므로 사용자가 먼저 대화를 시작해야 합니다
- 동적 상황 생성으로 같은 시나리오도 매번 다른 상황이 제공됩니다

### 5. AI 응답 특성
- AI는 사용자의 영어 실력에 맞는 난이도로 응답합니다
- 중요한 문법 오류나 어휘 오용 시에만 교정을 제안합니다
- 대화의 자연스러운 흐름을 유지합니다




---



