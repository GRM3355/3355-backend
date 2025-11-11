package com.grm3355.zonie.batchserver.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.grm3355.zonie.batchserver.BaseIntegrationTest;
import com.grm3355.zonie.batchserver.BatchServerApplication;
import com.grm3355.zonie.batchserver.job.FestivalDataSyncJob; // Mocking할 서비스

@SpringBatchTest
@SpringBootTest(classes = BatchServerApplication.class)
@ActiveProfiles("test")
class FestivalSyncBatchIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils; 		// Job을 실행시킬 테스트 유틸

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils; 	// Job 실행 이력 정리용

	@Autowired
	private Job festivalSyncJob;	// 테스트할 Job을 Bean 이름으로 주입

	@MockitoBean 	// 실제 서비스 로직 Mocking (DB, Redis, API 호출 방지)
	private FestivalDataSyncJob festivalDataSyncJob;

	@BeforeEach
	void setUp() {
		// 각 테스트 전에 DB에 저장된 배치 실행 이력 삭제
		jobRepositoryTestUtils.removeJobExecutions();
	}

	@Test
	@DisplayName("festivalSyncJob 실행 시 Job이 COMPLETED 상태로 종료")
	void festivalSyncJob_Success() throws Exception {
		// given
		// Job이 실행될 때 실제 로직(festivalDataSyncJob)이 성공했다고 가정
		doNothing().when(festivalDataSyncJob).syncFestivalData(any(LocalDate.class)); // void -> doNothing() 사용
		jobLauncherTestUtils.setJob(festivalSyncJob);
		JobParameters params = new JobParametersBuilder()
			.addString("test.run.time", LocalDateTime.now().toString())	// JobParameter 설정
			.toJobParameters();

		// when: Job 실행
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

		// then: 결과 검증
		// 1. Job이 성공적으로 완료되었는가?
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		// 2. Tasklet이 실제 서비스 로직(syncFestivalData)을 호출했는가?
		verify(festivalDataSyncJob).syncFestivalData(any(LocalDate.class));
	}

	@Test
	@DisplayName("서비스 로직 실패 시 Job이 FAILED 상태로 종료")
	void festivalSyncJob_Fails() throws Exception {
		// given
		// Job 실행 시 서비스 로직이 실패했다고 가정
		doThrow(new RuntimeException("테스트용 예외")).when(festivalDataSyncJob).syncFestivalData(any(LocalDate.class));
		jobLauncherTestUtils.setJob(festivalSyncJob);
		JobParameters params = new JobParametersBuilder()
			.addString("test.run.time", LocalDateTime.now().toString())
			.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

		// then
		// 1. Job이 실패 상태로 종료되었는가?
		assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode()); // 종료 코드만 비교

		// 2. 실패한 서비스 로직이 1회 호출되었는가?
		verify(festivalDataSyncJob, times(1)).syncFestivalData(any(LocalDate.class));
	}
}