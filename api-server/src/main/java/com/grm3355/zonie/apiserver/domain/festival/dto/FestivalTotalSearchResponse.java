package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.util.List;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;

public class FestivalTotalSearchResponse {
	private List<FestivalResponse> festivals;
	private List<ChatRoomResponse> chatRooms;

	public <T> FestivalTotalSearchResponse(
		PageResult<FestivalResponse> resultFetivalList,
		PageResult<ChatRoomResponse> resultRoomList) {
	}
}
