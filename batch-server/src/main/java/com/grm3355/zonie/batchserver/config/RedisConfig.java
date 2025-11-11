package com.grm3355.zonie.batchserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	/**
	 * Batch Job (RedisToDbSyncJob)에서 사용할 문자열 전용 RedisTemplate
	 * - 'chatroom:participants:*'
	 * - 'chatroom:last_msg_at:*'
	 * - last_msg_content는 sync할 필요가 없음
	 * 위 두 패턴의 키와 값을 조회할 때 사용됩니다.
	 */
	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}

	/**
	 * ChatRoomService에서 사용하는 범용 RedisTemplate
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		// Key는 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());
		// Value는 JSON으로 직렬화
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
		return template;
	}
}