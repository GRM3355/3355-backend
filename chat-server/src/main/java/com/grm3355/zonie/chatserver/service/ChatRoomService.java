package com.grm3355.zonie.chatserver.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@Slf4j
@Service("chatRoomParticipationService")
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository; // ChatRoom 엔티티용 레포지토리 (가정)
	private final ChatRoomUserRepository chatRoomUserRepository;
	private final UserRepository userRepository; // User 엔티티용 레포지토리 (가정)

	private final RedisTemplate<String, Object> redisTemplate;

	// Redis Key Prefix 정의 (재사용성 및 가독성 향상)
	private static final String KEY_PARTICIPANTS = "chatroom:participants:"; // 실시간 참여자 (Set)
	private static final String KEY_USER_ROOMS = "user:rooms:"; // 유저별 참여방 (Set)
	private static final long MAX_PARTICIPANTS = 300;

	private static final String NICKNAME_PREFIX = "#";

	/**
	 * 사용자가 채팅방에 입장할 때 호출되는 메소드
	 */
	@Transactional
	public String joinRoom(String userId, String roomId) {
		// 1. (REQ-CHATLIST-14) 정원 검증
		Long currentCount = redisTemplate.opsForSet().size(KEY_PARTICIPANTS + roomId);
		if (currentCount != null && currentCount >= MAX_PARTICIPANTS) {
			throw new BusinessException(ErrorCode.CONFLICT, "채팅방 최대 정원(" + MAX_PARTICIPANTS + "명)을 초과했습니다.");
		}

		// 2. 엔티티 조회 (DB)
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다."));
		log.info("USER FETCHED: {}", user.getUserId());
		log.info("ROOM FETCHED: {}", room.getChatRoomId());


		// 3. (REQ-CHAT-08) 재방문자 처리: DB에 ChatRoomUser 데이터가 있는지 확인
		Optional<ChatRoomUser> existingParticipant = chatRoomUserRepository.findByUserAndChatRoom(user, room);

		if (existingParticipant.isPresent()) {
			// 3-A. 재방문자 (이미 DB에 닉네임이 저장되어 있음)
			log.info("User {} is a returning member of room {}.", userId, roomId);

			// Redis에만 추가/갱신 (실시간 참여자 목록 및 역방향 매핑)
			redisTemplate.opsForSet().add(KEY_PARTICIPANTS + roomId, userId);
			redisTemplate.opsForSet().add(KEY_USER_ROOMS + userId, roomId);

			// existingParticipant에서 닉네임 반환
			return existingParticipant.get().getNickName();
		}

		// 3-B. 신규 입장자 처리 (Redis INCR 기반)

		// 1. Redis에서 닉네임 순번 획득 (원자성 보장)
		// KEY: chatroom:nickname_seq:{roomId}
		String redisKey = "chatroom:nickname_seq:" + roomId;
		redisTemplate.opsForValue().setIfAbsent(redisKey, 3354L);	// 3355번부터 부여
		Long nicknameSeq = redisTemplate.opsForValue().increment(redisKey, 1);
		String newNickname = NICKNAME_PREFIX + nicknameSeq;

		// 2. ChatRoomUser 엔티티 생성 및 DB 저장
		ChatRoomUser newParticipant = ChatRoomUser.builder()
			.user(user)
			.chatRoom(room)
			.nickName(newNickname)
			.lastReadAt(LocalDateTime.now()) // 최초 입장 시점
			.build();

		try {
			chatRoomUserRepository.save(newParticipant);
		} catch (DataIntegrityViolationException e) {
			// Redis 카운터 리셋 등 매우 희귀한 경우에 대비한 방어 코드
			log.error("Critical: Nickname collision occurred despite using Redis counter.", e);
			throw new BusinessException(ErrorCode.CONFLICT, "닉네임 생성에 실패했습니다. 시스템 오류.");
		}

		// 3. Redis 저장 (실시간 참여자 목록 및 역방향 매핑)
		redisTemplate.opsForSet().add(KEY_PARTICIPANTS + roomId, userId);
		redisTemplate.opsForSet().add(KEY_USER_ROOMS + userId, roomId);

		return newParticipant.getNickName();
	}

	/**
	 * 브라우저 닫기, 연결 끊김 등 명시적 /leave 없이 세션이 끊겼을 때 처리
	 * @param userId
	 */
	@Transactional
	public void disconnectUser(String userId) {
		log.info("Disconnect event received for User: {}", userId);

		// 1. (REQ-CHAT-10) 역방향 조회: 이 유저가 참여 중이던 모든 방을 Redis에서 조회
		Set<Object> roomIds = redisTemplate.opsForSet().members(KEY_USER_ROOMS + userId);

		if (roomIds == null || roomIds.isEmpty()) {
			log.info("User {} was not active in any tracked rooms in Redis.", userId);
			return;
		}

		// 2. Redis에서 실시간 참여자 수 감소 및 역방향 매핑 제거
		for (Object roomIdObj : roomIds) {
			String roomId = (String) roomIdObj;

			// 실시간 참여자 목록에서 제거
			redisTemplate.opsForSet().remove(KEY_PARTICIPANTS + roomId, userId);
			log.debug("Removed user {} from participants set of room {}", userId, roomId);
		}

		// 3. (Ext 2) DB 갱신: User가 참여 중이던 모든 방에 대해 last_read_at을 지금 시간으로 업데이트
		//    (안 읽은 메시지 계산 시 '끊어진 시점'까지 읽은 것으로 간주)
		chatRoomUserRepository.updateLastReadAtByUserId(userId, LocalDateTime.now());
		log.info("Updated lastReadAt for user {} in relevant chat rooms.", userId);

		// 4. 역방향 매핑 키 자체 삭제 (다음 접속 시 다시 Set에 추가됨)
		redisTemplate.delete(KEY_USER_ROOMS + userId);
	}

	/**
	 * 사용자가 "나가기" 버튼을 눌러 명시적으로 퇴장할 때 처리
	 * @param userId
	 * @param roomId
	 */
	@Transactional
	public void leaveRoom(String userId, String roomId) {
		log.info("Leave room event received for User: {}, Room: {}", userId, roomId);

		// 1. 엔티티 조회 (DB)
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

		// 2. Redis에서 실시간 참여자 수 감소 및 역방향 매핑 제거 (특정 방만)
		redisTemplate.opsForSet().remove(KEY_PARTICIPANTS + roomId, userId);
		redisTemplate.opsForSet().remove(KEY_USER_ROOMS + userId, roomId);

		// 3. (REQ-CHAT-08) DB 삭제: user_chat_rooms 테이블에서 해당 데이터 DELETE
		//    (재입장 시 신규 입장자로 간주되어 새 닉네임을 받게 됨)
		chatRoomUserRepository.deleteByUserAndChatRoom(user, room);
		log.info("Successfully removed user {} from room {} permanently.", userId, roomId);
	}
}