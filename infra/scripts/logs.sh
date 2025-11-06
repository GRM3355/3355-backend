#!/bin/bash

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

# 스크립트에 전달된 인자(서비스 이름)를 변수에 저장합니다.
# $@는 스크립트로 전달된 모든 인자를 의미합니다. (예: "api-server" 또는 "api-server chat-server")
SERVICE_NAMES="$@"

# 인자가 비어있는지 확인합니다.
if [ -z "$SERVICE_NAMES" ]; then
  # 인자가 없으면 (그냥 ./logs.sh 실행 시)
  echo "=========================================================="
  echo " Following logs for ALL Zonie services (Press Ctrl+C to stop) "
  echo "=========================================================="
else
  # 인자가 있으면 (예: ./logs.sh chat-server 실행 시)
  echo "=========================================================="
  echo " Following logs for: $SERVICE_NAMES (Press Ctrl+C to stop) "
  echo "=========================================================="
fi

# 모듈별 로그 스크립트 사용
#  ./scripts/logs.sh
#  ./scripts/logs.sh api-server chat-server
#  ./scripts/logs.sh chat-server
#  ./scripts/logs.sh api-server
# docker-compose logs:
# -f (follow): 로그를 실시간으로 스트리밍합니다.
# --tail 100: (선택) 마지막 100줄부터 보여줍니다 (너무 많은 과거 로그 방지)
# $@: 스크립트에 전달된 모든 인자를 docker-compose logs 명령으로 그대로 넘깁니다.
docker-compose logs -f --tail 300 $@