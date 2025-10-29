#!/bin/bash

# 부모 디렉토리(infra)로 이동합니다.
cd "$(dirname "$0")/.."

echo "=========================================================="
echo " Following logs for all Zonie services (Press Ctrl+C to stop) "
echo "=========================================================="

# docker-compose logs:
# -f (follow): 로그를 실시간으로 스트리밍합니다.
# --tail 100: (선택) 마지막 100줄부터 보여줍니다 (너무 많은 과거 로그 방지)
docker-compose logs -f --tail 100
