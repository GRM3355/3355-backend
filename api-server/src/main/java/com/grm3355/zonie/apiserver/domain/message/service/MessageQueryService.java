package com.grm3355.zonie.apiserver.domain.message.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.message.dto.MessageResponse;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageQueryService {

	private static final String LIKED_BY_KEY_PREFIX = "message:liked_by:";
	private static final String LIKE_COUNT_KEY_PREFIX = "message:like_count:";
	private static final int DEFAULT_PAGE_SIZE = 20;
	private final MessageRepository messageRepository; // Mongo
	private final StringRedisTemplate stringRedisTemplate;
	private final RedisScanService redisScanService;

	/**
	 * 채팅방의 과거 메시지 목록을 커서 기반 페이지네이션으로 조회합니다.
	 * 실시간 '좋아요' 데이터를 Redis에서 병합합니다.
	 */
	public Slice<MessageResponse> getMessages(String chatRoomId, String currentUserId, String cursorId) {

		Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

		// 1. MongoDB에서 메시지 조회
		Slice<Message> messagesSlice;
		if (cursorId == null || cursorId.isEmpty()) {
			// (P1) 최초 요청
			messagesSlice = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, pageable);
		} else {
			// (P2+) 커서 기반 다음 페이지 요청
			// 1) 커서 ID로 원본 메시지를 찾아 타임스탬프를 가져옵니다.
			Message cursorMessage = messageRepository.findById(cursorId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "페이징 커서 메시지를 찾을 수 없습니다."));

			LocalDateTime cursorTimestamp = cursorMessage.getCreatedAt();

			// 2) 해당 타임스탬프보다 오래된(LessThan) 메시지를 조회합니다.
			messagesSlice = messageRepository.findByChatRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(chatRoomId,
				cursorTimestamp, pageable);
		}

		List<Message> messages = messagesSlice.getContent();
		if (messages.isEmpty()) {
			return messagesSlice.map(msg -> null); // Empty Slice
		}

		// 2. 실시간 '좋아요' 데이터 Redis에서 일괄 조회
		Set<String> messageIds = messages.stream().map(Message::getId).collect(Collectors.toSet());

		// 2-1. messageId -> likeCount (Map)
		Set<String> likeCountKeys = messageIds.stream()
			.map(id -> LIKE_COUNT_KEY_PREFIX + id)
			.collect(Collectors.toSet());
		Map<String, String> likeCountStrMap = redisScanService.multiGetLastMessageTimestamps(
			likeCountKeys); // MGET (Key/Value)

		// 2-2. messageId -> likedByUserIds (Map)
		Set<String> likedByKeys = messageIds.stream()
			.map(id -> LIKED_BY_KEY_PREFIX + id)
			.collect(Collectors.toSet());
		Map<String, Set<String>> likedByMap = redisScanService.getSetMembers(likedByKeys); // SMEMBERS (Set)

		// 3. DTO로 변환 (Mongo 데이터 + Redis 데이터 병합)
		return messagesSlice.map(msg -> {
			String msgId = msg.getId();

			// 3-1. Redis에서 두 키 모두 조회
			String countStr = likeCountStrMap.get(LIKE_COUNT_KEY_PREFIX + msgId);
			Set<String> likedUserIds = likedByMap.get(LIKED_BY_KEY_PREFIX + msgId);

			int finalLikeCount;
			Set<String> finalLikedUserIds;

			// 3-2. "All or Nothing": 두 키가 모두 Redis에 존재할 때만 Redis 값을 사용
			if (countStr != null && likedUserIds != null) {
				// (A) Redis 데이터 사용
				finalLikeCount = Integer.parseInt(countStr);
				finalLikedUserIds = likedUserIds;
			} else {
				// (B) 하나라도 없으면, 둘 다 Mongo 백업본 사용 (데이터 정합성 유지)
				finalLikeCount = msg.getLikeCount();
				finalLikedUserIds = msg.getLikedByUserIds();
			}

			return MessageResponse.from(msg, finalLikeCount, finalLikedUserIds, currentUserId);
		});
	}
}
