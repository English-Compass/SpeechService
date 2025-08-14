# Spring Boot 애플리케이션을 위한 멀티 스테이지 빌드
FROM eclipse-temurin:17-jdk AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼 및 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드 (테스트 제외)
RUN ./gradlew clean build -x test

# 프로덕션 스테이지
FROM eclipse-temurin:17-jre AS production

# 디버깅 및 모니터링을 위한 필요 패키지 설치
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 보안을 위한 non-root 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# 빌더 스테이지에서 빌드된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# non-root 사용자로 소유권 변경
RUN chown -R appuser:appuser /app
USER appuser

# 포트 노출
EXPOSE 8081

# 헬스 체크 설정
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/speech/health || exit 1

# 컨테이너 환경을 위한 JVM 옵션 설정 (Java 17 호환)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Xlog:gc*:stdout:time"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]