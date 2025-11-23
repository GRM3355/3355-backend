package com.grm3355.zonie.chatserver.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

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

	public ChatLocationService(StringRedisTemplate redisTemplate, ChatRoomRepository chatRoomRepository,
		ObjectMapper objectMapper, @Value("${location.token.ttl-minutes}") long ttlMinutes,
		FestivalRepository festivalRepository) {
		this.redisTemplate = redisTemplate;
		this.chatRoomRepository = chatRoomRepository;
		this.objectMapper = objectMapper;
		this.tokenTtl = Duration.ofMinutes(ttlMinutes);
		this.festivalRepository = festivalRepository;
	}

	// Redis에서 토큰의 존재 여부만 확인
	private void validateLocationToken(String userId, Long festivalId) {
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
		Long festivalId = getFestivalIdForRoom(roomId);    // roomId로 festivalId 조회
		validateLocationToken(userId, festivalId);        // 토큰 유효성 검사 (거리 재계산 x)
		log.debug("Location token validation success for user {}.", userId);
	}

	private String buildKey(String userId, String contextId) {
		return "locationToken:" + userId + ":" + contextId;
	}

	private String buildLocationJson(String userId, double lat, double lon) {
		return String.format(
			"{\"userId\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"timestamp\":%d}",
			userId, lat, lon, System.currentTimeMillis()
		);
	}

	// locationToken 발행
	private void generateLocationToken(String userId, String contextId, double lat, double lon) {
		String redisKey = buildKey(userId, contextId);

		try {
			String infoJson = buildLocationJson(userId, lat, lon);
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

		String newValue = buildLocationJson(userId, lat, lon);
		redisTemplate.opsForValue().set(key, newValue, this.tokenTtl);

		return true;
	}

	/**
	 * STOMP /join 시 호출: 위치 인증 토큰을 갱신합니다. (TTL 연장 목적)
	 * API 서버에서 최초 발급 시 반경 검증을 완료했으므로, Chat 서버는 DB/PostGIS 접근 없이 Redis 갱신만 수행합니다.
	 */
	public void setLocationTokenOnJoin(String userId, String roomId, double lat, double lon) {
		// 1. 축제 ID 획득 (DB 접근은 최소화)
		Long festivalId = getFestivalIdForRoom(roomId);
		String contextId = String.valueOf(festivalId);

		// 2. 토큰 갱신/발급
		// 2-1. 토큰이 유효하게 존재하는지 확인하고, 존재하면 위치 갱신 (TTL 갱신)
		boolean updated = updateLocationInfo(userId, contextId, lat, lon);

		if (!updated) {
			// 2-2. 토큰이 존재하지 않으면 새로 생성 (API 서버에서 발급했어야 하지만, 혹시 모를 경우 대비)
			generateLocationToken(userId, contextId, lat, lon);
			log.warn("Location token was missing in Redis. Chat server re-issued a new token for user {}.", userId);
		}

		log.info("Location token TTL/position updated on join for user {} in room {}.", userId, roomId);
	}
}
