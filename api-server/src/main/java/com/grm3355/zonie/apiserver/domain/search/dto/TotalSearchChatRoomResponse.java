package com.grm3355.zonie.apiserver.domain.search.dto;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class TotalSearchChatRoomResponse {

	@Schema(description = "채팅방 아이디", example = "room:abc-def-ghi")
	private String chatRoomId;

	@Schema(description = "축제 아이디", example = "1")
	private Long festivalId;

	@Schema(description = "제목", example = "채팅방 제목입니다.")
	private String title;

	@Schema(description = "위도", example = "(123.233, 26.223)")
	private double lat;

	@Schema(description = "경도", example = "123.233")
	private double lon;

	@Schema(description = "축제명", example = "사과축제")
	private String festivalTitle;

	@Schema(description = "참석자수", example = "234")
	private Long participantCount;


	public static TotalSearchChatRoomResponse fromDto(ChatRoomInfoDto dto) {
		return TotalSearchChatRoomResponse.builder()
			.chatRoomId(dto.chatRoomId())
			.festivalId(dto.festivalId())
			// .userId(dto.chatRoom().getUser().getUserId())
			.title(dto.title())
			.lat(dto.lat())
			.lon(dto.lon())
			.festivalTitle(dto.festivalTitle())
			.participantCount(dto.participantCount())
			.build();
	}
}
