#!/bin/bash

# 빌드없이 컨테이너만 껐다 켰다할 때 사용하는 스크립트

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

# 스크립트에 전달된 인자(서비스 이름)를 변수에 저장합니다.
SERVICE_NAMES="$@"

if [ -z "$SERVICE_NAMES" ]; then
  echo "Usage: ./scripts/restart.sh [service-name]"
  echo "e.g., ./scripts/restart.sh api-server"
  exit 1
fi

echo "Restarting: $SERVICE_NAMES (without build)..."

# 'restart' 명령어는 빌드 없이 컨테이너만 재시작합니다.
docker-compose restart $@

echo "$SERVICE_NAMES restarted."