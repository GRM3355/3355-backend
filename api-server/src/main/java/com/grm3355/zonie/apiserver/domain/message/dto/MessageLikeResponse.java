package com.grm3355.zonie.apiserver.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Swagger가 제네릭 Map<String, Object>를 (올바른 제네릭 DTO 추론을 통해)
 * 문서화할 수 있도록 생성한 구체적인 DTO 클래스입니다.
 */
@Getter
@AllArgsConstructor
public class MessageLikeResponse {
	private boolean liked;
	private long likeCount;
}