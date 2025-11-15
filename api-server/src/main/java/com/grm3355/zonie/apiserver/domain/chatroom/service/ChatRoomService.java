package com.grm3355.zonie.apiserver.domain.chatroom.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.data.redis.core.RedisTemplate;
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
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

	private static final String PRE_FIX = "";
	private final RedisTokenService redisTokenService;
	private final FestivalInfoService festivalInfoService;
	private final RedisScanService redisScanService;
	private final ChatRoomRepository chatRoomRepository;
	private final UserRepository userRepository;
	private final StringRedisTemplate stringRedisTemplate;
	GeometryFactory geometryFactory = new GeometryFactory(); // GeometryFactory 생성 (보통 한 번만 만들어 재사용)

	private final RedisTemplate<String, Object> redisTemplate;
	private static final String JOIN_EVENT_CHANNEL = "chat-events:join";

	@Value("${chat.pre-create-day}")
	private int pre_create_days;    //시작하기전 몇일전부터 생성가능
	@Value("${chat.max-chat-person}")
	private long max_participants;    //최대인원스
	@Value("${chat.max-chat-room}")
	private int max_room;    //최개 채팅방 갯수
	@Value("${chat.radius}")
	private double max_radius;   //최대km

	/**
	 * 채팅방 생성
	 */
	public ChatRoomResponse setCreateChatRoom(long festivalId,
		ChatRoomRequest request, UserDetailsImpl userDetails) {

		// 0. 유저 조회
		String userId = userDetails.getUsername();
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 정보가 유효하지 않습니다."));

		// 1. 축제 조회
		String festivalIdStr = String.valueOf(festivalId);
		Festival festival = festivalInfoService.getDataValid(festivalId, pre_create_days);

		// 2. 위치 정보 객체 생성
		LocationDto currentLocation = LocationDto.builder()
			.lat(request.getLat())
			.lon(request.getLon())
			.build();

		// 3. 축제 거리계산하기 (PostGIS 반경 검증)
		boolean isWithinRadius = festivalInfoService.isUserWithinFestivalRadius(
			festivalId,
			currentLocation.getLat(),
			currentLocation.getLon(),
			max_radius
		);
		if (!isWithinRadius) {
			// 축제 반경(max_radius)을 벗어났다면 예외 발생
			throw new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설 반경(" + max_radius + "km)을 벗어났습니다.");
		}

		// 4. 위치 인증 토큰 발급/갱신 (반경 검증 통과 후 토큰 처리)
		// setToken은 토큰이 없으면 생성, 있으면 위치 갱신 및 TTL 갱신을 수행합니다.
		UserTokenDto userTokenDto = redisTokenService.setToken(userId, festivalIdStr, currentLocation);

		if (userTokenDto == null) {
			// setToken이 토큰 정보를 반환해야 하므로, 이 경우는 내부 오류로 처리
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "위치 인증 토큰 정보를 읽을 수 없습니다.");
		}

		// 5. 채팅방 갯수 체크
		if (festival.getChatRoomCount() >= max_room) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설은 " + max_room + "개까지 입니다.");
		}

		//5. 채팅방 저장
		// 채팅방의 위치(Point)는 갱신된(혹은 새로 생성된) 토큰의 위치를 사용합니다.
		Point point = geometryFactory.createPoint(
			new Coordinate(userTokenDto.getLon(), userTokenDto.getLat())); // lon=X, lat=Y

		String roomId = createRoomId();
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

		ChatRoom saveChatRoom = chatRoomRepository.save(chatRoom);

		//festival에 채팅방갯수 저장 /festivalId
		festivalInfoService.increaseChatRoomCount(festivalId);

		try {
			// 5. 이벤트 페이로드 생성
			Map<String, String> joinEvent = Map.of(
				"userId", user.getUserId(),
				"roomId", saveChatRoom.getChatRoomId()
			);

			// 6. Redis Pub/Sub으로 이벤트 발행
			redisTemplate.convertAndSend(JOIN_EVENT_CHANNEL, joinEvent);
			log.info("Published join event for User {} to Room {}", user.getUserId(), saveChatRoom.getChatRoomId());

		} catch (Exception e) {
			// 채팅방 생성 트랜잭션은 성공했으나, 이벤트 발행에 실패한 경우
			// 사용자는 수동으로 '참여' 버튼을 눌러야 할 수 있음.
			log.error("Failed to publish join event for User {} to Room {}: {}",
				user.getUserId(), saveChatRoom.getChatRoomId(), e.getMessage());
		}

		//dto 변환
		return ChatRoomResponse.builder()
			.chatRoomId(roomId)
			.festivalId(festivalId)
			.userId(userId)
			.title(request.getTitle())
			.lat(chatRoom.getPosition().getY())
			.lon(chatRoom.getPosition().getX())
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

		// 2. Redis에서 실시간 데이터 일괄 조회
		List<String> roomIds = pageList.getContent().stream().map(ChatRoomInfoDto::chatRoomId).toList();
		if (roomIds.isEmpty()) {
			return new PageImpl<>(Collections.emptyList(), pageable, pageList.getTotalElements());
		}

		// 2-1. Redis 조회를 위한 Key Set 3개 생성: roomIds 리스트 기반 실제 Redis 키 Set
		Set<String> participantKeys = roomIds.stream()
			.map(id -> "chatroom:participants:" + id)
			.collect(Collectors.toSet());

		Set<String> contentKeys = roomIds.stream()
			.map(id -> "chatroom:last_msg_content:" + id)
			.collect(Collectors.toSet());

		Set<String> timestampKeys = roomIds.stream()
			.map(id -> "chatroom:last_msg_at:" + id)
			.collect(Collectors.toSet());


		// 2-2. RedisScanService 메소드 호출
		// (1) 참여자 수 조회 (SCARD)
		Map<String, Long> participantCountsMap = redisScanService.getParticipantCounts(participantKeys);

		// (2) 마지막 대화 내용 조회 (MGET)
		// (MGET 범용 메서드인 multiGetLastMessageTimestamps를 재사용)
		Map<String, String> lastContentsMap = redisScanService.multiGetLastMessageTimestamps(contentKeys);

		// (3) 마지막 대화 시각 조회 (MGET)
		Map<String, String> lastTimestampsMap = redisScanService.multiGetLastMessageTimestamps(timestampKeys);

		// 3. DTO 변환 (PG 백업 데이터 + Redis 실시간 데이터 병합)
		List<MyChatRoomResponse> dtoPage = pageList.stream()
			.map(dto -> {
				String roomId = dto.chatRoomId();

				// 3-1. Key를 사용하여 Map에서 조회: roomIds가 아닌 완성된 키 이름으로 조회
				Long realTimeCount = participantCountsMap.get("chatroom:participants:" + roomId);
				Long finalCount = (realTimeCount != null) ? realTimeCount : dto.participantCount();

				String realTimeContent = lastContentsMap.get("chatroom:last_msg_content:" + roomId);
				String finalContent = realTimeContent; // 백업본은 사용 안 함

				String timestampStr = lastTimestampsMap.get("chatroom:last_msg_at:" + roomId);
				Long realTimeTimestamp = (timestampStr != null) ? Long.parseLong(timestampStr) : null;

				// 둘 중 더 최신 시간(max)을 클라이언트에 반환
				Long finalTimestamp = dto.lastMessageAt();
				if (realTimeTimestamp != null && (finalTimestamp == null || realTimeTimestamp > finalTimestamp)) {
					finalTimestamp = realTimeTimestamp;
				}

				// MyChatRoomResponse의 오버로딩된 fromDto 호출
				return MyChatRoomResponse.fromDto(dto, finalContent, finalCount, finalTimestamp);
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
		System.out.println("===============>keyword===>" + keyword);
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

}