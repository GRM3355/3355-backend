package com.grm3355.zonie.apiserver.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class LocationDto {

	@Schema(description = "위도", example = "37")
	@NotNull(message = "위치정보가 필요합니다.")
	@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
	private Double lat;

	@Schema(description = "경도", example = "127")
	@NotNull(message = "위치정보가 필요합니다.")
	@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
	private Double lon;

	@Override
	public String toString() {
		return "LocationDto{"
			   + "lat=" + lat
			   + ", lon=" + lon
			   + '}';
	}

	@AssertTrue(message = "위도, 경도 값이 0일 수 없습니다.")
	public boolean isValidCoordinates() {
		return !(lat == 0.0 && lon == 0.0);
	}

}
