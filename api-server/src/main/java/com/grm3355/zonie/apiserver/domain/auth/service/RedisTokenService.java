package com.grm3355.zonie.apiserver.domain.auth.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
public class RedisTokenService {

	private static final String REFRESH_TOKEN_PREFIX = "refreshToken:";
	private static final String USER_TOKENS_PREFIX = "user-tokens:";
	private final Duration tokenTtl;
	private final StringRedisTemplate redisTemplate;
	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;

	@Setter    // TestManagement: 비만료 토큰 발급 - TTL을 임시로 변경하기 위해 Setter 설정
	@Value("${jwt.refresh-token-expiration-time}")
	private long refreshTokenExpirationTime;

	public RedisTokenService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider,
		ObjectMapper objectMapper, @Value("${location.token.ttl-minutes}") long ttlMinutes) {
		this.redisTemplate = redisTemplate;
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
		this.tokenTtl = Duration.ofMinutes(ttlMinutes);
	}

	private String getRefreshTokenKey(String token) {
		return REFRESH_TOKEN_PREFIX + token;
	}

	private String getUserTokensKey(String userId) {
		return USER_TOKENS_PREFIX + userId;
	}

	/**
	 * 새로운 Refresh Token을 생성하고 Redis에 저장합니다.
	 */
	public String createRefreshToken(String userId) {

		String token = jwtTokenProvider.createRefreshToken(userId);
		RedisTokenService.RefreshTokenInfo info = new RedisTokenService.RefreshTokenInfo(userId, false);
		String redisKey = getRefreshTokenKey(token);
		String userTokensKey = getUserTokensKey(userId);

		try {
			String infoJson = objectMapper.writeValueAsString(info);
			redisTemplate.opsForValue().set(redisKey, infoJson, Duration.ofMillis(refreshTokenExpirationTime));
			redisTemplate.opsForSet().add(userTokensKey, token);
			redisTemplate.expire(userTokensKey, Duration.ofMillis(refreshTokenExpirationTime));
			log.info("사용자 {}를 위해 Redis에 리프레시 토큰을 생성하고 저장했습니다 : {}", userId, redisKey.substring(0, 30));
		} catch (JsonProcessingException e) {
			log.error("Redis를 위한 RefreshTokenInfo 직렬화에 실패했습니다: {}", e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "리프레시 토큰 저장 중 오류가 발생했습니다.");
		}
		return token;
	}

	// locationToken 발행 및 Redis에 clientIp, device, lat, lon 저장
	public void generateLocationToken(UserTokenDto info, String contextId) {
		String redisKey = buildKey(info.getUserId(), contextId);

		log.info("====================>generateLocationToken=" + redisKey);
		try {
			String infoJson = buildLocationJson(info.getUserId(), "", "", info.getLat(), info.getLon());
			redisTemplate.opsForValue().set(redisKey, infoJson, this.tokenTtl); // 15분 TTL

			// getLocationInfo 호출 시에도 contextId가 필요
			UserTokenDto userTokenDto = getLocationInfo(info.getUserId(), contextId);

			log.info("====================>generateLocationToken true" + userTokenDto.getLat() + "___"
					 + userTokenDto.getLon());
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

	//리프레시 토큰 값 체크
	public boolean validateRefreshToken(String token) {
		String redisKey = getRefreshTokenKey(token);
		String savedValue = redisTemplate.opsForValue().get(redisKey);
		return savedValue != null && !savedValue.isBlank();
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
		// String clientIp = extractValue(oldValue, "clientIp");
		// String device = extractValue(oldValue, "device");

		String newValue = buildLocationJson(userId, /*clientIp, device,*/ locationDto.getLat(), locationDto.getLon());
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

	/**
	 * 토큰 정보 읽기
	 */
	private Optional<RedisTokenService.RefreshTokenInfo> readTokenInfo(String token) {
		String redisKey = getRefreshTokenKey(token);
		String infoJson = redisTemplate.opsForValue().get(redisKey);
		if (infoJson == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(infoJson, RedisTokenService.RefreshTokenInfo.class));
		} catch (JsonProcessingException e) {
			log.error("Redis에서 토큰 {}에 대한 RefreshTokenInfo 역직렬화에 실패했습니다: {}", token, e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "리프레시 토큰 정보 조회 중 오류가 발생했습니다.");
		}
	}

	/**
	 * 토큰 정보 찾기
	 */
	public Optional<RedisTokenService.RefreshTokenInfo> findByToken(String token) {
		Optional<RedisTokenService.RefreshTokenInfo> tokenInfoOpt = readTokenInfo(token);
		if (tokenInfoOpt.isEmpty()) {
			return Optional.empty();
		}

		RedisTokenService.RefreshTokenInfo info = tokenInfoOpt.get();
		if (info.used()) {
			log.warn("사용자 {}의 리프레시 토큰 재사용이 감지되었습니다. 모든 세션을 종료합니다.", info.userId());
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 이미 사용되었습니다. 모든 세션이 종료됩니다.");
		}

		try {
			RedisTokenService.RefreshTokenInfo usedInfo = new RedisTokenService.RefreshTokenInfo(info.userId(), true);
			String redisKey = getRefreshTokenKey(token);
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(usedInfo), Duration.ofSeconds(5));
		} catch (JsonProcessingException e) {
			log.error("Redis에 사용된 RefreshTokenInfo를 직렬화하는 데 실패했습니다: {}", e.getMessage());
		}
		return Optional.of(info);
	}

	/**
	 * 토큰 삭제
	 */
	@Transactional
	public void deleteByToken(String token) {
		String redisKey = getRefreshTokenKey(token);
		readTokenInfo(token).ifPresent(info -> {
			String userTokensKey = getUserTokensKey(info.userId());
			redisTemplate.opsForSet().remove(userTokensKey, token);
		});
		Boolean deleted = redisTemplate.delete(redisKey);
		if (deleted) {
			log.info("Redis에서 리프레시 토큰을 삭제했습니다: {}", redisKey);
		} else {
			log.warn("Redis에 존재하지 않는 리프레시 토큰 삭제 시도: {}", redisKey);
		}
	}

	/**
	 * 특정 사용자의 모든 Refresh Token을 무효화합니다.
	 */
	@Transactional
	public void deleteAllTokensForUser(String userId) {
		String userTokensKey = getUserTokensKey(userId);
		Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
		if (tokens == null || tokens.isEmpty()) {
			return;
		}
		List<String> tokenKeys = tokens.stream()
			.map(this::getRefreshTokenKey)
			.collect(Collectors.toList());
		redisTemplate.delete(tokenKeys);
		redisTemplate.delete(userTokensKey);
		log.info("사용자 {}의 모든 리프레시 토큰이 무효화되었습니다.", userId);
	}

	private String buildKey(String userId, String contextId) {
		if (contextId == null || contextId.isBlank()) {
			log.warn("buildKey 호출 시 contextId가 null이거나 비어있습니다. UserId: {}", userId);
			// 혹은 예외: // throw new IllegalArgumentException("ContextId는 필수입니다.");
			// 임시로 UserId만 사용한 키를 반환하나, 이는 버그의 원인이 될 수 있음
			return "locationToken:" + userId + ":(unknown_context)";
		}
		return "locationToken:" + userId + ":" + contextId;
	}

	private String buildLocationJson(String userId, String clientIp, String device, double lat, double lon) {
		return String.format(
			"{\"userId\":\"%s\",\"clientIp\":\"%s\",\"device\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"timestamp\":%d}",
			userId, clientIp, device, lat, lon, System.currentTimeMillis()
		);
	}

	private String buildLocationJson(String userId, double lat, double lon) {
		return String.format(
			"{\"userId\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"timestamp\":%d}",
			userId, lat, lon, System.currentTimeMillis()
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

	public record RefreshTokenInfo(String userId, boolean used) {
	}
}
