package com.grm3355.zonie.apiserver.global.config;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.*;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

/**
 * * @Cacheable을 위한 cacheManager (타입 정보 o)
 * * Pub/Sub을 위한 redisTemplate (타입 정보 x)
 */
@EnableCaching
@Configuration
public class RedisConfig {

	/**
	 * 문자열 전용 RedisTemplate
	 * - Spring 기본 제공 StringRedisTemplate과 유사
	 * - 키/값 모두 String 직렬화
	 */
	@Bean
	@Primary
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}

	/**
	 * 범용 RedisTemplate (Pub/Sub & 일반 작업용)
	 * - 키: String
	 * - 값: JSON 직렬화 (타입 정보x)
	 */
	@Bean
	@Primary
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(connectionFactory);

		// 직렬화 설정
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		// 기본 Serializer
		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

		redisTemplate.setKeySerializer(stringSerializer);
		redisTemplate.setHashKeySerializer(stringSerializer);
		redisTemplate.setValueSerializer(jsonSerializer);
		redisTemplate.setHashValueSerializer(jsonSerializer);

		redisTemplate.afterPropertiesSet(); // 초기화 호출
		return redisTemplate;
	}

	/**
	 * Spring Cache 추상화를 위한 Redis 기반 캐시 매니저를 설정한다.
	 * 캐시 항목의 기본 TTL과 키/값 직렬화 방식을 정의한다.
	 * 값 직렬화 시에는 Java Record 타입을 지원하는 커스텀 ObjectMapper를 사용한다.
	 */
	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

		// 캐시 매니저만 타입 정보 포함된 ObjectMapper 사용
		ObjectMapper cacheObjectMapper = createRedisObjectMapper();

		RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(Duration.ofMinutes(1))
			.serializeKeysWith(fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(fromSerializer(
				new GenericJackson2JsonRedisSerializer(cacheObjectMapper)
			));

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(cacheConfiguration)
			.build();
	}

	/**
	 * Spring Cache(@Cacheable) 전용 ObjectMapper 생성기
	 * - Record 타입을 지원하고, 역직렬화를 위해 JSON에 타입 정보를 포함시킴.
	 *
	 * Redis에 Java Record 타입을 저장할 때, 역직렬화 시 타입을 명시하지 않으면 LinkedHashMap으로 변환되는 문제가 발생한다.
	 * 이 문제를 해결하기 위해 Jackson ObjectMapper에 Record 타입을 위한 커스텀 TypeResolver를 설정하여,
	 * JSON에 "@class" 같은 타입 정보를 명시적으로 포함시킨다.
	 */
	private ObjectMapper createRedisObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		// record 타입을 지원하는 커스텀 TypeResolver를 사용
		RecordSupportingTypeResolver typeResolver = new RecordSupportingTypeResolver(
			ObjectMapper.DefaultTyping.NON_FINAL,
			mapper.getPolymorphicTypeValidator()
		);
		StdTypeResolverBuilder initializedResolver = typeResolver.init(JsonTypeInfo.Id.CLASS, null);
		initializedResolver = initializedResolver.inclusion(JsonTypeInfo.As.PROPERTY);
		mapper.setDefaultTyping(initializedResolver);

		return mapper;
	}
}
