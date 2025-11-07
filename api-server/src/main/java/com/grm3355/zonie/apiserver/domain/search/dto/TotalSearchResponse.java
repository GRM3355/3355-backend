package com.grm3355.zonie.apiserver.domain.search.dto;

import java.util.List;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.PageResult;

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
