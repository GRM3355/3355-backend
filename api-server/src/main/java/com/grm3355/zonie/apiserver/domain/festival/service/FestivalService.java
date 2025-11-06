package com.grm3355.zonie.apiserver.domain.festival.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.SearchRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.service.FestivalInfoService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FestivalService {
	private final FestivalRepository festivalRepository;

	// GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	GeometryFactory geometryFactory = new GeometryFactory();

	public FestivalService(FestivalRepository festivalRepository) {
		this.festivalRepository = festivalRepository;
	}

	/**
	 * 축제별 채팅방 목록
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<ChatRoomResponse> getFestivalList(FestivalSearchRequest req) {

		Sort.Order order = Sort.Order.asc("eventStartDate");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<ChatRoom> pageList = getFestivalListTypeUser(req, pageable);

		//페이지 변환
		List<ChatRoomResponse> dtoPage = pageList.stream().map(ChatRoomResponse::fromEntity)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	private Page<ChatRoom> getFestivalListTypeUser(
		long festivalId, FestivalSearchRequest req, Pageable pageable) {

		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;

		//FestivalStatus
		return switch (req.getOrder()) {
			case DATE_ASC -> festivalRepository
				.chatFestivlList_DATE_ASC(regionStr, req.getKeyword(), pageable);
			case DATE_DESC -> festivalRepository
				.chatFestivlList_DATE_DESC(regionStr, req.getKeyword(), pageable);
			case TITLE_ASC -> festivalRepository
				.chatFestivlList_TITLE_ASC(regionStr, req.getKeyword(), pageable);
			case TITLE_DESC -> festivalRepository
				.chatFestivlList_TITLE_DESC(regionStr, req.getKeyword(), pageable);
		};
	}

	/**
	 * 나의 채팅방 목록
	 * @param userDetails
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<MyChatRoomResponse> getMyroomChatRoomList(UserDetailsImpl userDetails,
		SearchRequest req) {

		String userId = userDetails.getUsername();

		Sort.Order order = Sort.Order.desc("createdAt");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<ChatRoomInfoDto> pageList = getMyroomListTypeUser(userId, req, pageable);

		//페이지 변환
		List<MyChatRoomResponse> dtoPage = pageList.stream().map(MyChatRoomResponse::fromDto)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	private Page<ChatRoomInfoDto> getMyroomListTypeUser(String userId, SearchRequest req, Pageable pageable) {

		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;

		return switch (req.getOrder()) {
			case PART_ASC -> chatRoomRepository
				.chatMyRoomList_PARTICIPANTS_ASC(userId, regionStr, req.getKeyword(), pageable);
			case PART_DESC -> chatRoomRepository
				.chatMyRoomList_PARTICIPANTS_DESC(userId, regionStr, req.getKeyword(), pageable);
			case DATE_ASC -> chatRoomRepository
				.chatMyRoomList_CREATED_AT_ASC(userId, regionStr,req.getKeyword(), pageable);
			case DATE_DESC -> chatRoomRepository
				.chatMyRoomList_CREATED_AT_DESC(userId, regionStr,req.getKeyword(), pageable);
		};
	}

	//채팅룸 아이디 생성
	private String createRoomId(){
		return PRE_FIX + UUID.randomUUID();
	}

	//가능거리 계산
	private boolean festivalCaculator(LocationDto locationDto, LocationDto festivalDto) {
		double km = LocationService.getDistanceCalculator(locationDto, festivalDto);
		return (km>MAX_RADIUS)?true:false;
	}

	//사용자 위치정보
	private LocationDto getUserPostion(String userId){
		System.out.println("=======> userId="+userId);
		UserTokenDto userTokenDto= redisTokenService.getLocationInfo(userId);
		return  LocationDto.builder().lat(userTokenDto.getLat()).lon(userTokenDto.getLon()).build();
	}

	//축제위치정보
	private LocationDto getFestivalPosition(Festival festival){
		return LocationDto.builder()
			.lat(festival.getPosition().getY())
			.lon(festival.getPosition().getX()).build();
	}
}
