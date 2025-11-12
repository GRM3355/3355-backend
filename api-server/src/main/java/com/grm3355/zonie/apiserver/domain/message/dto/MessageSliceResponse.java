package com.grm3355.zonie.apiserver.domain.message.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import lombok.Getter;

/**
 * Swagger가 제네릭 Slice<MessageResponse>를 (올바른 제네릭 DTO 추론을 통해)
 * 문서화할 수 있도록 생성한 구체적인 DTO 클래스입니다.
 */
@Getter
public class MessageSliceResponse {
	private final List<MessageResponse> content;
	private final boolean hasNext;

	public MessageSliceResponse(Slice<MessageResponse> slice) {
		this.content = slice.getContent();
		this.hasNext = slice.hasNext();
	}
}
