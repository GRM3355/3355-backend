#!/bin/bash

# 인자 여부에 따라 서비스 빌드하고 띄우는 스크립트
# 인자가 없으면 (./start.sh): 모든 서비스를 빌드하고 띄움
# 인자가 있으면 (./start.sh api-server): 'api-server' 서비스만 빌드하고 띄움

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

# 스크립트에 전달된 인자를 변수에 저장 (서비스명)
# 예: ./start.sh api-server
SERVICE_NAMES="$@"

echo "============================================="
echo " Starting Zonie (3355) Local Dev Environment "
echo "============================================="

# Spring Boot 프로필을 'local'로 명시적으로 설정
export SPRING_PROFILES_ACTIVE=local

# -d: 백그라운드에서 실행
# --build: 소스 코드가 변경되었을 수 있으니 이미지를 새로 빌드
# $@: 스크립트에 전달된 인자(SERVICE_NAMES)를 docker-compose 명령으로 그대로 넘김
docker-compose up -d --build $@

# -z $VAR: $VAR가 비어있는지 확인
if [ -z "$SERVICE_NAMES" ]; then
  echo "Environment started for ALL services."
else
  echo "Environment started for: $SERVICE_NAMES"
fi

echo "Use './scripts/logs.sh' to see logs."