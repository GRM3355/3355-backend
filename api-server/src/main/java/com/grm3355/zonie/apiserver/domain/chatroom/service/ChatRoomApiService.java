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
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomCreateResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.enums.OrderType;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
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
public class ChatRoomApiService {

	private static final String PRE_FIX = "";
	private static final String JOIN_EVENT_CHANNEL = "chat-events:join";
	private final RedisTokenService redisTokenService;
	private final FestivalInfoService festivalInfoService;
	private final RedisScanService redisScanService;
	private final ChatRoomRepository chatRoomRepository;
	private final UserRepository userRepository;
	private final ChatRoomUserRepository chatRoomUserRepository;
	private final StringRedisTemplate stringRedisTemplate;
	private final RedisTemplate<String, Object> redisTemplate;
	GeometryFactory geometryFactory = new GeometryFactory(); // GeometryFactory 생성 (보통 한 번만 만들어 재사용)
	@Value("${chat.pre-create-day}")
	private int preCreateDays;    //시작하기전 몇일전부터 생성가능
	@Value("${chat.max-chat-person}")
	private long maxParticipants;    //최대인원스
	@Value("${chat.max-chat-room}")
	private int maxRoom;    //최개 채팅방 갯수
	@Value("${chat.radius}")
	private double maxRadius;   //최대km
	@Value("${chat.nickname-start}")
	private int nicknameStartNumber;

	/**
	 * 닉네임 순번을 획득하고 ChatRoomUser 엔티티를 생성 및 저장합니다.
	 * (문제 1: ChatRoomUser 생성, 문제 2: #3355 순번 부여 로직 중앙화)
	 * @param user 참여자 User 엔티티
	 * @param chatRoom 참여할 ChatRoom 엔티티
	 * @return 부여된 닉네임
	 */
	private String createAndSaveChatRoomUser(User user, ChatRoom chatRoom) {
		String roomId = chatRoom.getChatRoomId();

		// 1. Redis INCR을 사용하여 해당 방의 닉네임 순번을 획득 (1부터 시작)
		// 키: "chatroom:nickname_seq:[roomId]"
		Long sequence = stringRedisTemplate.opsForValue().increment("chatroom:nickname_seq:" + roomId);
		sequence = sequence == null ? nicknameStartNumber : sequence;

		// 2. 닉네임 순번 계산: nicknameStartNumber (3355) 부터 오름차순
		long newNumber = nicknameStartNumber + sequence - 1;

		// 3. 닉네임 형식 생성: #[순번]
		String nickName = "#" + newNumber;

		// 4. ChatRoomUser 엔티티 생성 및 저장
		ChatRoomUser participant = ChatRoomUser.builder()
			.user(user)
			.chatRoom(chatRoom)
			.nickName(nickName)
			.lastReadAt(java.time.LocalDateTime.now())
			.build();
		chatRoomUserRepository.save(participant);

		return nickName;
	}

	/**
	 * 채팅방 생성
	 * : 유저조회, 축제조회, 위치검증, 토큰발급, 채팅방제한개수체크, 엔티티생성저장, Redis Pub/Sub join 이벤트 발행
	 */
	public ChatRoomCreateResponse setCreateChatRoom(long festivalId,
		ChatRoomRequest request, UserDetailsImpl userDetails) {

		// 0. 유저 조회
		String userId = userDetails.getUsername();
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 정보가 유효하지 않습니다."));

		// 1. 축제 조회
		String festivalIdStr = String.valueOf(festivalId);
		Festival festival = festivalInfoService.getDataValid(festivalId, preCreateDays);

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
			maxRadius
		);
		if (!isWithinRadius) {
			// 축제 반경(max_radius)을 벗어났다면 예외 발생
			throw new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설 반경(" + maxRadius + "km)을 벗어났습니다.");
		}

		// 4. 위치 인증 토큰 발급/갱신 (반경 검증 통과 후 토큰 처리)
		// setToken은 토큰이 없으면 생성, 있으면 위치 갱신 및 TTL 갱신을 수행합니다.
		UserTokenDto userTokenDto = redisTokenService.setToken(userId, festivalIdStr, currentLocation);

