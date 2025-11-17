package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

		// 1. LocalDateTime 처리를 위한 ObjectMapper 생성
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule()); // JavaTimeModule 등록

		// 2. 역직렬화를 위해 타입 정보 포함 (RedisSubscriber에서 Message 객체로 변환 시 필요)
		objectMapper.activateDefaultTyping(
			objectMapper.getPolymorphicTypeValidator(),
			ObjectMapper.DefaultTyping.NON_FINAL,
			JsonTypeInfo.As.PROPERTY
		);

		// 3. 설정된 ObjectMapper를 Serializer에 주입: GenericJackson2JsonRedisSerializer
		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		// Key는 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());
		// Value는 JSON Serializer로 직렬화
		template.setValueSerializer(jsonSerializer);

		// Hash Key/Value도 똑같이 설정
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(jsonSerializer);

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
