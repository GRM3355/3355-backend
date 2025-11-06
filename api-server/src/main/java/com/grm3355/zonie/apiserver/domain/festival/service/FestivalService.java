package com.grm3355.zonie.apiserver.domain.festival.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
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

	// GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	GeometryFactory geometryFactory = new GeometryFactory();

	public FestivalService(FestivalRepository festivalRepository,
		ChatRoomService chatRoomService) {
		this.festivalRepository = festivalRepository;
		this.chatRoomService = chatRoomService;
	}

	/**
	 * 축제목록
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<FestivalResponse> getFestivalList(FestivalSearchRequest req) {

		System.out.println("===========> 11111");
		Sort.Order order = Sort.Order.asc("eventStartDate");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		System.out.println("===========> 22222");
		//ListType 내용 가져오기
		Page<Festival> pageList = getFestivalListType(req, pageable);

		System.out.println("===========> 33333");
		//페이지 변환
		List<FestivalResponse> dtoPage = pageList.stream().map(FestivalResponse::fromEntity)
			.collect(Collectors.toList());

		System.out.println("===========> 4444");
		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	public Page<Festival> getFestivalListType(
		FestivalSearchRequest req, Pageable pageable) {

		System.out.println("===========> 5555");
		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;

		FestivalStatus status = req.getStatus();
		String statusStr = status != null ? status.toString() : null;
		String order = req.getOrder().toString();

		System.out.println("===========> 6666");
		// 현재 시점 기준 +30일
		LocalDateTime endDateLimit = LocalDateTime.now().plusDays(30);
		LocalDate date = endDateLimit.toLocalDate();

		System.out.println("===========> 7777 endDateLimit ="+endDateLimit);
		System.out.println("===========> 7777 endDateLimit ="+date);
		System.out.println("===========> 7777 req.getOrder() ="+req.getOrder());
		return switch (req.getOrder()) {
			case DATE_ASC -> festivalRepository
				.getFestivalList_DATE_ASC(regionStr, statusStr, req.getKeyword(), date, pageable);
			case DATE_DESC -> festivalRepository
				.getFestivalList_DATE_DESC(regionStr, statusStr, req.getKeyword(), date, pageable);
			case TITLE_ASC -> festivalRepository
				.getFestivalList_TITLE_ASC(regionStr, statusStr, req.getKeyword(), date, pageable);
			case TITLE_DESC -> festivalRepository
				.getFestivalList_TITLE_DESC(regionStr, statusStr, req.getKeyword(), date, pageable);
		};
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
	 * 지역분류 가져오기
	 * @return
	 */
	public List getRegionList(){
		return Arrays.stream(Region.values()).map(region -> Map.of(
			"region", region.getName(),
			"code", region.name()
				)).toList();
	}

}
