package com.grm3355.zonie.apiserver.domain.location.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FestivalZoneVerifyResponse(
	@Schema(description = "접근여부", example = "true")
	boolean accessValue,

	@Schema(description = "계산한거리", example = "1.1(km기준)")
	double distance,

	@Schema(description = "축제아이디", example = "33")
	long festivalId,

	@Schema(description = "사용자아이디", example = "user:aaaa-bbbb-cccc-dddd")
	String userId
) {
}
