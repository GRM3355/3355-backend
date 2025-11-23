#!/bin/bash

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

echo "==========================================="
echo " Stopping Zonie (3355) Local Dev Environment "
echo "==========================================="

# 스크립트에 전달된 인자를 변수에 저장 (서비스명)
# 예: ./stop.sh api-server redis-cache
SERVICE_NAMES="$@"

# 인자가 있을 경우 docker-compose stop을 사용해 특정 서비스만 중지합니다.
if [ -n "$SERVICE_NAMES" ]; then
  # -n $VAR: $VAR가 비어있지 않은지 확인
  echo "Stopping services: $SERVICE_NAMES"
  docker-compose stop $SERVICE_NAMES

  # 중지된 서비스 컨테이너는 제거하지 않고 남겨둡니다. (재시작 용이)

# 인자가 없을 경우 (./stop.sh), 모든 서비스를 중지하고 컨테이너를 제거합니다.
else
  echo "Stopping and removing ALL services (docker-compose down)"
  # docker-compose down: 정의된 모든 컨테이너를 중지(stop)하고 제거(remove)합니다.
  docker-compose down
fi

echo "All specified services stopped."