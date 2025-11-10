package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyChatRoomResponse {

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

	@Schema(description = "마지막 메시지 날짜", example = "2025-11-02 00:00:00")
	private Long lastMessageAt;

	@Schema(description = "마지막 메시지", example = "마지막 메시지입니다.")
	private String lastContent;

	public static MyChatRoomResponse fromDto(ChatRoomInfoDto dto, String lastContent, Long participantCount, Long lastMessageAt) {
		return MyChatRoomResponse.builder()
			.chatRoomId(dto.chatRoomId())
			.festivalId(dto.festivalId())
			.title(dto.title())
			.lat(dto.lat())
			.lon(dto.lon())
			.festivalTitle(dto.festivalTitle())
			.participantCount(participantCount) // PG값이 아닌 실시간 값
			.lastMessageAt(lastMessageAt)       // PG값이 아닌 실시간 값
			.lastContent(lastContent)           // PG값이 아닌 실시간 값
			.build();
	}

	public static MyChatRoomResponse fromDto(ChatRoomInfoDto dto) {
		return fromDto(dto, null, null, null);
	}
}