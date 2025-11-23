package com.grm3355.zonie.chatserver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // JSON에 있지만 여기 없는 필드는 무시
public class ChatLocationDto {
	@NotNull(message = "위치정보(위도:lat)가 필요합니다.")
	@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
	private Double lat;

	@NotNull(message = "위치정보(경도:lon)가 필요합니다.")
	@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
	private Double lon;
}