		if (userTokenDto == null) {
			// setToken이 토큰 정보를 반환해야 하므로, 이 경우는 내부 오류로 처리
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "위치 인증 토큰 정보를 읽을 수 없습니다.");
		}

		// 5. 채팅방 제한 개수 체크
		if (festival.getChatRoomCount() >= maxRoom) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "채팅방 개설은 " + maxRoom + "개까지 입니다.");
		}

		// 6. 채팅방 저장
		// 채팅방의 위치(Point)는 갱신된(혹은 새로 생성된) 토큰의 위치를 사용합니다.

		// 사용자 위치 - 사용할수 있으므로 주석처리
		//Point point = geometryFactory.createPoint(
		//	new Coordinate(userTokenDto.getLon(), userTokenDto.getLat())); // lon=X, lat=Y

		// 축제위치
		Point point = geometryFactory.createPoint(
			new Coordinate(festival.getPosition().getX(), festival.getPosition().getY())); // lon=X, lat=Y

		String roomId = createRoomId();
		ChatRoom chatRoom = ChatRoom.builder()
			.chatRoomId(roomId)
			.festival(festival)
			.user(user)
			.title(request.getTitle())
			.maxParticipants(maxParticipants)
			.radius(maxRadius)
			.position(point)
			.memberCount(1L)
			.build();

		ChatRoom saveChatRoom = chatRoomRepository.save(chatRoom);
		festivalInfoService.increaseChatRoomCount(festivalId);

		// 방장 닉네임 순번 획득 및 ChatRoomUser 엔티티 생성 및 DB 저장
		String nickName = createAndSaveChatRoomUser(user, saveChatRoom);

		try {
			// 7. 이벤트 페이로드 생성
			Map<String, String> joinEvent = Map.of(
				"userId", user.getUserId(),
				"roomId", saveChatRoom.getChatRoomId(),
				"nickName", nickName // 생성자 닉네임 포함해 전달
			);

			// 8. Redis Pub/Sub으로 이벤트 발행
			redisTemplate.convertAndSend(JOIN_EVENT_CHANNEL, joinEvent);
			log.info("Published join event for User {} to Room {}", user.getUserId(), saveChatRoom.getChatRoomId());

		} catch (Exception e) {
			log.error("Failed to publish join event. Redis 이벤트 발행 실패. 트랜잭션을 롤백합니다. User {}, Room {}",
				user.getUserId(), saveChatRoom.getChatRoomId(), e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "채팅방 생성 중 오류가 발생했습니다. (이벤트 발행 실패)");
		}

		//dto 변환
		return ChatRoomCreateResponse.builder()
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
	 * 정렬 기본: 참여자 많은 순(PART_DESC)
	 */
	@Transactional
	public Page<ChatRoomResponse> getFestivalChatRoomList(long festivalId, ChatRoomSearchRequest req) {

		// 정렬
		if (req.getOrder() == null)
			req.setOrder(OrderType.PART_DESC);

		Sort sort = getSort(req.getOrder());
		Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize(), sort);

		// 1. PG에서 기본 정보 조회: ListType 내용 가져오기
		Page<ChatRoomInfoDto> pageList = getFestivalListTypeUser(festivalId, req, pageable);

		// 2. Redis 실시간 데이터 일괄 조회 및 병합
		return mergeChatRoomDataWithRedis(pageList, pageable);
	}

	// 축제별 채팅방 검색조건별 목록 가져오기
	public Page<ChatRoomInfoDto> getFestivalListTypeUser(
		long festivalId, ChatRoomSearchRequest req, Pageable pageable) {
		String keyword = (req.getKeyword() != null) ? req.getKeyword() : "";
		log.info("===============>ChatRoom.keyword===>{}", keyword);
		return chatRoomRepository.chatFestivalRoomList(festivalId, keyword, pageable);
	}

	/**
	 * 나의 채팅방 목록
	 * 정렬 기본: 최신 대화 순(ACTIVE_DESC)
	 */
	@Transactional
	public Page<ChatRoomResponse> getMyRoomChatRoomList(UserDetailsImpl userDetails,
		ChatRoomSearchRequest req) {
		String userId = userDetails.getUsername();

		// 정렬
		if (req.getOrder() == null)
			req.setOrder(OrderType.ACTIVE_DESC);

		Sort sort = getSort(req.getOrder());
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), sort);

		// 1. PG에서 기본 정보 조회
		Page<ChatRoomInfoDto> pageList = getMyRoomListTypeUser(userId, req, pageable);

		// 2. Redis 실시간 데이터 조회 및 병합
		return mergeChatRoomDataWithRedis(pageList, pageable);
	}

	// 축제별 채팅방 검색조건별 목록 가져오기
	private Page<ChatRoomInfoDto> getMyRoomListTypeUser(String userId, ChatRoomSearchRequest req, Pageable pageable) {
		String keyword = (req.getKeyword() != null) ? req.getKeyword() : "";
		log.info("===============>MyChatRoom.keyword===>{}", keyword);
		return chatRoomRepository.chatMyRoomList(userId, keyword, pageable);
	}

	/**
	 * 채팅방 가입 (DB 트랜잭션 기반 정원 검증 및 memberCount++)
	 * @param roomId 가입할 채팅방 ID
	 * @param userDetails 사용자 정보
	 * @return 가입 성공 시 ChatRoomUser 정보
	 */
	@Transactional
	public String joinRoom(String roomId, UserDetailsImpl userDetails) {
		String userId = userDetails.getUsername();

		// 1. User 조회
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 정보가 유효하지 않습니다."));

		// 2. ChatRoom 조회 및 PESSIMISTIC_WRITE 락 획득
		ChatRoom room = chatRoomRepository.findByChatRoomIdWithLock(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

		// 3. 재입장 방지 검증
		if (chatRoomUserRepository.findByUserAndChatRoom(user, room).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 채팅방에 입장되어 있습니다.");
		}

		// 4. 정원 검증 (memberCount는 이미 DB에 있는 값, PESSIMISTIC_WRITE 락으로 동시성 안전)
		if (room.getMemberCount() >= room.getMaxParticipants()) {
			throw new BusinessException(ErrorCode.CONFLICT, "채팅방 최대 정원(" + room.getMaxParticipants() + "명)을 초과했습니다.");
		}

		// 5. 닉네임 순번 획득 및 ChatRoomUser 엔티티 생성 및 DB 저장
		String nickName = createAndSaveChatRoomUser(user, room);

		// 6. ChatRoom.memberCount++
		room.setMemberCount(room.getMemberCount() + 1);
		chatRoomRepository.save(room);  // Dirty Checking | 명시적 save

		// 7. Redis Pub/Sub 이벤트 발행 (Chat Server로 실시간 연결 알림)
		try {
			Map<String, String> joinEvent = Map.of(
				"userId", userId,
				"roomId", roomId,
				"nickName", nickName
			);
			redisTemplate.convertAndSend(JOIN_EVENT_CHANNEL, joinEvent);
			log.info("Published join event for User {} to Room {} with Nickname {}", userId, roomId, nickName);
		} catch (Exception e) {
			log.error("Failed to publish join event after successful DB join. User {}, Room {}",
				userId, roomId, e);
			// 이벤트 발행 실패는 롤백하지 않고 로그만 남김.
		}

		return nickName;
	}

	/**
	 * 채팅방 퇴장 (DB 트랜잭션 기반 memberCount-- 및 ChatRoomUser 삭제)
	 * @param roomId 퇴장할 채팅방 ID
	 * @param userDetails 사용자 정보
	 */
	@Transactional
	public void leaveRoom(String roomId, UserDetailsImpl userDetails) {
		String userId = userDetails.getUsername();

		// 1. User 조회
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 정보가 유효하지 않습니다."));

		// 2. ChatRoom 조회
		ChatRoom room = chatRoomRepository.findByChatRoomId(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

		// 3. ChatRoomUser 찾기 (퇴장 대상)
		ChatRoomUser participant = chatRoomUserRepository.findByUserAndChatRoom(user, room)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 채팅방에 입장되어 있지 않습니다."));

		// 4. ChatRoomUser 삭제 (퇴장)
		chatRoomUserRepository.delete(participant);

		// 5. ChatRoom.memberCount--
		if (room.getMemberCount() > 0) {
			room.setMemberCount(room.getMemberCount() - 1);
			chatRoomRepository.save(room); // Dirty Checking | 명시적 save
		}

		// 6. Redis Pub/Sub 이벤트 발행 (Chat Server로 실시간 연결 알림)
		try {
			Map<String, String> leaveEvent = Map.of(
				"userId", userId,
				"roomId", roomId
			);
			// chat-events:leave 채널 사용
			redisTemplate.convertAndSend("chat-events:leave", leaveEvent);
			log.info("Published leave event for User {} from Room {}", userId, roomId);
		} catch (Exception e) {
			log.error("Failed to publish leave event after successful DB leave.", e);
		}
	}

	/**
	 * 실시간 데이터를 조회하고 병합
	 * getFestivalChatRoomList와 getMyRoomChatRoomList에서 사용할 공통 메서드
	 */
	private Page<ChatRoomResponse> mergeChatRoomDataWithRedis(Page<ChatRoomInfoDto> pageList, Pageable pageable) {
		List<String> roomIds = pageList.getContent().stream().map(ChatRoomInfoDto::chatRoomId).toList();
		if (roomIds.isEmpty()) {
			return new PageImpl<>(Collections.emptyList(), pageable, pageList.getTotalElements());
		}

		// 1. Redis 조회를 위한 Key Set 3개 생성
		Set<String> participantKeys = roomIds.stream()
			.map(id -> "chatroom:participants:" + id)
			.collect(Collectors.toSet());

		Set<String> contentKeys = roomIds.stream()
			.map(id -> "chatroom:last_msg_content:" + id)
			.collect(Collectors.toSet());

		Set<String> timestampKeys = roomIds.stream()
			.map(id -> "chatroom:last_msg_at:" + id)
			.collect(Collectors.toSet());

		// 2. RedisScanService 메소드 호출
		Map<String, Long> participantCountsMap = redisScanService.getParticipantCounts(
			participantKeys);        // (1) 참여자 수 조회 (SCARD)
		Map<String, String> lastContentsMap = redisScanService.multiGetLastMessageTimestamps(
			contentKeys);        // (2) 마지막 대화 내용 조회 (MGET)
		Map<String, String> lastTimestampsMap = redisScanService.multiGetLastMessageTimestamps(
			timestampKeys);    // (3) 마지막 대화 시각 조회 (MGET)

		// 3. DTO 변환 (PG 백업 데이터 + Redis 실시간 데이터 병합)
		List<ChatRoomResponse> dtoPage = pageList.stream()
			.map(dto -> {
				String roomId = dto.chatRoomId();

				// 참여자 수 병합
				Long realTimeCount = participantCountsMap.get("chatroom:participants:" + roomId);
				// Long finalCount = (realTimeCount != null) ? realTimeCount : dto.participantCount();	// 실시간 참여자 수는 로깅 목적으로만 남김
				Long finalCount = dto.participantCount();

				// 마지막 내용 병합
				String realTimeContent = lastContentsMap.get("chatroom:last_msg_content:" + roomId);
				String finalContent = realTimeContent;

				// 마지막 시각 병합
				String timestampStr = lastTimestampsMap.get("chatroom:last_msg_at:" + roomId);
				Long realTimeTimestamp = null;
				if (timestampStr != null) {
					// NumberFormatException 방지. 양 끝의 큰따옴표가 있다면 제거 (방어용)
					String cleanTimestampStr = timestampStr.replaceAll("^\"|\"$", "");
					try {
						realTimeTimestamp = Long.parseLong(cleanTimestampStr);
					} catch (NumberFormatException e) {
						// 파싱 실패 시 로그를 남기고 해당 값은 무시 (null 처리)
						log.error("Failed to parse timestamp for room {}. Input string: {}", roomId, timestampStr, e);
						realTimeTimestamp = null;
					}
				}

				Long finalTimestamp = dto.lastMessageAt();
				if (realTimeTimestamp != null && (finalTimestamp == null || realTimeTimestamp > finalTimestamp)) {
					finalTimestamp = realTimeTimestamp;
				}

				return ChatRoomResponse.fromDto(dto, finalContent, finalCount, finalTimestamp);
			})
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	/**
	 * DTO 리스트를 받아 Redis에서 마지막 대화 내용을 일괄 조회합니다. (MGET)
	 */
	@Deprecated
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

	/**
	 * 정렬 순서 가져오기 (복합 정렬 지원)
	 * @return Sort 객체
	 */
	private Sort getSort(OrderType orderType) {
		return switch (orderType) {
			// 참여자 많은 순: 참여자 수 내림차순 -> (동점시) 생성일 최신순
			case PART_DESC -> Sort.by(
				Sort.Order.desc("member_count"),
				Sort.Order.desc("created_at")
			);
			// 참여자 적은 순: 참여자 수 오름차순 -> (동점시) 생성일 최신순
			case PART_ASC -> Sort.by(
				Sort.Order.asc("member_count"),
				Sort.Order.desc("created_at")
			);
			// 생성 최신순
			case DATE_DESC -> Sort.by(Sort.Order.desc("created_at"));
			// 생성 오래된순
			case DATE_ASC -> Sort.by(Sort.Order.asc("created_at"));
			// 활성화 최신순
			case ACTIVE_DESC -> Sort.by(Sort.Order.desc("last_message_at"));
			// 활성화 오래된순
			case ACTIVE_ASC -> Sort.by(Sort.Order.asc("last_message_at"));
		};
	}

	// 정렬 순서 가져오기
	@Deprecated
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
