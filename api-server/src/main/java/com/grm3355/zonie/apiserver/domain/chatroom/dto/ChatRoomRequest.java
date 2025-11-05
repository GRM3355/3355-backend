package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ChatRoomRequest {

	@Schema(description = "축제 아이디", example = "1")
	private Long festivalId;

	@Schema(description = "채팅방 제목", example = "채팅방 제목입니다.")
	private String title;

}
