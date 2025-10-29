#!/bin/bash

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

echo "==========================================="
echo " Stopping Zonie (3355) Local Dev Environment "
echo "==========================================="

# docker-compose down:
# 정의된 모든 컨테이너를 중지(stop)하고 제거(remove)합니다.
docker-compose down

echo "All services stopped."