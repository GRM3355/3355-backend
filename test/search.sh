source test_vars.sh
AUTH_HEADER=$AUTH_HEADER
API_URL=$API_URL
ROOM_A_ID=$ROOM_A_ID
ROOM_B_ID=$ROOM_B_ID

if [ -z "$ROOM_A_ID" ] || [ -z "$ROOM_B_ID" ]; then
    echo "FATAL ERROR: Room ID가 확보되지 않아 통합 검색 테스트를 건너뜁니다."
    exit 1
fi
echo "Room A Title: Room A (Older), Room B Title: Room B (Newer)"
echo "Test Festival Title: 실시간 정렬 테스트 축제"

# --- 테스트 1: 채팅방 제목 키워드 검색 ---
# Room A, Room B 제목에 공통으로 포함된 키워드 (예: Older, Newer를 피하기 위해 "Room" 사용)
KEYWORD_ROOM="Room"
echo -e "\n1. 채팅방 제목 키워드 검색 시작 (Keyword: $KEYWORD_ROOM)"

SEARCH_ROOM_RESPONSE=$(curl -s -X GET "${API_URL}/search?keyword=$KEYWORD_ROOM" \
  -H "$AUTH_HEADER")

CHAT_ROOM_COUNT=$(echo "$SEARCH_ROOM_RESPONSE" | jq -r '.data.chatRooms.data | length')

echo "Raw Response (ChatRooms): $SEARCH_ROOM_RESPONSE"

if [ "$CHAT_ROOM_COUNT" -ge 2 ]; then
    echo "[성공] 채팅방 검색: 키워드 '$KEYWORD_ROOM'로 2개 이상의 방이 검색되었습니다. (결과 수: $CHAT_ROOM_COUNT)"
else
    echo "[실패] 채팅방 검색: 예상보다 적은 수의 방이 검색되었습니다. (결과 수: $CHAT_ROOM_COUNT). Native Query 확인 필요."
fi

# --- 테스트 2: 축제 제목 키워드 검색 (채팅방 제목에 없는 키워드) ---
# 기획 의도: 축제 제목으로 검색해도, 채팅방 제목에 키워드가 없으면 검색되지 않아야 함.
KEYWORD_FESTIVAL="정렬"
echo -e "\n2. 축제 제목 키워드 검색 시작 (Keyword: $KEYWORD_FESTIVAL)"

# URL 인코딩 적용 (Mac/Linux 환경에서 일반적으로 사용 가능)
ENCODED_KEYWORD=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$KEYWORD_FESTIVAL'))")

# SEARCH_FESTIVAL_RESPONSE 호출
SEARCH_FESTIVAL_RESPONSE=$(curl -s -X GET "${API_URL}/search?keyword=$ENCODED_KEYWORD" \
  -H "$AUTH_HEADER")

FESTIVAL_COUNT=$(echo "$SEARCH_FESTIVAL_RESPONSE" | jq -r '.data.festivals.data | length')
CHAT_ROOM_COUNT_FESTIVAL=$(echo "$SEARCH_FESTIVAL_RESPONSE" | jq -r '.data.chatRooms.data | length')


echo "--- 결과 확인 ---"
echo "축제 목록 수 (기대: 1 이상): $FESTIVAL_COUNT"
echo "채팅방 목록 수 (기대: 0): $CHAT_ROOM_COUNT_FESTIVAL"


if [ "$FESTIVAL_COUNT" -ge 1 ] && [ "$CHAT_ROOM_COUNT_FESTIVAL" -eq 0 ]; then
    echo "[성공] 축제 검색: 축제 목록은 확인되었고, 채팅방 목록은 기획 의도대로 검색되지 않았습니다. (0개)"
else
    echo "[실패] 축제 검색: 축제 목록 필터링 또는 채팅방 목록 필터링이 기획 의도와 다릅니다. (축제 수: $FESTIVAL_COUNT, 채팅방 수: $CHAT_ROOM_COUNT_FESTIVAL)"
    echo "Raw Response (Festivals): $SEARCH_FESTIVAL_RESPONSE"
fi