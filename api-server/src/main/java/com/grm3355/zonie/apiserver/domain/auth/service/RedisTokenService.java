package com.grm3355.zonie.apiserver.domain.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.common.jwt.JwtProvider;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

@Service
public class RedisTokenService {

	private static final Duration TOKEN_TTL = Duration.ofMinutes(60); // TTL 5분 --> 임시로 60분으로 변경
	private final StringRedisTemplate redisTemplate;
	private final JwtProvider jwtProvider;
	private final ObjectMapper objectMapper;

	public RedisTokenService(StringRedisTemplate redisTemplate, JwtProvider jwtProvider, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.jwtProvider = jwtProvider;
		this.objectMapper = objectMapper;
	}

	// accessToken 발행 (Redis 미사용)
	//public String generateAccessToken(String userId) {
	//	return jwtProvider.generateAccessToken(userId);
	//}

	// locationToken 발행 및 Redis에 clientIp, device, lat, lon 저장
	public void generateLocationToken(UserTokenDto info) {
		String redisKey = buildKey(info.getUserId());

		System.out.println("====================>generateLocationToken="+redisKey);
		try {
			String infoJson = objectMapper.writeValueAsString(info);
			// Redis에 10분 TTL로 저장
			redisTemplate.opsForValue().set(redisKey, infoJson, TOKEN_TTL);

			UserTokenDto userTokenDto = getLocationInfo(info.getUserId());

			System.out.println("====================>generateLocationToken true"+userTokenDto.getLat()+"___"+userTokenDto.getLon());
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Redis 저장 중 오류 발생", e);
		}

		// JWT에는 userId만 포함 //나중에 사용예정
		//return jwtProvider.generateLocationToken(info.getUserId(), null);
	}

	// Redis에서 locationToken 정보 조회
	public UserTokenDto getLocationInfo(String token) {

		String redisKey = buildKey(token);
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
	public boolean validateLocationToken(String userId) {
		String token = redisTemplate.opsForValue().get(buildKey(userId));
		return token != null && !token.isBlank();
	}

	/**
	 * 위치 + 디바이스 정보 업데이트 (TTL 갱신 포함)
	 */
	public boolean updateLocationInfo(LocationDto locationDto, String savedToken) {
		String key = buildKey(savedToken);
		String oldValue = redisTemplate.opsForValue().get(key);

		if (oldValue == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 토큰입니다.");
			//return false; // 유효하지 않은 토큰
		}

		// 기존 정보 유지 + 좌표만 갱신
		String userId = extractValue(oldValue, "userId");
		String clientIp = extractValue(oldValue, "clientIp");
		String device = extractValue(oldValue, "device");

		String newValue = buildLocationJson(userId, clientIp, device, locationDto.getLat(), locationDto.getLon());
		redisTemplate.opsForValue().set(key, newValue, TOKEN_TTL);

		return true;
	}

	private String buildKey(String token) {
		return "locationToken:" + token;
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
