package com.grm3355.zonie.apiserver.domain.search.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class TotalSearchDto {

	@Schema(description = "검색어")
	@NotBlank(message = "검색어는 필수입니다.")
	private String keyword;

}
