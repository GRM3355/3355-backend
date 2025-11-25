package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ChatRoomRequest {

	@Schema(description = "채팅방 제목", example = "채팅방 제목입니다.")
	@NotBlank(message = "채팅방 제목은 필수입니다.")
	private String title;

	@Schema(description = "위도", example = "37")
	@NotNull(message = "위치정보가 필요합니다.")
	@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
	private double lat;

	@Schema(description = "경도", example = "127")
	@NotNull(message = "위치정보가 필요합니다.")
	@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
	private double lon;

}
