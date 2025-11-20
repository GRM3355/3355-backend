package com.grm3355.zonie.chatserver.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("chatRoomParticipationService")
@RequiredArgsConstructor
public class ChatRoomService {

	// Redis Key Prefix 정의 (재사용성 및 가독성 향상)
	private static final String KEY_PARTICIPANTS = "chatroom:participants:"; // 실시간 참여자 (Set)
	private static final String KEY_USER_ROOMS = "user:rooms:"; // 유저별 참여방 (Set)
	private static final String NICKNAME_PREFIX = "#";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomUserRepository chatRoomUserRepository;
	private final UserRepository userRepository;
	private final RedisTemplate<String, Object> redisTemplate;

	@Value("${chat.max-chat-person}")
	private long maxParticipants = 300;

	/**
	 * 사용자가 채팅방에 입장할 때 호출되는 메소드
	 * : API Server에서 DB 가입 완료 후 호출됨 (Redis 실시간 상태 관리만 수행)
	 * @param nickName API Server에서 부여된 닉네임 (Redis Pub/Sub을 통해 전달됨)
	 */
	@Transactional
	public void joinRoom(String userId, String roomId, String nickName) {
		/**
		 * api-server 에서 이미 완료
		 * // 1. 정원 검증
		 * // 2. 엔티티 조회 (DB)
		 * // 3. 재방문자 처리: DB에 ChatRoomUser 데이터가 있는지 확인
		 * 		// 3-A. 재방문자 (이미 DB에 닉네임이 저장되어 있음)
		 * 		// 3-B. 신규 입장자 처리 (Redis INCR 기반 닉네임 순번 획득, ChatRoomUser 엔티티 생성 및 DB 저장)
		 */

		// 4. Redis 저장 (실시간 참여자 목록만, 역방향 매핑)
		redisTemplate.opsForSet().add(KEY_PARTICIPANTS + roomId, userId);
		redisTemplate.opsForSet().add(KEY_USER_ROOMS + userId, roomId);
		log.info("Redis state updated: User {} joined Room {} with Nickname {}", userId, roomId, nickName);
	}

	/**
	 * 연결 끊김 (Disconnect)
	 * : 브라우저 닫기, 연결 끊김 등 명시적 /leave 없이 세션이 끊겼을 때 처리
	 * - Redis 참여자 수 감소 (o)
	 * - DB(ChatRoomUser) 삭제 (x) DB lastReadAt 갱신 (o)
	 * 재방문 (Reconnect) 시 Redis 참여자 수 다시 증가, DB(ChatRoomUser) 조회
	 */
	@Transactional
	public void disconnectUser(String userId) {
		log.info("Disconnect event received for User: {}", userId);

		// 1. 역방향 조회: 이 유저가 참여 중이던 모든 방을 Redis에서 조회
		Set<Object> roomIds = redisTemplate.opsForSet().members(KEY_USER_ROOMS + userId);
		if (roomIds == null || roomIds.isEmpty()) {
			log.info("User {} was not active in any tracked rooms in Redis.", userId);
			return;
		}

		// 2. Redis에서 실시간 참여자 수 감소 및 역방향 매핑 제거
		for (Object roomIdObj : roomIds) {
			String roomId = (String)roomIdObj;

			// 실시간 참여자 목록에서 제거 (**)
			redisTemplate.opsForSet().remove(KEY_PARTICIPANTS + roomId, userId);
			log.debug("Removed user {} from participants set of room {}", userId, roomId);
		}

		// 3. DB 갱신: 마지막 읽은 위치 갱신 (삭제는 안 함 -> 나중에 돌아오면 이어서 보기 가능)
		// User가 참여 중이던 모든 방에 대해 last_read_at을 지금 시간으로 업데이트
		// (안 읽은 메시지 계산 시 '끊어진 시점'까지 읽은 것으로 간주)
		chatRoomUserRepository.updateLastReadAtByUserId(userId, LocalDateTime.now());
		log.info("Updated lastReadAt for user {} in relevant chat rooms.", userId);

		// 4. 역방향 매핑 키 자체 삭제 (다음 접속 시 다시 Set에 추가됨)
		redisTemplate.delete(KEY_USER_ROOMS + userId);
	}

	/**
	 * 사용자가 "나가기" 버튼을 눌러 명시적으로 퇴장할 때 처리
	 * : API Server에서 DB 퇴장 완료 후 호출됨 (Redis 실시간 상태 관리만 수행)
	 */
	@Transactional
	public void leaveRoom(String userId, String roomId) {
		log.info("Leave room event received for User: {}, Room: {}", userId, roomId);

		// 1. 엔티티 조회 (DB): API Server에서 이미 완료

		// 2. Redis에서 실시간 참여자 수 감소 및 역방향 매핑 제거
		redisTemplate.opsForSet().remove(KEY_PARTICIPANTS + roomId, userId);
		redisTemplate.opsForSet().remove(KEY_USER_ROOMS + userId, roomId);

		// 3. DB 삭제: user_chat_rooms 테이블에서 해당 데이터 DELETE (재입장 시 신규 입장자로 간주되어 새 닉네임을 받게 됨): API Server에서 이미 완료

		log.info("Successfully removed user {} from room {} permanently.", userId, roomId);
	}
}
