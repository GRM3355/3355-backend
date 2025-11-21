package com.grm3355.zonie.apiserver.domain.chatroom.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomCreateResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.enums.Role;

@Disabled
@SpringBootTest
class ChatRoomApiServiceIntegrationTest {

	GeometryFactory geometryFactory = new GeometryFactory();
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private ChatRoomApiService chatRoomApiService;
	@MockitoBean
	private RedisTokenService redisTokenService;
	@MockitoBean
	private FestivalInfoService festivalInfoService;
	@MockitoBean
	private ChatRoomRepository chatRoomRepository;
	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean
	private ChatRoomUserRepository chatRoomUserRepository;

	@Test
	void shouldHandleDirtyTimestampFromRedis() {
		long festivalId = 1L;
		String roomId = "test-room-2";
		String redisKey = "chatroom:last_msg_at:" + roomId;

		// 1. Redis에 수동으로 Dirty 데이터 삽입
		// String dirtyValue = "\"1763691934350\""; // -> 이걸로 테스트하면 방어 코드로 인해 더티 데이터로 인식하지 않음
		String dirtyValue = "BAD\"1763691934350\"DATA";
		stringRedisTemplate.opsForValue().set(redisKey, dirtyValue);
		// 다른 필수 Redis 키도 Mock 데이터로. (getParticipantCounts, multiGetLastMessageTimestamps)
		stringRedisTemplate.opsForValue().set("chatroom:last_msg_content:" + roomId, "Mock Content");
		stringRedisTemplate.opsForSet().add("chatroom:participants:" + roomId, "1");

		// 2. Mocking 설정: getFestivalListTypeUser가 DB 결과를 반환하도록 설정
		ChatRoomInfoDto mockDto = new ChatRoomInfoDto(
			roomId, festivalId, "제목", 5L, 1763691630246L, "축제제목", 37.5, 127.0
		);
		List<ChatRoomInfoDto> dtoList = Collections.singletonList(mockDto);

		ChatRoomSearchRequest req = new ChatRoomSearchRequest();
		req.setPage(1);
		req.setPageSize(10);

		// Pageable 객체 생성을 위해 임시로 설정
		// Pageable pageable = chatRoomApiService.getSort(req.getOrder()) != null ? Pageable.unpaged() : Pageable.ofSize(10);
		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), Sort.unsorted());

		Page<ChatRoomInfoDto> mockPage = new PageImpl<>(dtoList, pageable, dtoList.size());

		// ChatRoomApiService 내의 getFestivalListTypeUser가 chatRoomRepository.chatFestivalRoomList를 호출하므로, 이를 Mocking.
		given(chatRoomRepository.chatFestivalRoomList(
			org.mockito.ArgumentMatchers.eq(festivalId),
			org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		)).willReturn(mockPage);

		// 3. ChatRoomApiService의 병합 로직 호출
		Page<ChatRoomResponse> resultPage = chatRoomApiService.getFestivalChatRoomList(festivalId, req);

		// 4. 검증: API 호출 시 NumberFormatException이 발생하지 않았으며 (테스트 성공), 결과 DTO를 통해 안전하게 처리되었는지 확인
		assertNotNull(resultPage, "결과 페이지는 null이 아니어야 합니다.");
		assertEquals(1, resultPage.getTotalElements(), "결과는 1개의 요소여야 합니다.");

		// Redis의 Dirty 데이터(1763691934350)는 파싱 실패 후 무시되거나 PG 데이터(1763691630246)로 대체되어야 함
		// 테스트 정상적 완료: NumberFormatException이 잡혔음
		// 최종 Timestamp는 PG 값(1763691630246) (Dirty Redis 값이 무시되므로)
		assertEquals(1763691630246L, resultPage.getContent().getFirst().getLastMessageAt(),
			"Dirty 데이터는 무시되고 PG 데이터가 반환되어야 합니다.");
	}

	@Test
	@DisplayName("채팅방 생성 및 가입 시 #3355부터 순차적인 닉네임이 부여되는지 검증")
	void testSequentialNicknameGeneration() {
		String userId1 = "test-user-1";
		String userId2 = "test-user-2";
		long festivalId = 100L;
		String roomId = "mock-room-id";
		String sequenceKey = "chatroom:nickname_seq:" + roomId;
		stringRedisTemplate.delete(sequenceKey);

		// Mock User/Festival/Token Setup (실제 DB에 의존하지 않도록 Mocking 필요)
		User mockUser1 = User.builder().userId(userId1).role(Role.USER).build();
		User mockUser2 = User.builder().userId(userId2).role(Role.USER).build();
		Point festivalPosition = geometryFactory.createPoint(new Coordinate(127.0, 37.5));
		Festival mockFestival = Festival.builder().festivalId(festivalId).position(festivalPosition).build();

		// --- Mocking ---
		ChatRoom mockRoom = ChatRoom.builder()
			.chatRoomId(roomId)
			.festival(mockFestival) // Festival 엔티티를 연결
			.maxParticipants(100L)  // joinRoom 정원 검증
			.memberCount(1L)        // 방장 생성 후 1명이므로
			.build();

		// 1. setCreateChatRoom에 필요한 Mocking
		given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
			ChatRoom room = invocation.getArgument(0);
			room.setChatRoomId("mock-room-id");
			return room;
		});
		// 2. findByChatRoomId/findByChatRoomIdWithLock
		given(chatRoomRepository.findByChatRoomId(anyString())).willReturn(Optional.of(mockRoom));
		given(chatRoomRepository.findByChatRoomIdWithLock(anyString())).willReturn(Optional.of(mockRoom));

		given(userRepository.findByUserId(userId1)).willReturn(Optional.of(mockUser1));
		given(userRepository.findByUserId(userId2)).willReturn(Optional.of(mockUser2));

		// 2. joinRoom/leaveRoom/setCreateChatRoom에 필요한 Mocking
		given(festivalInfoService.getDataValid(anyLong(), anyInt())).willReturn(mockFestival);
		given(festivalInfoService.isUserWithinFestivalRadius(anyLong(), anyDouble(), anyDouble(),
			anyDouble())).willReturn(true);

		given(redisTokenService.setToken(anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
			.willReturn(UserTokenDto.builder().lat(37.0).lon(127.0).build());

		// 3. ChatRoomUser 존재 여부 Mocking
		given(chatRoomUserRepository.findByUserAndChatRoom(eq(mockUser2), any(ChatRoom.class))).willReturn(
			Optional.empty());

		// ===============================================================
		// 1. 채팅방 생성 (방장: 첫유저) -> 닉네임 #3355
		// ===============================================================
		ChatRoomRequest createRequest = ChatRoomRequest.builder()
			.title("순차 닉네임 테스트")
			.lat(37.5)
			.lon(127.0)
			.build();

		ChatRoomCreateResponse createResponse = chatRoomApiService.setCreateChatRoom(festivalId, createRequest,
			UserDetailsImpl.build(mockUser1));
		roomId = createResponse.getChatRoomId();

		// 닉네임 검증: 방장은 3355번을 받아야 함
		// DB에서 ChatRoomUser를 조회하여 검증
		// Redis 직접 검증: 닉네임 시퀀스가 1인지 확인
		assertEquals("1", stringRedisTemplate.opsForValue().get(sequenceKey), "시퀀스는 1이어야 합니다.");

		// DB 저장이 올바른 닉네임을 포함했는지 검증 (ArgumentCaptor 필요)
		ArgumentCaptor<ChatRoomUser> captor = ArgumentCaptor.forClass(ChatRoomUser.class);
		verify(chatRoomUserRepository, times(1)).save(captor.capture());
		assertEquals("#3355", captor.getValue().getNickName(), "방장의 닉네임은 #3355이어야 합니다.");

		// ===============================================================
		// 2. 참가자 가입 (참가자: 둘유저) -> 닉네임 #3356
		// ===============================================================
		String nickName2 = chatRoomApiService.joinRoom(roomId, UserDetailsImpl.build(mockUser2));

		// 닉네임 검증: 참가자는 3356번을 받아야 함
		assertEquals("#3356", nickName2, "참가자의 닉네임은 #3356이어야 합니다.");

		// Redis 직접 검증: 닉네임 시퀀스가 2인지 확인
		assertEquals("2", stringRedisTemplate.opsForValue().get(sequenceKey), "시퀀스는 2이어야 합니다.");

		// DB 저장 검증 (joinRoom 내부에서 한 번 더 save 호출됨)
		verify(chatRoomUserRepository, times(2)).save(captor.capture());
		assertEquals("#3356", captor.getValue().getNickName(), "참가자의 닉네임은 #3356이어야 합니다.");

	}
}
