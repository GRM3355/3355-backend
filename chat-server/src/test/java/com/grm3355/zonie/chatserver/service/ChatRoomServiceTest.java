package com.grm3355.zonie.chatserver.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import com.grm3355.zonie.chatserver.util.JwtChannelInterceptor;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

	@Mock
	private ChatRoomRepository chatRoomRepository;
	@Mock
	private ChatRoomUserRepository chatRoomUserRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	// Mocking이 필요한 Redis의 세부 동작들
	@Mock
	private ValueOperations<String, Object> valueOperations;
	@Mock
	private SetOperations<String, Object> setOperations;
	@Mock
	private JwtChannelInterceptor jwtChannelInterceptor;

	@InjectMocks
	private ChatRoomService chatRoomService;

	@BeforeEach
	void setUp() {
		// redisTemplate.opsForValue()가 valueOperations Mock 객체를 반환하도록 설정
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
	}

	@Test
	@DisplayName("신규 유저가 입장하면 닉네임 '#1'을 받고 DB에 저장된다")
	void testJoinRoom_NewUser() {
		// given
		String userId = "new-user";
		String roomId = "room-1";
		User mockUser = User.builder().build(); // Mockito 대신 실제 객체 사용도 가능
		ChatRoom mockRoom = ChatRoom.builder().build();

		// (1) 방이 꽉 차지 않음
		when(setOperations.size(anyString())).thenReturn(0L);
		// (2) DB에서 User, ChatRoom 조회 성공
		when(userRepository.findByUserId(userId)).thenReturn(Optional.of(mockUser));
		when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
		// (3) 재방문자가 아님
		when(chatRoomUserRepository.findByUserAndChatRoom(any(), any())).thenReturn(Optional.empty());
		// (4) 닉네임 카운터가 1을 반환
		when(valueOperations.increment(anyString(), eq(1L))).thenReturn(1L);

		// when
		String nickname = chatRoomService.joinRoom(userId, roomId);

		// then
		assertThat(nickname).isEqualTo("#1");
		// (5) DB에 "참가자1" 닉네임으로 저장되었는지 검증
		verify(chatRoomUserRepository, times(1)).save(argThat(
			userRoom -> userRoom.getNickName().equals("#1")
		));
	}

	@Test
	@DisplayName("재방문 유저가 입장하면 기존 닉네임을 반환하고 DB 저장을 생략한다")
	void testJoinRoom_ReturningUser() {
		// given
		// ... (user, room Mocking) ...
		ChatRoomUser existingUser = ChatRoomUser.builder().nickName("기존닉네임").build();

		// (1) 방이 꽉 차지 않음
		when(setOperations.size(anyString())).thenReturn(10L);
		// (2) DB 조회 성공
		when(userRepository.findByUserId(anyString())).thenReturn(Optional.of(User.builder().build()));
		when(chatRoomRepository.findById(anyString())).thenReturn(Optional.of(ChatRoom.builder().build()));
		// (3) 재방문자로 확인됨
		when(chatRoomUserRepository.findByUserAndChatRoom(any(), any())).thenReturn(Optional.of(existingUser));

		// when
		String nickname = chatRoomService.joinRoom("returning-user", "room-1");

		// then
		assertThat(nickname).isEqualTo("기존닉네임");
		// (4) DB 저장이 호출되지 않았는지 검증
		verify(chatRoomUserRepository, never()).save(any());
		// (5) Redis에는 추가되었는지 검증
		verify(setOperations, times(1)).add("chatroom:participants:room-1", "returning-user");
	}

	@Test
	@DisplayName("채팅방 정원이 꽉 차면 BusinessException이 발생한다")
	void testJoinRoom_RoomFull() {
		// given
		// (1) 방이 꽉 참 (300명)
		when(setOperations.size("chatroom:participants:room-1")).thenReturn(300L);

		// when & then
		// 예외가 발생하는지 검증
		assertThatThrownBy(() -> {
			chatRoomService.joinRoom("any-user", "room-1");
		})
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("채팅방 최대 정원");
	}
}