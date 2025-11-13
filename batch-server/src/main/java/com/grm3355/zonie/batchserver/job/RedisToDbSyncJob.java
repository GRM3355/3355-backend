package com.grm3355.zonie.batchserver.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomSyncDto;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomSyncRepository;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

@Slf4j
@Component
@RequiredArgsConstructor // final 필드 생성자 자동 주입
public class RedisToDbSyncJob {

	private static final String PARTICIPANTS_KEY_PATTERN = "chatroom:participants:*";
	private static final String LAST_MSG_AT_KEY_PATTERN = "chatroom:last_msg_at:*";

	private final RedisScanService redisScanService;
	private final ChatRoomSyncRepository chatRoomSyncRepository;

	/**
	 * 1분마다 실행되는 Redis to DB 동기화 Job
	 */
	@Scheduled(fixedRate = 60000) // fixedRate = 60000ms = 1분
	public void syncRedisToDb() {
		log.info("RedisToDbSyncJob 시작: Redis 데이터를 PostgreSQL로 동기화합니다.");

		// 1. Redis 키 탐색: RedisScanService (SCAN)
		Set<String> participantKeys = redisScanService.scanKeys(PARTICIPANTS_KEY_PATTERN); 	// (1) 실시간 참여자 수 키 스캔
		Set<String> lastMsgAtKeys = redisScanService.scanKeys(LAST_MSG_AT_KEY_PATTERN);		// (2) 마지막 대화 시각 키 스캔

		if (participantKeys.isEmpty() && lastMsgAtKeys.isEmpty()) {
			log.info("동기화할 데이터가 없습니다. Job을 종료합니다.");
			return;
		}
		log.info("키 탐색 완료: 참여자 수 키 {}개, 마지막 대화 시각 키 {}개", participantKeys.size(), lastMsgAtKeys.size());

		// 2. dto 변환 (SCARD, MGET 파이프라인 호출 후 변환)
		Map<String, Long> participantCounts = redisScanService.getParticipantCounts(participantKeys);
		Map<String, String> lastMessageTimestamps = redisScanService.multiGetLastMessageTimestamps(lastMsgAtKeys);
		List<ChatRoomSyncDto> syncDataList = mergeSyncData(participantCounts, lastMessageTimestamps); // DTO 리스트로 변환

		if (syncDataList.isEmpty()) {
			log.warn("키는 탐색되었으나 유효한 데이터를 DTO로 변환하지 못했습니다. Job을 종료합니다.");
			return;
		}
		log.info("데이터 가공 완료: 총 {}개의 채팅방 데이터 DTO 변환 성공", syncDataList.size());

		// 3. PostgreSQL Bulk Update 실행
		try {
			ChatRoomSyncRepository.SyncDataWrapper wrapper = new ChatRoomSyncRepository.SyncDataWrapper(syncDataList);	// Native Query용 래퍼
			String roomIdsArray = listToPgArray(wrapper.getRoomIds());
			String countsArray = listToPgArray(wrapper.getCounts());
			String timestampsArray = listToPgArray(wrapper.getTimestamps());

			chatRoomSyncRepository.bulkUpdateChatRooms(
				roomIdsArray,
				countsArray,
				timestampsArray
			);
			log.info("PostgreSQL Bulk Update 완료: {}건 처리", syncDataList.size());
		} catch (Exception e) {
			log.error("PostgreSQL Bulk Update 중 심각한 오류 발생. Redis 데이터는 삭제되지 않습니다.", e);
			throw e; // 스케줄러가 오류를 인지하도록 예외를 다시 던짐
		}

		// 4. Redis 캐시 정리 (3번 DB 저장 성공 시 실행)
		try {
			// participantKeys와 lastMsgAtKeys를 합쳐서 한 번에 삭제
			// Set<String> allKeysToDelete = Stream.concat(
			// 	participantKeys.stream(),
			// 	lastMsgAtKeys.stream()
			// ).collect(Collectors.toSet());
			// lastMsgAtKeys만 삭제
			Set<String> allKeysToDelete = lastMsgAtKeys;

			if (!allKeysToDelete.isEmpty()) {
				redisScanService.deleteKeys(allKeysToDelete);
				log.info("Redis 캐시 정리 완료: 총 {}개의 키 삭제", allKeysToDelete.size());
			}
		} catch (Exception e) {
			// DB 저장은 성공했으나 Redis 삭제에 실패한 경우
			// - 다음 1분 주기에 데이터가 중복 처리될 수 있지만
			//   - participant_count는 덮어쓰기되고,
			//   - last_message_at은 CASE 문으로 방어됨: ChatRoomSyncRepository 쿼리문 (타임스탬프 비교해 최신값일 때만 덮어씀)
			log.error("DB 동기화는 성공했으나 Redis 키 삭제 중 오류 발생. 로그만 남깁니다.", e);
		}
		log.info("RedisToDbSyncJob 완료");
	}

