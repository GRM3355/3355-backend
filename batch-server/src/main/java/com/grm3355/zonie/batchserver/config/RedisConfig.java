package com.grm3355.zonie.batchserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

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
}