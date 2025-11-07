package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import java.time.LocalDateTime;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyChatRoomResponse {
	String chatRoomId;
	Long festivalId;
	String userId;
	String title;
	double lat;
	double lon;
	String festivalTitle;
	Long participantCount;
	LocalDateTime lastMessageAt;

	public static MyChatRoomResponse fromDto(ChatRoomInfoDto dto) {
		return new MyChatRoomResponse(
			dto.chatRoom().getChatRoomId(),
			dto.chatRoom().getFestival().getFestivalId(),
			dto.chatRoom().getUser().getUserId(),
			dto.chatRoom().getTitle(),
			dto.chatRoom().getPosition().getY(),
			dto.chatRoom().getPosition().getX(),
			dto.festivalTitle(),
			dto.participantCount(),
			dto.lastMessageAt()
		);
	}

}