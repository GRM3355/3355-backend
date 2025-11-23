# API Server 포트: 8080
API_URL="http://localhost:8080/api/v1"

echo "\n1. Redis Chat/Like Keys 초기화"
curl -X DELETE "${API_URL}/test-management/redis/flush-chat-keys"
