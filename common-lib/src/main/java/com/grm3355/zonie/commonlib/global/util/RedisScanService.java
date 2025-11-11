package com.grm3355.zonie.commonlib.global.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service("chatServerRedisScanService")
public class RedisScanService {

	private final StringRedisTemplate stringRedisTemplate;

	public RedisScanService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/**
	 * 패턴에 일치하는 모든 키를 조회합니다. (SCAN: Non-Blocking: 끊어 읽기, 커서 기반)
	 *
	 * @param pattern 조회할 키 패턴 (예: "chatroom:participants:*")
	 * @return 일치하는 모든 키의 Set
	 */
	public Set<String> scanKeys(String pattern) {
		return stringRedisTemplate.execute((RedisConnection connection) -> {

			Set<String> matchingKeys = new HashSet<>();

			ScanOptions options = ScanOptions.scanOptions()
				.match(pattern)
				.count(1000)        // 한 번에 스캔할 수 - 성능 고려
				.build();

			try (Cursor<byte[]> cursor = connection.scan(options)) {
				while (cursor.hasNext()) {
					matchingKeys.add(new String(cursor.next()));        // 키는 byte[]로 반환됨
				}
			}

			return matchingKeys;
		});
	}

	/**
	 * participantKeys 키 목록을 받아, 각 키의 멤버수를 파이프라인으로 일괄 조회합니다. (SCARD)
	 * - 모든 채팅방의 cardinality를 파이프라인으로 묶어서 처리 (executePipelined)
	 *
	 * @param keys (예: "chatroom:participants:1", "chatroom:participants:2", ...)
	 * @return Map<String, Long> (예: {"chatroom:participants:1": 5L, "chatroom:participants:2": 10L})
	 */
	public Map<String, Long> getParticipantCounts(Set<String> keys) {

		List<Object> results = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
			StringRedisConnection stringConnection = (StringRedisConnection)connection; // 문자열 기반 명령
			for (String key : keys) {
				stringConnection.sCard(key);    // 파이프라인에 쌓아두기
			}
			return null;                        // null 반환 시 executePipelined가 쌓인 명령 실행
		});

		Map<String, Long> countMap = new HashMap<>();
		int i = 0;
		for (String key : keys) {
			countMap.put(key, (Long) results.get(i++));
		}

		return countMap;
	}

	/**
	 * lastMsgAtKeys 키 목록을 받아, 각 키의 값을 파이프라인으로 일괄 조회합니다. (GET)
	 *
	 * @param keys (예: "chatroom:last_msg_at:1", "chatroom:last_msg_at:2", ...)
	 * @return Map<String, String> (예: {"chatroom:last_msg_at:1": "1678886400000", ...})
	 */
	public Map<String, String> multiGetLastMessageTimestamps(Set<String> keys) {
		List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);		// MGET: 파이프라인과 유사한 동작

		Map<String, String> timestampMap = new HashMap<>();
		int i = 0;
		for (String key : keys) {
			String value = values.get(i++);
			if (value != null) { // 키가 중간에 삭제되었을 경우를 대비
				timestampMap.put(key, value);
			}
		}

		return timestampMap;
	}

	/**
	 * likedByKeys 키(Set) 목록을 받아, 각 키의 모든 멤버를 파이프라인으로 일괄 조회합니다. (SMEMBERS)
	 *
	 * @param keys (예: "message:liked_by:msg1", "message:liked_by:msg2", ...)
	 * @return Map<String, Set<String>> (예: {"message:liked_by:msg1": {"user1", "user2"}, ...})
	 */
	public Map<String, Set<String>> getSetMembers(Set<String> keys) {
		List<Object> results = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
			StringRedisConnection stringConnection = (StringRedisConnection) connection;
			for (String key : keys) {
				stringConnection.sMembers(key);
			}
			return null;
		});

		Map<String, Set<String>> membersMap = new HashMap<>();
		int i = 0;
		for (String key : keys) {
			membersMap.put(key, (Set<String>) results.get(i++));
		}
		return membersMap;
	}

	/**
	 * 주어진 키 목록을 파이프라인으로 일괄 삭제합니다. (DEL)
	 *
	 * @param keys 삭제할 키 Set
	 */
	public void deleteKeys(Set<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		stringRedisTemplate.delete(keys); // 내부적으로 여러 키 파이프라인 또는 단일 DEL 처리
	}

}
