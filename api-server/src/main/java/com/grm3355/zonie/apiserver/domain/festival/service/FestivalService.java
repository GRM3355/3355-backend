package com.grm3355.zonie.apiserver.domain.festival.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalLocationBasedRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalOrderType;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalStatus;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FestivalService {

	private final FestivalRepository festivalRepository;
	private final ChatRoomService chatRoomService;
	@Value("${chat.pre-view-day}")
	private int preview_days; //시작하기전 몇일전부터 보여주기

	public FestivalService(FestivalRepository festivalRepository,
		ChatRoomService chatRoomService) {
		this.festivalRepository = festivalRepository;
		this.chatRoomService = chatRoomService;
	}

	/**
	 * 축제목록
	 * @param req 검색dto
	 * @return page
	 */
	@Transactional
	public Page<FestivalResponse> getFestivalList(FestivalSearchRequest req) {

		Sort.Order order;
		if (req.getOrder() == FestivalOrderType.DATE_ASC) {
			order = Sort.Order.asc("event_start_date");
		} else if (req.getOrder() == FestivalOrderType.DATE_DESC) {
			order = Sort.Order.desc("event_start_date");
		} else if (req.getOrder() == FestivalOrderType.TITLE_ASC) {
			order = Sort.Order.asc("title");
		} else if (req.getOrder() == FestivalOrderType.TITLE_DESC) {
			order = Sort.Order.desc("title");
		} else {
			order = Sort.Order.asc("event_start_date");
		}
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<Festival> pageList = getFestivalListType(req, pageable);

		//페이지 변환
		List<FestivalResponse> dtoPage = pageList.stream().map(FestivalResponse::fromEntity)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	public Page<Festival> getFestivalListType(FestivalSearchRequest req, Pageable pageable) {

		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;

		FestivalStatus status = req.getStatus();
		String statusStr = status != null ? status.toString() : null;

		return festivalRepository
			.getFestivalList(regionStr, statusStr, req.getKeyword(), preview_days, pageable);

	}

	/**
	 * 위치기반 축제목록
	 * @param req 검색dto
	 * @return page
	 */
	@Transactional
	public Page<FestivalResponse> getFestivalLocationBased(FestivalLocationBasedRequest req) {

		Sort.Order order = Sort.Order.asc("position");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<Festival> pageList = getFestivalLocationBasedType(req, pageable);

		//페이지 변환
		List<FestivalResponse> dtoPage = pageList.stream().map(FestivalResponse::fromEntity)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	public Page<Festival> getFestivalLocationBasedType(FestivalLocationBasedRequest req, Pageable pageable) {

		return festivalRepository
			.getFestivalLocationBased(req.getLat(), req.getLon(), req.getRadius(), preview_days, pageable);

	}

	/**
	 * 축제 상세내용
	 * @param festivalId 축제 아이디
	 * @return 페스티벌 Response
	 */
	@Transactional
	public FestivalResponse getFestivalContent(long festivalId) {
		Festival festival = festivalRepository.findById(festivalId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 내용을 찾을 수 없습니다."));
		return FestivalResponse.fromEntity(festival);
	}

	/**
	 * 지역분류 가져오기
	 * @return list
	 */
	public List<?> getRegionList() {
		return Arrays.stream(Region.values()).map(region -> Map.of(
			"region", region.getName(),
			"code", region.name()
		)).toList();
	}

}
