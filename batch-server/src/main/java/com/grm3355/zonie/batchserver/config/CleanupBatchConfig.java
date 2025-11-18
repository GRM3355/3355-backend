package com.grm3355.zonie.batchserver.config;

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

import com.grm3355.zonie.batchserver.job.ChatRoomDeletionJob;
import com.grm3355.zonie.batchserver.job.ChatRoomRedisCleanupJob;
import com.grm3355.zonie.batchserver.job.MessageLikeCleanupJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CleanupBatchConfig {

	// 1. 기존 Job 로직 주입
	private final ChatRoomRedisCleanupJob chatRoomRedisCleanupJob;
	private final MessageLikeCleanupJob messageLikeCleanupJob;
	private final ChatRoomDeletionJob chatRoomDeletionJob;

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;

	// ======== Job 1: 채팅방 Redis 정리 ========

	@Bean
	public Job chatRoomCleanupBatchJob() {
		return new JobBuilder("chatRoomCleanupBatchJob", jobRepository)    // JobBuilder 객체로 곧바로 Job 정의
			.start(chatRoomCleanupStep())                                        // 1개만
			.build();
	}

	@Bean
	public Step chatRoomCleanupStep() {
		return new StepBuilder("chatRoomCleanupStep", jobRepository)    // Step 정의
			.tasklet(chatRoomCleanupTasklet(), transactionManager)
			.build();
	}

	@Bean
	public Tasklet chatRoomCleanupTasklet() {                                // Tasklet 정의
		return (contribution, chunkContext) -> {
			log.info(">>>>> Spring Batch: ChatRoomRedisCleanupJob 시작");
			try {
				chatRoomRedisCleanupJob.cleanupStaleChatRoomKeys();
				log.info(">>>>> Spring Batch: ChatRoomRedisCleanupJob 완료");
				return RepeatStatus.FINISHED;
			} catch (Exception e) {
				log.error(">>>>> Spring Batch: ChatRoomRedisCleanupJob 실패", e);
				throw e; // FAILED 상태로 기록
			}
		};
	}

	// ======== Job 2: 메시지 '좋아요' Redis 정리 ========

	@Bean
	public Job messageLikeCleanupBatchJob() {
		return new JobBuilder("messageLikeCleanupBatchJob", jobRepository)
			.start(messageLikeCleanupStep())
			.build();
	}

	@Bean
	public Step messageLikeCleanupStep() {
		return new StepBuilder("messageLikeCleanupStep", jobRepository)
			.tasklet(messageLikeCleanupTasklet(), transactionManager)
			.build();
	}

	@Bean
	public Tasklet messageLikeCleanupTasklet() {
		return (contribution, chunkContext) -> {
			log.info(">>>>> Spring Batch: MessageLikeCleanupJob 시작");
			try {
				messageLikeCleanupJob.cleanupOldMessageLikes();
				log.info(">>>>> Spring Batch: MessageLikeCleanupJob 완료");
				return RepeatStatus.FINISHED;
			} catch (Exception e) {
				log.error(">>>>> Spring Batch: MessageLikeCleanupJob 실패", e);
				throw e; // FAILED 상태로 기록
			}
		};
	}

	// ======== Job 3: 채팅방 DB 삭제 (24시간 경과/0명/축제종료) ========

	@Bean
	public Job chatRoomDbDeletionBatchJob() {
		return new JobBuilder("chatRoomDbDeletionBatchJob", jobRepository)
			.start(chatRoomDbDeletionStep())
			.build();
	}

	@Bean
	public Step chatRoomDbDeletionStep() {
		return new StepBuilder("chatRoomDbDeletionStep", jobRepository)
			.tasklet(chatRoomDbDeletionTasklet(), transactionManager)
			.build();
	}

	@Bean
	public Tasklet chatRoomDbDeletionTasklet() {
		return (contribution, chunkContext) -> {
			log.info(">>>>> Spring Batch: ChatRoomDbDeletionJob 시작");
			// 여기서 ChatRoomDeletionJob의 비즈니스 로직 호출
			chatRoomDeletionJob.executeDeletionLogic();
			log.info(">>>>> Spring Batch: ChatRoomDbDeletionJob 완료");
			return RepeatStatus.FINISHED;
		};
	}
}
