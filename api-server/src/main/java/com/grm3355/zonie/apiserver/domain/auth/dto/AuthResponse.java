package com.grm3355.zonie.apiserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public record AuthResponse(
	@Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1...")
	String accessToken
) {
}
