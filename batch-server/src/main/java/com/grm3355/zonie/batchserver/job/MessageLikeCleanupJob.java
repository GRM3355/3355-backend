package com.grm3355.zonie.batchserver.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageLikeCleanupJob {

	// MessageLikeSyncJob과 동일한 키 정의 사용
	private static final String LIKED_BY_KEY_PATTERN = "message:liked_by:*";
	private static final String LIKE_COUNT_KEY_PATTERN = "message:like_count:*";
	private static final String LIKED_BY_PREFIX = "message:liked_by:";
	private static final String LIKE_COUNT_PREFIX = "message:like_count:";

	/**
	 * Redis '좋아요' 키의 보관 주기 (일)
	 * 예: 30일이 지난 메시지의 '좋아요' 키는 Redis에서 삭제합니다.
	 */
	private static final int DAYS_TO_KEEP_LIKES_IN_REDIS = 30;

	private final RedisScanService redisScanService;
	private final MessageRepository messageRepository; // MongoDB Repository

	/**
	 * 매일 새벽 3시에 실행 (cron = "0 0 3 * * ?")
	 * 오래된 메시지의 '좋아요' 관련 Redis 키를 정리합니다.
	 */
	@Scheduled(cron = "0 0 4 * * ?")
	public void cleanupOldMessageLikes() {
		log.info("MessageLikeCleanupJob 시작: 오래된 '좋아요' 데이터를 Redis에서 삭제합니다.");

		// 1. Redis 키 탐색
		Set<String> likedByKeys = redisScanService.scanKeys(LIKED_BY_KEY_PATTERN);
		Set<String> likeCountKeys = redisScanService.scanKeys(LIKE_COUNT_KEY_PATTERN);

		if (likedByKeys.isEmpty() && likeCountKeys.isEmpty()) {
			log.info("정리할 '좋아요' 키가 없습니다. Job 종료.");
			return;
		}

		// 2. 키에서 Message ID 목록을 추출
		Set<String> idsFromLikedBy = likedByKeys.stream()
			.map(key -> key.substring(LIKED_BY_PREFIX.length()))
			.collect(Collectors.toSet());

		Set<String> idsFromLikeCount = likeCountKeys.stream()
			.map(key -> key.substring(LIKE_COUNT_PREFIX.length()))
			.collect(Collectors.toSet());

		Set<String> allMessageIds = Stream.concat(idsFromLikedBy.stream(), idsFromLikeCount.stream())
			.collect(Collectors.toSet());

		if (allMessageIds.isEmpty()) {
			log.info("키는 존재했으나, 유효한 messageId가 없습니다. Job 종료.");
			return;
		}

		// 3. MongoDB에서 해당 메시지들의 생성 시간을 조회
		List<Message> messages = messageRepository.findAllById(allMessageIds);

		// 4. 삭제할 메시지 ID 선별 (오래된 메시지)
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DAYS_TO_KEEP_LIKES_IN_REDIS);

		Set<String> idsToDelete = messages.stream()
			.filter(msg -> msg.getCreatedAt() != null && msg.getCreatedAt().isBefore(cutoffDate))
			.map(Message::getId)
			.collect(Collectors.toSet());

		if (idsToDelete.isEmpty()) {
			log.info("삭제할 만큼 오래된(30일 이상) '좋아요' 데이터가 없습니다. Job 종료.");
			return;
		}

		// 5. 삭제할 Redis 키 목록 생성
		Set<String> keysToDelete = idsToDelete.stream()
			.flatMap(id -> Stream.of(LIKE_COUNT_PREFIX + id, LIKED_BY_PREFIX + id))
			.collect(Collectors.toSet());

		// 6. Redis 키 일괄 삭제
		try {
			if (!keysToDelete.isEmpty()) {
				redisScanService.deleteKeys(keysToDelete);
				log.info("Redis '좋아요' 캐시 정리 완료: 총 {}개의 키 삭제 (오래된 메시지 {}개 분량)", keysToDelete.size(), idsToDelete.size());
			}
		} catch (Exception e) {
			log.error("Redis '좋아요' 캐시 정리 중 오류 발생.", e);
		}

		log.info("MessageLikeCleanupJob 완료.");
	}
}
