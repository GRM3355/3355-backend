package com.grm3355.zonie.chatserver.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.chatserver.dto.ChatUserLocationDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLocationService {

	private final StringRedisTemplate redisTemplate;
	private final ChatRoomRepository chatRoomRepository;
	private final ObjectMapper objectMapper;

	// 1. Redis에서 사용자 위치(DTO) 가져오기
	private ChatUserLocationDto getUserLocation(String userId) {
		String redisKey = "locationToken:" + userId;
		String savedJson = redisTemplate.opsForValue().get(redisKey);

		if (savedJson == null) {
			log.warn("Cannot find location token in Redis for userId: {}", userId);
			throw new BusinessException(ErrorCode.FORBIDDEN, "위치 인증 토큰이 없습니다. 채팅방 목록에서 위치 갱신을 해주세요.");
		}

		try {
			return objectMapper.readValue(savedJson, ChatUserLocationDto.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize location token from Redis", e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "위치 정보 처리 중 오류 발생");
		}
	}

	// 2. DB에서 채팅방 위치(Entity) 가져오기
	private ChatRoom getChatRoomLocation(String roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방 정보를 찾을 수 없습니다."));
	}

	// 3. 거리 계산
	private double getDistanceCalculator(double lat1, double lon1, double lat2, double lon2) {
		double R = 6371; // 지구 반지름 (km)
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c; // 단위: km
		return Math.round(distance * 100.0) / 100.0;
	}

	// 4. 메인 검증 메서드
	public void validateChatRoomEntry(String userId, String roomId) {
		// 1, 2 단계 실행
		ChatUserLocationDto userLocation = getUserLocation(userId);
		ChatRoom chatRoom = getChatRoomLocation(roomId);

		// 3단계 실행
		double distance = getDistanceCalculator(
			userLocation.getLat(),
			userLocation.getLon(),
			chatRoom.getPosition().getY(), // Lat
			chatRoom.getPosition().getX()  // Lon
		);

		// 4단계: 반경 1km를 초과하는지 검사 (하드코딩***)
		// TODO: chatRoom.getRadius()를 사용
		double allowedRadiusKm = 1.0;

		if (distance > allowedRadiusKm) {
			log.warn("Location validation failed for user {}. Distance: {}km", userId, distance);
			throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 반경 " + allowedRadiusKm + "km 이내에서만 채팅을 보낼 수 있습니다.");
		}

		log.debug("Location validation success for user {}. Distance: {}km", userId, distance);
	}
}