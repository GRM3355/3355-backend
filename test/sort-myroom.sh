# 3단계 스크립트: sort_check.sh

source test_vars.sh
AUTH_HEADER=$AUTH_HEADER
API_URL=$API_URL
ROOM_A_ID=$ROOM_A_ID
ROOM_B_ID=$ROOM_B_ID

if [ -z "$ROOM_A_ID" ] || [ -z "$ROOM_B_ID" ]; then
    echo "FATAL ERROR: Room ID가 확보되지 않아 정렬 테스트를 건너뜁니다."
    exit 1
fi
echo "room a id = ${ROOM_A_ID}, room b id = ${ROOM_B_ID}"

echo -e "\n6. 초기 정렬 확인: Room B (Newer)가 상단에 와야 함 (CreatedAt 복합 정렬)"
LIST_RESPONSE=$(curl -s -X GET "${API_URL}/chat-rooms/my-rooms?page=1&pageSize=10&order=ACTIVE_DESC" \
  -H "$AUTH_HEADER")

echo "--- 초기 목록 순서 ---"
echo "Raw List Response: ${LIST_RESPONSE}"

FIRST_ROOM_ID=$(echo $LIST_RESPONSE | jq -r '.data.content[0].chatRoomId')
SECOND_ROOM_ID=$(echo $LIST_RESPONSE | jq -r '.data.content[1].chatRoomId')

echo "--- 초기 목록 순서 ---"
echo "1위: $(echo $LIST_RESPONSE | jq -r '.data.content[0].title') (ID: ${FIRST_ROOM_ID})"
echo "2위: $(echo $LIST_RESPONSE | jq -r '.data.content[1].title') (ID: ${SECOND_ROOM_ID})"

if [ "$FIRST_ROOM_ID" == "$ROOM_B_ID" ] && [ "$SECOND_ROOM_ID" == "$ROOM_A_ID" ]; then
    echo "[성공] 초기 정렬: Room B (Newer) > Room A (Older). CreatedAt 복합 정렬 작동."
else
    echo "[실패] 초기 정렬: 순서가 기대와 다릅니다. CreatedAt 복합 정렬 실패."
fi

# 7. Room A에 메시지 전송 흉내 (ZSET Score 갱신)
# 실제로는 STOMP 클라이언트 툴을 이용해 메세지를 전송해봐야 함
echo -e "\n7. Room A에 메시지 전송 흉내 (실시간 정렬 유도)"
# Room B의 createdAt보다 훨씬 큰 값(예: +10분)을 하드코딩
CURRENT_TIME="1763938439093"
echo "강제로 Room A의 ZSET Score를 ${CURRENT_TIME}으로 갱신."
docker exec zonie_redis_cache redis-cli ZADD chatroom:active_rooms $CURRENT_TIME $ROOM_A_ID > /dev/null

# Redis String Last Message At 값도 갱신
echo "강제로 Room A의 Last Message At String 값을 ${CURRENT_TIME}으로 갱신."
docker exec zonie_redis_cache redis-cli SET "chatroom:last_msg_at:${ROOM_A_ID}" "${CURRENT_TIME}" > /dev/null

sleep 1

# 8. 실시간 목록 조회 (Room A가 상단으로 이동했는지 확인)
echo -e "\n8. 실시간 정렬 확인: Room A가 상단으로 이동했는지 확인 (Last_message_at 정렬)"
LIST_RESPONSE_AFTER_MSG=$(curl -s -X GET "${API_URL}/chat-rooms/my-rooms?page=1&pageSize=10&order=ACTIVE_DESC" \
  -H "$AUTH_HEADER")
FIRST_ROOM_ID_AFTER=$(echo $LIST_RESPONSE_AFTER_MSG | jq -r '.data.content[0].chatRoomId')

echo "--- 갱신 후 목록 순서 ---"
echo "1위: $(echo $LIST_RESPONSE_AFTER_MSG | jq -r '.data.content[0].title') (ID: ${FIRST_ROOM_ID_AFTER})"

if [ "$FIRST_ROOM_ID_AFTER" == "$ROOM_A_ID" ]; then
    echo "[최종 성공] 실시간 정렬: Room A가 LastMessageAt 기준으로 즉시 상단 이동 확인."
else
    echo "[최종 실패] 실시간 정렬: Room A가 상단으로 이동하지 못했습니다."
fi