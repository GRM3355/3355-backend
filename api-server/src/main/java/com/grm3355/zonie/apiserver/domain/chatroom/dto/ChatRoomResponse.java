package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ChatRoomResponse { // 채팅방 생성 시점에서의 채팅방 정보 응답

	@Schema(description = "채팅방 아이디", example = "room:abc-def-ghi")
	private String chatRoomId;

	@Schema(description = "축제 아이디", example = "1")
	private Long festivalId;

	@Schema(description = "사용자 아이디", example = "user:aaa-bbb-ccc")
	private String userId;

	@Schema(description = "제목", example = "채팅방 제목입니다.")
	private String title;

	@Schema(description = "위도", example = "(123.233, 26.223)")
	private double lat;

	@Schema(description = "경도", example = "123.233")
	private double lon;

	public static ChatRoomResponse fromEntity(ChatRoom chatRoom) {
		return new ChatRoomResponse(
			chatRoom.getChatRoomId(),
			chatRoom.getFestival().getFestivalId(),
			chatRoom.getUser().getUserId(),
			chatRoom.getTitle(),
			chatRoom.getPosition().getY(),
			chatRoom.getPosition().getX()
		);
	}


}
