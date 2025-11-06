package com.grm3355.zonie.apiserver.domain.festival.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.grm3355.zonie.apiserver.common.enums.OrderType;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.SearchRequest;
import com.grm3355.zonie.apiserver.domain.search.dto.ListWithCount;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchDto;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.PageResult;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalOrderType;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Service
public class TotalSearchService {

	private final FestivalService festivalService;
	private final ChatRoomService chatRoomService;

	public TotalSearchService(FestivalService festivalService, ChatRoomService chatRoomService) {
		this.festivalService = festivalService;
		this.chatRoomService = chatRoomService;
	}

	/**
	 * 통합검색
	 * @param request
	 * @return
	 */
	public TotalSearchResponse getTotalSearch(FestivalSearchDto request){

		String keyword = request.getKeyword();

		//축제목록
		FestivalSearchRequest festival = FestivalSearchRequest.builder()
			.keyword(keyword)
			.order(FestivalOrderType.DATE_ASC)
			.build();

		Page<Festival> festivalPageList = festivalService.getFestivalListType(festival, PageRequest.of(0, 10));

		//채팅방 목록
		SearchRequest searchRequest = SearchRequest.builder()
			.keyword(keyword)
			.order(OrderType.DATE_ASC)
			.build();
		Page<ChatRoomInfoDto> chatroomPageList = chatRoomService.getFestivalListTypeUser(0, searchRequest,
			PageRequest.of(0, 10));

		//데이터 합치기
		TotalSearchResponse response = new TotalSearchResponse(
			new ListWithCount<>(festivalPageList.getTotalElements(),
				festivalPageList.stream().map(FestivalResponse::fromEntity).toList()),
			new ListWithCount<>(chatroomPageList.getTotalElements(),
			chatroomPageList.stream().map(TotalSearchChatRoomResponse::fromDto).toList())
		);

		return response;
	}

}
