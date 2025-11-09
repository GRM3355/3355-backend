package com.grm3355.zonie.apiserver.domain.festival.service;


import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalOrderType;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FestivalServiceTest {

	@Mock
	private FestivalRepository festivalRepository;

	@Mock
	private ChatRoomService chatRoomService;

	@InjectMocks
	private FestivalService festivalService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("축제 목록 조회 - DATE_ASC 정렬로 페이지 반환")
	void getFestivalList_DATE_ASC() {
		// given
		FestivalSearchRequest req = new FestivalSearchRequest();
		req.setOrder(FestivalOrderType.DATE_ASC);
		req.setPage(1);
		req.setPageSize(10);

		Festival mockFestival = Festival.builder().build();
		Page<Festival> mockPage = new PageImpl<>(List.of(mockFestival));

		when(festivalRepository.getFestivalList(
			any(), any(), any(), anyInt(), any(Pageable.class)
		)).thenReturn(mockPage);

		// when
		Page<FestivalResponse> result = festivalService.getFestivalList(req);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		verify(festivalRepository, times(1))
			.getFestivalList(any(), any(), any(), anyInt(), any(Pageable.class));
	}

	@Test
	@DisplayName("축제 상세 조회 성공")
	void getFestivalContent_Success() {
		// given
		long festivalId = 100L;
		Festival mockFestival = Festival.builder().build();
		when(festivalRepository.findById(festivalId))
			.thenReturn(Optional.of(mockFestival));

		// when
		FestivalResponse response = festivalService.getFestivalContent(festivalId);

		// then
		assertThat(response).isNotNull();
		verify(festivalRepository, times(1)).findById(festivalId);
	}

	@Test
	@DisplayName("축제 상세 조회 실패 - 존재하지 않음")
	void getFestivalContent_NotFound() {
		// given
		long festivalId = 999L;
		when(festivalRepository.findById(festivalId))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> festivalService.getFestivalContent(festivalId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("관련 내용을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("getRegionList() - 모든 지역 코드와 이름을 반환")
	void getRegionList_ReturnsAllRegions() {
		// when
		List<Map<String, String>> result = festivalService.getRegionList();

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.get(0)).containsKeys("region", "code");
	}
}
