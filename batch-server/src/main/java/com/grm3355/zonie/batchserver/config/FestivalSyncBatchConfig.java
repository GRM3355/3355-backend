package com.grm3355.zonie.batchserver.config;

import java.time.LocalDate;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.grm3355.zonie.batchserver.job.FestivalDataSyncJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FestivalSyncBatchConfig {        // Spring Batch의 Job, Step, Tasklet을 정의하는 설정 역할

	// 1. 기존 서비스 로직(festivalDataSyncJob) 주입
	private final FestivalDataSyncJob festivalDataSyncJob;
	private final JobRepository jobRepository;                        // Spring Batch가 Job 이력 저장할 DB
	private final PlatformTransactionManager transactionManager;    // 트랜잭션 관리

	// 2. Spring Batch "Job" 정의 (이름: festivalSyncJob)
	@Bean
	public Job festivalSyncJob() {
		return new JobBuilder("festivalSyncJob", jobRepository)    // JobBuilder 객체 직접 사용 - 5.x
			.start(festivalSyncStep())                                // 이 Job은 "festivalSyncStep" 1개로 구성됨
			.build();
	}

	// 3. "Step" 정의 (Tasklet 방식)
	@Bean
	public Step festivalSyncStep() {
		return new StepBuilder("festivalSyncStep", jobRepository)
			.tasklet(festivalSyncTasklet(), transactionManager)         // 4. 이 Step이 실행할 Tasklet
			.build();
	}

	// 4. "Tasklet" 정의 (실제 수행할 작업)
	@Bean
	public Tasklet festivalSyncTasklet() {
		return (contribution, chunkContext) -> {
			log.info(">>>>> Spring Batch: FestivalDataSyncJob 시작");

			// 5. festivalDataSyncJob 호출
			try {
				festivalDataSyncJob.syncFestivalData(LocalDate.now());
				log.info(">>>>> Spring Batch: FestivalDataSyncJob 완료");
				return RepeatStatus.FINISHED; // 성공
			} catch (Exception e) {
				log.error(">>>>> Spring Batch: FestivalDataSyncJob 실패", e);
				// 예외를 던져야 Batch 상태가 FAILED로 기록됨
				throw e;
			}
		};
	}
}
