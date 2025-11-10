package com.grm3355.zonie.batchserver.job;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomRedisCleanupJob {

	private static final String PARTICIPANTS_KEY_PATTERN = "chatroom:participants:*";
	private static final String PARTICIPANTS_KEY_PREFIX = "chatroom:participants:";

	private final RedisScanService redisScanService;
	private final ChatRoomRepository chatRoomRepository; // PGDB Repository

	/**
	 * 매일 새벽 5시에 실행 (cron = "0 0 5 * * ?")
	 * PG DB에 존재하지 않는 (삭제된) 채팅방의 참여자 수 Redis 키를 정리합니다.
	 */
	@Scheduled(cron = "0 0 5 * * ?")
	public void cleanupStaleChatRoomKeys() {
		log.info("ChatRoomRedisCleanupJob 시작: 오래된 '참여자 수' Redis 키를 정리합니다.");

		// 1. Redis에서 모든 '참여자 수' 키 탐색
		Set<String> allParticipantKeys = redisScanService.scanKeys(PARTICIPANTS_KEY_PATTERN);

		if (allParticipantKeys.isEmpty()) {
			log.info("정리할 '참여자 수' 키가 없습니다. Job 종료.");
			return;
		}

		// 2. 키에서 Room ID 목록 추출
		Set<String> allRoomIdsFromRedis = allParticipantKeys.stream()
			.map(key -> key.substring(PARTICIPANTS_KEY_PREFIX.length()))
			.collect(Collectors.toSet());

		if (allRoomIdsFromRedis.isEmpty()) {
			log.info("키는 존재했으나, 유효한 RoomId가 없습니다. Job 종료.");
			return;
		}

		// 3. PostgreSQL DB에서 해당 Room ID가 실제로 존재하는지 확인
		List<ChatRoom> existingRooms = chatRoomRepository.findAllByChatRoomIdIn(allRoomIdsFromRedis);

		Set<String> existingRoomIds = existingRooms.stream()
			.map(ChatRoom::getChatRoomId)
			.collect(Collectors.toSet());

		// 4. 삭제할 키 선별 (Redis에는 있으나, PG에는 없는 ID)
		Set<String> staleRoomIds = allRoomIdsFromRedis.stream()
			.filter(redisId -> !existingRoomIds.contains(redisId))
			.collect(Collectors.toSet());

		if (staleRoomIds.isEmpty()) {
			log.info("DB와 일치하지 않는 오래된 '참여자 수' 키가 없습니다. Job 종료.");
			return;
		}

		// 5. 삭제할 Redis 키 목록 생성 (e.g., "chatroom:participants:stale-id-1")
		Set<String> keysToDelete = staleRoomIds.stream()
			.map(id -> PARTICIPANTS_KEY_PREFIX + id)
			.collect(Collectors.toSet());

		// 6. Redis 키 일괄 삭제
		try {
			if (!keysToDelete.isEmpty()) {
				redisScanService.deleteKeys(keysToDelete);
				log.info("ChatRoomRedisCleanupJob 완료: 총 {}개의 오래된 '참여자 수' 키 삭제.", keysToDelete.size());
			}
		} catch (Exception e) {
			log.error("오래된 '참여자 수' Redis 키 정리 중 오류 발생.", e);
		}

		log.info("ChatRoomRedisCleanupJob 완료.");
	}
}
