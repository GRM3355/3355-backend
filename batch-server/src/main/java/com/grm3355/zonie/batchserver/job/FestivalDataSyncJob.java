package com.grm3355.zonie.batchserver.job;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;
import com.grm3355.zonie.batchserver.service.FestivalApiService;
import com.grm3355.zonie.batchserver.service.FestivalBatchMapper;
import com.grm3355.zonie.batchserver.service.FestivalDetailImageApiService;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalDataSyncJob {

	private final FestivalApiService festivalApiService;
	private final FestivalRepository festivalRepository;
	private final FestivalBatchMapper festivalBatchMapper;
	private final RedisTemplate<String, String> redisTemplate; // 캐싱용 Redis
	private final ObjectMapper objectMapper;
	private final FestivalDetailImageApiService festivalDetailImageService;
	@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
	@Value("${festival.batch.date}")
	private int FESTIVAL_BATCH_DATE;

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
			// 1. PostgreSQL에 벌크 저장/업데이트 실행

			//getContentId 추출
			List<Integer> contentIds = newFestivals.stream()
				.map(ApiFestivalDto::getContentid)
				.filter(Objects::nonNull)
				.filter(s -> !s.isBlank())
				.map(Integer::parseInt)
				.toList();

			//Map 변환
			Map<Integer, Festival> existingFestivalMap = festivalRepository
				.findByContentIdIn(contentIds)
				.stream()
				.collect(Collectors.toMap(Festival::getContentId, f -> f));

			//비교후 업데이트 또는 저장
			List<Festival> upsertEntities = new ArrayList<>();
			for (ApiFestivalDto dto : newFestivals) {

				String contentIdStr = dto.getContentid();
				if (contentIdStr == null || contentIdStr.isBlank()) {
					log.warn("contentId가 null이므로 스킵: {}", dto);
					continue;
				}

				int contentId = Integer.parseInt(contentIdStr);

				// Map에서 조회 (O(1))
				Festival existing = existingFestivalMap.get(contentId);

				if (existing != null) {
					// 기존 엔티티 → 업데이트
					Festival existingUpdate = festivalBatchMapper.updateFromDto(existing, dto);
					upsertEntities.add(existingUpdate);
				} else {
					// 신규 엔티티 → 생성
					Festival newEntity = festivalBatchMapper.toEntity(dto);
					upsertEntities.add(newEntity);
				}
			}
			festivalRepository.saveAll(upsertEntities);

			// 상세 이미지 저장 로직 추가
			festivalDetailImageService.saveFestivalDetailImages(upsertEntities);

			// 2. Redis 캐싱 (festivalId를 키로 사용)
			upsertEntities.forEach(festival -> {
				try {
					String key = "festival:" + festival.getFestivalId();
					String value = objectMapper.writeValueAsString(festival);
					redisTemplate.opsForValue().set(key, value);
				} catch (Exception e) {
					log.warn("Redis 캐싱 실패: {}", e.getMessage());
				}
			});
			log.info("Redis 캐싱 완료: {}건", upsertEntities.size());

			// 3. 종료된 축제 정리 (syncDate를 기준으로 만료된 축제 정리)
			long deletedCount = festivalRepository.deleteByEventEndDateBefore(syncDate);
			log.info("종료된 축제 {}건 삭제", deletedCount);

			log.info("--- [Batch] 동기화 완료: 총 {}건 처리 ---", upsertEntities.size());
		} catch (Exception e) {
			log.error("--- [Batch] 축제 동기화 실패 ---", e);
			throw new RuntimeException("축제 동기화 중 오류 발생", e);    // Tasklet에서 이 예외를 받아 FAILED 처리
		}
	}
}
