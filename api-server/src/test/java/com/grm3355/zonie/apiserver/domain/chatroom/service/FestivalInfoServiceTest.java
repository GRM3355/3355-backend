package com.grm3355.zonie.apiserver.domain.chatroom.service;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FestivalInfoServiceTest {

	@Mock
	private FestivalRepository festivalRepository;

	@InjectMocks
	private FestivalInfoService festivalInfoService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("getDataValid() - 유효한 축제 데이터를 반환한다")
	void getDataValid_Success() {
		// given
		long festivalId = 100L;
		int dayNum = 1;
		Festival mockFestival = Festival.builder().build();
		when(festivalRepository.findByIsValidFestival(festivalId, dayNum))
			.thenReturn(Optional.of(mockFestival));

		// when
		Festival result = festivalInfoService.getDataValid(festivalId, dayNum);

		// then
		assertThat(result).isNotNull();
		verify(festivalRepository, times(1))
			.findByIsValidFestival(festivalId, dayNum);
	}

	@Test
	@DisplayName("getDataValid() - 유효하지 않은 축제면 BusinessException 발생")
	void getDataValid_NotFound() {
		// given
		long festivalId = 999L;
		int dayNum = 3;
		when(festivalRepository.findByIsValidFestival(festivalId, dayNum))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> festivalInfoService.getDataValid(festivalId, dayNum))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("축제 관련정보가 없습니다.");

		verify(festivalRepository, times(1))
			.findByIsValidFestival(festivalId, dayNum);
	}

	@Test
	@DisplayName("increaseChatRoomCount() - repository의 update 메서드가 호출된다")
	void increaseChatRoomCount_Success() {
		// given
		Long festivalId = 10L;

		// when
		festivalInfoService.increaseChatRoomCount(festivalId);

		// then
		verify(festivalRepository, times(1))
			.updateFestivalChatRoomCount(festivalId);
	}
}