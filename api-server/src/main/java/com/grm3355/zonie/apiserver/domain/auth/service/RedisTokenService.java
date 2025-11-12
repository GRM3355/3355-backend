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
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;
import com.grm3355.zonie.commonlib.global.util.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisTokenService {

	private static final String REFRESH_TOKEN_PREFIX = "refreshToken:";
	private static final String USER_TOKENS_PREFIX = "user-tokens:";

	private static final Duration TOKEN_TTL = Duration.ofMinutes(60); // TTL 5분 --> 임시로 60분으로 변경
	private final StringRedisTemplate redisTemplate;
	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;

	@Value("${jwt.refresh-token-expiration-time}")
	private long refreshTokenExpirationTime;

	public RedisTokenService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider,
		ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
	}
	private String getRefreshTokenKey(String token) {
		return REFRESH_TOKEN_PREFIX + token;
	}

	private String getUserTokensKey(String userId) {
		return USER_TOKENS_PREFIX + userId;
	}

	// accessToken 발행 (Redis 미사용)
	//public String generateAccessToken(String userId) {
	//	return jwtProvider.generateAccessToken(userId);
	//}

	/**
	 * 새로운 Refresh Token을 생성하고 Redis에 저장합니다.
	 */
	public String createRefreshToken(String userId) {

		String token = jwtTokenProvider.createRefreshToken(userId);
		RefreshTokenInfo info = new RefreshTokenInfo(userId, false);
		String redisKey = getRefreshTokenKey(token);
		String userTokensKey = getUserTokensKey(userId);

		try {
			String infoJson = objectMapper.writeValueAsString(info);
			redisTemplate.opsForValue().set(redisKey, infoJson, Duration.ofMillis(refreshTokenExpirationTime));
			redisTemplate.opsForSet().add(userTokensKey, token);
			redisTemplate.expire(userTokensKey, Duration.ofMillis(refreshTokenExpirationTime));
			log.info("사용자 {}를 위해 Redis에 리프레시 토큰을 생성하고 저장했습니다: {}", userId, redisKey);
		} catch (JsonProcessingException e) {
			log.error("Redis를 위한 RefreshTokenInfo 직렬화에 실패했습니다: {}", e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "리프레시 토큰 저장 중 오류가 발생했습니다.");
		}
		return token;
	}

	public record RefreshTokenInfo(String userId, boolean used) {
	}

	/**
	 * locationToken 발행 및 Redis에 lat, lon 저장
	 * @param info
	 * @param userId
	 */
	public void generateLocationToken(LocationDto info, String userId) {
		String redisKey = buildKey(userId);
		try {
			String infoJson = objectMapper.writeValueAsString(info);
			// Redis에 10분 TTL로 저장
			redisTemplate.opsForValue().set(redisKey, infoJson, TOKEN_TTL);
			UserTokenDto userTokenDto = getLocationInfo(userId);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Redis 저장 중 오류 발생", e);
		}
		// JWT에는 userId만 포함 //나중에 사용예정
		//return jwtProvider.generateLocationToken(info.getUserId(), null);
	}

	/**
	 * Redis에서 locationToken 정보 조회
	 * @param token
	 * @return
	 */
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

	/**
	 * location 토큰 값 체크
	 * @param userId
	 * @return
	 */
	//토큰 값 체크
	public boolean validateLocationToken(String userId) {
		String token = redisTemplate.opsForValue().get(buildKey(userId));
		return token != null && !token.isBlank();
	}

	/**
	 * locationToken 현재위도, 경도  정보 업데이트 (TTL 갱신 포함)
	 * @param locationDto
	 * @param userDetails
	 * @return
	 */
	public boolean updateLocationInfo(LocationDto locationDto, UserDetailsImpl userDetails) {
		String savedUserId = userDetails.getUsername();
		if (savedUserId == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "유효하지 않은 토큰입니다.");
		}
		//generateLocationToken(locationDto, savedUserId);
		String key = buildKey(savedUserId);
		String oldValue = redisTemplate.opsForValue().get(key);

		System.out.println("lat====>"+locationDto.getLat()+"___lon="+locationDto.getLon());
		if(oldValue == null){	//위치정보가 없으면 등록
			generateLocationToken(locationDto, savedUserId);
		}else{ //있으면 갱신
			// 기존 정보 유지 + 좌표만 갱신
			String userId = extractValue(oldValue, "userId");
			String newValue = buildLocationJson(userId, locationDto.getLat(), locationDto.getLon());
			redisTemplate.opsForValue().set(key, newValue, TOKEN_TTL);
		}
		return true;
	}

	/**
	 * 토큰 정보 읽기
	 * @param token
	 * @return
	 */
	private Optional<RefreshTokenInfo> readTokenInfo(String token) {
		String redisKey = getRefreshTokenKey(token);
		String infoJson = redisTemplate.opsForValue().get(redisKey);
		if (infoJson == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(infoJson, RefreshTokenInfo.class));
		} catch (JsonProcessingException e) {
			log.error("Redis에서 토큰 {}에 대한 RefreshTokenInfo 역직렬화에 실패했습니다: {}", token, e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "리프레시 토큰 정보 조회 중 오류가 발생했습니다.");
		}
	}

	public Optional<RefreshTokenInfo> findByToken(String token) {
		Optional<RefreshTokenInfo> tokenInfoOpt = readTokenInfo(token);
		if (tokenInfoOpt.isEmpty()) {
			return Optional.empty();
		}

		RefreshTokenInfo info = tokenInfoOpt.get();
		if (info.used()) {
			log.warn("사용자 {}의 리프레시 토큰 재사용이 감지되었습니다. 모든 세션을 종료합니다.", info.userId());
			throw new BusinessException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 이미 사용되었습니다. 모든 세션이 종료됩니다.");
		}

		try {
			RefreshTokenInfo usedInfo = new RefreshTokenInfo(info.userId(), true);
			String redisKey = getRefreshTokenKey(token);
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(usedInfo), Duration.ofSeconds(5));
		} catch (JsonProcessingException e) {
			log.error("Redis에 사용된 RefreshTokenInfo를 직렬화하는 데 실패했습니다: {}", e.getMessage());
		}
		return Optional.of(info);
	}

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

	private String buildKey(String token) {
		return "locationToken:" + token;
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
}
