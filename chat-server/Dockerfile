# chat-server/Dockerfile
# --- 빌드(Build) 단계 ---
# 1. Build (빌드 환경)
# Java 21과 호환되는 Gradle 8.7+ 버전 사용
FROM gradle:8.7-jdk21-alpine AS builder
# 작업 디렉토리 설정
WORKDIR /app

# 2. Gradle 캐시 최적화
# 빌드 속도 향상을 위해 Gradle 설정 파일만 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

# 3. 필요한 모듈의 build.gradle만 복사 (의존성 캐시용)
COPY common-lib/build.gradle ./common-lib/
COPY chat-server/build.gradle ./chat-server/

# 4. 의존성 미리 다운로드 (RUN ./gradlew dependencies) (:chat-server:dependencies --no-daemon; 최적화)
RUN ./gradlew :chat-server:dependencies --no-daemon

# 5. (COPY .) 대신 필요한 모듈의 소스 코드만 복사
COPY common-lib ./common-lib
COPY chat-server ./chat-server

# 6. 빌드
# gradlew 실행 권한 부여
RUN chmod +x ./gradlew
# chat-server 모듈의 bootJar만 빌드 (시간 절약)
RUN ./gradlew :chat-server:bootJar --no-daemon


# --- 실행(Runtime) 단계 ---
# 1. 준비
# Java 21 JRE(Java 실행 환경)만 포함된 가벼운 이미지 사용
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# chat-server의 .jar 파일을 복사: builder 스테이지에서 빌드한 jar 파일만 복사
# (경로 주의: batch-server-0.0.1-SNAPSHOT.jar 같은 파일을 찾음)
COPY --from=builder /app/chat-server/build/libs/*.jar app.jar

# 2. chat-server는 8081 포트 사용
EXPOSE 8081

# 3. 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]