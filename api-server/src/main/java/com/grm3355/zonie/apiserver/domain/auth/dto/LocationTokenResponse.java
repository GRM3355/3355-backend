package com.grm3355.zonie.apiserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationTokenResponse(
	@Schema(description = "처리메시지", example = "갱신되었습니다.")
	String message

) {
}
