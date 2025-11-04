# chat-server/Dockerfile
# 1. 빌드(Build) 단계
FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# 빌드 속도 향상을 위해 Gradle 설정 파일만 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

# 각 모듈의 build.gradle 복사 (의존성 캐시용)
COPY common-lib/build.gradle ./common-lib/
COPY api-server/build.gradle ./api-server/
COPY chat-server/build.gradle ./chat-server/
COPY batch-server/build.gradle ./batch-server/

# 의존성을 먼저 다운로드 (소스 코드가 바뀌어도 이 단계는 캐시 재사용)
RUN ./gradlew dependencies

# 전체 소스 코드 복사
COPY . .

# chat-server 모듈의 bootJar를 빌드
RUN ./gradlew :chat-server:bootJar --no-daemon

# 2. 실행(Runtime) 단계
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 빌드 단계(builder)에서 만든 .jar 파일만 복사
COPY --from=builder /app/chat-server/build/libs/*.jar app.jar

# chat-server는 8081 포트 사용
EXPOSE 8081

# 컨테이너 시작 시 실행
ENTRYPOINT ["java", "-jar", "app.jar"]