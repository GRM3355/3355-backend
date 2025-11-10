package com.grm3355.zonie.apiserver.domain.search.dto;

import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TotalSearchResponse {
	private ListWithCount<FestivalResponse> festivals;
	private ListWithCount<TotalSearchChatRoomResponse> chatRooms;
}
