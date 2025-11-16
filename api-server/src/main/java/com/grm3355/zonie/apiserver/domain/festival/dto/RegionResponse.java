package com.grm3355.zonie.apiserver.domain.festival.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Swagger가 제네릭 List<?>를 (올바른 제네릭 DTO 추론을 통해)
 * 문서화할 수 있도록 생성한 ('region'과 'code'를 담는) 구체적인 DTO 클래스입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegionResponse {
	private String region;
	private String code;
}
