package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * * Pub/Sub을 위한 redisTemplate (타입 정보 x)
 */
@Configuration("chatServerRedisConfig")
public class RedisConfig {

	// (1) MessageService에서 사용할 RedisTemplate<String, Object> Bean 등록
	//     (Message 객체를 JSON으로 직렬화/역직렬화하기 위함)
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key는 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());

		// Value는 JSON으로 직렬화 (GenericJackson2JsonRedisSerializer 사용)
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

		// Hash Key/Value도 동일하게 설정 (필요시)
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

		return template;
	}

	// (2) EchoHandler에서 사용하는 RedisTemplate<String, String> Bean 등록
	//     (StringRedisTemplate Bean으로 이미 자동 등록되지만, 명시적으로 만듦)
	@Bean
	public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key/Value 모두 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());

		return template;
	}
}
