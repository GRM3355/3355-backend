# --- STAGE 1: Build (빌드 환경) ---
# Java 21과 호환되는 Gradle 8.7+ 버전 사용
FROM gradle:8.7-jdk21-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 모노레포 전체 코드를 복사해야 common-lib와 함께 빌드 가능 (*)
COPY . .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 'api-server' 모듈의 bootJar만 빌드 (시간 절약)
RUN ./gradlew :api-server:bootJar --no-daemon


# --- STAGE 2: Run (실행 환경) ---
# Java 21 JRE(Java 실행 환경)만 포함된 가벼운 이미지 사용
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# builder 스테이지에서 빌드한 jar 파일만 복사
# (경로 주의: api-server-0.0.1-SNAPSHOT.jar 같은 파일을 찾음)
COPY --from=builder /app/api-server/build/libs/api-server-*.jar app.jar

# 8080 포트 개방 (Spring Boot 기본 포트)
EXPOSE 8080

# 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
