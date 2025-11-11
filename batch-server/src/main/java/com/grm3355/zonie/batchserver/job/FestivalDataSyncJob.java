package com.grm3355.zonie.batchserver.job;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;
import com.grm3355.zonie.batchserver.service.FestivalBatchMapper;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.batchserver.service.FestivalApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalDataSyncJob {

	@Value("${festival.batch.date}")
	private int FESTIVAL_BATCH_DATE;
	private final FestivalApiService festivalApiService;
	private final FestivalRepository festivalRepository;
	private final FestivalBatchMapper festivalBatchMapper;
	private final RedisTemplate<String, String> redisTemplate; // 캐싱용 Redis
	private final ObjectMapper objectMapper;

	// @Scheduled가 제거: 순수 비즈니스 로직: -> Batch Step에서 관리
	// - @Scheduled에 의해 호출되는 메서드
	// 매일 새벽 4시에 실행
	// @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE) // 잠시 후 실행 (오늘 날짜 사용)
	// @Scheduled(cron = "0 0 4 * * *")
	// @Transactional
	public void syncPublicFestivalData() {
		this.syncFestivalData(LocalDate.now());
	}

	// - 동기화 로직
	public void syncFestivalData(LocalDate syncDate) {
		log.info("--- [Batch] 공공데이터 축제 동기화 시작: {} ---", syncDate);
		LocalDate endDate = syncDate.plusDays(FESTIVAL_BATCH_DATE);

		try {
			List<ApiFestivalDto> newFestivals = festivalApiService.fetchAndParseFestivals(syncDate, endDate);

			// 0. DTO를 Entity로 변환 및 Upsert를 위한 준비
			List<Festival> entities = newFestivals.stream()
				.map(festivalBatchMapper::toEntity)
				.collect(Collectors.toList());

			// 1. PostgreSQL에 벌크 저장/업데이트 실행
			festivalRepository.saveAll(entities);

			// 2. Redis 캐싱 (festivalId를 키로 사용)
			entities.forEach(festival -> {
				try {
					String festivalId = String.valueOf(festival.getFestivalId()); 	// 엔티티의 ID 필드 사용
					String key = "festival:" + festivalId;
					String value = objectMapper.writeValueAsString(festival); 		// DTO로 변환 후 저장하기
					redisTemplate.opsForValue().set(key, value);
				} catch (Exception e) {
					log.warn("Redis 캐싱 실패 (festivalId: {}): {}", festival.getFestivalId(), e.getMessage());
				}
			});
			log.info("Redis 캐싱 완료: {}건", entities.size());

			// 3. 종료된 축제 정리 (syncDate를 기준으로 만료된 축제 정리)
			long deletedCount = festivalRepository.deleteByEventEndDateBefore(syncDate);
			log.info("종료된 축제 {}건 삭제", deletedCount);

			log.info("--- [Batch] 동기화 완료: 총 {}건 처리 ---", entities.size());
		}
		catch (Exception e) {
			log.error("--- [Batch] 축제 동기화 실패 ---", e);
			throw new RuntimeException("축제 동기화 중 오류 발생", e);	// Tasklet에서 이 예외를 받아 FAILED 처리
		}
	}
}
