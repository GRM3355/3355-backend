package com.grm3355.zonie.apiserver.domain.auth.controller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.grm3355.zonie.apiserver.domain.auth.service.LocationTokenService;

public class LocationTokenServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	@InjectMocks
	private LocationTokenService locationTokenService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
	}

	@Test
	@DisplayName("토큰 생성 시 Redis에 저장되고 UUID 형식 토큰이 반환된다")
	void createLocationToken_Success() {
		// given
		String userId = "user123";
		String clientIp = "192.168.0.10";
		String device = "Android";
		double lat = 37.123456;
		double lon = 127.654321;

		// when
		String token = locationTokenService.createLocationToken(userId, clientIp, device, lat, lon);

		// then
		assertThat(token).isNotNull();
		assertThat(token).hasSizeGreaterThan(10); // UUID 형식
		verify(valueOps, times(1)).set(startsWith("location:token:"), anyString(), eq(Duration.ofMinutes(10)));
	}

	@Test
	@DisplayName("위치 업데이트 성공 시 true 반환 및 TTL 갱신")
	void updateLocation_Success() {
		// given
		String token = "test-token";
		String key = "location:token:" + token;
		String oldJson = "{\"userId\":\"user123\",\"clientIp\":\"127.0.0.1\",\"device\":\"iPhone\",\"lat\":37.0,\"lon\":128.0}";
		when(valueOps.get(key)).thenReturn(oldJson);

		// when
		boolean result = locationTokenService.updateLocation(token, 37.5, 128.5);

		// then
		assertThat(result).isTrue();
		verify(valueOps, times(1)).get(key);
		verify(valueOps, times(1)).set(eq(key), contains("\"lat\":37.500000"), eq(Duration.ofMinutes(10)));
	}

	@Test
	@DisplayName("유효하지 않은 토큰 업데이트 시 false 반환")
	void updateLocation_Fail_InvalidToken() {
		// given
		when(valueOps.get(anyString())).thenReturn(null);

		// when
		boolean result = locationTokenService.updateLocation("invalid-token", 37.1, 128.1);

		// then
		assertThat(result).isFalse();
		verify(valueOps, times(1)).get("location:token:invalid-token");
	}

	@Test
	@DisplayName("토큰으로 저장된 정보 조회")
	void getLocationInfo_Success() {
		// given
		String token = "test-token";
		String expectedJson = "{\"userId\":\"abc\",\"clientIp\":\"1.1.1.1\",\"device\":\"PC\"}";
		when(valueOps.get("location:token:" + token)).thenReturn(expectedJson);

		// when
		String result = locationTokenService.getLocationInfo(token);

		// then
		assertThat(result).isEqualTo(expectedJson);
	}

	@Test
	@DisplayName("토큰 유효성 검증 - 존재할 때 true 반환")
	void validateLocationToken_True() {
		String token = "valid-token";
		when(redisTemplate.hasKey("location:token:" + token)).thenReturn(true);

		boolean result = locationTokenService.validateLocationToken(token);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("토큰 유효성 검증 - 존재하지 않을 때 false 반환")
	void validateLocationToken_False() {
		String token = "invalid-token";
		when(redisTemplate.hasKey("location:token:" + token)).thenReturn(false);

		boolean result = locationTokenService.validateLocationToken(token);

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("토큰 삭제 호출 시 Redis delete 실행됨")
	void deleteLocationToken_Success() {
		String token = "delete-me";

		locationTokenService.deleteLocationToken(token);

		verify(redisTemplate, times(1)).delete("location:token:" + token);
	}
}
