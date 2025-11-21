package com.grm3355.zonie.chatserver.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;

// 실제 Redis에 쓰는 것을 테스트하기 위해 @SpringBootTest 유지
@SpringBootTest
class MessageServiceIntegrationTest {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private MessageService messageService;

	// DB 의존성 Mocking
	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean
	private ChatRoomRepository chatRoomRepository;
	@MockitoBean
	private ChatRoomUserRepository chatRoomUserRepository;

	@Test
	void shouldWriteCleanTimestampToRedis() {
		String userId = "test-user";
		String roomId = "test-room-1";
		String redisKey = "chatroom:last_msg_at:" + roomId;

		// 테스트 전 Redis 데이터 정리
		stringRedisTemplate.delete(redisKey);

		// 1. Mocking 설정: sendMessage가 실행될 수 있도록 필수 객체 반환 설정
		User mockUser = User.builder().userId(userId).build();
		ChatRoom mockRoom = ChatRoom.builder().chatRoomId(roomId).title("Test Room").build();
		ChatRoomUser mockChatRoomUser = ChatRoomUser.builder()
			.nickName("닉넴")
			.user(mockUser)
			.chatRoom(mockRoom)
			.build();

		given(userRepository.findByUserId(userId)).willReturn(Optional.of(mockUser));
		given(chatRoomRepository.findByChatRoomId(roomId)).willReturn(Optional.of(mockRoom));
		given(chatRoomUserRepository.findByUserAndChatRoom(mockUser, mockRoom)).willReturn(
			Optional.of(mockChatRoomUser));

		// 2. 메시지 전송 로직 실행
		messageService.sendMessage(userId, roomId, "Hello Integration Test");

		// 3. Redis에 저장된 값 확인
		String storedValue = stringRedisTemplate.opsForValue().get(redisKey);

		// 4. 값 검증: 이중 따옴표가 없어야 하고 Long으로 파싱되어야 함
		assertNotNull(storedValue, "Redis에 값이 저장되어야 합니다.");
		assertDoesNotThrow(() -> {
			Long.parseLong(storedValue);
		}, "저장된 값은 Long으로 파싱되어야 합니다. (NumberFormatException 방지)");

		assertFalse(storedValue.contains("\""), "저장된 값은 이중 따옴표가 없어야 합니다.");
	}
}
