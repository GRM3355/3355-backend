package com.grm3355.zonie.apiserver.domain.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomApiService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
import com.grm3355.zonie.apiserver.domain.search.dto.ListWithCount;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchDto;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@Service
public class TotalSearchService {

	private final FestivalService festivalService;
	private final ChatRoomApiService chatRoomApiService;

	public TotalSearchService(FestivalService festivalService, ChatRoomApiService chatRoomApiService) {
		this.festivalService = festivalService;
		this.chatRoomApiService = chatRoomApiService;
	}

	/**
	 * 통합검색
	 * @param req 검색 dto
	 * @return TotalSearchResponse 응답
	 */
	public TotalSearchResponse getTotalSearch(TotalSearchDto req) {

		String keyword = req.getKeyword();

		// 축제목록
		FestivalSearchRequest festival = FestivalSearchRequest.builder()
			.keyword(keyword)
			.build();
		Pageable pageable = PageRequest.of(0, 10,
			Sort.by(Sort.Order.asc("created_at")));
		Page<Festival> festivalPageList = festivalService.getFestivalListType(festival, pageable);

		// 채팅방 목록
		ChatRoomSearchRequest searchRequest = ChatRoomSearchRequest.builder()
			.keyword(keyword)
			.build();
		Pageable pageable2 = PageRequest.of(0, 10,
			Sort.by(Sort.Order.desc("member_count")));
		Page<ChatRoomInfoDto> chatroomPageList = chatRoomApiService.getTotalChatRoomPageList(
			searchRequest, pageable2);

		//데이터 합치기
		return new TotalSearchResponse(
			new ListWithCount<>(festivalPageList.getTotalElements(),
				festivalPageList.stream().map(FestivalResponse::fromEntity).toList()),
			new ListWithCount<>(chatroomPageList.getTotalElements(),
				chatroomPageList.stream().map(TotalSearchChatRoomResponse::fromDto).toList())
		);
	}

	/**
	 * 통합검색 - 페스티벌
	 * @param request 검색dto
	 * @return Page<FestivalResponse>
	 */
	public Page<FestivalResponse> getFestivalTotalSearch(FestivalSearchRequest request) {

		//키워드 체크
		checkKeyWord(request.getKeyword());

		//축제목록
		return festivalService.getFestivalList(request);
	}

	/**
	 * 통합검색 - 채팅방
	 * @param request 검색dto
	 * @return Page<MyChatRoomResponse>
	 */
	public Page<ChatRoomResponse> getChatroomTotalSearch(ChatRoomSearchRequest request) {

		// 키워드 체크
		checkKeyWord(request.getKeyword());

		// 채팅방 목록
		return chatRoomApiService.getTotalChatRoomList(request);
	}

	// 키워드 체크
	private void checkKeyWord(String keyword) {
		if (keyword == null || keyword.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "검색어는 필수입니다.");
		}
	}
}
