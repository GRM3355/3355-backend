package com.grm3355.zonie.apiserver.domain.auth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationDto {

	@Schema(description = "위도정보", example = "37.5894939323")
	@NotNull(message = "위치정보가 필요합니다.")
	@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
	private Double lat;

	@Schema(description = "경도정보", example = "127.0167863252")
	@NotNull(message = "위치정보가 필요합니다.")
	@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
	private Double lon;

	public LocationDto() {
	}

	public LocationDto(Double lat, Double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	@Override
	public String toString() {
		return "LocationDto{" +
			"lat=" + lat +
			", lon=" + lon +
			'}';
	}
}
