package com.grm3355.zonie.batchserver.job;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageLikeSyncJob {

	private static final String LIKED_BY_KEY_PATTERN = "message:liked_by:*";
	private static final String LIKE_COUNT_KEY_PATTERN = "message:like_count:*";
	private static final String LIKED_BY_PREFIX = "message:liked_by:";
	private static final String LIKE_COUNT_PREFIX = "message:like_count:";

	private final RedisScanService redisScanService;
	private final MessageRepository messageRepository; // MongoDB Repository

	@Scheduled(fixedRate = 60000) // 1분마다 실행
	@Transactional
	public void syncMessageLikes() {
		log.info("MessageLikeSyncJob 시작: '좋아요' 데이터를 MongoDB로 동기화합니다.");

		// 1. Redis 키 탐색
		Set<String> likedByKeys = redisScanService.scanKeys(LIKED_BY_KEY_PATTERN);
		Set<String> likeCountKeys = redisScanService.scanKeys(LIKE_COUNT_KEY_PATTERN);

		if (likedByKeys.isEmpty() && likeCountKeys.isEmpty()) {
			log.info("동기화할 '좋아요' 데이터가 없습니다. Job 종료.");
			return;
		}

		// 2. Redis 데이터 일괄 조회
		// (1) messageId -> 좋아요 누른 유저 Set
		Map<String, Set<String>> likedByMap = redisScanService.getSetMembers(likedByKeys).entrySet().stream()
			.collect(Collectors.toMap(
				entry -> entry.getKey().substring(LIKED_BY_PREFIX.length()), // key -> messageId
				Map.Entry::getValue
			));

		// (2) messageId -> 좋아요 개수(String)
		Map<String, String> likeCountStrMap = redisScanService.multiGetLastMessageTimestamps(likeCountKeys).entrySet().stream()
			.collect(Collectors.toMap(
				entry -> entry.getKey().substring(LIKE_COUNT_PREFIX.length()), // key -> messageId
				Map.Entry::getValue
			));

		// 3. messageId 기준으로 데이터 병합
		Set<String> allMessageIds = Stream.concat(likedByMap.keySet().stream(), likeCountStrMap.keySet().stream())
			.collect(Collectors.toSet());

		if (allMessageIds.isEmpty()) {
			log.info("키는 존재했으나, 유효한 messageId가 없습니다. Job 종료.");
			return;
		}

		// 4. MongoDB에서 원본 메시지 조회
		List<Message> messagesToUpdate = messageRepository.findAllById(allMessageIds);

		// 5. Redis 데이터로 Mongo 엔티티 업데이트
		for (Message msg : messagesToUpdate) {
			String messageId = msg.getId();

			// 5-1. likedByUserIds (Set) 업데이트
			Set<String> likedUserIds = likedByMap.get(messageId);
			if (likedUserIds != null) {
				msg.setLikedByUserIds(likedUserIds);
			}

			// 5-2. likeCount (Integer) 업데이트
			String countStr = likeCountStrMap.get(messageId);
			if (countStr != null) {
				try {
					msg.setLikeCount(Integer.parseInt(countStr));
				} catch (NumberFormatException e) {
					log.warn("MessageLikeSyncJob: like_count 파싱 실패 (messageId: {}), 0으로 설정.", messageId);
					msg.setLikeCount(0);
				}
			}
		}

		// 6. MongoDB에 일괄 저장
		if (!messagesToUpdate.isEmpty()) {
			messageRepository.saveAll(messagesToUpdate);
			log.info("MongoDB '좋아요' 동기화 완료: {}건 처리", messagesToUpdate.size());
		}

		// 7. Redis 키 삭제
		try {
			Set<String> allKeysToDelete = Stream.concat(
				likedByKeys.stream(),
				likeCountKeys.stream()
			).collect(Collectors.toSet());

			if (!allKeysToDelete.isEmpty()) {
				redisScanService.deleteKeys(allKeysToDelete);
				log.info("Redis '좋아요' 캐시 정리 완료: 총 {}개의 키 삭제", allKeysToDelete.size());
			}
		} catch (Exception e) {
			log.error("MongoDB 동기화는 성공했으나 Redis 키 삭제 중 오류 발생.", e);
		}

		log.info("MessageLikeSyncJob 완료.");
	}
}