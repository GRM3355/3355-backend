package com.grm3355.zonie.apiserver.domain.auth.controller.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.UserTokenDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.commonlib.domain.auth.JwtTokenProvider;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RedisTokenServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private RedisTokenService redisTokenService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
	}

	@Test
	@DisplayName("generateLocationToken() - Redis에 UserTokenDto JSON 저장 성공")
	void generateLocationToken_Success() throws Exception {
		// given
		UserTokenDto dto = UserTokenDto.builder()
			.userId("user123")
			.lat(37.123456)
			.lon(127.654321)
			.build();

		// when
		redisTokenService.generateLocationToken(dto);

		// then
		verify(valueOps, times(1))
			.set(eq("locationToken:user123"), contains("\"lat\":37.123456"), any(Duration.class));
	}

	@Test
	@DisplayName("getLocationInfo() - Redis에서 UserTokenDto를 정상적으로 역직렬화")
	void getLocationInfo_Success() throws JsonProcessingException {
		// given
		UserTokenDto dto = UserTokenDto.builder()
			.userId("user123")
			.lat(37.55)
			.lon(126.97)
			.build();

		String json = new ObjectMapper().writeValueAsString(dto);
		when(valueOps.get("locationToken:user123")).thenReturn(json);

		// when
		UserTokenDto result = redisTokenService.getLocationInfo("user123");

		// then
		assertThat(result).isNotNull();
		assertThat(result.getUserId()).isEqualTo("user123");
		assertThat(result.getLat()).isEqualTo(37.55);
		assertThat(result.getLon()).isEqualTo(126.97);
	}

	@Test
	@DisplayName("getLocationInfo() - Redis에 값이 없으면 null 반환")
	void getLocationInfo_Null() {
		when(valueOps.get("locationToken:user123")).thenReturn(null);

		UserTokenDto result = redisTokenService.getLocationInfo("user123");

		assertThat(result).isNull();
	}

	@Test
	@DisplayName("validateLocationToken() - Redis에 키가 존재하면 true 반환")
	void validateLocationToken_True() {
		when(valueOps.get("locationToken:user123")).thenReturn("{\"test\":\"data\"}");

		boolean result = redisTokenService.validateLocationToken("user123");

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("validateLocationToken() - Redis에 값이 없으면 false 반환")
	void validateLocationToken_False() {
		when(valueOps.get("locationToken:user123")).thenReturn(null);

		boolean result = redisTokenService.validateLocationToken("user123");

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("updateLocationInfo() - 기존 데이터 유지하며 좌표 갱신")
	void updateLocationInfo_Success() {
		// given
		String oldJson = """
            {"userId":"user123","clientIp":"1.1.1.1","device":"PC","lat":37.000000,"lon":127.000000,"timestamp":11111}
            """;
		when(valueOps.get("locationToken:user123")).thenReturn(oldJson);

		LocationDto newLocation = new LocationDto(37.55, 126.98);

		// when
		boolean result = redisTokenService.updateLocationInfo(newLocation, "user123");

		// then
		assertThat(result).isTrue();
		verify(valueOps, times(1)).set(
			eq("locationToken:user123"),
			contains("\"lat\":37.550000"),
			any(Duration.class)
		);
	}

	@Test
	@DisplayName("updateLocationInfo() - 존재하지 않는 토큰이면 BusinessException 발생")
	void updateLocationInfo_InvalidToken() {
		when(valueOps.get("locationToken:invalid")).thenReturn(null);

		LocationDto dto = new LocationDto(37.1, 128.1);

		assertThatThrownBy(() ->
			redisTokenService.updateLocationInfo(dto, "invalid")
		)
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
	}
}
