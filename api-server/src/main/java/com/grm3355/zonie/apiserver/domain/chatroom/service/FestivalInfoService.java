package com.grm3355.zonie.apiserver.domain.chatroom.service;

import org.springframework.stereotype.Service;

import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@Service
public class FestivalInfoService {
	private final FestivalRepository festivalRepository;

	public FestivalInfoService(FestivalRepository festivalRepository, LocationService locationService) {
		this.festivalRepository = festivalRepository;
	}

	//축제테이블에 존재여부체크
	public Festival getDataValid(long festivalId) {
		return festivalRepository
			.findByIsValidFestival(festivalId)
			.orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND,"관련정보가 없습니다."));
	}

	//채팅방 등록시 chatRoomCount +1
	@Transactional
	public void increaseChatRoomCount(Long festivalId) {
		festivalRepository.incrementChatRoomCount(festivalId);
	}
}
