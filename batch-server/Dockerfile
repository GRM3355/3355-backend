# batch-server/Dockerfile
# 1. Build (빌드 환경)
# Java 21과 호환되는 Gradle 8.7+ 버전 사용
FROM gradle:8.7-jdk21-alpine AS builder

# 작업 디렉토리 설정
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

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 'batch-server' 모듈의 bootJar만 빌드 (시간 절약)
RUN ./gradlew :batch-server:bootJar --no-daemon


# 2. 실행(Runtime) 단계
# Java 21 JRE(Java 실행 환경)만 포함된 가벼운 이미지 사용
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# builder 스테이지에서 빌드한 jar 파일만 복사
# (경로 주의: batch-server-0.0.1-SNAPSHOT.jar 같은 파일을 찾음)
COPY --from=builder /app/batch-server/build/libs/batch-server-*.jar app.jar

# ~~8080 포트 개방 (Spring Boot 기본 포트)~~ (Batch 서버는 외부 포트를 열 필요가 없음)
#EXPOSE 8080

# 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
