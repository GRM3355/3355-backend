package com.grm3355.zonie.batchserver.festival.scheduler;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.apiserver.domain.batch.service.FestivalBatchService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchScheduler {

	private final FestivalBatchService festivalBatchService;

	// 3일에 한번 새벽 3시 (cron: 초 분 시 일 월 요일)
	@Scheduled(cron = "0 0 3 1/3 * *")
	public void runFestivalBatch() {
		System.out.println(MessageFormat.format("Festival 배치작업 시작 - {0}", LocalDateTime.now()));
		festivalBatchService.runBatch();
		System.out.println("Festival 배치작업 종료 - " + java.time.LocalDateTime.now());
	}
}