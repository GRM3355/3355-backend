#!/bin/bash

# 로컬에서 IDE로 앱을 직접 실행할 때 사용하는 스크립트
# api-server, chat-server 등을 빌드하지 않고 오직 DB 컨테이너 3개만 실행
# 이 스크립트 실행 후
# 로컬 IDE에서 앱을 직접 실행하거나 터미널에서 ./gradlew :api-server:bootRun 와 같이 실행해 사용하면 됨

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

echo "============================================="
echo " Starting ONLY Database Services (Hybrid Mode) "
echo "============================================="

# Spring Boot 프로필을 'local'로 명시적으로 설정
export SPRING_PROFILES_ACTIVE=local

# docker-compose.yml에 정의된 DB 서비스 3개만 명시적으로 실행합니다.
docker-compose up -d postgres_db redis_cache mongo_db

echo "DB services (Postgres, Redis, Mongo) are up."
echo "Now, run your Spring Boot apps (api, chat, batch) from your IDE or terminal."
echo "e.g., ./gradlew :api-server:bootRun"