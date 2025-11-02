package com.grm3355.zonie.commonlib.domain.batch.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public class BatchDto {

	private Long id;
	private String targetType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private int totalCount;

}
