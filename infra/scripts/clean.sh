#!/bin/bash

cd "$(dirname "$0")/.."

# stop.sh은 컨테이너만 제거
# clean.sh은 DB 데이터(볼륨)까지 초기화
echo "========================================================================="
echo " WARNING: This will STOP all services AND DELETE ALL LOCAL DATA "
echo " (PostgreSQL, Redis, MongoDB data will be wiped.)"
echo "========================================================================="
echo

# 사용자에게 위험한 작업임을 확인받습니다.
read -p "Are you sure you want to continue? (y/n) " -n 1 -r
echo    # (new line)

if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo "Stopping containers and removing all volumes..."

    # docker-compose down:
    # -v (volumes): 컨테이너와 연결된 '볼륨(데이터)'까지 함께 삭제합니다.
    docker-compose down -v

    echo "All data wiped successfully."
else
    echo "Canceled."
fi
