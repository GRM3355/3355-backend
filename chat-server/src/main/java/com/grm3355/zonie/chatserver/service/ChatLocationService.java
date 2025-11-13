package com.grm3355.zonie.chatserver.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.chatserver.dto.ChatUserLocationDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatLocationService {

	private final StringRedisTemplate redisTemplate;
	private final FestivalRepository festivalRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ObjectMapper objectMapper; // DTO 역직렬화를 위해 추가
	private final Duration tokenTtl;
	@Value("${chat.radius}")
	private double max_radius;

	public ChatLocationService(StringRedisTemplate redisTemplate, ChatRoomRepository chatRoomRepository, ObjectMapper objectMapper, @Value("${location.token.ttl-minutes}") long ttlMinutes,
		FestivalRepository festivalRepository) {
		this.redisTemplate = redisTemplate;
		this.chatRoomRepository = chatRoomRepository;
		this.objectMapper = objectMapper;
		this.tokenTtl = Duration.ofMinutes(ttlMinutes);
		this.festivalRepository = festivalRepository;
	}

	// Redis에서 토큰의 존재 여부만 확인
	private void validateLocationToken(String userId, Long festivalId) {
		// api-server의 RedisTokenService.buildKey와 동일한 키 사용
		String redisKey = buildKey(userId, String.valueOf(festivalId));
		Boolean hasKey = redisTemplate.hasKey(redisKey);

		if (!hasKey) {
			log.warn("Cannot find location token in Redis for userId: {}, festivalId: {}", userId, festivalId);
			throw new BusinessException(ErrorCode.FORBIDDEN, "위치 인증 토큰이 없거나 만료되었습니다.");
		}
	}

	// DB: 채팅방 ID -> 축제 ID
	private Long getFestivalIdForRoom(String roomId) {
		ChatRoom chatRoom = chatRoomRepository.findByChatRoomId(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방 정보를 찾을 수 없습니다."));
		return chatRoom.getFestival().getFestivalId();
	}

	// 메인 검증 메소드
	public void validateChatRoomEntry(String userId, String roomId) {
		Long festivalId = getFestivalIdForRoom(roomId);	// roomId로 festivalId 조회
		validateLocationToken(userId, festivalId);		// 토큰 유효성 검사 (거리 재계산 x)
		log.debug("Location token validation success for user {}.", userId);
	}

	private String buildKey(String userId, String contextId) {
		return "locationToken:" + userId + ":" + contextId;
	}

	private String buildLocationJson(String userId, String clientIp, String device, double lat, double lon) {
		return String.format(
			"{\"userId\":\"%s\",\"clientIp\":\"%s\",\"device\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"timestamp\":%d}",
			userId, clientIp, device, lat, lon, System.currentTimeMillis()
		);
	}

	private String extractValue(String json, String field) {
		String search = "\"" + field + "\":\"";
		int start = json.indexOf(search);
		if (start == -1)
			return null;
		start += search.length();
		int end = json.indexOf("\"", start);
		return json.substring(start, end);
	}

	// Redis에서 locationToken 정보 조회
	private ChatUserLocationDto getLocationInfo(String userId, String contextId) {
		String redisKey = buildKey(userId, contextId);
		String saved = redisTemplate.opsForValue().get(redisKey);
		if (saved == null)
			return null;

		try {
			// ChatUserLocationDto는 apiserver의 UserTokenDto와 구조가 유사하나,
			// chatserver의 DTO를 사용한다고 가정합니다.
			return objectMapper.readValue(saved, ChatUserLocationDto.class);
		} catch (JsonProcessingException e) {
			log.error("Error reading location info from Redis for user {}: {}", userId, e.getMessage());
			return null;
		}
	}

	// locationToken 발행 (반경 무시)
	private void generateLocationToken(String userId, String contextId, double lat, double lon) {
		String redisKey = buildKey(userId, contextId);

		try {
			// clientIp와 device는 빈 문자열로 저장하여 JSON 구조 유지
			String infoJson = buildLocationJson(userId, "", "", lat, lon);
			redisTemplate.opsForValue().set(redisKey, infoJson, this.tokenTtl); // TTL 적용

			log.debug("Generated location token for user {} in context {}.", userId, contextId);
		} catch (Exception e) {
			throw new RuntimeException("Redis 저장 중 오류 발생", e);
		}
	}

	// 위치 + 디바이스 정보 업데이트 (TTL 갱신 포함)
	private boolean updateLocationInfo(String userId, String contextId, double lat, double lon) {
		String key = buildKey(userId, contextId);
		String oldValue = redisTemplate.opsForValue().get(key);

		if (oldValue == null) {
			return false; // 토큰이 없으면 갱신 대신 생성
		}

		// 기존 정보 유지 + 좌표만 갱신
		String clientIp = extractValue(oldValue, "clientIp");
		String device = extractValue(oldValue, "device");

		// null 체크하여 JSON 포맷 에러 방지
		String finalClientIp = (clientIp == null) ? "" : clientIp;
		String finalDevice = (device == null) ? "" : device;

		String newValue = buildLocationJson(userId, finalClientIp, finalDevice, lat, lon);
		redisTemplate.opsForValue().set(key, newValue, this.tokenTtl);

		return true;
	}

	/**
	 * STOMP /join 시 호출: 위치 인증 토큰을 설정하거나 갱신합니다. (반경 검사 수행)
	 */
	public void setLocationTokenOnJoin(String userId, String roomId, double lat, double lon) {
		Long festivalId = getFestivalIdForRoom(roomId);
		String contextId = String.valueOf(festivalId);

		// PostGIS를 사용하여 거리를 계산합니다.
		double distanceKm = festivalRepository.findDistanceToFestival(festivalId, lon, lat)
			.orElse(Double.MAX_VALUE); // 축제 정보 없으면 MAX_VALUE로 처리하여 반경 밖으로 간주

		if (distanceKm > max_radius) {
			// 1. 반경을 벗어난 경우: 토큰 발급/갱신을 하지 않고, 예외 없이 함수를 종료합니다.
			log.warn("User {} is outside the radius ({}km) for room {}. Location token is NOT issued/updated. (Joining allowed)",
				userId, distanceKm, roomId);
			return; // 토큰 발급 로직만 건너뛰고 정상 종료
		}

		// 2. 반경 내에 있을 경우: 토큰 갱신 또는 발급을 진행합니다.
		// 2-1. 토큰이 유효하게 존재하는지 확인하고, 존재하면 위치 갱신 (TTL 갱신)
		boolean updated = updateLocationInfo(userId, contextId, lat, lon);
		if (!updated) {
			// 2-2. 토큰이 존재하지 않으면 새로 생성
			generateLocationToken(userId, contextId, lat, lon);
		}

		log.info("Location token set/updated on join for user {} in room {}. Within radius check passed.", userId, roomId);
	}

	/**
	 * 사용자가 축제 반경 내에 있는지 PostGIS를 이용해 검증합니다.
	 * (API Server의 FestivalInfoService 로직과 동일)
	 */
	private void checkUserWithinRadius(Long festivalId, double lat, double lon) {
		double distanceKm = festivalRepository.findDistanceToFestival(festivalId, lon, lat)
			.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "축제 위치 정보를 찾을 수 없습니다."));

		if (distanceKm > max_radius) {
			// 반경을 벗어났다면 예외 발생
			throw new BusinessException(ErrorCode.BAD_REQUEST,
				"채팅방 입장은 반경(" + max_radius + "km) 내에서만 가능합니다.");
		}
	}
}