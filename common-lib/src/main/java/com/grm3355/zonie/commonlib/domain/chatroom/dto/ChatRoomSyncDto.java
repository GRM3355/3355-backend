package com.grm3355.zonie.commonlib.domain.chatroom.dto;

import java.time.LocalDateTime;

public record ChatRoomSyncDto(
	String roomId,
	Long participantCount,
	Long lastMessageTimestamp
) {

	// 참여자 수O 타임스탬프 X - 특정 채팅방 내 참여자 수
	public static ChatRoomSyncDto withParticipantCount(String roomId, Long count) {
		return new ChatRoomSyncDto(roomId, count, null);
	}

	// 참여자 수X 타임스탬프 O - 특정 채팅방의 마지막 대화 시각
	public static ChatRoomSyncDto withLastMessageTimestamp(String roomId, Long timestamp) {
		return new ChatRoomSyncDto(roomId, null, timestamp);
	}
}
