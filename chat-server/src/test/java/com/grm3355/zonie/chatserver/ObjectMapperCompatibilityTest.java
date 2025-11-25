package com.grm3355.zonie.chatserver;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.enums.MessageType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@Disabled("CI test 환경 불일치로 임시 비활성화")
class ObjectMapperCompatibilityTest {

	// RedisConfig가 사용하는 RedisTemplate (발행자 측)
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	// RedisSubscriberConfig가 주입받는 ObjectMapper (구독자 측)
	@Autowired
	private ObjectMapper objectMapper; // Spring의 기본 ObjectMapper

	@Test
	@DisplayName("Redis 발행자/구독자 ObjectMapper 직렬화/역직렬화 호환성 테스트")
	void testPubSubObjectMapperCompatibility() throws Exception {

		// 1. 테스트할 메시지 객체 생성 (문제의 필드 포함)
		Message originalMessage = Message.builder()
			.id("msg-123")
			.chatRoomId("room-456")
			.userId("user-789")
			.nickname("테스터")
			.content("이 메시지는 LocalDateTime과 Set을 포함합니다.")
			.type(MessageType.TEXT)
			.createdAt(LocalDateTime.now()) //
			.likeCount(2)
			.likedByUserIds(Set.of("user-A", "user-B")) //
			.build();

		// 2. [발행 시뮬레이션]
		// RedisTemplate의 ValueSerializer를 가져와서 객체를 byte[]로 직렬화
		RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
		byte[] serializedBytes = valueSerializer.serialize(originalMessage);

		assertNotNull(serializedBytes, "직렬화에 실패했습니다.");

		// (디버깅용)
		String jsonString = new String(serializedBytes);
		log.info("직렬화된 JSON: " + jsonString);
		assertFalse(jsonString.contains("@class"), "타입 정보(@class)가 여전히 포함되어 있습니다!");

		// 3. [구독 시뮬레이션]
		// 실제 Subscriber처럼, 구독자의 ObjectMapper로 byte[]를 Message.class로 변환
		Message deserializedMessage = objectMapper.readValue(serializedBytes, Message.class);

		// 4. 검증
		assertNotNull(deserializedMessage, "역직렬화에 실패했습니다.");

		// 두 필드가 모두 올바르게 복원되었는지 확인
		assertEquals(originalMessage.getId(), deserializedMessage.getId());
		assertEquals(originalMessage.getCreatedAt(), deserializedMessage.getCreatedAt());
		assertEquals(originalMessage.getLikedByUserIds(), deserializedMessage.getLikedByUserIds());

		log.info("LocalDateTime 복원됨: " + deserializedMessage.getCreatedAt());
		log.info("Set<String> 복원됨: " + deserializedMessage.getLikedByUserIds());
	}
}
