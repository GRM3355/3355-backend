package com.grm3355.zonie.apiserver.domain.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.Getter;

@Getter
@Service
public class RedisTokenService {

	private final Duration tokenTtl;
	private final StringRedisTemplate redisTemplate;
	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;

	public RedisTokenService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper, @Value("${location.token.ttl-minutes}") long ttlMinutes) {
		this.redisTemplate = redisTemplate;
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
		this.tokenTtl = Duration.ofMinutes(ttlMinutes);
	}

	// accessToken 발행 (Redis 미사용)
	//public String generateAccessToken(String userId) {
	//	return jwtProvider.generateAccessToken(userId);
	//}

	// locationToken 발행 및 Redis에 clientIp, device, lat, lon 저장
	public void generateLocationToken(UserTokenDto info, String contextId) {
		String redisKey = buildKey(info.getUserId(), contextId);

		System.out.println("====================>generateLocationToken="+redisKey);
		try {
			String infoJson = objectMapper.writeValueAsString(info);
			redisTemplate.opsForValue().set(redisKey, infoJson, this.tokenTtl); // 15분 TTL

			// getLocationInfo 호출 시에도 contextId가 필요
			UserTokenDto userTokenDto = getLocationInfo(info.getUserId(), contextId);

			System.out.println("====================>generateLocationToken true" + userTokenDto.getLat() + "___" + userTokenDto.getLon());
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Redis 저장 중 오류 발생", e);
		}

		// JWT에는 userId만 포함 //나중에 사용예정
		//return jwtProvider.generateLocationToken(info.getUserId(), null);
	}

	// Redis에서 locationToken 정보 조회
	public UserTokenDto getLocationInfo(String userId, String contextId) {
		String redisKey = buildKey(userId, contextId); // [수정]
		String saved = redisTemplate.opsForValue().get(redisKey);
		if (saved == null)
			return null;

		try {
			return objectMapper.readValue(saved, UserTokenDto.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	//토큰 값 체크
	public boolean validateLocationToken(String userId, String contextId) {
		String token = redisTemplate.opsForValue().get(buildKey(userId, contextId)); // [수정]
		return token != null && !token.isBlank();
	}

	/**
	 * 위치 + 디바이스 정보 업데이트 (TTL 갱신 포함)
	 */
	public boolean updateLocationInfo(LocationDto locationDto, String userId, String contextId) {
		String key = buildKey(userId, contextId); // [수정]
		String oldValue = redisTemplate.opsForValue().get(key);

		if (oldValue == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 토큰입니다.");
		}

		// 기존 정보 유지 + 좌표만 갱신
		// userId는 파라미터로 받은 값 사용
		String clientIp = extractValue(oldValue, "clientIp");
		String device = extractValue(oldValue, "device");

		String newValue = buildLocationJson(userId, clientIp, device, locationDto.getLat(), locationDto.getLon());
		redisTemplate.opsForValue().set(key, newValue, this.tokenTtl);

		return true;
	}

	private String buildKey(String userId, String contextId) {
		return "locationToken:" + userId + ":" + contextId;
	}

	private String buildLocationJson(String userId, String clientIp, String device, double lat, double lon) {
		return String.format(
			"{\"userId\":\"%s\",\"clientIp\":\"%s\",\"device\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"timestamp\":%d}",
			userId, clientIp, device, lat, lon, System.currentTimeMillis()
		);
	}

	private String extractValue(String json, String field) {
		String search = "\"" + field + "\":\"";
		int start = json.indexOf(search);
		if (start == -1)
			return null;
		start += search.length();
		int end = json.indexOf("\"", start);
		return json.substring(start, end);
	}
}
