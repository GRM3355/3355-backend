# --- STAGE 1: Build (빌드 환경) ---
FROM gradle:8.7-jdk21-alpine AS builder

WORKDIR /app

# (중요) 모노레포 전체 코드를 복사
COPY . .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# (변경) 'chat-server' 모듈의 bootJar를 빌드
RUN ./gradlew :chat-server:bootJar --no-daemon


# --- STAGE 2: Run (실행 환경) ---
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# (변경) chat-server의 build/libs에서 jar 파일을 복사
COPY --from=builder /app/chat-server/build/libs/chat-server-*.jar app.jar

# (변경) chat-server는 다른 포트(예: 8081) 사용 권장
EXPOSE 8081

# 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
