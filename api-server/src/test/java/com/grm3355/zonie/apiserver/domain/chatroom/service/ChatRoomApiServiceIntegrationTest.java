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
import com.grm3355.zonie.apiserver.domain.chatroom.enums.OrderType;
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
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

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
	private RedisScanService redisScanService;
	@MockitoBean
	private FestivalInfoService festivalInfoService;
	@MockitoBean
	private ChatRoomRepository chatRoomRepository;
	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean
	private ChatRoomUserRepository chatRoomUserRepository;

	// 서비스가 정렬 조건을 Repository에 올바르게 전달했는지 검증하는 테스트
	@Test
	@DisplayName("나의 채팅방 목록: 활성화 최신순(lastMessageAt) 동점 시 생성일 최신순(createdAt) 복합 정렬 검증")
	void testMyRoomListSorting() {
		String userId = "test-user-sort";
		String roomId1 = "room-active-older-created"; // last_message_at: 1000, created_at: OLD
		String roomId2 = "room-active-newer-created"; // last_message_at: 1000, created_at: NEW

		// 1. Mock Data: lastMessageAt이 동일하지만 createdAt이 다른 두 방 설정
		ChatRoomInfoDto mockDto1 = new ChatRoomInfoDto(
			roomId1, 1L, "Room A", 5L, 1000L, "Festival A", 37.0, 127.0
		); // 생성일이 '오래됨'을 가정한 DB 순서
		ChatRoomInfoDto mockDto2 = new ChatRoomInfoDto(
			roomId2, 1L, "Room B", 5L, 1000L, "Festival A", 37.0, 127.0
		); // 생성일이 '최신'임을 가정한 DB 순서

		// DB에서 조회될 순서 (last_message_at이 동일하므로 DB의 자연 정렬 순서대로)
		List<ChatRoomInfoDto> dtoList = List.of(mockDto1, mockDto2);

		ChatRoomSearchRequest req = new ChatRoomSearchRequest();
		req.setPage(1);
		req.setPageSize(10);
		req.setOrder(OrderType.ACTIVE_DESC); // ACTIVE_DESC 정렬 요청 // OrderType 제거해서 테스트해도 동일한 결과

		// Pageable을 Mocking하지 않고 실제 Sort를 포함하여 생성
		Sort expectedSort = Sort.by(
			Sort.Order.desc("last_message_at"),
			Sort.Order.desc("created_at")
		);
		Pageable expectedPageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), expectedSort);

		Page<ChatRoomInfoDto> mockPage = new PageImpl<>(dtoList, PageRequest.of(0, 10, expectedSort), dtoList.size());

		// 2. Mocking: getMyRoomListTypeUser가 정렬된 Page를 반환하도록 설정
		given(chatRoomRepository.chatMyRoomList(
			org.mockito.ArgumentMatchers.eq(userId),
			org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.eq(expectedPageable) // 복합 정렬이 포함된 Pageable을 기대
		)).willReturn(mockPage);

		// Redis Scan Mocking (실시간 데이터 없음을 가정)
		given(redisScanService.getParticipantCounts(anySet())).willReturn(Collections.emptyMap());
		given(redisScanService.multiGetLastMessageTimestamps(anySet())).willReturn(Collections.emptyMap());

		// 3. 테스트 실행: getMyRoomChatRoomList 호출
		Page<ChatRoomResponse> resultPage = chatRoomApiService.getMyRoomChatRoomList(
			UserDetailsImpl.build(User.builder().userId(userId).build()), req);

		// 4. 검증
		assertNotNull(resultPage, "결과 페이지는 null이 아니어야 합니다.");
		assertEquals(2, resultPage.getTotalElements(), "결과는 2개의 요소여야 합니다.");

		// 마지막 대화 시각이 동일할 때, created_at이 최신인 (DTO 순서상 2번째) 방이 1순위로 와야 한다.
		// 현재 chatMyRoomList는 DB 쿼리이므로, Repository Mocking 시 Sort를 넣어주면 Spring Data JPA가 실제 DB에서 정렬된 결과를 반환함.
		// Mocking 결과가 '정렬된 결과'라고 가정하고, 그 결과를 확인.
		// Redis 병합 로직에서 데이터가 덮어씌워지지 않았으므로, Mocking된 결과의 순서대로 와야 한다.

		// DB 쿼리에서 last_message_at이 같으면 created_at DESC로 정렬되었기 때문에
		// mockDto2 (Newer Created)가 mockDto1 (Older Created)보다 앞에 와야 한다.
		//
		// Mocking 시 Pageable에 Sort가 올바르게 전달되었는지 확인 (BDDMockito.verify 사용)
		// 실제로 서비스 계층이 Repository를 호출할 때 정의된 expectedSort가 포함된 Pageable 객체를 사용했는지 // <- 핵심적인 검증 포인트
		verify(chatRoomRepository, times(1)).chatMyRoomList(
			eq(userId),
			anyString(),
			eq(expectedPageable)
		);

		// 5. 최종 결과 순서 검증: 실제 반환된 List의 순서가 기대한 순서와 일치하는지 확인
		// mockDto1, mockDto2의 created_at을 알 수 없으므로, Sort 기준에 따라 DTO를 미리 정렬해야 함.
		// created_at이 더 늦은(최신) 방이 앞에 와야 한다. (mockDto2가 더 최신 생성이라고 가정)
		assertEquals(roomId2, resultPage.getContent().get(0).getChatRoomId(),
			"LastMessageAt 동점 시, created_at이 최신인 방이 1순위여야 합니다.");
		assertEquals(roomId1, resultPage.getContent().get(1).getChatRoomId(),
			"LastMessageAt 동점 시, created_at이 오래된 방이 2순위여야 합니다.");

		// 검증 로직이 복잡해질 수 있으므로, 테스트를 통과하도록 DTO 순서를 바꿔 Mocking하는 것이 일반적
		// dtoList = List.of(mockDto2, mockDto1); 로 Mocking하면 테스트 통과.
		// 하지만 Mocking 시점에서는 DB의 created_at 순서를 알 수 없으므로,
		// DTO 순서는 DB에서 정렬된 상태로 온다고 가정하고, expectedPageable이 Repository로 전달되었는지를 검증하는 것이 더 적절함.
	}

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
