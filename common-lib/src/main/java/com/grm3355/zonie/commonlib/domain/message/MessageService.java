package com.grm3355.zonie.commonlib.domain.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

	private final MessageRepository messageRepository;
	private final RedisTemplate<String, Object> redisTemplate; // (Pub/Sub용)
	private final ObjectMapper objectMapper;

	// (PR #12 머지 후 주입받아야 함)
	// private final ChatRoomUserRepository chatRoomUserRepository;

	public void sendMessage(String userId, String roomId, String content) {

		// 1. (차단됨) 닉네임 가져오기
		// TODO: PR #12 머지 후, chatRoomUserRepository에서 닉네임 조회 로직 추가
		// User user = ...; ChatRoom room = ...;
		// String nickname = chatRoomUserRepository.findByUserAndChatRoom(user, room)
		//         .map(ChatRoomUser::getNickname)
		//         .orElse("Unknown");
		String nickname = "임시 닉네임"; // (임시 하드코딩)

		// 2. MongoDB에 메시지 즉시 저장 (R&R 1번)
		Message message = Message.builder()
			.chatRoomId(roomId)
			.userId(userId)
			.nickname(nickname)
			.content(content)
			.type(MessageType.TEXT)
			.createdAt(LocalDateTime.now())
			.build();
		messageRepository.save(message);

		// 3. Redis Pub/Sub으로 다른 서버에 전파
		// (채팅방 구독자들에게 브로드캐스팅)
		try {
			// 객체를 JSON 문자열로 직렬화하여 Publish
			String messageJson = objectMapper.writeValueAsString(message);
			redisTemplate.convertAndSend("chat-room:" + roomId, messageJson);
		} catch (JsonProcessingException e) {
			log.error("Message 직렬화 실패", e);
		}

		// 4. Redis에 마지막 대화 시각 갱신
		redisTemplate.opsForValue().set("chatroom:last_msg_at:" + roomId, String.valueOf(System.currentTimeMillis()));
		// (확장 기능: 마지막 메시지 내용 갱신)
		// redisTemplate.opsForValue().set("chatroom:last_msg_content:" + roomId, content);
	}
}
