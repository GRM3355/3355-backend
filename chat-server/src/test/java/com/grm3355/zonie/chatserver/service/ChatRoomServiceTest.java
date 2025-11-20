package com.grm3355.zonie.chatserver.service;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

	private static final Long MOCK_MAX_PARTICIPANTS = 300L;
	private static final String KEY_PARTICIPANTS = "chatroom:participants:";
	private static final String KEY_USER_ROOMS = "user:rooms:";
	@Mock
	private ChatRoomRepository chatRoomRepository;
	@Mock
	private ChatRoomUserRepository chatRoomUserRepository;
	@Mock
	private UserRepository userRepository;
	// Redis dependencies
	@Mock
	private RedisTemplate<String, Object> redisTemplate;
	@Mock
	private ValueOperations<String, Object> valueOperations;
	@Mock
	private SetOperations<String, Object> setOperations;
	@InjectMocks
	private ChatRoomService chatRoomService;

	@BeforeEach
	void setUp() {
		// Setup RedisTemplate mocks
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

		// @Value - Reflection
		ReflectionTestUtils.setField(chatRoomService, "maxParticipants", MOCK_MAX_PARTICIPANTS);
	}

	@Test
	@DisplayName("채팅방 입장은 Pub/Sub을 통해 호출되며, Redis Set에 userId를 추가한다")
	void testJoinRoomUpdatesRedisSets() {
		// given
		String userId = "test-user";
		String roomId = "test-room-1";
		String nickName = "#5";

		// when
		chatRoomService.joinRoom(userId, roomId, nickName);

		// then
		// 1. 실시간 참여자 Set에 추가되었는지 검증
		verify(setOperations, times(1)).add(KEY_PARTICIPANTS + roomId, userId);
		// 2. 유저의 참여방 목록 Set에 추가되었는지 검증
		verify(setOperations, times(1)).add(KEY_USER_ROOMS + userId, roomId);

		// DB repositories should not be involved in the refactored join
		verifyNoInteractions(chatRoomRepository, userRepository);
	}

	@Test
	@DisplayName("채팅방 퇴장은 Pub/Sub을 통해 호출되며, Redis Set에서 userId를 제거한다")
	void testLeaveRoomRemovesFromRedisSets() {
		// given
		String userId = "test-user";
		String roomId = "test-room-1";

		// when
		chatRoomService.leaveRoom(userId, roomId);

		// then
		// 1. 실시간 참여자 Set에서 제거되었는지 검증
		verify(setOperations, times(1)).remove(KEY_PARTICIPANTS + roomId, userId);
		// 2. 유저의 참여방 목록 Set에서 제거되었는지 검증
		verify(setOperations, times(1)).remove(KEY_USER_ROOMS + userId, roomId);

		// DB repositories should not be involved in the refactored leave
		verifyNoInteractions(chatRoomRepository, userRepository);
	}

	@Test
	@DisplayName("연결 끊김(disconnect) 시 참여 중이던 모든 방에서 제거되고 lastReadAt이 갱신된다")
	void testDisconnectUser() {
		// given
		String userId = "disconnected-user";
		String roomId1 = "room-a";
		String roomId2 = "room-b";
		Set<Object> activeRooms = new HashSet<>();
		activeRooms.add(roomId1);
		activeRooms.add(roomId2);

		when(redisTemplate.opsForSet().members(KEY_USER_ROOMS + userId)).thenReturn(activeRooms);

		// when
		chatRoomService.disconnectUser(userId);

		// then
		// 1. 각 방의 참여자 Set에서 userId 제거
		verify(setOperations, times(1)).remove(KEY_PARTICIPANTS + roomId1, userId);
		verify(setOperations, times(1)).remove(KEY_PARTICIPANTS + roomId2, userId);

		// 2. DB lastReadAt 갱신 (ChatRoomUserRepository)
		ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(chatRoomUserRepository, times(1)).updateLastReadAtByUserId(eq(userId), timeCaptor.capture());

		// 3. 역방향 매핑 키 자체 삭제
		verify(redisTemplate, times(1)).delete(KEY_USER_ROOMS + userId);
	}

	@Test
	@DisplayName("연결 끊김 시 참여 중인 방이 없으면 Redis/DB 작업을 생략한다")
	void testDisconnectUserNoActiveRooms() {
		// given
		String userId = "no-room-user";
		when(redisTemplate.opsForSet().members(KEY_USER_ROOMS + userId)).thenReturn(Collections.emptySet());

		// when
		chatRoomService.disconnectUser(userId);

		// then
		// Redis remove/delete 호출이 없었는지 검증
		verify(setOperations, never()).remove(anyString(), any());
		verify(redisTemplate, never()).delete(anyString());
		// DB lastReadAt 갱신 호출이 없었는지 검증
		verify(chatRoomUserRepository, never()).updateLastReadAtByUserId(anyString(), any(LocalDateTime.class));
	}
}
