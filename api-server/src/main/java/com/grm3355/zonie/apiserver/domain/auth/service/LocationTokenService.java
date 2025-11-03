package com.grm3355.zonie.apiserver.domain.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class LocationTokenService {

	private final StringRedisTemplate redisTemplate;
	private static final Duration TOKEN_TTL = Duration.ofMinutes(10); // TTL 5분

	public LocationTokenService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * LocationToken 생성
	 */
	public String createLocationToken(String userId, String clientIp, String device, double lat, double lon) {
		String token = UUID.randomUUID().toString();
		String key = buildKey(token);

		String value = buildLocationJson(userId, clientIp, device, lat, lon);
		redisTemplate.opsForValue().set(key, value, TOKEN_TTL);

		return token;
	}

	/**
	 * 위치 + 디바이스 정보 업데이트 (TTL 갱신 포함)
	 */
	public boolean updateLocation(String token, double lat, double lon) {
		String key = buildKey(token);
		String oldValue = redisTemplate.opsForValue().get(key);

		if (oldValue == null) {
			return false; // 유효하지 않은 토큰
		}

		// 기존 정보 유지 + 좌표만 갱신
		String userId = extractValue(oldValue, "userId");
		String clientIp = extractValue(oldValue, "clientIp");
		String device = extractValue(oldValue, "device");

		String newValue = buildLocationJson(userId, clientIp, device, lat, lon);
		redisTemplate.opsForValue().set(key, newValue, TOKEN_TTL);

		return true;
	}

	/**
	 * 토큰 정보 조회
	 */
	public String getLocationInfo(String token) {
		return redisTemplate.opsForValue().get(buildKey(token));
	}

	/**
	 * 토큰 유효성 검사
	 */
	public boolean validateLocationToken(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(token)));
	}

	/**
	 * 토큰 삭제
	 */
	public void deleteLocationToken(String token) {
		redisTemplate.delete(buildKey(token));
	}

	private String buildKey(String token) {
		return "location:token:" + token;
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
		if (start == -1) return null;
		start += search.length();
		int end = json.indexOf("\"", start);
		return json.substring(start, end);
	}
}
