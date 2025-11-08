package com.grm3355.zonie.apiserver.domain.search.service;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchDto;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TotalSearchServiceTest {

	@Mock
	private FestivalService festivalService;

	@Mock
	private ChatRoomService chatRoomService;

	@InjectMocks
	private TotalSearchService totalSearchService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("통합검색 - 축제와 채팅방이 정상적으로 합쳐진 결과 반환")
	void getTotalSearch_Success() {
		// given
		TotalSearchDto req = new TotalSearchDto();
		req.setKeyword("음악");

		// Mock Festival
		Festival festival = new Festival();
		Page<Festival> festivalPage = new PageImpl<>(List.of(festival), PageRequest.of(0, 10), 1);
		when(festivalService.getFestivalListType(any(FestivalSearchRequest.class), any(Pageable.class)))
			.thenReturn(festivalPage);

		// Mock ChatRoom
		ChatRoomInfoDto chatRoomInfo = ChatRoomInfoDto.builder()
			.chatRoomId(1L)
			.title("음악 페스티벌 톡방")
			.participantCount(100L)
			.build();
		Page<ChatRoomInfoDto> chatRoomPage = new PageImpl<>(List.of(chatRoomInfo), PageRequest.of(0, 10), 1);
		when(chatRoomService.getFestivalListTypeUser(anyLong(), any(ChatRoomSearchRequest.class), any(Pageable.class)))
			.thenReturn(chatRoomPage);

		// when
		TotalSearchResponse result = totalSearchService.getTotalSearch(req);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getFestivalList().getCount()).isEqualTo(1);
		assertThat(result.getChatRoomList().getCount()).isEqualTo(1);

		verify(festivalService, times(1)).getFestivalListType(any(), any());
		verify(chatRoomService, times(1)).getFestivalListTypeUser(anyLong(), any(), any());
	}

	@Test
	@DisplayName("통합검색 - 페스티벌 전용 검색")
	void getFestivalTotalSearch_Success() {
		// given
		FestivalSearchRequest req = new FestivalSearchRequest();
		Page<FestivalResponse> page = new PageImpl<>(List.of(FestivalResponse.builder().title("테스트").build()));
		when(festivalService.getFestivalList(req)).thenReturn(page);

		// when
		Page<FestivalResponse> result = totalSearchService.getFestivalTotalSearch(req);

		// then
		assertThat(result.getContent()).hasSize(1);
		verify(festivalService, times(1)).getFestivalList(req);
	}

	@Test
	@DisplayName("통합검색 - 채팅방 전용 검색")
	void getChatroomTotalSearch_Success() {
		// given
		ChatRoomSearchRequest req = new ChatRoomSearchRequest();
		Page<MyChatRoomResponse> page = new PageImpl<>(List.of(MyChatRoomResponse.builder().chatRoomId(1L).build()));
		when(chatRoomService.getFestivalChatRoomList(anyLong(), eq(req))).thenReturn(page);

		// when
		Page<MyChatRoomResponse> result = totalSearchService.getChatroomTotalSearch(req);

		// then
		assertThat(result.getContent()).hasSize(1);
		verify(chatRoomService, times(1)).getFestivalChatRoomList(0, req);
	}
}
