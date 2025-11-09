package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyChatRoomResponse {
	String chatRoomId;
	Long festivalId;
	String title;
	double lat;
	double lon;
	String festivalTitle;
	Long participantCount;
	Long lastMessageAt;
	String lastContent;

	public static MyChatRoomResponse fromDto(ChatRoomInfoDto dto, String lastContent) {
		return MyChatRoomResponse.builder()
			.chatRoomId(dto.chatRoomId())
			.festivalId(dto.festivalId())
			// .userId(dto.chatRoom().getUser().getUserId())
			.title(dto.title())
			// dto.chatRoom().getPosition().getY(),
			// dto.chatRoom().getPosition().getX(),
			.lat(0.0)
			.lon(0.0)
			.festivalTitle(dto.festivalTitle())
			.participantCount(dto.participantCount())
			.lastMessageAt(dto.lastMessageAt())
			.lastContent(lastContent)
			.build();
	}

	public static MyChatRoomResponse fromDto(ChatRoomInfoDto dto) {
		return fromDto(dto, null);
	}
}