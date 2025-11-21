package com.grm3355.zonie.apiserver.domain.chatroom.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;

@SpringBootTest
class ChatRoomApiServiceIntegrationTest {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private ChatRoomApiService chatRoomApiService;

	@MockitoBean
	private ChatRoomRepository chatRoomRepository;

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
}
