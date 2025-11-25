package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FestivalCreateRequest {

	@Schema(description = "제목", example = "임시 테스트 축제")
	@NotNull(message = "제목은 필수입니다.")
	private String title;

	@Schema(description = "주소", example = "서울 강남구 강남대로 23")
	private String addr1;

	@Schema(description = "시작일", example = "2025-12-01")
	@NotNull(message = "시작 일자는 필수입니다.")
	private LocalDate eventStartDate;

	@Schema(description = "종료일", example = "2025-12-05")
	@NotNull(message = "종료 일자는 필수입니다.")
	private LocalDate eventEndDate;

	@Schema(description = "이미지 URL", example = "http://tong.visitkorea.or.kr/cms/resource/sample.jpg")
	private String firstImage;

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

	@Schema(description = "지역명", example = "SEOUL")
	private String region;

	@Schema(description = "Content ID", example = "99999")
	private int contentId;
}