	/**
	 * 두 종류의 Redis 데이터(참여자 수, 마지막 대화 시각)를 roomId를 기준으로 병합하여 DTO 리스트로 변환합니다.
	 */
	private List<ChatRoomSyncDto> mergeSyncData(Map<String, Long> participantCounts, Map<String, String> lastMessageTimestamps) {

		// 1. (Key, Value) -> (RoomId, Dto)로 변환
		Map<String, ChatRoomSyncDto> participantDtoMap = participantCounts.entrySet().stream()
			.collect(Collectors.toMap(
				entry -> parseRoomId(entry.getKey()), // "chatroom:participants:123" -> 123
				entry -> ChatRoomSyncDto.withParticipantCount(parseRoomId(entry.getKey()), entry.getValue())
			));

		Map<String, ChatRoomSyncDto> timestampDtoMap = lastMessageTimestamps.entrySet().stream()
			.collect(Collectors.toMap(
				entry -> parseRoomId(entry.getKey()), // "chatroom:last_msg_at:123" -> 123
				entry -> {
					String timestampStr = entry.getValue();
					if (timestampStr.startsWith("\"") && timestampStr.endsWith("\"")) { //  양 끝의 큰따옴표를 제거
						timestampStr = timestampStr.substring(1, timestampStr.length() - 1);
					}
					return ChatRoomSyncDto.withLastMessageTimestamp(
						parseRoomId(entry.getKey()),
						Long.parseLong(timestampStr) // 수정된 문자열로 파싱 시도
					);
				}
			));

		// 2. 두 개의 Map을 roomId 기준으로 병합
		return Stream.concat(participantDtoMap.entrySet().stream(), timestampDtoMap.entrySet().stream())
			.collect(Collectors.toMap(
				Map.Entry::getKey,   // roomId
				Map.Entry::getValue, // ChatRoomSyncDto
				// 3. Key(roomId)가 중복될 경우 (참여자 수, 타임스탬프 둘 다 있는 경우) 두 DTO를 하나로 합칩니다.
				(dto1, dto2) -> new ChatRoomSyncDto(
					dto1.roomId(),
					dto1.participantCount() != null ? dto1.participantCount() : dto2.participantCount(),
					dto1.lastMessageTimestamp() != null ? dto1.lastMessageTimestamp() : dto2.lastMessageTimestamp()
				)
			))
			.values() // Map<String, ChatRoomSyncDto> -> Collection<ChatRoomSyncDto>
			.stream()
			.toList(); // Collection -> List<ChatRoomSyncDto>
	}

	/**
	 * Redis 키에서 채팅방 ID를 파싱합니다.
	 * (예: "chatroom:participants:123" -> 123)
	 */
	private String parseRoomId(String key) {
		// "chatroom:participants:"
		if (key.startsWith(PARTICIPANTS_KEY_PATTERN.replace("*", ""))) {
			return key.substring(PARTICIPANTS_KEY_PATTERN.length() - 1);
		}
		// "chatroom:last_msg_at:"
		if (key.startsWith(LAST_MSG_AT_KEY_PATTERN.replace("*", ""))) {
			return key.substring(LAST_MSG_AT_KEY_PATTERN.length() - 1);
		}
		log.warn("알 수 없는 키 패턴에서 RoomId 파싱 실패: {}", key);
		return null; // merge 로직에서 필터링됨
	}

	/**
	 * List를 PostgreSQL 배열 문자열 (예: '{item1, item2}')로 변환하는 유틸리티 메서드
	 */
	private <T> String listToPgArray(List<T> list) {
		if (list == null || list.isEmpty()) {
			return "{}"; // 빈 배열
		}
		String content = list.stream()
			.map(item -> {
				// null일 경우 "NULL" 문자열을 반환
				if (item == null) {
					return "NULL";
				}

				// String 타입만 따옴표를 추가
				if (item instanceof String) {
					return "\"" + item + "\"";
				}

				// Long/Integer는 그대로 사용
				return item.toString();
			})
			.collect(Collectors.joining(","));

		return "{" + content + "}";
	}
}