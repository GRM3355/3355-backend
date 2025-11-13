package com.grm3355.zonie.apiserver.domain.auth.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserQuitResponse(
	@Schema(description = "탈퇴사유", example = "개인적인 사정으로 탈퇴합니다.")
	@NotNull(message = "탈퇴사유를 입력하시기 바랍니다.")
	String comment
) {
}