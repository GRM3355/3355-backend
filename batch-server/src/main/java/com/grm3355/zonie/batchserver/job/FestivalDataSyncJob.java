package com.grm3355.zonie.batchserver.job;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;
import com.grm3355.zonie.batchserver.service.FestivalBatchMapper;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.batchserver.service.FestivalApiService;

@Component
public class FestivalDataSyncJob {

	private final FestivalApiService festivalApiService;
	private final FestivalRepository festivalRepository;
	private final FestivalBatchMapper festivalBatchMapper;

	public FestivalDataSyncJob(FestivalApiService festivalApiService,
		FestivalRepository festivalRepository,
		FestivalBatchMapper festivalBatchMapper) {
		this.festivalApiService = festivalApiService;
		this.festivalRepository = festivalRepository;
		this.festivalBatchMapper = festivalBatchMapper;
	}

	// 1. @Scheduled에 의해 호출되는 메서드
	// 매일 새벽 4시에 실행
	// @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE) // 잠시 후 실행 (오늘 날짜 사용)
	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void syncPublicFestivalData() {
		this.syncFestivalData(LocalDate.now());
	}

	// 2. 핵심 동기화 로직
	public void syncFestivalData(LocalDate syncDate) {
		System.out.println("--- [Batch] 공공데이터 축제 동기화 시작: " + syncDate + " ---");

		try {
			List<ApiFestivalDto> newFestivals = festivalApiService.fetchAndParseFestivals(syncDate);

			// DTO를 Entity로 변환 및 Upsert를 위한 준비
			List<Festival> entities = newFestivals.stream()
				.map(festivalBatchMapper::toEntity)
				.collect(Collectors.toList());

			// PostgreSQL에 벌크 저장/업데이트 실행
			festivalRepository.saveAll(entities);

			// 종료된 축제 정리 (syncDate를 기준으로 만료된 축제 정리)
			festivalRepository.deleteByEventEndDateBefore(syncDate);

			System.out.println("--- [Batch] 동기화 완료: 총 " + entities.size() + "건 처리 ---");
		}
		catch (Exception e)
		{
			System.err.println("--- [Batch] 축제 동기화 실패: " + e.getMessage() + " ---");
		}
	}
}
