package com.grm3355.zonie.apiserver.domain.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Swagger가 제네릭 Map<String, Object>를 (올바른 제네릭 DTO 추론을 통해)
 * 문서화할 수 있도록 생성한 구체적인 DTO 클래스입니다.
 */
@Getter
@AllArgsConstructor
public class MessageLikeResponse {
	@Schema(description = "사용자의 좋아요 여부", example = "true")
	private boolean liked;
	@Schema(description = "메세지의 좋아요 수", example = "11")
	private long likeCount;
}
