#!/bin/bash

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

echo "============================================="
echo " Starting Zonie (3355) Local Dev Environment "
echo "============================================="

# Spring Boot 프로필을 'local'로 명시적으로 설정
export SPRING_PROFILES_ACTIVE=local

# -d: 백그라운드에서 실행
# --build: 소스 코드가 변경되었을 수 있으니 이미지를 새로 빌드
docker-compose up -d --build

echo "Environment started. Use './scripts/logs.sh' to see logs."
