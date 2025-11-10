package com.grm3355.zonie.apiserver.domain.message.service;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위치 검증: '좋아요' 기능도 위치 검증 필요함
 * '좋아요' 토글 (Redis): 취소/누르기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLikeService {

	private final MessageRepository messageRepository; 			// Mongo
	private final ChatRoomRepository chatRoomRepository; 		// JPA
	private final RedisTokenService redisTokenService; 			// Redis (사용자 위치)
	private final StringRedisTemplate stringRedisTemplate; 		// Redis (좋아요)

	private static final String LIKED_BY_KEY_PREFIX = "message:liked_by:"; 		// Set
	private static final String LIKE_COUNT_KEY_PREFIX = "message:like_count:"; 	// Counter

	/**
	 * 메시지 '좋아요' 토글 (추가 또는 취소)
	 * @return Map<String, Object> - e.g., {"liked": true, "likeCount": 5}
	 */
	public Map<String, Object> toggleLike(String userId, String messageId) {

		// 1. 위치 검증 (요구사항)
		validateLocation(userId, messageId);

		// 2. Redis 키 정의
		String likedByKey = LIKED_BY_KEY_PREFIX + messageId;
		String likeCountKey = LIKE_COUNT_KEY_PREFIX + messageId;

		// 3. '좋아요' 토글 로직
		boolean isAlreadyLiked = Boolean.TRUE.equals(
			stringRedisTemplate.opsForSet().isMember(likedByKey, userId)
		);

		Long newLikeCount;

		if (isAlreadyLiked) {
			// "좋아요" 취소
			stringRedisTemplate.opsForSet().remove(likedByKey, userId);					// Set에서 userId 제거
			newLikeCount = stringRedisTemplate.opsForValue().decrement(likeCountKey);	// 카운터 증가
		} else {
			// "좋아요" 누르기
			stringRedisTemplate.opsForSet().add(likedByKey, userId);
			newLikeCount = stringRedisTemplate.opsForValue().increment(likeCountKey);
		}

		// 4. Batch Job이 MongoDB에 동기화할 때까지 Redis 카운터가 음수가 되지 않도록 보정
		if (newLikeCount == null || newLikeCount < 0) {
			newLikeCount = 0L;
			stringRedisTemplate.opsForValue().set(likeCountKey, "0");
		}

		log.info("Message like toggled: User={}, Message={}, Liked={}, Count={}",
			userId, messageId, !isAlreadyLiked, newLikeCount);

		return Map.of(
			"liked", !isAlreadyLiked,
			"likeCount", newLikeCount
		);
	}

	/**
	 * '좋아요'를 누르기 전, 사용자가 해당 채팅방 반경 내에 있는지 검증
	 */
	private void validateLocation(String userId, String messageId) {

		// 1. 사용자 현재 위치 (Redis)
		UserTokenDto userLocationDto = redisTokenService.getLocationInfo(userId);
		if (userLocationDto == null) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "위치 인증이 만료되었거나 없습니다. 위치 갱신이 필요합니다.");
		}
		LocationDto userLocation = new LocationDto(userLocationDto.getLat(), userLocationDto.getLon());

		// 2. 메시지가 속한 채팅방 위치 (Mongo -> JPA)
		log.info("Attempting to find message with ID: {}", messageId);
		Message message = messageRepository.findById(messageId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메시지를 찾을 수 없습니다."));
		ChatRoom chatRoom = chatRoomRepository.findById(message.getChatRoomId())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다."));
		log.info("Message found successfully. ChatRoomId: {}", message.getChatRoomId());

		LocationDto chatRoomLocation = new LocationDto(
			chatRoom.getPosition().getY(), // lat
			chatRoom.getPosition().getX()  // lon
		);

		// 3. 거리 계산
		double distance = LocationService.getDistanceCalculator(userLocation, chatRoomLocation);
		double allowedRadius = chatRoom.getRadius(); // km

		if (distance > allowedRadius) {
			log.warn("Like location validation failed for user {}. Distance: {}km, Allowed: {}km",
				userId, distance, allowedRadius);
			throw new BusinessException(ErrorCode.FORBIDDEN,
				String.format("채팅방 반경 %.1fkm 이내에서만 '좋아요'를 누를 수 있습니다.", allowedRadius));
		}
	}
}