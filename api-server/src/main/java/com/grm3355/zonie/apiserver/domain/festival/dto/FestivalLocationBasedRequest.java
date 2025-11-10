package com.grm3355.zonie.apiserver.domain.festival.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.hibernate.annotations.ColumnDefault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalLocationBasedRequest {

	@Schema(description = "페이지번호", example = "1")
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	//@NotNull(message = "페이지 번호는 필수입니다.")
	@ColumnDefault("'1'")
	private Integer page;

	@Schema(description = "한페이지 데이터 갯수", example = "10", nullable = true)
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	//@NotNull(message = "페이지 갯수 필수입니다.")
	@ColumnDefault("'10'")
	private Integer pageSize;

	@Schema(description = "위도", example = "37.5894939323")
	@Min(value = -90, message = "위도 범위는 -90~90입니다.")
	@Max(value = 90, message = "위도 범위는 -90~90입니다.")
	private Double lat;

	@Schema(description = "경도", example = "127.0167863252")
	@Min(value = -180, message = "경도 범위는 -180~180입니다.")
	@Max(value = 180, message = "경도 범위는 -180~180입니다.")
	private Double lon;

	@Schema(description = "반경")
	@DecimalMin(value = "1.0", message = "반경은 최소 1 이상이어야 합니다.")
	private Double radius;

	public int getPage() {
		return page != null ? page : 1;
	}

	public Integer getPageSize() {
		return pageSize != null ? pageSize : 10;
	}

}
