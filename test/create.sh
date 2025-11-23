# 2단계 스크립트: create_and_check.sh

source test_vars.sh
AUTH_HEADER=$AUTH_HEADER
API_URL=$API_URL

echo -e "\n3. 테스트 축제 생성 (Event Date: 2025-11-01)"
FESTIVAL_RESPONSE=$(curl -s -X POST "${API_URL}/test-management/festivals" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "실시간 정렬 테스트 축제",
    "addr1": "테스트 주소",
    "eventStartDate": "2025-11-01",
    "eventEndDate": "2025-12-31",
    "region": "SEOUL",
    "lat": 37.568131, "lon": 126.969649,
    "contentId":9999999
  }')
echo "Festival created."

# 3. 응답에서 DB가 할당한 festivalId를 추출
FESTIVAL_ID=$(echo $FESTIVAL_RESPONSE | jq -r '.data.festivalId')
echo "Extracted FESTIVAL ID: $FESTIVAL_ID"

if [ "$FESTIVAL_ID" == "null" ] || [ -z "$FESTIVAL_ID" ]; then
    echo "FATAL ERROR: Festival ID 추출 실패. 응답 JSON을 확인하세요."
    echo "Raw Response: $FESTIVAL_RESPONSE"
    exit 1
fi

# 4. Room A 생성 시도
echo -e "\n4. Room A 생성 시도 (유효성 통과 여부 확인)"
ROOM_A_TITLE="Room A (Older)"
ROOM_A_RESPONSE=$(curl -s -X POST "${API_URL}/festivals/${FESTIVAL_ID}/chat-rooms" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"$ROOM_A_TITLE\",\"lat\":37.56813,\"lon\":126.9696}")
echo "Room A Raw Response: $ROOM_A_RESPONSE"

ROOM_A_ID=$(echo $ROOM_A_RESPONSE | jq -r '.data.chatRoomId')
echo "Room A ID: $ROOM_A_ID"

if [ "$ROOM_A_ID" == "null" ] || [ -z "$ROOM_A_ID" ]; then
    echo "[실패 지점 확인] ChatRoom 생성 실패. 원인은 NOT_FOUND (유효성 검증 실패)."
else
    echo "[예상 밖 성공] ChatRoom 생성 성공. 다음 단계로 진행합니다."
    echo "export ROOM_A_ID=\"$ROOM_A_ID\"" >> test_vars.sh
    sleep 2

    # Room B 생성 시도 (Room A가 성공했을 경우에만 시도)
    echo -e "\n5. Room B 생성 시도"
    ROOM_B_TITLE="Room B (Newer)"
    ROOM_B_RESPONSE=$(curl -s -X POST "${API_URL}/festivals/${FESTIVAL_ID}/chat-rooms" \
      -H "$AUTH_HEADER" \
      -H "Content-Type: application/json" \
      -d "{\"title\":\"$ROOM_B_TITLE\",\"lat\":37.56813,\"lon\":126.9696}")
    ROOM_B_ID=$(echo $ROOM_B_RESPONSE | jq -r '.data.chatRoomId')
    echo "Room B ID: $ROOM_B_ID"
    echo "export ROOM_B_ID=\"$ROOM_B_ID\"" >> test_vars.sh
fi