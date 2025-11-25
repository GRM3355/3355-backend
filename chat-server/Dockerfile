# [수정] 'builder' 스테이지(FROM gradle...) 전부 삭제

# --- 실행(Runtime) 단계 ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# CI 러너에서 chat-server의 .jar 파일을 '최종 이미지'로 복사
COPY chat-server/build/libs/chat-server-*.jar app.jar

# chat-server는 8081 포트 사용
EXPOSE 8081

# 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]