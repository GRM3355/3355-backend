package com.grm3355.zonie.batchserver.job;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.grm3355.zonie.batchserver.BaseIntegrationTest;
import com.grm3355.zonie.batchserver.BatchServerApplication;

@SpringBootTest(classes = BatchServerApplication.class)
@ActiveProfiles("test")
class FestivalDataSyncJobTest extends BaseIntegrationTest {

	@Autowired
	private FestivalDataSyncJob festivalDataSyncJob;

	// @Test
	@DisplayName("공공데이터 축제 동기화 Job 수동 실행 테스트")
	void testSyncPublicFestivalDataExecution() {
		System.out.println("--- [Test] Festival Data Sync Job 수동 실행 요청 ---");

		// Job의 main 메서드를 직접 호출하여 스케줄링과 무관하게 즉시 실행합니다.
		festivalDataSyncJob.syncFestivalData(LocalDate.now());

		// Job의 실행 결과를 Console 출력(System.out)으로 확인합니다.
	}

	// @Test
	@DisplayName("특정 일자(미래)로 축제 동기화 테스트")
	void testSyncPublicFestivalData_FutureDate() {
		LocalDate futureDate = LocalDate.of(2025, 12, 31); // 2025년 12월 31일로 설정

		// Job의 핵심 로직을 특정 날짜를 지정하여 호출
		festivalDataSyncJob.syncFestivalData(futureDate);

		// ... DB 확인 로직 추가 ...
	}

	// @Test
	@DisplayName("과거 일자로 축제 만료 및 정리 테스트")
	void testSyncPublicFestivalData_PastDateCleanup() {
		LocalDate pastDate = LocalDate.of(2023, 1, 1); // 2023년 1월 1일로 설정

		// 이 날짜를 기준으로 API 호출(2023년 1월 1일 이후 축제만 호출) 및
		// 이 날짜 이전에 종료된 축제는 DB에서 삭제됩니다.
		festivalDataSyncJob.syncFestivalData(pastDate);

		// ... DB 확인 로직 추가 ...
	}
}