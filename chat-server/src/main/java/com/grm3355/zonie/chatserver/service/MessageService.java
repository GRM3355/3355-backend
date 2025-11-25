package com.grm3355.zonie.chatserver.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.enums.MessageType;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

	private final MessageRepository messageRepository;
	private final RedisTemplate<String, Object> redisTemplate;    // (Pub/Sub용)
	private final StringRedisTemplate stringRedisTemplate;        // 순수 문자열 저장용
	private final ObjectMapper objectMapper;
	private final ChatRoomUserRepository chatRoomUserRepository;
	private final UserRepository userRepository;
	private final ChatRoomRepository chatRoomRepository;

	public void sendMessage(String userId, String roomId, String content) {

		// 1. 사용자 정보 및 채팅방 정보 - 닉네임 조회
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다. (MessageService)"));

		ChatRoom room = chatRoomRepository.findByChatRoomId(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다. (MessageService)"));

		String nickname = chatRoomUserRepository.findByUserAndChatRoom(user, room)
			.map(ChatRoomUser::getNickName)
			.orElse("알 수 없음");

		// 2. MongoDB에 메시지 즉시 저장
		Message message = Message.builder()
			.chatRoomId(roomId)
			.userId(userId)
			.nickname(nickname)
			.content(content)
			.type(MessageType.TEXT)
			.createdAt(LocalDateTime.now())
			.likeCount(0)
			.likedByUserIds(new java.util.HashSet<>())
			.build();
		messageRepository.save(message);

		// 3. Redis Pub/Sub으로 다른 서버에 전파 (채팅방 구독자들에게 브로드캐스팅)
		try {
			// message 객체 원본 전송
			redisTemplate.convertAndSend("chat-room:" + roomId, message);
		} catch (Exception e) {
			log.error("Message Pub/Sub 발행 실패", e);
		}

		// 4. 마지막 대화 시각
		long createdAtLong = room.getCreatedAt()
			.atZone(ZoneId.systemDefault())
			.toInstant()
			.toEpochMilli(); // LocalDateTime -> Long
		Long lastMessageAtLong = System.currentTimeMillis();

		// 4-1. Composite Score 계산 및 ZSET 갱신
		// Score = Last Message At (정수부) + Created At (소수부)
		// CreatedAt을 10^16으로 나누어 소수점 이하로 만듦 (정밀도 이슈 감수)
		double compositeScore = lastMessageAtLong.doubleValue() + ((double)createdAtLong / 10000000000000000D);

		// 4-1. Redis에 ZSET에 등록/갱신
		// ZADD key score member
		// 실시간 정렬 및 페이지네이션 (Score를 통한 랭킹 관리) 목적으로만 사용함
		stringRedisTemplate.opsForZSet().add("chatroom:active_rooms", roomId, compositeScore);

		// 4-2. Redis에 갱신
		// 정확한 실시간 타임스탬프 원본 Long 값
		stringRedisTemplate.opsForValue().set("chatroom:last_msg_at:" + roomId, String.valueOf(lastMessageAtLong));

		// 5. Redis에 마지막 메시지 내용 갱신 - String Template
		String lastContent = nickname + ": " + content;
		stringRedisTemplate.opsForValue().set("chatroom:last_msg_content:" + roomId, lastContent);
	}
}
