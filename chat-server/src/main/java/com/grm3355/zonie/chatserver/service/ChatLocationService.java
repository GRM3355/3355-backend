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

	// Redis에서 토큰의 존재 여부만 확인
	private void validateLocationToken(String userId, Long festivalId) {
		// api-server의 RedisTokenService.buildKey와 동일한 키 사용
		String redisKey = "locationToken:" + userId + ":" + String.valueOf(festivalId);
		Boolean hasKey = redisTemplate.hasKey(redisKey);

		if (!hasKey) {
			log.warn("Cannot find location token in Redis for userId: {}, festivalId: {}", userId, festivalId);
			throw new BusinessException(ErrorCode.FORBIDDEN, "위치 인증 토큰이 없거나 만료되었습니다.");
		}
	}

	// DB: 채팅방 ID -> 축제 ID
	private Long getFestivalIdForRoom(String roomId) {
		ChatRoom chatRoom = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방 정보를 찾을 수 없습니다."));
		return chatRoom.getFestival().getFestivalId();
	}

	// 메인 검증 메소드
	public void validateChatRoomEntry(String userId, String roomId) {
		Long festivalId = getFestivalIdForRoom(roomId);	// roomId로 festivalId 조회
		validateLocationToken(userId, festivalId);		// 토큰 유효성 검사 (거리 재계산 x)
		log.debug("Location token validation success for user {}.", userId);
	}
}