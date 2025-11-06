package com.grm3355.zonie.apiserver.domain.festival.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchDto;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalTotalSearchResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.PageResult;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalStatus;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
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
	private final ChatRoomRepository chatRoomRepository;

	// GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	GeometryFactory geometryFactory = new GeometryFactory();

	public FestivalService(FestivalRepository festivalRepository, ChatRoomRepository chatRoomRepository) {
		this.festivalRepository = festivalRepository;
		this.chatRoomRepository = chatRoomRepository;
	}

	/**
	 * 축제목록
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<FestivalResponse> getFestivalList(FestivalSearchRequest req) {

		Sort.Order order = Sort.Order.asc("eventStartDate");
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
	private Page<Festival> getFestivalListType(
		FestivalSearchRequest req, Pageable pageable) {

		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;
		FestivalStatus status = req.getStatus();
		String statusStr = status != null ? status.toString() : null;
		String order = req.getOrder().toString();

		return festivalRepository.getFestivalList(regionStr, statusStr,
			order,req.getKeyword(), pageable);
	}

	/**
	 * 축제 상세내용
	 * @param festivalId
	 * @return
	 */
	@Transactional
	public FestivalResponse getFestivalContent(long festivalId) {
		Festival festival = festivalRepository.findById(festivalId)
			.orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND,"관련 내용을 찾을 수 없습니다."));
		return FestivalResponse.fromEntity(festival);
	}

	/**
	 * 통합검색
	 * @param request
	 * @return
	 */
	public FestivalTotalSearchResponse getTotalSearch(FestivalSearchDto request){

		String keyword = request.getKeyword();

		//축제목록
		Page<Festival> festivalPageList = festivalRepository.getFestivalList(null, null, null,
			keyword, PageRequest.of(0, 10));
		//List<FestivalResponse> festivalList = festivalPageList.stream().map(FestivalResponse::fromEntity).collect(Collectors.toList());
		PageResult<FestivalResponse> resultFestivalList = PageResult.of(
			festivalPageList.map(FestivalResponse::fromEntity)
		);

		//채팅방 목록
		Page<ChatRoom> chatroomPageList = chatRoomRepository.chatFestivalRoomList(0, null, null,
			keyword, PageRequest.of(0, 10));
		//List<ChatRoomResponse> chatroomList = chatroomPageList.stream().map(ChatRoomResponse::fromEntity).collect(Collectors.toList());
		PageResult<ChatRoomResponse> resultRoomList = PageResult.of(
			chatroomPageList.map(ChatRoomResponse::fromEntity)
		);

		return new FestivalTotalSearchResponse(
			resultFestivalList,
			resultRoomList
		);

	}


	/**
	 * 지역분류 가져오기
	 * @return
	 */
	public List getRegionList(){
		List<Region> regions = Arrays.asList(Region.values());
		return regions;
	}

}
