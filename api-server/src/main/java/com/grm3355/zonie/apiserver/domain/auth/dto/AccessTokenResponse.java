package com.grm3355.zonie.apiserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record AccessTokenResponse(
	@Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIJleH...")
	@NotBlank(message = "액세스 토큰은 필수 입력 값입니다.")
	String accessToken
) {
}
