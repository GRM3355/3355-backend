# api-server/Dockerfile
# 1. 빌드(Build) 단계
FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# (chat-server와 동일: Gradle 캐시 최적화)
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY common-lib/build.gradle ./common-lib/
COPY api-server/build.gradle ./api-server/
COPY chat-server/build.gradle ./chat-server/
COPY batch-server/build.gradle ./batch-server/
RUN ./gradlew dependencies
COPY . .

# api-server 모듈을 빌드
RUN ./gradlew :api-server:bootJar --no-daemon

# 2. 실행(Runtime) 단계
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# api-server의 .jar 파일을 복사
COPY --from=builder /app/api-server/build/libs/*.jar app.jar

# api-server는 8080 포트 사용
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]