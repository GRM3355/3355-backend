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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	// locationToken 발행 및 Redis에 clientIp, device, lat, lon 저장
	public void generateLocationToken(UserTokenDto info, String contextId) {
		String redisKey = buildKey(info.getUserId(), contextId);

		log.info("====================>generateLocationToken="+redisKey);
		try {
			String infoJson = buildLocationJson(info.getUserId(), "", "", info.getLat(), info.getLon());
			redisTemplate.opsForValue().set(redisKey, infoJson, this.tokenTtl); // 15분 TTL

			// getLocationInfo 호출 시에도 contextId가 필요
			UserTokenDto userTokenDto = getLocationInfo(info.getUserId(), contextId);

			log.info("====================>generateLocationToken true" + userTokenDto.getLat() + "___" + userTokenDto.getLon());
		} catch (Exception e) {
			throw new RuntimeException("Redis 저장 중 오류 발생", e);
		}

		// JWT에는 userId만 포함 //나중에 사용예정
		//return jwtProvider.generateLocationToken(info.getUserId(), null);
	}

	// Redis에서 locationToken 정보 조회
	public UserTokenDto getLocationInfo(String userId, String contextId) {
		String redisKey = buildKey(userId, contextId);
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
		String token = redisTemplate.opsForValue().get(buildKey(userId, contextId));
		return token != null && !token.isBlank();
	}

	/**
	 * 위치 + 디바이스 정보 업데이트 (TTL 갱신 포함)
	 */
	public boolean updateLocationInfo(LocationDto locationDto, String userId, String contextId) {
		String key = buildKey(userId, contextId);
		String oldValue = redisTemplate.opsForValue().get(key);

		if (oldValue == null) {
			// throw new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 토큰입니다.");
			// 기존 토큰을 갱신하는 용도로만 사용, 토큰이 없으면 false 반환
			return false;
		}

		// 기존 정보 유지 + 좌표만 갱신
		// userId는 파라미터로 받은 값 사용
		String clientIp = extractValue(oldValue, "clientIp");
		String device = extractValue(oldValue, "device");

		String newValue = buildLocationJson(userId, clientIp, device, locationDto.getLat(), locationDto.getLon());
		redisTemplate.opsForValue().set(key, newValue, this.tokenTtl);

		return true;
	}

	/**
	 * 위치 인증 토큰을 설정하거나 갱신합니다. (토큰이 없으면 생성, 있으면 위치 갱신 및 TTL 갱신)
	 * 이 메서드는 위치의 유효성 검사(축제 반경 체크)를 담당하지 않고, 순수하게 토큰 데이터 처리만 합니다.
	 * @return 생성/갱신된 위치 정보 (UserTokenDto)
	 */
	public UserTokenDto setToken(String userId, String contextId, LocationDto locationDto) {
		// 1. 토큰이 이미 존재하는지 확인하고, 존재하면 갱신
		boolean updated = updateLocationInfo(locationDto, userId, contextId);

		if (!updated) {
			// 2. 토큰이 존재하지 않으면 새로 생성 (이 시점에서는 반경 체크 없이 일단 생성)
			UserTokenDto info = UserTokenDto.builder()
				.userId(userId)
				.lat(locationDto.getLat())
				.lon(locationDto.getLon())
				.build();
			generateLocationToken(info, contextId);
		}

		// 3. 최종적으로 저장된 위치 정보를 반환
		return getLocationInfo(userId, contextId);
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
