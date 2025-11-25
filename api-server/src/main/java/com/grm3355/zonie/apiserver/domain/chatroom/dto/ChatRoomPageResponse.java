package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import org.springframework.data.domain.Page;

import com.grm3355.zonie.apiserver.global.dto.PageResponse;

/**
 * Swagger가 제네릭 PageResponse<MyChatRoomResponse>를 (올바른 제네릭 DTO 추론을 통해)
 * 문서화할 수 있도록 생성한 구체적인 DTO 클래스입니다.
 */
public class ChatRoomPageResponse extends PageResponse<ChatRoomResponse> {
	public ChatRoomPageResponse(Page<ChatRoomResponse> page, int size) {
		super(page, size);
	}
}
