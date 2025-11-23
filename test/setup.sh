# 1단계 스크립트: setup.sh

# API Server 포트: 8080
API_URL="http://localhost:8080/api/v1"

#echo "1. Redis Chat/Like Keys 초기화"
curl -X DELETE "${API_URL}/test-management/redis/flush-chat-keys"

echo -e "\n2. 게스트 유저 생성 및 토큰 획득"
GUEST_RESPONSE=$(curl -s -X POST "${API_URL}/test-management/auth/tokens" \
  -H "Content-Type: application/json" \
  -d '{"lat": 37.568131, "lon": 126.969649}')

ACCESS_TOKEN=$(echo $GUEST_RESPONSE | jq -r '.data.accessToken')

if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    echo "ERROR: Access Token 획득 실패."
    echo "Guest Response: $GUEST_RESPONSE"
    exit 1
fi
AUTH_HEADER="Authorization: Bearer $ACCESS_TOKEN"
echo "ACCESS_TOKEN (Prefix): ${ACCESS_TOKEN:0:20}..."

# 환경 변수 저장 (다음 단계에서 사용)
echo "export AUTH_HEADER=\"$AUTH_HEADER\"" > test_vars.sh
echo "export API_URL=\"$API_URL\"" >> test_vars.sh

echo -e "\n1단계 완료. 'test_vars.sh' 파일이 생성되었습니다."