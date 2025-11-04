package com.grm3355.zonie.apiserver.domain.batch.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.batch.service.FestivalBatchService;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class FestivalBatchController {

	private final FestivalBatchService festivalBatchService;

	private final JobLauncher jobLauncher;
	private final Job festivalJsonJob;

	/**
	 * 현재 콘트롤러 배치처리는 일시 보류 그래서 실행안됨.
	 */
	@PostMapping("/festival")
	public ResponseEntity<?> fetchFestivalData() {
		try {
			log.info("=======> fetchFestivalData 1");

			festivalBatchService.runBatch();
			log.info("=======> fetchFestivalData 2");
			//return ResponseEntity.ok("Festival batch completed successfully!");
			return ResponseEntity.ok(ApiResponse.success());

		} catch (Exception e) {
			log.info("json to DB 실패");
			return ResponseEntity.internalServerError().body("Batch failed: " + e.getMessage());
		}
	}

	@PostMapping("/festival-json")
	public ResponseEntity<ApiResponse<?>> runCsvToDbBatchFullReload() {
		try {
			log.info("json to DB 전체 재구성 배치 작업 시작 요청됨");
			startBatchJobAsync(festivalJsonJob, "festivalJsonJob");
			return ResponseEntity.ok(
				ApiResponse.success("json to DB 전체 재구성 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("json to DB 전체 재구성 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
					"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	/**
	 * 배치 작업을 비동기 스레드에서 실행합니다.
	 */
	private void startBatchJobAsync(Job jobToRun, String jobName) {
		new Thread(() -> {
			try {
				JobParameters jobParameters = new JobParametersBuilder()
					.addString("JobID", String.valueOf(System.currentTimeMillis()))
					.toJobParameters();
				jobLauncher.run(jobToRun, jobParameters);
				log.info("{} 배치 작업 완료됨", jobName);
			} catch (Exception e) {
				log.error("{} 배치 작업 실행 중 오류 발생", jobName, e);
			}
		}).start();
	}

}
