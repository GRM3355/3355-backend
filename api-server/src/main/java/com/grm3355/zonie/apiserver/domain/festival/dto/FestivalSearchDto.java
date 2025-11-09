package com.grm3355.zonie.apiserver.domain.festival.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FestivalSearchDto {

	@Schema(description = "검색어")
	@NotBlank(message = "검색어는 필수입니다.")
	private String keyword;
}

