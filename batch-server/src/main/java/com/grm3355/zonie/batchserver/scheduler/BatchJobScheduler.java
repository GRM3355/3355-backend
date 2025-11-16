package com.grm3355.zonie.batchserver.scheduler;

import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchJobScheduler {				// 시간 맞춰 Job을 실행하는 트리거 역할만

	private final JobLauncher jobLauncher; 		// Batch Job 실행기
	private final ApplicationContext context; 	// Bean을 이름으로 찾기 위해

	// 일단, 관리가 필요한 일일 작업만 Spring Batch로 옮기고, 잦은 동기화 작업은 Scheduler로 남김
	// * FestivalDataSyncJob, ChatRoomRedisCleanupJob, MessageLikeCleanupJob은 스프링 배치로 리팩토링
	// - 축제 공공데이터 OpenAPI
	// - 채팅방 클린
	// - 좋아요 클린
	// * RedisToDbSyncJob, MessageLikeSyncJob은 스케줄러로 남김
	// - 1분마다;
	// - 참여자수, 마지막대화시각
	// - 좋아요수

	// 1. 축제 데이터 동기화
	@Scheduled(cron = "0 10 11 * * *")
	public void runFestivalSyncJob() throws Exception {
		Job job = context.getBean("festivalSyncJob", Job.class);		// FestivalSyncBatchConfig에 정의한 Bean 이름: "festivalSyncJob" 이름으로 등록된 Job Bean을 찾아서 실행

		JobParameters params = new JobParametersBuilder()
			.addString("run.time", LocalDateTime.now().toString())		// Job 실행 시 파라미터 전달 - run time: 고유한 값 (매번 다른 실행으로 인식되도록)
			.toJobParameters();

		jobLauncher.run(job, params);
	}

	// 2. ChatRoomRedisCleanupJob
	@Scheduled(cron = "0 0 4 * * ?")
	public void runChatRoomCleanupJob() throws Exception {
		Job job = context.getBean("chatRoomCleanupBatchJob", Job.class); 	// CleanupBatchConfig에 정의한 Bean 이름
		JobParameters params = new JobParametersBuilder()
			.addString("run.time", LocalDateTime.now().toString())
			.toJobParameters();
		jobLauncher.run(job, params);
	}

	// 3. MessageLikeCleanupJob
	@Scheduled(cron = "0 0 4 * * ?")
	public void runMessageLikeCleanupJob() throws Exception {
		Job job = context.getBean("messageLikeCleanupBatchJob", Job.class); // CleanupBatchConfig에 정의한 Bean 이름
		JobParameters params = new JobParametersBuilder()
			.addString("run.time", LocalDateTime.now().toString())
			.toJobParameters();
		jobLauncher.run(job, params);
	}
}