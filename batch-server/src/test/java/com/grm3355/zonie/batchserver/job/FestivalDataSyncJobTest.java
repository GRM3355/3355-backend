package com.grm3355.zonie.batchserver.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;
import com.grm3355.zonie.batchserver.service.FestivalApiService;
import com.grm3355.zonie.batchserver.service.FestivalBatchMapper;
import com.grm3355.zonie.batchserver.service.FestivalDetailImageApiService;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalDetailImageRepository;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;

@ExtendWith(MockitoExtension.class)
class FestivalDataSyncJobTest {

	@Mock
	private FestivalApiService festivalApiService;
	@Mock
	private FestivalRepository festivalRepository;
	@Mock
	private FestivalBatchMapper festivalBatchMapper;
	@Mock
	private RedisTemplate<String, String> redisTemplate;
	@Mock
	private ObjectMapper objectMapper;
	@Mock // RedisTemplate.opsForValue()가 반환할 Mock 객체
	private ValueOperations<String, String> valueOperations;

	@Mock
	private FestivalDetailImageRepository festivalDetailImageRepository;

	@Mock
	private FestivalDetailImage	festivalDetailImage;

	@Mock
	private FestivalDetailImageApiService festivalDetailImageService;


	@InjectMocks // @Mock 객체들을 주입받을 대상
	private FestivalDataSyncJob festivalDataSyncJob;

	private final int TEST_BATCH_DATE = 7;

	@BeforeEach
	void setUp() {
		// @Value 필드 수동 주입
		ReflectionTestUtils.setField(festivalDataSyncJob, "FESTIVAL_BATCH_DATE", TEST_BATCH_DATE);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations); // mock ValueOperations
	}

	@Test
	@DisplayName("축제 동기화 Job 로직 전체 테스트")
	void syncFestivalData_Success() throws Exception {
		// given: 테스트용 데이터 준비
		LocalDate syncDate = LocalDate.now();
		LocalDate expectedEndDate = syncDate.plusDays(TEST_BATCH_DATE);

		ApiFestivalDto dto = new ApiFestivalDto(); // 테스트용 DTO
		Festival entity = Festival.builder().festivalId(1L).build(); // 테스트용 Entity
		String entityAsJson = "{\"festivalId\":1}"; // 캐싱될 JSON
		long deletedCount = 5L; // 삭제 건수

		when(festivalApiService.fetchAndParseFestivals(syncDate, expectedEndDate))
			.thenReturn(List.of(dto));
		when(festivalBatchMapper.toEntity(dto)).thenReturn(entity);
		when(festivalRepository.saveAll(anyList())).thenReturn(List.of(entity));
		when(objectMapper.writeValueAsString(entity)).thenReturn(entityAsJson);
		when(festivalRepository.deleteByEventEndDateBefore(syncDate)).thenReturn(deletedCount);

		// when: 테스트할 메서드 실행
		festivalDataSyncJob.syncFestivalData(syncDate);

		// then: 각 컴포넌트가 올바르게 호출되었는지 검증
		// 1. API Service가 올바른 날짜로 호출되었는가?
		verify(festivalApiService).fetchAndParseFestivals(syncDate, expectedEndDate);
		// 2. DB에 저장했는가?
		verify(festivalRepository).saveAll(anyList());
		// 3. Redis에 캐싱했는가?
		verify(redisTemplate.opsForValue()).set(eq("festival:1"), eq(entityAsJson));
		// 4. 만료된 축제를 삭제했는가?
		verify(festivalRepository).deleteByEventEndDateBefore(syncDate);
	}

	@Test
	@DisplayName("API 호출 실패 시 RuntimeException 발생")
	void syncFestivalData_ApiFails() {
		// given
		LocalDate syncDate = LocalDate.now();
		when(festivalApiService.fetchAndParseFestivals(any(), any()))
			.thenThrow(new RuntimeException("API 통신 오류"));

		// when & then
		// Job이 RuntimeException을 던져서 Batch가 FAILED 처리하도록 함
		assertThrows(RuntimeException.class, () -> {
			festivalDataSyncJob.syncFestivalData(syncDate);
		});

		// 실패 시 DB 저장이나 Redis 캐싱은 시도하지 않아야 함
		verify(festivalRepository, never()).saveAll(anyList());
		verify(redisTemplate.opsForValue(), never()).set(anyString(), anyString());
	}
}