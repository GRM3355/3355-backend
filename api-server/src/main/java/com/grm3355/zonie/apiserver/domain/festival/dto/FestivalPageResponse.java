package com.grm3355.zonie.apiserver.domain.festival.dto;

import org.springframework.data.domain.Page;

import com.grm3355.zonie.apiserver.global.dto.PageResponse;

/**
 * Swagger가 제네릭 PageResponse<FestivalPageResponse>를 (올바른 제네릭 DTO 추론을 통해)
 * 문서화할 수 있도록 생성한 구체적인 DTO 클래스입니다.
 */
public class FestivalPageResponse extends PageResponse<FestivalResponse> {
	public FestivalPageResponse(Page<FestivalResponse> page, int size) {
		super(page, size);
	}
}