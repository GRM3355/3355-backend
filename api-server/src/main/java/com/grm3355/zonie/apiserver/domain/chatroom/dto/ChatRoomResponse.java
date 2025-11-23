package com.grm3355.zonie.apiserver.domain.chatroom.dto;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomResponse {

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

	@Schema(description = "참석자수 (채팅방 인원 수: ChatRoomUser 레코드의 개수)", example = "234")
	private Long participantCount;

	@Schema(description = "마지막 메시지 날짜", example = "2025-11-02 00:00:00")
	private Long lastMessageAt;

	@Schema(description = "마지막 메시지", example = "마지막 메시지입니다.")
	private String lastContent;

	@Schema(description = "채팅방 생성 시각", example = "1678886400000")
	private Long createdAt;

	public static ChatRoomResponse fromDto(ChatRoomInfoDto dto, String lastContent, Long participantCount,
		Long lastMessageAt) {
		return ChatRoomResponse.builder()
			.chatRoomId(dto.chatRoomId())
			.festivalId(dto.festivalId())
			.title(dto.title())
			.lat(dto.lat())
			.lon(dto.lon())
			.festivalTitle(dto.festivalTitle())
			.participantCount(participantCount) // PG값: ChatRoomUser의 개수(Member Count)
			.lastMessageAt(lastMessageAt)       // PG값이 아닌 실시간 값
			.lastContent(lastContent)           // PG값이 아닌 실시간 값
			.createdAt(dto.createdAt())
			.build();
	}

	public static ChatRoomResponse fromDto(ChatRoomInfoDto dto) {
		return fromDto(dto, null, null, null);
	}
}
