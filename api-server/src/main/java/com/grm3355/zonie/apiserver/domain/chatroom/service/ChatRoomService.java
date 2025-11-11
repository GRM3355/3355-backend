package com.grm3355.zonie.apiserver.domain.chatroom.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.enums.OrderType;
import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
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
	private final RedisTokenService redisTokenService;
	private final FestivalInfoService festivalInfoService;
	private final ChatRoomRepository chatRoomRepository;
	private final UserRepository userRepository;
	private final StringRedisTemplate stringRedisTemplate;
	GeometryFactory geometryFactory = new GeometryFactory(); // GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	@Value("${chat.pre-create-day}")
	private int pre_create_days;    //시작하기전 몇일전부터 생성가능
	@Value("${chat.max-chat-person}")
	private int max_participants;    //최대인원스
	@Value("${chat.max-chat-room}")
	private int max_room;    //최개 채팅방 갯수
	@Value("${chat.radius}")
	private double max_radius;   //최대km

	public ChatRoomService(RedisTokenService redisTokenService, FestivalInfoService festivalInfoService,
		ChatRoomRepository chatRoomRepository, UserRepository userRepository, StringRedisTemplate stringRedisTemplate
	) {
		this.redisTokenService = redisTokenService;
		this.festivalInfoService = festivalInfoService;
		this.chatRoomRepository = chatRoomRepository;
		this.userRepository = userRepository;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/**
	 * 채팅방 생성
	 */
	public ChatRoomResponse setCreateChatRoom(long festivalId,
		ChatRoomRequest request, UserDetailsImpl userDetails) {

		//0. 사용자 아이디 가져오기
		String userId = userDetails.getUsername();
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 정보가 유효하지 않습니다."));

		//1. 토큰값 체크
		boolean isTokenValidate = redisTokenService.validateLocationToken(userId);
		if (!isTokenValidate) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "토큰이 유효하지 않습니다.");
		}

		//2. 축제 존재여부체크
		Festival festival = festivalInfoService.getDataValid(festivalId, pre_create_days);

		//3. 축제 거리계산하기
		LocationDto location1 = getUserPosition(userId);
		LocationDto location2 = getFestivalPosition(festival);
		boolean isValidDistance = festivalCalculator(location1, location2);
		if (!isValidDistance) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설 반경이 아닙니다.");
		}

		//4. 채팅방 갯수 체크
		if (festival.getChatRoomCount() >= max_room) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설은 " + max_room + "개까지 입니다.");
		}

		//5. 채팅방 저장
		// 채팅룸 아이디 생성
		String roomId = createRoomId();
		// 위치 세팅
		Point point = geometryFactory.createPoint(
			new Coordinate(location2.getLon(), location2.getLat())); // lon=X, lat=Y

		ChatRoom chatRoom = ChatRoom.builder()
			.chatRoomId(roomId)
			.festival(festival)
			.user(user)
			.title(request.getTitle())
			.maxParticipants(max_participants)
			.radius(max_radius)
			.position(point)
			.participantCount(0L)
			.build();
		// ChatRoom saveChatRoom = chatRoomRepository.save(chatRoom);

		// 디버깅을 위해 일시적으로 saveAndFlush 사용함 - 추후 복구하기
		ChatRoom saveChatRoom;
		try {
			saveChatRoom = chatRoomRepository.saveAndFlush(chatRoom);
		} catch (Exception e) {
			log.error("채팅방 저장 중 DB 예외 발생: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "채팅방 저장 중 오류가 발생했습니다.");
		}

		//festival에 채팅방갯수 저장 /festivalId
		festivalInfoService.increaseChatRoomCount(festivalId);

		//dto 변환
		return ChatRoomResponse.builder()
			.chatRoomId(saveChatRoom.getChatRoomId())
			.festivalId(saveChatRoom.getFestival().getFestivalId())
			.userId(saveChatRoom.getUser().getUserId())
			.title(saveChatRoom.getTitle())
			//.lat(0.0).lon(0.0)
			.lat(saveChatRoom.getPosition().getY())
			.lon(saveChatRoom.getPosition().getX())
			.build();
	}

	/**
	 * 축제별 채팅방 목록
	 */
	@Transactional
	public Page<MyChatRoomResponse> getFestivalChatRoomList(long festivalId, ChatRoomSearchRequest req) {

		//정렬 순서
		if (req.getOrder() == null)
			req.setOrder(OrderType.DATE_ASC);

		Sort.Order order = getSortOrder(req.getOrder());

		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), Sort.by(order));

		// 1. PG에서 기본 정보 조회: ListType 내용 가져오기
		Page<ChatRoomInfoDto> pageList = getFestivalListTypeUser(festivalId, req, pageable);

		// 2. Redis에서 마지막 대화 내용 일괄 조회
		Map<String, String> lastContentsMap = getLastContents(pageList.getContent());

		// 3. DTO 변환
		List<MyChatRoomResponse> dtoPage = pageList.stream()
			.map(dto -> {
				String lastContent = lastContentsMap.getOrDefault( // 마지막 대화 내용 포함
					dto.chatRoomId(),
					null // 대화 내용이 없으면 null
				);
				// MyChatRoomResponse의 오버로딩된 fromDto 호출
				return MyChatRoomResponse.fromDto(dto, lastContent);
			})
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	// 축제별 채팅방 검색조건별 목록 가져오기
	public Page<ChatRoomInfoDto> getFestivalListTypeUser(
		long festivalId, ChatRoomSearchRequest req, Pageable pageable) {

		String keyword = (req.getKeyword() != null) ? req.getKeyword() : "";

		return chatRoomRepository.chatFestivalRoomList(festivalId, keyword, pageable);

	}

	/**
	 * 나의 채팅방 목록
	 */
	@Transactional
	public Page<MyChatRoomResponse> getMyRoomChatRoomList(UserDetailsImpl userDetails,
		ChatRoomSearchRequest req) {
		String userId = userDetails.getUsername();

		//정렬 순서
		if (req.getOrder() == null)
			req.setOrder(OrderType.PART_DESC);

		Sort.Order order = getSortOrder(req.getOrder());

		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<ChatRoomInfoDto> pageList = getMyRoomListTypeUser(userId, req, pageable);

		//페이지 변환
		List<MyChatRoomResponse> dtoPage = pageList.stream().map(MyChatRoomResponse::fromDto)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	private Page<ChatRoomInfoDto> getMyRoomListTypeUser(String userId, ChatRoomSearchRequest req, Pageable pageable) {

		String keyword = (req.getKeyword() != null) ? req.getKeyword() : "";
		return chatRoomRepository.chatMyRoomList(userId, keyword, pageable);

	}

	/**
	 * DTO 리스트를 받아 Redis에서 마지막 대화 내용을 일괄 조회합니다. (MGET)
	 */
	private Map<String, String> getLastContents(List<ChatRoomInfoDto> dtoList) {
		if (dtoList.isEmpty()) {
			return Collections.emptyMap();
		}

		// 1. RoomId 리스트 추출 (Redis 키 생성)
		List<String> redisKeys = dtoList.stream()
			.map(dto -> "chatroom:last_msg_content:" + dto.chatRoomId())
			.toList();

		// 2. Redis MGET
		List<String> contents = stringRedisTemplate.opsForValue().multiGet(redisKeys);

		// 3. Map<RoomId, Content>로 변환
		Map<String, String> contentMap = new HashMap<>();
		for (int i = 0; i < dtoList.size(); i++) {
			String roomId = dtoList.get(i).chatRoomId();
			String content = (contents != null && i < contents.size() && contents.get(i) != null)
				? contents.get(i)
				: null;
			contentMap.put(roomId, content);
		}
		return contentMap;
	}

	//정렬 순서 가져오기
	private Sort.Order getSortOrder(OrderType orderType) {
		return switch (orderType) {
			case PART_ASC -> Sort.Order.asc("participant_count");
			case PART_DESC -> Sort.Order.desc("participant_count");
			case DATE_ASC -> Sort.Order.asc("created_at");
			case DATE_DESC -> Sort.Order.desc("created_at");
			case ACTIVE_ASC -> Sort.Order.asc("last_message_at");
			case ACTIVE_DESC -> Sort.Order.desc("last_message_at");
		};
	}

	// 채팅룸 아이디 생성
	private String createRoomId() {
		return PRE_FIX + UUID.randomUUID();
	}

	// 가능거리 계산
	private boolean festivalCalculator(LocationDto locationDto, LocationDto festivalDto) {
		double km = LocationService.getDistanceCalculator(locationDto, festivalDto);
		return km <= max_radius; // 로직: km이 MAX_RADIUS(1.0km)보다 작거나 같을 때 true 반환 (반경 내에 있을 때만 생성 가능)
	}

	//사용자 위치정보
	private LocationDto getUserPosition(String userId) {
		UserTokenDto userTokenDto = redisTokenService.getLocationInfo(userId);
		return LocationDto.builder().lat(userTokenDto.getLat()).lon(userTokenDto.getLon()).build();
	}

	//축제위치정보
	private LocationDto getFestivalPosition(Festival festival) {
		return LocationDto.builder()
			.lat(festival.getPosition().getY())
			.lon(festival.getPosition().getX()).build();
	}
}
