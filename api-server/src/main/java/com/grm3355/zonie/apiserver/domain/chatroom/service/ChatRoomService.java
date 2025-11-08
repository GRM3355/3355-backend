package com.grm3355.zonie.apiserver.domain.chatroom.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.chatroom.enums.OrderType;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class ChatRoomService {

	private static final String PRE_FIX = "room:";

	@Value("${chat.pre-create-day}")
	private int PRE_CREATE_DAYS; //시작하기전 몇일전부터 생성가능

	@Value("${chat.max-chat-person}")
	private int MAX_PARTICIPANTS; //최대인원스

	@Value("${chat.max-chat-room}")
	private int MAX_ROOM; //최개 채팅방 갯수

	@Value("${chat.radius}")
	private double MAX_RADIUS; //최대km

	private final RedisTokenService redisTokenService;
	private final FestivalInfoService festivalInfoService;
	private final ChatRoomRepository chatRoomRepository;
	private final UserRepository userRepository;

	// GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	GeometryFactory geometryFactory = new GeometryFactory();

	public ChatRoomService(RedisTokenService redisTokenService, FestivalInfoService festivalInfoService,
		ChatRoomRepository chatRoomRepository, UserRepository userRepository
	) {
		this.redisTokenService = redisTokenService;
		this.festivalInfoService = festivalInfoService;
		this.chatRoomRepository = chatRoomRepository;
		this.userRepository = userRepository;
	}

	/**
	 * 채팅방 생성
	 * @param festivalId
	 * @param request
	 * @param userDetails
	 * @return
	 */
	@Transactional
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

		//2. 축제 존재여부체크, 축제시작일 1일주일 전부터 생성가능하도록 변경
		// 시작일에서 -7일부터 생성가능
		//LocalDateTime endDateLimit = LocalDateTime.now().plusDays(MAX_DAYS);
		//LocalDate date = endDateLimit.toLocalDate();
		System.out.println("==============>PRE_DAYS="+PRE_CREATE_DAYS);
		System.out.println("==============>festivalId="+festivalId);
		Festival festival = festivalInfoService.getDataValid(festivalId, PRE_CREATE_DAYS);

		//3. 축제 거리계산하기 - 거리계산은 1주일 전부터 생성이므로 보류시킨다.
		 LocationDto location1 = getUserPostion(userId);	//유저위치
		 LocationDto location2 = getFestivalPosition(festival);	//축제위치
		// boolean isValidDistince = festivalCaculator(location1, location2);
		// if(!isValidDistince)
		// 	new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설 반경이 아닙니다.");

		//4. 채팅방 갯수 체크
		if (festival.getChatRoomCount() >= MAX_ROOM) {
			new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설은 "+MAX_ROOM+"개까지 입니다.");
		}

		//5. 채팅방 저장
		//채팅룸 아이디 생성
		String roomId = createRoomId();
		
		//위치 세팅
		//Point point = geometryFactory.createPoint(new Coordinate(location2.getLon(), location2.getLat()));
		Point point = geometryFactory.createPoint(new Coordinate(location1.getLon(), location1.getLat()));
		ChatRoom chatRoom = ChatRoom.builder()
			.chatRoomId(roomId)
			.festival(festival)
			.user(user)
			.title(request.getTitle())
			.maxParticipants(MAX_PARTICIPANTS)
			.radius(MAX_RADIUS)
			.position(point).build();
		System.out.println("=====> 채팅방 저장");
		ChatRoom saveChatRoom = chatRoomRepository.save(chatRoom);
		System.out.println("=====> 채팅방 저장된 아이디 "+saveChatRoom.getChatRoomId());
		System.out.println("=====> 채팅방 저장된 등록일 "+saveChatRoom.getCreatedAt());

		//festival에 채팅방갯수 저장 /festivalId
		festivalInfoService.increaseChatRoomCount(festivalId);

		//dto 변환
		ChatRoomResponse chatRoomResponse = ChatRoomResponse.builder()
			.chatRoomId(saveChatRoom.getChatRoomId())
			.festivalId(saveChatRoom.getFestival().getFestivalId())
			.userId(saveChatRoom.getUser().getUserId())
			.title(saveChatRoom.getTitle())
			.lat(saveChatRoom.getPosition().getY())
			.lon(saveChatRoom.getPosition().getX())
			.build();
		return chatRoomResponse;
	}

	/**
	 * 축제별 채팅방 목록
	 * @param festivalId
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<MyChatRoomResponse> getFestivalChatRoomList(long festivalId,
		ChatRoomSearchRequest req) {

		System.out.println("===============>festivalId="+festivalId);
		//Sort.Order order = order = Sort.Order.desc("createdAt");
		Sort.Order order = null;
		if (req.getOrder() == OrderType.DATE_ASC){
			order = Sort.Order.asc("createdAt");
		}else if (req.getOrder() == OrderType.DATE_DESC) {
			order = Sort.Order.desc("createdAt");
		}else if (req.getOrder() == OrderType.PART_ASC) {
			order = Sort.Order.asc("participantCount");
		}else if (req.getOrder() == OrderType.PART_DESC) {
			order = Sort.Order.desc("participantCount");
		}else{
			order = Sort.Order.desc("createdAt");
		}
		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<ChatRoomInfoDto> pageList = getFestivalListTypeUser(festivalId, req, pageable);

		//페이지 변환
		List<MyChatRoomResponse> dtoPage = pageList.stream().map(MyChatRoomResponse::fromDto)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	public Page<ChatRoomInfoDto> getFestivalListTypeUser(long festivalId,
		ChatRoomSearchRequest req, Pageable pageable) {

		String keyword = (req.getKeyword() != null ) ? req.getKeyword() : null;

		return chatRoomRepository.chatFestivalRoomList(festivalId, keyword, pageable);
		/*
		return switch (order) {
			case PART_ASC -> chatRoomRepository
				.chatFestivalRoomList_PART_ASC(festivalId, keyword, pageable);
			case PART_DESC -> chatRoomRepository
				.chatFestivalRoomList_PART_DESC(festivalId, keyword, pageable);
			case DATE_ASC -> chatRoomRepository
				.chatFestivalRoomList_DATE_ASC(festivalId, keyword, pageable);
			case DATE_DESC -> chatRoomRepository
				.chatFestivalRoomList_DATE_DESC(festivalId, keyword, pageable);
		};
		 */
	}

	/**
	 * 나의 채팅방 목록
	 * @param userDetails
	 * @param req
	 * @return
	 */
	@Transactional
	public Page<MyChatRoomResponse> getMyroomChatRoomList(UserDetailsImpl userDetails,
		ChatRoomSearchRequest req) {
		String userId = userDetails.getUsername();

		System.out.println("===============>getMyroomChatRoomList userId="+userId);
		//Sort.Order order = order = Sort.Order.desc("createdAt");
		Sort.Order order = null;
		if (req.getOrder() == OrderType.DATE_ASC){
			order = Sort.Order.asc("createdAt");
		}else if (req.getOrder() == OrderType.DATE_DESC) {
			order = Sort.Order.desc("createdAt");
		}else if (req.getOrder() == OrderType.PART_ASC) {
			order = Sort.Order.asc("participantCount");
		}else if (req.getOrder() == OrderType.PART_DESC) {
			order = Sort.Order.desc("participantCount");
		}else{
			order = Sort.Order.desc("createdAt");
		}
		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<ChatRoomInfoDto> pageList = getMyroomListTypeUser(userId, req, pageable);

		//페이지 변환
		List<MyChatRoomResponse> dtoPage = pageList.stream().map(MyChatRoomResponse::fromDto)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	private Page<ChatRoomInfoDto> getMyroomListTypeUser(String userId,
		ChatRoomSearchRequest req, Pageable pageable) {


		String keyword = (req.getKeyword() != null ) ? req.getKeyword() : null;

		System.out.println("===============>getMyroomChatRoomList userId===>"+userId);
		System.out.println("===============>getMyroomChatRoomList keyword===>"+keyword);
		//return chatRoomRepository.chatMyRoomList(userId, keyword, pageable);

		return chatRoomRepository.chatMyRoomList(userId, keyword, pageable);
		/*
		return switch (order) {
			case PART_ASC -> chatRoomRepository
				.chatMyRoomList_PART_ASC(userId, keyword, pageable);
			case PART_DESC -> chatRoomRepository
				.chatMyRoomList_PART_DESC(userId, keyword, pageable);
			case DATE_ASC -> chatRoomRepository
				.chatMyRoomList_DATE_ASC(userId, keyword, pageable);
			case DATE_DESC -> chatRoomRepository
				.chatMyRoomList_DATE_DESC(userId, keyword, pageable);
		};
		 */

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
