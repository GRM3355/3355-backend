package com.grm3355.zonie.apiserver.domain.chatroom.service;

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

import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.SearchRequest;
import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalStatus;
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
public class ChatRoomService {
	private static final String PRE_FIX = "room:";
	private static int MAX_ROOM = 30;
	private static double MAX_RADIUS = 1.0;
	private final RedisTokenService redisTokenService;
	private final FestivalInfoService festivalInfoService;
	private final ChatRoomRepository chatRoomRepository;
	private final UserRepository userRepository;
	private final FestivalRepository festivalRepository;

	// GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	GeometryFactory geometryFactory = new GeometryFactory();

	public ChatRoomService(RedisTokenService redisTokenService, FestivalInfoService festivalInfoService,
		ChatRoomRepository chatRoomRepository, UserRepository userRepository, FestivalRepository festivalRepository
	) {
		this.redisTokenService = redisTokenService;
		this.festivalInfoService = festivalInfoService;
		this.chatRoomRepository = chatRoomRepository;
		this.userRepository = userRepository;
		this.festivalRepository = festivalRepository;
	}

	/**
	 * 채팅방 생성
	 * @param festivalId
	 * @param request
	 * @param userDetails
	 * @return
	 */
	public ChatRoomResponse setCreateChatRoom(long festivalId,
		ChatRoomRequest request, UserDetailsImpl userDetails) {

		//0. 사용자 아이디 가져오기
		String userId = userDetails.getUsername();
		User user = userRepository.findByUserId(userId)
			.orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND, "사용자 정보가 유효하지 않습니다."));

		//1. 토큰값 체크
		boolean isTokenValidate = redisTokenService.validateLocationToken(userId);
		if(!isTokenValidate) {
			new BusinessException(ErrorCode.NOT_FOUND, "토큰이 유효하지 않습니다.");
		}

		//2. 축제 존재여부체크
		Festival festival = festivalInfoService.getDataValid(festivalId);

		//3. 축제 거리계산하기
		LocationDto location1 = getUserPostion(userId);
		LocationDto location2 = getFestivalPosition(festival);
		boolean isValidDistince = festivalCaculator(location1, location2);
		if(!isValidDistince)
			new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설 반경이 아닙니다.");

		//4. 채팅방 갯수 체크
		if (festival.getChatRoomCount() >= MAX_ROOM) {
			new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설은 "+MAX_ROOM+"개까지 입니다.");
		}

		//5. 채팅방 저장
		//채팅룸 아이디 생성
		String roomId = createRoomId();
		//위치 세팅
		Point point = geometryFactory.createPoint(new Coordinate(location2.getLon(), location2.getLat()));

		ChatRoom chatRoom = ChatRoom.builder()
			.chatRoomId(roomId)
			.festival(festival)
			.user(user)
			.title(request.getTitle())
			.maxParticipants(MAX_ROOM)
			.radius(MAX_ROOM)
			.position(point).build();
		ChatRoom saveChatRoom = chatRoomRepository.save(chatRoom);

		//festival에 채팅방갯수 저장 /festivalId
		festivalInfoService.increaseChatRoomCount(festivalId);

		//dto 변환
		ChatRoomResponse chatRoomResponse = ChatRoomResponse.builder()
			.chatRoomId(roomId)
			.festivalId(festivalId)
			.userId(userId)
			.title(request.getTitle())
			.lat(chatRoom.getPosition().getY())
			.lon(chatRoom.getPosition().getX())
			.build();

		//return
		return chatRoomResponse;
	}

	/**
	 * 축제별 채팅방 목록
	 * @param festivalId
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<ChatRoomResponse> getFestivalChatRoomList(long festivalId,
		SearchRequest req) {

		Sort.Order order = Sort.Order.desc("createdAt");
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<ChatRoom> pageList = getFestivalListTypeUser(festivalId, req, pageable);

		//페이지 변환
		List<ChatRoomResponse> dtoPage = pageList.stream().map(ChatRoomResponse::fromEntity)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	private Page<ChatRoom> getFestivalListTypeUser(
		long festivalId, SearchRequest req, Pageable pageable) {

		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;
		String order = req.getOrder().toString();

		return chatRoomRepository
			.chatFestivlRoomList(festivalId, regionStr, order, req.getKeyword(), pageable);

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
		String order = req.getOrder().toString();

		return chatRoomRepository
			.chatMyRoomList(userId, regionStr, order, req.getKeyword(), pageable);

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
