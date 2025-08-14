# Speech Service

스프링 부트 기반의 음성 변환 서비스입니다.

## 기능

- Text-to-Speech (TTS): 텍스트를 음성으로 변환
- Speech-to-Text (STT): 음성을 텍스트로 변환
- REST API 제공
- H2 인메모리 데이터베이스 사용

## 기술 스택

- Java 17
- Spring Boot 3.2.0
- Gradle 8.5
- H2 Database
- JPA/Hibernate

## 실행 방법

### 1. Java 17 설치 확인
```bash
java -version
```

### 2. 프로젝트 빌드
```bash
./gradlew build
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```

또는
```bash
java -jar build/libs/speech-service-0.0.1-SNAPSHOT.jar
```

## API 엔드포인트

### 헬스 체크
```
GET /api/speech/health
```

### Text-to-Speech
```
POST /api/speech/text-to-speech
Content-Type: text/plain

텍스트 내용
```

### Speech-to-Text
```
POST /api/speech/speech-to-text
Content-Type: application/octet-stream

[오디오 바이너리 데이터]
```

## 개발 환경

- IDE: IntelliJ IDEA, Eclipse, VS Code
- 빌드 도구: Gradle
- 데이터베이스: H2 Console (http://localhost:8081/h2-console)

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/example/speechservice/
│   │       ├── SpeechServiceApplication.java
│   │       ├── controller/
│   │       │   └── SpeechController.java
│   │       └── service/
│   │           └── SpeechService.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/
        └── com/example/speechservice/
            └── SpeechServiceApplicationTests.java
```

## 라이센스

Apache License 2.0
