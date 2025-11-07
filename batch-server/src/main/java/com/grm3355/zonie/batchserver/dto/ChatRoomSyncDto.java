package com.grm3355.zonie.batchserver.dto;

public record ChatRoomSyncDto(
	Long roomId,
	Long participantCount,
	Long lastMessageTimestamp
) {

	// 참여자 수O 타임스탬프 X - 특정 채팅방 내 참여자 수
	public static ChatRoomSyncDto withParticipantCount(Long roomId, Long count) {
		return new ChatRoomSyncDto(roomId, count, null);
	}

	// 참여자 수X 타임스탬프 O - 특정 채팅방의 마지막 대화 시각
	public static ChatRoomSyncDto withLastMessageTimestamp(Long roomId, Long timestamp) {
		return new ChatRoomSyncDto(roomId, null, timestamp);
	}
}
