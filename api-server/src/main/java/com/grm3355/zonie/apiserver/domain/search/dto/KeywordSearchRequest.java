package com.grm3355.zonie.apiserver.domain.search.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class KeywordSearchRequest {

	@Schema(description = "검색어", example = "페스티벌")
	@NotBlank(message = "검색어는 필수입니다.")
	private String keyword;

	@Schema(description = "페이지번호", example = "1", defaultValue = "1")
	@Digits(integer = 2, fraction = 0, message = "숫자만 가능합니다.")
	@Builder.Default
	private Integer page = 1;

	@Schema(description = "한페이지 데이터 갯수 (고정값 사용 시 무시)", example = "10", defaultValue = "10")
	@Digits(integer = 3, fraction = 0, message = "숫자만 가능합니다.")
	@Builder.Default
	private Integer pageSize = 10;

	public int getPage() {
		return page != null ? page : 1;
	}

	public Integer getPageSize() {
		return pageSize != null ? pageSize : 10;
	}
}