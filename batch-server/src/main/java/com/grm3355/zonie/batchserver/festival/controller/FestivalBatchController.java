package com.grm3355.zonie.batchserver.festival.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.batchserver.festival.service.FestivalBatchService;
import com.grm3355.zonie.commonlib.domain.batch.dto.BatchDto;
import com.grm3355.zonie.commonlib.global.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class FestivalBatchController {

	private final FestivalBatchService festivalBatchService;

	@PostMapping("/festival")
	public ResponseEntity<?> fetchFestivalData() {
		try {
			BatchDto dto = festivalBatchService.runBatch();
			//return ResponseEntity.ok("Festival batch completed successfully!");
			return CustomApiResponse.ok(dto).toResponseEntity();

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Batch failed: " + e.getMessage());
		}
	}
}
